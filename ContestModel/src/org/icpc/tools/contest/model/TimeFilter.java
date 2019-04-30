package org.icpc.tools.contest.model;

/**
 * Removes all submissions, judgements, runs, and clarifications after a given time, in ms. The
 * cutoff for judgements and runs is based on the time of the submission they are associated with,
 * not the time they happened. This filter should not be used for role-based filtering by contest
 * freeze time - use the FreezeFilter for this purpose.
 */
public class TimeFilter implements IContestObjectFilter {
	protected IContest contest;
	protected int time;

	public TimeFilter(IContest contest, int time) {
		this.contest = contest;
		this.time = time;
	}

	@Override
	public IContestObject filter(IContestObject obj) {
		if (obj instanceof ISubmission) {
			ISubmission s = (ISubmission) obj;
			if (s.getContestTime() >= time)
				return null;
		}
		if (obj instanceof IClarification) {
			IClarification c = (IClarification) obj;
			if (c.getContestTime() >= time)
				return null;
		}
		if (obj instanceof IJudgement) {
			IJudgement j = (IJudgement) obj;
			ISubmission s = contest.getSubmissionById(j.getSubmissionId());
			if (s == null || s.getContestTime() >= time)
				return null;
		}
		if (obj instanceof IRun) {
			IRun r = (IRun) obj;
			IJudgement j = contest.getJudgementById(r.getJudgementId());
			if (j == null)
				return null;
			ISubmission s = contest.getSubmissionById(j.getSubmissionId());
			if (s == null || s.getContestTime() >= time)
				return null;
		}
		return obj;
	}
}