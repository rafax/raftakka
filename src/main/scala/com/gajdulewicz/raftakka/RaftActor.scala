package com.gajdulewicz.raftakka

import java.util.Date

import akka.actor.{Cancellable, Actor}
import akka.event.LoggingReceive
import com.gajdulewicz.raftakka.RaftActor._

import scala.util.Random
import scala.concurrent.duration._

class RaftActor[TEntry](id: MemberId, stateMachine: StateMachine[TEntry], storage: StableStorage[TEntry]) extends Actor {

  import context._

  println(s"Starting ${self.path.name}")

  private[this] var votesReceived = 0

  override def receive: Receive = LoggingReceive {
    case cmd: Request =>
      lastRpc = Some(new Date())
      cmd match {
        case ae: AppendEntries =>
          println(s"${id} Heartbeat received")
          sender ! handle(ae)
        case rv: RequestVote => sender ! handle(rv)
      }
    case r: Response =>
      r match {
        case RequestVoteResponse(term, granted) =>
          if (!isLeader) {
            if (granted) {
              votesReceived += 1
            }
            if (votesReceived > (others.size + 1) / 2) {
              // I am the leader
              println(s"${id} elected to leader")
              isLeader = true
              system.scheduler.schedule(0.seconds, electionTimeout, self, SendHeartbeat)
            }
          }
        case AppendEntriesResponse(_, _) =>
      }
    case i: InternalMessage =>
      i match {
        case maybeStartElection: StartElection =>
          if (!isLeader && lastRpc.map(_.before(maybeStartElection.started)).getOrElse(true)) {
            storage.term = storage.term + 1
            votesReceived = 1
            println(s"$id: Starting election for term ${storage.term}")
            others.foreach(_ ! RequestVote(storage.term, id, 0, 0))
          }
        case UpdateConfig(cc) =>
          println(s"Received config with ${cc.members}")
          clusterConfig = cc
          startElectionTimer
        case SendHeartbeat => others.foreach(a => a ! AppendEntries(storage.term, id, 0, 0, Nil, 0))
      }
  }

  private[raftakka] val electionTimeout = ClusterConfig.electionTimeout.plus(Random.nextInt(ClusterConfig.electionTimeout.toMillis.toInt).millis)

  private[raftakka] var clusterConfig: ClusterConfig = Initializing
  private[raftakka] var isLeader = false

  private[raftakka] def others = clusterConfig match {
    case Initializing => Set.empty
    case FunctionalCluster(m) => m - self
  }

  private[raftakka] def startElectionTimer: Cancellable =
    context.system.scheduler.schedule(electionTimeout, electionTimeout, self, StartElection(storage.term, new Date()))

  private[raftakka] def handle(ae: AppendEntries): AppendEntriesResponse = {
    storage.term = ae.term
    AppendEntriesResponse(storage.term, true)
  }

  private[raftakka] def handle(rv: Request with RequestVote): RequestVoteResponse = {
    val voteGranted = {
      if (rv.term <= storage.term) {
        false
      } else {
        storage.votedFor.map(_ == rv.candidateId).getOrElse({
          storage.votedFor = rv.candidateId
          true
        })
      }
    }
    println(s"$id: Term ${storage.term} vote request for ${rv.candidateId} was granted? $voteGranted")
    RequestVoteResponse(storage.term, voteGranted)
  }

  private[raftakka] var lastRpc: Option[Date] = None

}


object RaftActor {

  type MemberId = Int
  type Term = Int

  sealed trait Command

  sealed trait Request extends Command

  sealed trait Response extends Command

  sealed trait InternalMessage extends Command

  case class AppendEntries(term: Term, leaderId: MemberId, prevLogIndex: Int, prevLogTerm: Term, entries: List[AnyRef], commitIndex: Int) extends Request

  case class AppendEntriesResponse(currentTerm: Term, success: Boolean) extends Response

  case class RequestVote(term: Term, candidateId: MemberId, lastLogIndex: Int, lastLogTerm: Term) extends Request

  case class RequestVoteResponse(term: Term, voteGranted: Boolean) extends Response

  case class StartElection(term: Term, started: Date) extends InternalMessage

  case class UpdateConfig(config: FunctionalCluster) extends InternalMessage

  case object SendHeartbeat extends InternalMessage

}