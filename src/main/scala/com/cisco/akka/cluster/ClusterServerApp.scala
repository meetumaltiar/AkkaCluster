package com.cisco.akka.cluster

import com.typesafe.config.ConfigFactory

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.routing._
import akka.contrib.pattern._
import akka.routing._

object ClusterServerApp extends App {
  if (args.nonEmpty) System.setProperty("akka.remote.netty.tcp.port", args(0))
  val system = ActorSystem("ClusterSystem")
  val router = system.actorOf(Props[Worker].withRouter(RoundRobinRouter(nrOfInstances = 4)), "workers")
  Cluster(system)
  val clusterRouterSettings = ClusterRouterSettings(totalInstances = 100, routeesPath = "/user/workers", allowLocalRoutees = true, useRole = None)
  val clusterAwareWorkerRouter = system.actorOf(Props.empty.withRouter(ClusterRouterConfig(RoundRobinRouter(), clusterRouterSettings)), name = "workerRouter")
  ClusterReceptionistExtension(system).registerService(clusterAwareWorkerRouter)
}

class Worker extends Actor {
  def receive = {
    case msg: String => println(msg + " " + self.path)
  }
}