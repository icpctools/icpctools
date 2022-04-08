package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IClarification;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.ISubmission;

/**
 * Filter that adds things teams can see compared to public:
 * <ul>
 * <li>Their own submissions and judgements (even if outside contest time)</li>
 * <li>Clarifications to or from the team, broadcasts</li>
 * </ul>
 *
 * and removes commentary.
 */
public class TeamContest extends PublicContest {
	private String teamId;

	public TeamContest(IAccount account) {
		super();
		username = account.getUsername();
		teamId = account.getTeamId();
	}

	@Override
	public void add(IContestObject obj) {
		IContestObject.ContestType cType = obj.getType();

		switch (cType) {
			case SUBMISSION: {
				ISubmission sub = (ISubmission) obj;

				// teams always see their own submissions
				if (teamId.equals(sub.getTeamId())) {
					addIt(sub);
					return;
				}

				break;
			}
			case JUDGEMENT: {
				IJudgement j = (IJudgement) obj;

				// teams always see their own judgements
				ISubmission s = super.getSubmissionById(j.getSubmissionId());
				if (s == null)
					return;

				if (teamId.equals(s.getTeamId())) {
					addIt(j);
					return;
				}

				break;
			}
			case CLARIFICATION: {
				IClarification clar = (IClarification) obj;

				// teams see messages to or from them
				if (clar.getFromTeamId() != null && teamId.equals(clar.getFromTeamId())) {
					addIt(clar);
					return;
				}

				if (clar.getToTeamId() != null && teamId.equals(clar.getToTeamId())) {
					addIt(clar);
					return;
				}

				break;
			}
			default:
				break;
		}
		super.add(obj);
	}
}