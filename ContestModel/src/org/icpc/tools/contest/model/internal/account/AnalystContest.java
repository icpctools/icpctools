package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IRun;
import org.icpc.tools.contest.model.ISubmission;

/**
 * Filter that adds things analysts can see compared to public:
 * <ul>
 * <li>Runs before the freeze</li>
 * <li>Clarifications</li>
 * </ul>
 */
public class AnalystContest extends PublicContest {
	public AnalystContest(IAccount account) {
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
		switch (cType) {
			case RUN: {
				IRun run = (IRun) obj;

				IJudgement j = getJudgementById(run.getJudgementId());
				if (j == null)
					return;

				if (isJudgementHidden(j))
					return;

				ISubmission s = getSubmissionById(j.getSubmissionId());
				if (s == null)
					return;

				// hide runs for submissions outside the contest time
				long time = s.getContestTime();
				if (time < 0 || time >= getDuration())
					return;

				// hide runs for submissions after freeze
				if (getFreezeDuration() != null) {
					long freezeTime = getDuration() - getFreezeDuration();
					if (s.getContestTime() >= freezeTime)
						return;
				}

				addIt(run);
				return;
			}
			case CLARIFICATION: {
				addIt(obj);
				return;
			}
			default:
				break;
		}
		super.add(obj);
	}
}