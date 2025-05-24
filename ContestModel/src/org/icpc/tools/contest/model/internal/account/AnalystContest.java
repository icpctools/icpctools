package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IPerson;
import org.icpc.tools.contest.model.IRun;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.internal.Person;

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
	public boolean allowProperty(IContestObject obj, String property) {
		switch (obj.getType()) {
			case TEAM: {
				if (property.startsWith("backup") || property.startsWith("tool_data") || property.startsWith("key_log"))
					return (this.getState().getStarted() != null && !this.getState().isFrozen());

				return super.allowProperty(obj, property);
			}
			case SUBMISSION: {
				ISubmission s = (ISubmission) obj;
				if (property.startsWith("entry_point") || property.startsWith("files") || property.startsWith("reaction")) {
					return this.isBeforeFreeze(s);
				}
				return super.allowProperty(obj, property);
			}
			default:
				return super.allowProperty(obj, property);
		}
	}

	@Override
	public boolean canAccessProperty(IContestObject.ContestType type, String property) {
		switch (type) {
			case TEAM: {
				if (property.startsWith("backup"))
					return true;

				return super.canAccessProperty(type, property);
			}
			case SUBMISSION: {
				if (property.startsWith("entry_point") || property.startsWith("files") || property.startsWith("reaction")) {
					return true;
				}
				return super.canAccessProperty(type, property);
			}
			default:
				return super.canAccessProperty(type, property);
		}
	}
}
