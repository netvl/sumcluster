package cc.cu.netvl.sumcluster

import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, ShouldMatchers}
import akka.testkit.{ImplicitSender, TestKit, TestActorRef}
import akka.actor.{Props, ActorSystem}

class ClusterManagerTest
  extends TestKit(ActorSystem("clusterManagerTest")) with ImplicitSender
  with FlatSpecLike with ShouldMatchers with BeforeAndAfterAll {

  override protected def afterAll() {
    TestKit.shutdownActorSystem(system)
  }
  
  "ClusterManager" should "create children with the specified strategy" in {
    val strategy = new TestSuccessfulStrategy(8)

    val clusterManager = system.actorOf(Props[ClusterManager])

    clusterManager ! ClusterManager.Initialize(8, strategy)
    expectMsg(ClusterManager.Initialized(8))

    strategy.calledProps should be (Set(0, 1, 2, 3, 4, 5, 6, 7))
    strategy.nodes.map(_.id) should be (Set(0, 1, 2, 3, 4, 5, 6, 7))
    strategy.doneNodes shouldBe empty
  }

  it should "start the cluster operation and report its completion" in {
    val strategy = new TestSuccessfulStrategy(8)

    val clusterManager = TestActorRef[ClusterManager]

    clusterManager ! ClusterManager.Initialize(8, strategy)
    expectMsg(ClusterManager.Initialized(8))

    clusterManager ! ClusterManager.Start
    expectMsg(ClusterManager.Done)

    strategy.doneNodes.map(_.id) should be (Set(0, 1, 2, 3, 4, 5, 6, 7))
  }

  it should "provide access to cluster nodes state" in {
    val strategy = new TestSuccessfulStrategy(8)

    val clusterManager = system.actorOf(Props[ClusterManager])

    clusterManager ! ClusterManager.Initialize(8, strategy)
    expectMsg(ClusterManager.Initialized(8))

    clusterManager ! ClusterManager.Start
    expectMsg(ClusterManager.Done)

    for (i <- 0 until 8) {
      clusterManager ! ClusterManager.QueryResult(i)
      expectMsg(ClusterManager.QueryResultResponse(i, strategy.sum))

      clusterManager ! ClusterManager.QueryValue(i)
      expectMsg(ClusterManager.QueryValueResponse(i, i))
    }
  }
}

