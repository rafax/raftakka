package com.gajdulewicz.raftakka

import com.gajdulewicz.raftakka.RaftActor.{MemberId, Term}

trait StableStorage[TEntry] {
  def term_=(t: Term): Term

  def term: Term

  def votedFor_=(mid: MemberId)

  def votedFor: Option[MemberId]

  def append(e: TEntry)

  def log: List[TEntry]
}

class MemoryStorage[TEntry] extends StableStorage[TEntry] {
  private[this] var _term: Term = -1
  private[this] var _votedFor = Map[Term, MemberId]()
  private[this] var _log = List[TEntry]()

  override def term_=(t: Term): Term = {
    _term = t
    _term
  }

  override def votedFor_=(mid: MemberId): Unit = {
    if (_votedFor.get(_term).isDefined) {
      throw new RuntimeException("Already voted in this term")
    } else {
      _votedFor = _votedFor + (term -> mid)
    }
  }

  override def append(e: TEntry): Unit = _log = _log :+ e

  override def term: Term = _term

  override def votedFor: Option[MemberId] = _votedFor.get(_term)

  override def log: List[TEntry] = _log
}