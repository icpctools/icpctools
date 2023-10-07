package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IPerson;
import org.icpc.tools.contest.model.IRun;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.internal.Person;
import org.icpc.tools.contest.model.internal.Submission;

/**
 * Filter that adds things analysts can see compared to spectators:
 * <ul>
 * <li>Runs (until the freeze)</li>
 * <li>Person emails</li>
 * <li>Submission files (for submissions before the freeze)</li>
 * <li>Judgement max runtime</li>
 * <li>Team backups, tool data, and key logs (until the freeze)</li>
 * </ul>
 */
public class AnalystContest extends SpectatorContest {
	public AnalystContest(IAccount account) {
		super(account);
	}

	@Override
	public void add(IContestObject obj) {
		if (obj instanceof IDelete) {
			addIt(obj);
			return;
		}

		IContestObject.ContestType cType = obj.getType();
		switch (cType) {
			case RUN: { // TODO - access block for live
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
			default:
				super.add(obj);
		}
	}

	@Override
	protected ISubmission filterSubmission(ISubmission sub) {
		Submission s = (Submission) ((Submission) sub).clone();

		if (!isBeforeFreeze(s)) {
			s.setFiles(null);
			s.add("entry_point", null);
			s.setReaction(null);
		}
		return s;
	}

	@Override
	protected IPerson filterPerson(IPerson person) {
		Person p = (Person) ((Person) person).clone();
		p.add("email", null);
		return p;
	}

	@Override
	protected IJudgement filterJudgement(IJudgement jud) {
		return jud;
	}

	@Override
	public boolean allowFileReference(IContestObject obj, String property) {
		switch (obj.getType()) {
			case TEAM: {
				if ("backup".equals(property) || "tool_data".equals(property) || "key_log".equals(property))
					return !this.getState().isFrozen();

				return super.allowFileReference(obj, property);
			}
			case SUBMISSION: {
				ISubmission s = (ISubmission) obj;
				if ("files".equals(property)) {
					return this.isBeforeFreeze(s);
				}
				return super.allowFileReference(obj, property);
			}
			default:
				return true;
		}
	}
}