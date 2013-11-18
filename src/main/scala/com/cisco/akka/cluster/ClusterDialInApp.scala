package com.cisco.akka.cluster

import akka.actor._
import akka.cluster._
import akka.cluster.ClusterEvent._
import com.typesafe.config._

object ClusterDialInApp extends App {
  args.length match {
    case 1 =>
      val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=${args(0)}").withFallback(ConfigFactory.load())
      val system = ActorSystem("ClusterSystem", config)
      val frontend = system.actorOf(Props[FrontEndActor], name = "frontend")
    case _ => println(s"Will not start node as port and role not provided...")
  }

}

class FrontEndActor extends Actor {
  var actors = List.empty[(ActorRef, String)]
  def receive = {
    case message: ClusterMessage => sendMessage(message)
    case BackendRegistration(routingKey) =>
      actors = (sender, routingKey) :: actors
      context.watch(sender)
    case Terminated(terminatedActor) =>
      actors = actors filterNot { actor => actor == terminatedActor }
  }

  def sendMessage(message: ClusterMessage) = {
    val (actor, key) = (actors.filter { case (actor, routingKey) => routingKey == message.company }).head
    actor ! message
  }
}

class BackendRegisterActor(localBackendWorkerRouter: ActorRef, key: String) extends Actor {
  val cluster = Cluster(context.system)
  var routingKey = key

  override def preStart = cluster.subscribe(self, classOf[MemberUp])
  override def postStop = cluster.unsubscribe(self)

  def receive = {
    case message: ClusterMessage => localBackendWorkerRouter ! message
    case state: CurrentClusterState => state.members.filter(_.status == MemberStatus.Up) foreach register
    case MemberUp(member) => register(member)
  }

  def register(member: Member) = context.actorSelection(RootActorPath(member.address) / "user" / "frontend") ! BackendRegistration(routingKey)
}

class BackendWorker extends Actor {
  def receive = {
    case msg => println(s"Message is: $msg in $self")
  }
}

case class ClusterMessage(company: String)