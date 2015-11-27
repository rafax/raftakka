package com.gajdulewicz.raftakka

import akka.actor.ActorRef
import scala.concurrent.duration._

sealed trait ClusterConfig

object ClusterConfig {
  val electionTimeout = 1000.milli
  val heartbeatTimeout = 1000.milli
}

case class FunctionalCluster(members: Set[ActorRef]) extends ClusterConfig

case object Initializing extends ClusterConfig

