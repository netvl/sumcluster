package cc.netvl.sumcluster

import akka.actor.{ActorLogging, ActorRef, Actor}
import cc.netvl.sumcluster.strategies.Strategy

/**
 * Cluster manager actor. Controls and provides access to the cluster of worker nodes.
 */
class ClusterManager extends Actor with ActorLogging {
  import ClusterManager._

  override def receive = {
    case Initialize(n, strategy) =>
      log.info("Initializing {} workers with strategy: {}", n, strategy.name)
      val workers = for (i <- 0 until n) yield context.actorOf(strategy.workerProps(i), s"worker-$i")
      sender ! Initialized(n)
      log.info("Ready to start")
      context become ready(workers, strategy, n, None)
  }

  /**
   * Ready to start cluster operation.
   */
  private def ready(workers: Seq[ActorRef], strategy: Strategy, remaining: Int,
                    handler: Option[ActorRef]): Receive = {
    case Start =>
      log.info("Starting the operation")
      strategy.initializeAndStart(workers, self)
      context become ready(workers, strategy, remaining, Some(sender()))

    case Strategy.Done(i) =>
      log.info("Worker {} finished, {} remaining", i, remaining - 1)
      if (remaining == 1) {
        log.info("Operation has finished, ready for queries")
        handler foreach (_ ! Done)
        context become done(workers)
      } else context become ready(workers, strategy, remaining - 1, handler)
  }

  /**
   * Cluster operation has finished, processing queries.
   */
  private def done(workers: Seq[ActorRef]): Receive = {
    case QueryResult(id) =>
      if (0 <= id && id < workers.size) {
        workers(id) ! Strategy.QueryResult(sender())
      }

    case Strategy.QueryResultResponse(result, id, originalSender) =>
      originalSender ! QueryResultResponse(id, result)

    case QueryValue(id) =>
      if (0 <= id && id < workers.size) {
        workers(id) ! Strategy.QueryValue(sender())
      }

    case Strategy.QueryValueResponse(value, id, originalSender) =>
      originalSender ! QueryValueResponse(id, value)
  }
}

object ClusterManager {
  sealed trait Message

  /**
   * Initialize the cluster with the given number of nodes using given strategy
   */
  case class Initialize(n: Int, strategy: Strategy) extends Message

  /**
   * Notifies the sender of [[Initialize]] that the cluster has been initialized.
   */
  case class Initialized(n: Int) extends Message

  /**
   * Initiate the cluster operation. Only handled after [[Initialize]].
   */
  case object Start extends Message

  /**
   * Signals that one of the nodes in the cluster has finished its operations.
   */
  case object Done extends Message

  /**
   * Retrieve final result from the given cluster node.
   */
  case class QueryResult(id: Int) extends Message

  /**
   * A response to [[QueryResult]].
   */
  case class QueryResultResponse(id: Int, result: Int) extends Message

  /**
   * Retrieve initial value from the given cluster node.
   */
  case class QueryValue(id: Int) extends Message

  /**
   * A response to [[QueryValue]].
   */
  case class QueryValueResponse(id: Int, value: Int) extends Message
}

