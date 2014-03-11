name := "AkkaCluster"

scalaVersion := "2.10.2"

version := "1.0"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.0"

libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.3.0"

libraryDependencies += "com.typesafe.akka" %% "akka-contrib" % "2.3.0"

libraryDependencies += "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.0"
