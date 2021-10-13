package org.icpc.tools.contest.model.internal;

import org.icpc.tools.contest.model.IStanding;

public class Standing implements IStanding {
	private int penalty;
	private int numSolved;
	private int lastSolution;
	private String rank;
	private double score;

	public Standing() {
		// do nothing
	}

	public void init(int numSolved2, int penalty2, double score2, int lastSolution2) {
		this.numSolved = numSolved2;
		this.penalty = penalty2;
		this.score = score2;
		this.lastSolution = lastSolution2;
	}

	public void setPenalty(int penalty) {
		this.penalty = penalty;
	}

	@Override
	public int getTime() {
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
	public int getLastSolutionTime() {
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
		return "Standing [" + rank + ", " + numSolved + ", " + penalty + ", " + lastSolution + "]";
	}
}