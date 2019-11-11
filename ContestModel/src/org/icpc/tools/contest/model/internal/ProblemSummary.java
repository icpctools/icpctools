package org.icpc.tools.contest.model.internal;

import org.icpc.tools.contest.model.IProblemSummary;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.Status;

public class ProblemSummary implements IProblemSummary {
	private int numPending;
	private int pendingTime;
	private int numSolved;
	private int solvedTime;
	private int numFailed;
	private int failedTime;

	public ProblemSummary() {
		super();
	}

	@Override
	public int getNumPending() {
		return numPending;
	}

	@Override
	public int getPendingContestTime() {
		return failedTime;
	}

	@Override
	public int getNumSolved() {
		return numSolved;
	}

	@Override
	public int getSolvedContestTime() {
		return failedTime;
	}

	@Override
	public int getNumFailed() {
		return numFailed;
	}

	@Override
	public int getFailedContestTime() {
		return failedTime;
	}

	@Override
	public int getNumSubmissions() {
		return numPending + numFailed + numSolved;
	}

	public void addResult(IResult result) {
		if (result == null)
			return;

		Status resultStatus = result.getStatus();
		if (resultStatus == null || resultStatus == Status.UNATTEMPTED)
			return;

		if (resultStatus == Status.SUBMITTED) {
			numPending++;
			pendingTime = Math.max(pendingTime, result.getContestTime());
		} else if (resultStatus == Status.SOLVED) {
			numSolved++;
			solvedTime = Math.max(solvedTime, result.getContestTime());
		} else if (resultStatus == Status.FAILED) {
			numFailed++;
			failedTime = Math.max(failedTime, result.getContestTime());
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ProblemSummary))
			return false;

		ProblemSummary ps = (ProblemSummary) obj;
		if (numPending != ps.numPending)
			return false;
		if (numSolved != ps.numSolved)
			return false;
		if (numFailed != ps.numFailed)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return numSolved + numFailed * 7 + numPending * 49;
	}

	@Override
	public String toString() {
		return "ProblemSummary [" + numPending + ", " + numSolved + ", " + numFailed + "]";
	}
}