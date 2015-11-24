package com.gajdulewicz.raftakka

abstract class StateMachine[_] {
  def execute[T](cmd: Command[T]): T
}

trait Command[T]