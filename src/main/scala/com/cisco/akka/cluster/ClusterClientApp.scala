package com.cisco.akka.cluster

import com.typesafe.config.ConfigFactory

import akka.actor._
import akka.contrib.pattern.ClusterClient

object ClusterClientApp extends App {
  val conf = ConfigFactory.load("client-application.conf")
  val system = ActorSystem("client", conf)
  val initialContacts = Set(
    system.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:2551/user/receptionist"),
    system.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:2552/user/receptionist"))
  val clusterClient = system.actorOf(ClusterClient.props(initialContacts))
  Thread.sleep(5000)

  (1 to 100000) map { i => send }

  def send = {
    Thread.sleep(500)
    println("sending")
    clusterClient ! ClusterClient.Send("/user/workerRouter", "hello", localAffinity = true)
  }
}