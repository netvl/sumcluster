package cc.netvl.sumcluster.strategies

import akka.actor.{Props, ActorRef}
import scala.util.Random

/**
 * Date: 15.05.14
 * Time: 0:02
 *
 * @author Vladimir Matveev
 */
class RingStrategy extends Strategy {
  override def name = "ring"

  override def workerProps(i: Int) = {
    val n = Random.nextInt(100)
    _sum += n
    Props(new Node(i, n))
  }

  override def start(workers: Seq[ActorRef], handler: ActorRef) = {
    if (workers.size > 0) {
      workers foreach (_ ! Initialize(workers, handler))
      workers foreach (_ ! Start)
    }
  }

  private case class Initialize(workers: Seq[ActorRef], handler: ActorRef)
  private case object Start

  class Node(override val id: Int, value: Int) extends BaseNode {

    def next(implicit w: Seq[_]) = if (id == w.size-1) 0 else id+1
    def prev(implicit w: Seq[_]) = if (id == 0) w.size-1 else id-1

    override def receive = {
      case Initialize(workers, handler) =>
        log.info("Worker {} is ready, private value: {}", id, value)
        context become ready(handler)(workers)
    }

    private def ready(handler: ActorRef)(implicit workers: Seq[ActorRef]): Receive = {
      case Start if id == 0 =>
        sendTo(next, value)
        recvFrom(prev) { result =>
          log.info("Worker {} received final result: {}, propagating it", id, result)
          sendTo(next, result)
          becomeDone(result, handler)
        }

      case Start =>
        recvFrom(prev) { tempResult =>
          log.info("Worker {} received intermediate result: {}, increasing and propagating", id, tempResult)
          sendTo(next, tempResult + value)
          recvFrom(prev) { result =>
            log.info("Worker {} received final result: {}, propagating it", id, result)
            sendTo(next, result)
            becomeDone(result, handler)
          }
        }
    }
  }
}
