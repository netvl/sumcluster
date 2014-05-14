package cc.netvl.sumcluster.strategies

import akka.actor.{ActorLogging, Props, ActorRef}
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
      for (Seq(prev, curr, next) <- (workers.last +: workers :+ workers.head).grouped(3).toSeq) {
        curr ! Initialize(prev, next, handler)
      }
      workers.head ! Start
    }
  }

  private case class Initialize(prev: ActorRef, next: ActorRef, handler: ActorRef)
  private case object Start

  class Node(override val id: Int, value: Int) extends BaseNode with ActorLogging {

    override def receive = ???
  }
}
