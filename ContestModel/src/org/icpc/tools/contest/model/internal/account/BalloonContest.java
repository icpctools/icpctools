package org.icpc.tools.contest.model.internal.account;

import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.ISubmission;

public class BalloonContest extends PublicContest {
	public BalloonContest(IAccount account) {
		super();
		username = account.getUsername();
	}

	@Override
	public void add(IContestObject obj) {
		IContestObject.ContestType cType = obj.getType();
		if (cType == ContestType.JUDGEMENT) {
			IJudgement j = (IJudgement) obj;

			ISubmission s = getSubmissionById(j.getSubmissionId());
			if (s == null)
				return;

			// hide judgements for submissions outside the contest time
			int time = s.getContestTime();
			if (time < 0 || time >= getDuration())
				return;

			// don't show judgements for hidden teams
			if (isJudgementHidden(j))
				return;

			// do show before the freeze
			if (getFreezeDuration() != null) {
				int freezeTime = getDuration() - getFreezeDuration();
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
		if (teamId == null)
			return 0;

		List<String> solved = new ArrayList<>();
		ISubmission[] submissions = getSubmissions();
		for (ISubmission s : submissions) {
			if (teamId.equals(s.getTeamId()) && isSolved(s)) {
				if (!solved.contains(s.getProblemId()))
					solved.add(s.getProblemId());
			}
		}

		return solved.size();
	}
}