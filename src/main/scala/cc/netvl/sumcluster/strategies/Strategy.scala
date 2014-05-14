package cc.netvl.sumcluster.strategies

import akka.actor.{ActorRef, Props}

/**
 * Date: 14.05.14
 * Time: 21:39
 *
 * @author Vladimir Matveev
 */
trait Strategy {
  def name: String

  protected final var _sum: Int = 0
  def sum = _sum

  def workerProps(i: Int): Props
  def start(workers: Seq[ActorRef], handler: ActorRef)
}

object Strategy {
  sealed trait Message

  case class Done(i: Int) extends Message

  case class Query(originalSender: ActorRef) extends Message
  case class QueryResult(result: Int, id: Int, originalSender: ActorRef) extends Message
}
