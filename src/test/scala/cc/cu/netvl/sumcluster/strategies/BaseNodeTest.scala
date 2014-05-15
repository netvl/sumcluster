package cc.cu.netvl.sumcluster.strategies

import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import akka.actor.{ActorRef, Props, ActorSystem}
import org.scalatest.{BeforeAndAfterAll, ShouldMatchers, FlatSpecLike}
import scala.collection.mutable
import scala.util.Random

class BaseNodeTest
  extends TestKit(ActorSystem("baseNodeTest")) with ImplicitSender
  with FlatSpecLike with ShouldMatchers with BeforeAndAfterAll {

  override protected def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "BaseNode" should "perform sendTo operation asynchronously" in {
    implicit val workers = mutable.Buffer.empty[ActorRef]

    class TestBaseNode(override val id: Int) extends BaseNode {
      import scala.concurrent.duration._

      val system = context.system
      import system.dispatcher

      override protected def value = 0

      override def receive = {
        case 'run =>
          sendTo(1, 20)
          sendTo(2, 30)
          sendTo(3, 40)
          testActor ! 'ok_0

        case BaseNode.Value(20) if id == 1 =>
          context.system.scheduler.scheduleOnce(500.milliseconds, testActor, 'ok)

        case BaseNode.Value(30) if id == 2 =>
          context.system.scheduler.scheduleOnce(500.milliseconds, testActor, 'ok)

        case BaseNode.Value(40) if id == 3 =>
          context.system.scheduler.scheduleOnce(500.milliseconds, testActor, 'ok)
      }
    }

    workers ++= (0 to 3) map (i => system actorOf Props(new TestBaseNode(i)))

    workers(0) ! 'run
    expectMsg('ok_0)

    for (_ <- 1 to 3) {
      expectMsg('ok)
    }
  }

  it should "recvFrom the specific node only" in {
    implicit val workers = mutable.Buffer.empty[ActorRef]

    class TestBaseNode(override val id: Int) extends BaseNode {
      override protected def value = 0

      override def receive = {
        case 'run if id == 0 =>
          recvFrom(1) { value =>
            testActor ! value
            recvFrom(2) { value =>
              testActor ! value
              recvFrom(3) { value =>
                testActor ! value
              }
            }
          }

        case 'run =>
          sendTo(0, id)
      }
    }

    workers ++= (0 to 3) map (i => system actorOf Props(new TestBaseNode(i)))

    workers foreach (_ ! 'run)
    expectMsg(1)
    expectMsg(2)
    expectMsg(3)
  }

  it should "becomeDone and respond to queries" in {
    class TestBaseNode extends BaseNode {
      override val id = Random.nextInt()
      override val value = Random.nextInt()
      val result = Random.nextInt()

      override def receive = {
        case 'run => becomeDone(result, testActor)
      }
    }

    val node = TestActorRef(new TestBaseNode)

    node ! 'run
    expectMsg(Strategy.Done(node.underlyingActor.id))

    node ! Strategy.QueryResult(testActor)
    expectMsg(Strategy.QueryResultResponse(node.underlyingActor.result, node.underlyingActor.id, testActor))

    node ! Strategy.QueryValue(testActor)
    expectMsg(Strategy.QueryValueResponse(node.underlyingActor.value, node.underlyingActor.id, testActor))
  }
}
