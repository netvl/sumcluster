package cc.cu.netvl.sumcluster

import cc.cu.netvl.sumcluster.strategies.Strategy
import akka.actor.{Actor, Props, ActorRef}
import scala.collection.mutable

/**
 * Date: 15.05.2014
 * Time: 11:15
 */
class TestSuccessfulStrategy(n: Int) extends Strategy {
  override def name = "test strategy"

  val sum = (0 until n).sum

  private final val workerPropsSeq = (0 until n) map { i => Props(new TestNode(i, sum)) }

  val calledProps = mutable.Set.empty[Int]

  override def workerProps(i: Int) = {
    calledProps += i
    workerPropsSeq(i)
  }

  override def initializeAndStart(workers: Seq[ActorRef], handler: ActorRef) {
    workers foreach (_ ! SendDone(handler))
  }

  private case class SendDone(handler: ActorRef)

  val nodes = mutable.Set.empty[TestNode]
  val doneNodes = mutable.Set.empty[TestNode]

  class TestNode(val id: Int, val result: Int) extends Actor {
    nodes += this

    override def receive = {
      case SendDone(handler) =>
        doneNodes += this
        handler ! Strategy.Done(id)

      case Strategy.QueryResult(originalSender) =>
        sender ! Strategy.QueryResultResponse(result, id, originalSender)

      case Strategy.QueryValue(originalSender) =>
        sender ! Strategy.QueryValueResponse(id, id, originalSender)
    }
  }
}
