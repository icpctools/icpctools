package org.icpc.tools.contest.model.internal;

import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ITeamResult;
import org.icpc.tools.contest.model.feed.RelativeTime;

public class Standing implements IStanding {
	private ITeamResult tr;
	private int rank;

	public Standing(TeamResult tr2) {
		this.tr = tr2;
	}

	@Override
	public long getTime() {
		return tr.getTime();
	}

	@Override
	public int getNumSolved() {
		return tr.getNumSolved();
	}

	@Override
	public long getLastSolutionTime() {
		return tr.getLastSolutionTime();
	}

	@Override
	public double getScore() {
		return tr.getScore();
	}

	@Override
	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	@Override
	public String toString() {
		return "Standing [" + rank + ", " + getNumSolved() + ", " + RelativeTime.format(getTime()) + ", "
				+ RelativeTime.format(getLastSolutionTime()) + ", " + getScore() + "]";
	}
}