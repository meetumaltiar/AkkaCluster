package com.cisco.akka.cluster

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

object SimpleClusterApp extends App {
  if (args.nonEmpty) System.setProperty("akka.remote.netty.tcp.port", args(0))
  val system = ActorSystem("ClusterSystem")
  val clusterListener = system.actorOf(Props[SimpleClusterListener], name = "clusterListener")
  Cluster(system).subscribe(clusterListener, classOf[ClusterDomainEvent])
}

class SimpleClusterListener extends Actor {
  def receive = {
    case state: CurrentClusterState => println("Current members: {}", state.members.mkString(", "))
    case MemberUp(member) => println("Member is Up: {}", member.address)
    case UnreachableMember(member) => println("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) => println("Member is Removed: {} after {}", member.address, previousStatus)
    case _: ClusterDomainEvent => // ignore
  }
}