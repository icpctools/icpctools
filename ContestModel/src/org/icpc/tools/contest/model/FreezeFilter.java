package org.icpc.tools.contest.model;

/**
 * Freeze filter. All judgements and runs that occur for submissions that come in after the contest
 * freeze are blocked.
 */
public class FreezeFilter implements IContestObjectFilter {
	protected IContest contest;
	protected int freezeTime;

	public FreezeFilter(IContest contest) {
		this.contest = contest;
		freezeTime = contest.getDuration() - contest.getFreezeDuration();
	}

	@Override
	public IContestObject filter(IContestObject obj) {
		if (contest.getFreezeDuration() < 0)
			return obj;

		if (obj instanceof IJudgement) {
			IJudgement j = (IJudgement) obj;
			ISubmission s = contest.getSubmissionById(j.getSubmissionId());
			if (s == null || s.getContestTime() >= freezeTime)
				return null;
		} else if (obj instanceof IRun) {
			IRun r = (IRun) obj;
			IJudgement j = contest.getJudgementById(r.getJudgementId());
			ISubmission s = contest.getSubmissionById(j.getSubmissionId());
			if (s == null || s.getContestTime() >= freezeTime)
				return null;
		}

		return obj;
	}
}