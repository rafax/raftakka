package com.gajdulewicz.raftakka

abstract class StateMachine[TEntry] {
  def apply(entry: TEntry)
}
