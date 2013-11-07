AkkaCluster
===========

Bring up two nodes in the cluster with the following commands:
1. sbt "run-main com.cisco.akka.cluster.ClusterServerApp 2551"
2. sbt "run-main com.cisco.akka.cluster.ClusterServerApp 2552"

Then use the client to send to the cluster via cluster router
sbt "run-main com.cisco.akka.cluster.ClusterClientApp"

You will see the message flowing in all the nodes in the cluster.
