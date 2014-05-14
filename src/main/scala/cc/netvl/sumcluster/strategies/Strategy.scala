package cc.netvl.sumcluster.strategies

import akka.actor.{ActorRef, Props}

/**
 * A strategy for cluster behavior.
 */
trait Strategy {
  /**
   * Name of the strategy.
   */
  def name: String

  /**
   * Props for a cluster worker identified by the given number.
   */
  def workerProps(i: Int): Props

  /**
   * Perform internal initialization and start processing.
   */
  def initializeAndStart(workers: Seq[ActorRef], handler: ActorRef)
}

object Strategy {
  sealed trait Message

  /**
   * Sent to `handler` passed to [[Strategy.initializeAndStart]] when the given node
   * has finished its operations.
   */
  case class Done(i: Int) extends Message

  /**
   * Sent to a node. Query the result accumulated in the node.
   */
  case class QueryResult(originalSender: ActorRef) extends Message

  /**
   * A response to [[QueryResult]] message.
   */
  case class QueryResultResponse(result: Int, id: Int, originalSender: ActorRef) extends Message

  /**
   * Sent to a node. Query the initial value in the node.
   */
  case class QueryValue(originalSender: ActorRef) extends Message

  /**
   * A response to [[QueryValue]] message.
   */
  case class QueryValueResponse(value: Int, id: Int, originalSender: ActorRef) extends Message
}
