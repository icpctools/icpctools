package org.icpc.tools.contest.model.internal;

import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.IContest.ScoreboardType;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.Status;

public class Result implements IResult {
	private Status status = Status.UNATTEMPTED;
	private int numPending;
	private int numJudged;
	private long time;
	private long penalty;
	private long pendingPenalty;
	private double score;
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
	public long getPenaltyTime() {
		return penalty;
	}

	@Override
	public long getContestTime() {
		return time;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	@Override
	public double getScore() {
		return score;
	}

	protected void addSubmission(Contest contest, ISubmission s, IJudgement j, IJudgementType jt) {
		if (status == Status.SOLVED) {
			// Already solved. For pass-fail we do nothing. For scoring:
			// - If we have no judgement type, it is a solve after the freeze, so reset the status
			// - If we do have a judgement type and the score increased, update the score
			if (contest.getScoreboardType() == ScoreboardType.SCORE) {
				if (jt == null) {
					// If the score is already the max score for the problem, don't change the status
					IProblem problem = contest.getProblemById(s.getProblemId());
					if (problem == null)
						return;
					if (problem.getMaxScore() != null && score < problem.getMaxScore()) {
						numPending++;
						status = Status.SUBMITTED;
					}
				} else if (jt.isSolved() && j.getScore() != null) {
					if (j.getScore() > score) {
						score = j.getScore();
						numJudged++;
						time = s.getContestTime();
					}
				}
			}

			return;
		}

		if (jt == null) {
			numPending++;
			status = Status.SUBMITTED;
		} else {
			if (jt.isSolved()) {
				status = Status.SOLVED;
				numJudged++;
				penalty = pendingPenalty;
				if (j.getScore() != null)
					score = j.getScore();
			} else if (jt.isPenalty()) {
				status = Status.FAILED;
				Long penaltyTime = contest.getPenaltyTime();
				if (penaltyTime != null) {
					pendingPenalty += penaltyTime;
				}
				numJudged++;
			} // else compile or judgement error that doesn't count as an attempt or penalty
		}

		time = s.getContestTime();

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
		return getNumSubmissions() * 80000 + (int) getContestTime();
	}

	@Override
	public String toString() {
		return "Result [" + getStatus() + ", " + getNumSubmissions() + ", " + getContestTime() + "]";
	}
}
