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
		if (contest.getFreezeDuration() != null)
			freezeTime = contest.getDuration() - contest.getFreezeDuration();
	}

	@Override
	public IContestObject filter(IContestObject obj) {
		if (contest.getFreezeDuration() == null)
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