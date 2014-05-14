package cc.netvl.sumcluster.strategies

import akka.actor.{Props, ActorRef}
import scala.util.Random

/**
 * Date: 14.05.14
 * Time: 21:40
 *
 * @author Vladimir Matveev
 */
class TreeStrategy extends Strategy {
  override val name = "binary tree"

  override def workerProps(i: Int) = {
    val n = Random.nextInt(100)
    _sum += n
    Props(new Node(i, n))
  }

  override def start(workers: Seq[ActorRef], handler: ActorRef) = {
    workers foreach (_ ! Initialize(workers, handler))
    workers foreach (_ ! Start)
  }

  private case class Initialize(workers: Seq[ActorRef], handler: ActorRef)
  private case object Start

  class Node(override val id: Int, value: Int) extends BaseNode {
    val parent: Int = (id-1)/2
    val leftChild: Int = 2*id + 1
    val rightChild: Int = 2*id + 2

    override def receive = {
      case Initialize(workers, handler) =>
        log.info("Worker {} is ready, private value: {}", id, value)
        context become ready(handler)(workers)
    }

    def aggregateChildren(callback: Int => Unit)(implicit workers: Seq[ActorRef]) {
      if (has(leftChild)) recvFrom(leftChild) { n1 =>
        if (has(rightChild)) recvFrom(rightChild) { n2 =>
          callback(n1 + n2)
        } else callback(n1)
      } else callback(0)
    }

    def ready(handler: ActorRef)(implicit workers: Seq[ActorRef]): Receive = {
      case Start if id == 0 =>
        log.info("Root worker started, waiting for children")
        aggregateChildren { result =>
          val finalResult = result + value
          log.info("Final result has been aggregated: {}, dispatching it", finalResult)

          sendTo(leftChild, finalResult)
          sendTo(rightChild, finalResult)

          becomeDone(finalResult, handler)
        }

      case Start =>
        log.info("Worker {} started, waiting for children", id)

        aggregateChildren { result =>
          val tempResult = result + value
          log.info("Worker {} received temp result from children: {}, with itself: {}, sending to parent",
            id, result, tempResult)

          sendTo(parent, tempResult)

          recvFrom(parent) { finalResult =>
            log.info("Worker {} received final result {}, propagating to children", id, finalResult)
            sendTo(leftChild, finalResult)
            sendTo(rightChild, finalResult)

            becomeDone(finalResult, handler)
          }
        }
    }
  }
}
