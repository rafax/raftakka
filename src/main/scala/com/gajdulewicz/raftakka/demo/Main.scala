package com.gajdulewicz.raftakka.demo

import akka.actor.ActorSystem
import com.gajdulewicz.raftakka.Command

object Main extends App {
  val system = ActorSystem("MyActorSystem")
  val pingActor = system.actorOf(PingActor.props, "pingActor")
  pingActor ! PingActor.Initialize
  // This example app will ping pong 3 times and thereafter terminate the ActorSystem - 
  // see counter logic in PingActor
  system.awaitTermination()
}


sealed trait CounterCommand extends Command[Int] {
  def key: String
}

case class Get(key: String) extends CounterCommand

case class Set(key: String, value: Int) extends CounterCommand