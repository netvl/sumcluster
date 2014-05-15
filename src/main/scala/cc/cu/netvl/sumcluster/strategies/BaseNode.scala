package cc.cu.netvl.sumcluster.strategies

import akka.actor.{ActorLogging, Stash, ActorRef, Actor}

/**
 * Base class for cluster nodes. Does its best to emulate synchronous interface from the original problem.
 */
abstract class BaseNode extends Actor with Stash with ActorLogging {
  import BaseNode._

  /**
   * Identifier of the node.
   */
  def id: Int

  /**
   * Initial value of the node.
   */
  protected def value: Int

  /**
   * Checkes whether the given identifier of a worker is valid.
   */
  protected final def has(id: Int)(implicit workers: Seq[ActorRef]): Boolean =
    0 <= id && id < workers.size

  /**
   * Asynchronously send a number to the given worker.
   */
  protected final def sendTo(i: Int, value: Int)(implicit workers: Seq[ActorRef]): Boolean = {
    if (has(i)) {
      workers(i) ! Value(value)
      true
    } else false
  }

  /**
   * "Synchronously" receive a number from the given worker.
   *
   * Because Akka is principially asynchronous, this operation is also async and emulated
   * via a callback and messages stashing.
   *
   * If a message is received from unexpected node, it is stashed. Otherwise, all messages are unstashed,
   * and the given callback is called, which can setup further actor behavior.
   */
  protected final def recvFrom(i: Int)(action: Int => Unit)(implicit workers: Seq[ActorRef]) {
    context become {
      case Value(value) if sender() == workers(i) =>
        unstashAll()
        action(value)

      case _ =>
        stash()
    }
  }

  /**
   * Switches to final state, notifying the handler and beginning to answer to queries.
   */
  protected final def becomeDone(result: Int, handler: ActorRef) {
    log.info("Worker {} is done, result: {}", id, result)

    handler ! Strategy.Done(id)
    context become {
      case Strategy.QueryResult(originalSender) =>
        sender() ! Strategy.QueryResultResponse(result, id, originalSender)

      case Strategy.QueryValue(originalSender) =>
        sender() ! Strategy.QueryValueResponse(value, id, originalSender)
    }
  }
}

object BaseNode {
  case class Value(value: Int)
}
