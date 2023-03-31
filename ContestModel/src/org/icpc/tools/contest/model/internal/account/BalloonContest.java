package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;

/**
 * This contest matches what spectators see except that if a team has less than 3 solved problems
 * and they solve another one during the freeze, that judgement is shown. This visibility level is
 * used at the World Finals to control the balloon printer, thus allowing 3 balloons to be sent to
 * teams.
 */
public class BalloonContest extends PublicContest {
	public BalloonContest(IAccount account) {
		super();
		username = account.getUsername();
	}

	@Override
	public void add(IContestObject obj) {
		if (obj instanceof IDelete) {
			addIt(obj);
			return;
		}

		IContestObject.ContestType cType = obj.getType();
		if (cType == ContestType.JUDGEMENT) {
			IJudgement j = (IJudgement) obj;

			ISubmission s = getSubmissionById(j.getSubmissionId());
			if (s == null)
				return;

			// hide judgements for submissions outside the contest time
			long time = s.getContestTime();
			if (time < 0 || time >= getDuration())
				return;

			// don't show judgements for hidden teams
			if (isJudgementHidden(j))
				return;

			// do show before the freeze
			if (getFreezeDuration() != null) {
				long freezeTime = getDuration() - getFreezeDuration();
				if (time < freezeTime) {
					addIt(j);
					return;
				}
			}

			// but after, only up to 3 judgements
			if (getNumSolved(s.getTeamId()) < 3) {
				addIt(j);
				return;
			}

			return;
		}

		super.add(obj);
	}

	protected int getNumSolved(String teamId) {
		ITeam team = getTeamById(teamId);
		if (team == null)
			return 0;

		return getStanding(team).getNumSolved();
	}
}