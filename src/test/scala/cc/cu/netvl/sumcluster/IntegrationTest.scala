package cc.cu.netvl.sumcluster

import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, ShouldMatchers, FlatSpecLike}
import cc.cu.netvl.sumcluster.strategies.{RingStrategy, Strategy, TreeStrategy}
import scala.util.Random

/**
 * A test for the whole cluster infrastructure with different strategies.
 */
class IntegrationTest
  extends TestKit(ActorSystem("integrationTest")) with ImplicitSender
  with FlatSpecLike with ShouldMatchers with BeforeAndAfterAll {

  override protected def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  import scala.concurrent.duration._

  "A cluster" should "work correctly with tree strategy" in {
    testStrategy(TreeStrategy)
  }

  it should "also work correctly with ring strategy" in {
    testStrategy(RingStrategy)
  }

  def testStrategy(strategy: Strategy) {
    val clusterManager = system.actorOf(Props[ClusterManager])

    val n = Random.nextInt(31) + 1
    clusterManager ! ClusterManager.Initialize(n, strategy)
    expectMsg(ClusterManager.Initialized(n))

    clusterManager ! ClusterManager.Start
    expectMsg(ClusterManager.Done)

    val values = for (i <- 0 until n) yield {
      clusterManager ! ClusterManager.QueryValue(i)
      receiveOne(3.seconds) match {
        case ClusterManager.QueryValueResponse(`i`, v) => v
        case msg => fail(s"Unexpected message from $i node: $msg")
      }
    }
    val expectedSum = values.sum

    for (i <- 0 until n) {
      clusterManager ! ClusterManager.QueryResult(i)
      expectMsg(ClusterManager.QueryResultResponse(i, expectedSum))
    }
  }
}
