package com.cisco.akka.cluster

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.routing._
import akka.contrib.pattern._
import com.typesafe.config.ConfigFactory

object ClusterServerApp extends App {
  if (args.nonEmpty) System.setProperty("akka.remote.netty.tcp.port", args(0))
  val system = ActorSystem("ClusterSystem")
  val router = system.actorOf(Props[Worker].withRouter(RoundRobinRouter(nrOfInstances = 4)), "workers")
  Cluster(system).subscribe(router, classOf[ClusterDomainEvent])
  ClusterReceptionistExtension(system).registerService(router)
}

class Worker extends Actor {
  def receive = {
    case msg: String => println(msg)
  }
}

object ClusterClientApp extends App {
  val conf = ConfigFactory.load("client-application.conf")
  val system = ActorSystem("client", conf)
  val initialContacts = Set(
    system.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:2551/user/receptionist"),
    system.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:2552/user/receptionist"))
  val clusterClient = system.actorOf(ClusterClient.props(initialContacts))
  Thread.sleep(3000)
  clusterClient ! ClusterClient.Send("/user/workers", "hello", localAffinity = true)
}