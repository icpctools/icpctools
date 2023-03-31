package org.icpc.tools.contest.model.util;

import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IState;

public class ScoreboardData {
	public static class STeam {
		public String rank;
		public String teamId;
		public int numSolved;
		public long totalTime;
		public SProblem[] problems;

		@Override
		public String toString() {
			return rank + " " + teamId + " " + numSolved + " " + totalTime;
		}
	}

	public static class SProblem {
		public String problemId;
		public int numJudged;
		public int numPending;
		public boolean solved;
		public int time;
		public boolean firstToSolve;

		@Override
		public String toString() {
			return problemId + " " + solved;
		}
	}

	public static class SState implements IState {
		public Long started;
		public Long ended;
		public Long frozen;
		public Long thawed;
		public Long finalized;
		public Long endOfUpdates;

		@Override
		public ContestType getType() {
			return ContestType.STATE;
		}

		@Override
		public Map<String, Object> getProperties() {
			return null;
		}

		@Override
		public String getId() {
			return null;
		}

		@Override
		public List<String> validate(IContest contest) {
			return null;
		}

		@Override
		public Long getStarted() {
			return started;
		}

		@Override
		public Long getEnded() {
			return ended;
		}

		@Override
		public Long getFrozen() {
			return frozen;
		}

		@Override
		public Long getThawed() {
			return thawed;
		}

		@Override
		public Long getFinalized() {
			return finalized;
		}

		@Override
		public Long getEndOfUpdates() {
			return endOfUpdates;
		}

		@Override
		public boolean isRunning() {
			return started != null && ended == null;
		}

		@Override
		public boolean isFrozen() {
			return frozen != null && thawed == null;
		}

		@Override
		public boolean isFinal() {
			return finalized != null;
		}

		@Override
		public boolean isDoneUpdating() {
			return endOfUpdates != null;
		}
	}

	public String eventId;
	public Long time;
	public Long contestTime;
	public IState state;

	public STeam[] teams;
}