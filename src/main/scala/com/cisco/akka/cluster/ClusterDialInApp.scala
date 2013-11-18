package com.cisco.akka.cluster

import akka.actor._
import akka.cluster._
import akka.cluster.ClusterEvent._
import com.typesafe.config._
import akka.routing._
import akka.contrib.pattern._
import akka.cluster.routing._

object ClusterDialInApp extends App {
  args.length match {
    case 2 =>
      val port = args(0)
      val key = args(1)
      println(s"starting actor system on port $port with key $key")
      val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=${port}").withFallback(ConfigFactory.load())
      val system = ActorSystem("ClusterSystem", config)
      val frontend = system.actorOf(Props[FrontEndActor], name = "frontend")
      val localBackendWorkerRouter = system.actorOf(Props[BackendWorker].withRouter(RoundRobinRouter(nrOfInstances = 16)), "workers")
      val backendRegisterActor = system.actorOf(Props(new BackendRegisterActor(localBackendWorkerRouter, key)), name = "backend")
      Cluster(system)
      val clusterRouterSettings = ClusterRouterSettings(totalInstances = 100, routeesPath = "/user/frontend", allowLocalRoutees = true, useRole = None)
      val clusterAwareWorkerRouter = system.actorOf(Props.empty.withRouter(ClusterRouterConfig(RoundRobinRouter(), clusterRouterSettings)), name = "frontendRouter")
      ClusterReceptionistExtension(system).registerService(clusterAwareWorkerRouter)

    case _ => println(s"Will not start node as port and role not provided...")
  }
}

object ClusterDialInAppClient extends App {
  var clusterClient: ActorRef = _
  args.length match {
    case 1 =>
      val key = args(0)
      println(s"The client will send the Cluster message with key: $key")
      val conf = ConfigFactory.load("client-application.conf")
      val system = ActorSystem("client", conf)
      val initialContacts = Set(system.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:2551/user/receptionist"))
      clusterClient = system.actorOf(ClusterClient.props(initialContacts))
      Thread.sleep(5000)
      (1 to 100000) map { i => send(key) }
    case _ =>
  }

  def send(key: String) = {
    Thread.sleep(500)
    println("sending")
    clusterClient ! ClusterClient.Send("/user/frontendRouter", ClusterMessage(key), localAffinity = true)
  }
}

class FrontEndActor extends Actor {
  var actors = List.empty[(ActorRef, String)]
  def receive = {
    case message: ClusterMessage => sendMessage(message)
    case BackendRegistration(routingKey) =>
      println(s"backend registraion request arrived from sender: $sender for key $routingKey")
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
case class BackendRegistration(key: String) 