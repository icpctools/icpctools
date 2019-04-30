package org.icpc.tools.contest.model.internal;

import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.Status;

public class Result implements IResult {
	private Status status = Status.UNATTEMPTED;
	private int numPending;
	private int numJudged;
	private int time;
	private int penalty;
	private int pendingPenalty;
	private boolean isFTS;

	public Result() {
		super();
	}

	@Override
	public int getNumPending() {
		return numPending;
	}

	@Override
	public int getNumJudged() {
		return numJudged;
	}

	@Override
	public int getNumSubmissions() {
		return numPending + numJudged;
	}

	@Override
	public int getPenaltyTime() {
		return penalty;
	}

	@Override
	public int getContestTime() {
		return time;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	protected void addSubmission(Contest contest, int submissionTime, IJudgementType jt) {
		if (status == Status.SOLVED)
			return;

		if (jt == null) {
			numPending++;
			status = Status.SUBMITTED;
		} else {
			if (jt.isSolved()) {
				status = Status.SOLVED;
				numJudged++;
				penalty = pendingPenalty;
			} else if (jt.isPenalty()) {
				status = Status.FAILED;
				pendingPenalty += 20;
				numJudged++;
			} // else compile or judgement error that doesn't count as an attempt or penalty
		}

		time = submissionTime;

	}

	@Override
	public boolean isFirstToSolve() {
		return isFTS;
	}

	protected void setFTS() {
		isFTS = true;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Result))
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
		return "Result [" + getStatus() + ", " + getNumSubmissions() + ", " + getContestTime() + "]";
	}
}