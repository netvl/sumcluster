package cc.netvl.sumcluster

import akka.actor.{ActorLogging, ActorRef, Actor}
import cc.netvl.sumcluster.strategies.Strategy

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

  private def ready(workers: Seq[ActorRef], strategy: Strategy, remaining: Int,
                    handler: Option[ActorRef]): Receive = {
    case Start =>
      log.info("Starting the operation")
      strategy.start(workers, self)
      context become ready(workers, strategy, remaining, Some(sender()))

    case Strategy.Done(i) =>
      log.info("Worker {} finished, {} remaining", i, remaining - 1)
      if (remaining == 1) {
        log.info("Operation has finished, ready for queries")
        handler foreach (_ ! Done)
        context become done(workers)
      } else context become ready(workers, strategy, remaining - 1, handler)
  }

  private def done(workers: Seq[ActorRef]): Receive = {
    case Query(id) =>
      if (0 <= id && id < workers.size) {
        workers(id) ! Strategy.Query(sender())
      }

    case Strategy.QueryResult(result, id, originalSender) =>
      originalSender ! QueryResult(id, result)
  }
}

object ClusterManager {
  sealed trait Message

  case class Initialize(n: Int, strategy: Strategy) extends Message
  case class Initialized(n: Int) extends Message

  case object Start extends Message
  case object Done extends Message

  case class Query(i: Int) extends Message
  case class QueryResult(i: Int, result: Int) extends Message
}

