package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IRun;

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
				if (isJudgementHidden(j))
					return;

				// hide runs after freeze
				if (getFreezeDuration() != null) {
					int freezeTime = getDuration() - getFreezeDuration();
					if (run.getContestTime() >= freezeTime)
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