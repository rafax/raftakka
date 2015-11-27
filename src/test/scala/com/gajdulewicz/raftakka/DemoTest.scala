package com.gajdulewicz.raftakka

import com.gajdulewicz.raftakka.demo.WordAppendStateMachine
import org.scalatest.{FlatSpec, ShouldMatchers}

class DemoTest extends FlatSpec with ShouldMatchers {
  "WordAppendStateMachine" should "append word on apply" in {
    val sm = new WordAppendStateMachine
    val word = "first"
    sm.apply(word)
    sm.get should equal(word)
  }

  it should "add spaces between words" in {
    val sm = new WordAppendStateMachine
    val f = "first"
    val s = "second"
    sm.apply(f)
    sm.apply(s)
    sm.get should equal(f + " " + s)
  }

  "MemoryStorage" should "update term and reset voted for" in {
    val ms = new MemoryStorage[String]
    ms.term = 1
    ms.term should equal(1)
    ms.votedFor should not be defined
  }

  it should "add entry to log on append" in {
    val ms = new MemoryStorage[String]
    val elem = "elem"
    ms.append(elem)
    ms.log.last == elem
  }

  it should "throw when overriding votedFor when it was previously set for current term" in {
    val ms = new MemoryStorage[String]
    ms.votedFor = 1
    intercept[RuntimeException] {
      ms.votedFor = 2
    }
  }

}
