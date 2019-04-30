package org.icpc.tools.contest.model.internal;

import org.icpc.tools.contest.model.IProblemSummary;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.Status;

public class ProblemSummary implements IProblemSummary {
	private Status status = Status.UNATTEMPTED;
	private int numPending;
	private int numSolved;
	private int numFailed;
	private int time;

	public ProblemSummary() {
		super();
	}

	@Override
	public int getNumPending() {
		return numPending;
	}

	@Override
	public int getNumSolved() {
		return numSolved;
	}

	@Override
	public int getNumFailed() {
		return numFailed;
	}

	@Override
	public int getNumSubmissions() {
		return numPending + numFailed + numSolved;
	}

	@Override
	public int getContestTime() {
		return time;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	public void addResult(IResult result) {
		if (result == null)
			return;

		Status status2 = result.getStatus();
		if (status2 == Status.SUBMITTED) {
			numPending++;
			if (status == null || status == Status.UNATTEMPTED) {
				status = Status.SUBMITTED;
				time = result.getContestTime();
			}
		} else if (status2 == Status.FAILED) {
			numFailed++;
			if (status == null || status == Status.UNATTEMPTED || status == Status.SUBMITTED) {
				status = Status.FAILED;
				time = result.getContestTime();
			}
		} else if (status2 == Status.SOLVED) {
			numSolved++;
			if (status != Status.SOLVED) {
				status = Status.SOLVED;
				time = result.getContestTime();
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ProblemSummary))
			return false;

		IResult r = (IResult) obj;

		if (getNumSubmissions() != r.getNumSubmissions())
			return false;
		if (getContestTime() != r.getContestTime())
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return getNumSubmissions() * 80000 + getContestTime();
	}

	@Override
	public String toString() {
		return "ProblemSummary [" + getStatus() + ", " + getNumSubmissions() + ", " + getContestTime() + "]";
	}
}