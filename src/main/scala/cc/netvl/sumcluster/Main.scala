package cc.netvl.sumcluster

import akka.actor._
import cc.netvl.sumcluster.strategies.{RingStrategy, Strategy, TreeStrategy}

/**
 * Date: 14.05.14
 * Time: 21:14
 *
 * @author Vladimir Matveev
 */
object Main {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("SumCluster")

    val strategy = new RingStrategy

    val manager = system.actorOf(Props[ClusterManager], "clusterManager")
    val iface = system.actorOf(Props(new InterfaceActor(manager, strategy)), "interface")

    manager.tell(ClusterManager.Initialize(16, strategy), iface)

    System.in.read()

    system.shutdown()
  }

  class InterfaceActor(clusterManager: ActorRef, strategy: Strategy) extends Actor with ActorLogging {
    override def receive = {
      case ClusterManager.Initialized(n) =>
        log.info("Cluster is initialized, starting the operation")
        clusterManager ! ClusterManager.Start
        context become waiting(n)
    }

    def waiting(n: Int): Receive = {
      case ClusterManager.Done =>
        log.info("Cluster has finished the operation")
        log.info("Accumulated sum in the strategy: {}", strategy.sum)
        log.info("Querying nodes in the cluster")
        for (i <- 0 until n) clusterManager ! ClusterManager.Query(i)
        context become counting(n)
    }

    def counting(n: Int): Receive = {
      case ClusterManager.QueryResult(i, result) =>
        log.info("Node {} contains result {}, {}", i, result,
          if (result == strategy.sum) "seems to be okay"
          else s"differs from the actual sum (${strategy.sum})")
        if (n == 1) {
          log.info("Queried all nodes, press enter to exit")

        } else {
          context become counting(n - 1)
        }
    }
  }
}
