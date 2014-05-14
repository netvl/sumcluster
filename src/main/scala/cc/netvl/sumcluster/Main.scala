package cc.netvl.sumcluster

import akka.actor._
import cc.netvl.sumcluster.strategies.{RingStrategy, Strategy, TreeStrategy}

/**
 * Entry point, contains actor system initialization code and "UI" actor.
 */
object Main {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("SumCluster")

    val strategy = new RingStrategy  // select the strategy

    // Create main actors
    val manager = system.actorOf(Props[ClusterManager], "clusterManager")
    val iface = system.actorOf(Props(new InterfaceActor(manager, strategy)), "interface")

    // Initialize the cluster
    manager.tell(ClusterManager.Initialize(16, strategy), iface)

    // Wait for user input
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

    // waiting for cluster nodes to finish their work
    def waiting(n: Int): Receive = {
      case ClusterManager.Done =>
        log.info("Cluster has finished the operation, querying the nodes for their values")
        for (i <- 0 until n) clusterManager ! ClusterManager.QueryValue(i)
        context become summing(n, n, 0)
    }

    // retrieve and sum values from cluster nodes
    def summing(n: Int, c: Int, tempSum: Int): Receive = {
      case ClusterManager.QueryValueResponse(id, value) =>
        log.info("Node {} has value {}, accumulating", id, value)
        val sum = tempSum + value
        if (c == 1) {
          log.info("Values from all nodes are accumulated, total sum: {}", sum)
          log.info("Checking results in cluster nodes")
          for (i <- 0 until n) clusterManager ! ClusterManager.QueryResult(i)
          context become checking(n, sum)
        } else context become summing(n, c-1, sum)
    }

    // check that all nodes contain valid result
    def checking(n: Int, sum: Int): Receive = {
      case ClusterManager.QueryResultResponse(i, result) =>
        log.info("Node {} contains result {}, {}", i, result,
          if (result == sum) "seems to be okay"
          else s"different from the actual sum ($sum)")
        if (n == 1) {
          log.info("Checked all nodes, press enter to exit")
          // finished
        } else {
          context become checking(n-1, sum)
        }
    }
  }
}
