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
  def workerProps(i: Int): Props
  def initializeAndStart(workers: Seq[ActorRef], handler: ActorRef)
}

object Strategy {
  sealed trait Message

  case class Done(i: Int) extends Message

  case class QueryResult(originalSender: ActorRef) extends Message
  case class QueryResultResponse(result: Int, id: Int, originalSender: ActorRef) extends Message
  
  case class QueryValue(originalSender: ActorRef) extends Message
  case class QueryValueResponse(value: Int, id: Int, originalSender: ActorRef) extends Message
}
