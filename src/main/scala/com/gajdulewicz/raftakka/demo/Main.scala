package com.gajdulewicz.raftakka.demo

import akka.actor.{ActorSystem, Props}
import com.gajdulewicz.raftakka.RaftActor.UpdateConfig
import com.gajdulewicz.raftakka.{FunctionalCluster, MemoryStorage, RaftActor, StateMachine}

object Main extends App {
  val clusterSize = 5
  val system = ActorSystem("MyActorSystem")
  val members = (1 to clusterSize).map(id => system.actorOf(Props(new RaftActor[String](id, new WordAppendStateMachine, new MemoryStorage[String])), "raft_" + id)).toSet
  members.foreach(a => a ! UpdateConfig(FunctionalCluster(members)))
  system.awaitTermination()
}

class WordAppendStateMachine extends StateMachine[String] {
  private[this] val builder = new StringBuilder

  override def apply(entry: String): Unit = builder.append(entry + " ")

  def get: String = builder.toString.trim
}