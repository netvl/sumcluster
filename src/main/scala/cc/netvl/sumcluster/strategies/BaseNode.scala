package cc.netvl.sumcluster.strategies

import akka.actor.{ActorLogging, Stash, ActorRef, Actor}

/**
 * Date: 14.05.14
 * Time: 22:08
 *
 * @author Vladimir Matveev
 */
abstract class BaseNode extends Actor with Stash with ActorLogging {
  import BaseNode._

  def id: Int
  protected def value: Int

  protected final def has(i: Int)(implicit workers: Seq[ActorRef]): Boolean =
    0 <= i && i < workers.size

  protected final def sendTo(i: Int, value: Int)(implicit workers: Seq[ActorRef]): Boolean = {
    if (has(i)) {
      workers(i) ! Value(value)
      true
    } else false
  }

  protected final def recvFrom(i: Int)(action: Int => Unit)(implicit workers: Seq[ActorRef]) {
    context become {
      case Value(value) if sender() == workers(i) =>
        unstashAll()
        action(value)

      case _ =>
        stash()
    }
  }

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
