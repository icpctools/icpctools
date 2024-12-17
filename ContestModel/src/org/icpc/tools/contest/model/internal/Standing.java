package org.icpc.tools.contest.model.internal;

import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.feed.RelativeTime;

public class Standing implements IStanding {
	private long penalty;
	private int numSolved;
	private long lastSolution;
	private String rank;
	private double score;

	public Standing() {
		// do nothing
	}

	public void init(int numSolved2, long penalty2, double score2, long lastSolution2) {
		this.numSolved = numSolved2;
		this.penalty = penalty2;
		this.score = score2;
		this.lastSolution = lastSolution2;
	}

	public void setPenalty(long penalty) {
		this.penalty = penalty;
	}

	@Override
	public long getTime() {
		return penalty;
	}

	public void setNumSolved(int numSolved) {
		this.numSolved = numSolved;
	}

	@Override
	public int getNumSolved() {
		return numSolved;
	}

	@Override
	public long getLastSolutionTime() {
		return lastSolution;
	}

	@Override
	public String getRank() {
		return rank;
	}

	public void setRank(String rank) {
		this.rank = rank;
	}

	@Override
	public double getScore() {
		return score;
	}

	@Override
	public String toString() {
		return "Standing [" + rank + ", " + numSolved + ", " + RelativeTime.format(penalty) + ", "
				+ RelativeTime.format(lastSolution) + ", " + score + "]";
	}
}