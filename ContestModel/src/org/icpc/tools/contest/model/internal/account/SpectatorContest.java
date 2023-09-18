package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Problem;
import org.icpc.tools.contest.model.internal.Submission;

/**
 * Filter that adds things spectators can see compared to public/team area:
 * <ul>
 * <li>Problem test data count</li>
 * <li>Team desktop, webcams (until the freeze)</li>
 * <li>Team tool data, key log</li>
 * <li>Submission language</li>
 * <li>Submission reaction videos (until the freeze)</li>
 * <li>Commentary</li>
 * </ul>
 */
public class SpectatorContest extends PublicContest {
	public SpectatorContest(IAccount account) {
		super();
		username = account.getUsername();
	}

	@Override
	public void add(IContestObject obj) {
		IContestObject.ContestType cType = obj.getType();
		if (obj instanceof IDelete) {
			addIt(obj);
			return;
		}

		switch (cType) {
			case SUBMISSION: {
				ISubmission sub = (ISubmission) obj;

				// hide submissions from outside the contest time
				long time = sub.getContestTime();
				if (time < 0 || time >= getDuration())
					return;

				// hide submissions from hidden teams
				ITeam team = getTeamById(sub.getTeamId());
				if (isTeamHidden(team))
					return;

				// TODO - language
				super.add(sub);
				return;
			}
			case COMMENTARY: {
				addIt(obj);
				return;
			}
			default: {
				super.add(obj);
			}
		}
	}

	@Override
	protected IProblem filterProblem(IProblem problem) {
		Problem p = (Problem) ((Problem) problem).clone();
		p.setPackage(null);
		return p;
	}

	@Override
	protected ISubmission filterSubmission(ISubmission sub) {
		Submission s = (Submission) ((Submission) sub).clone();
		s.setFiles(null);
		s.add("entry_point", null);

		if (!isBeforeFreeze(s))
			s.setReaction(null);
		return s;
	}

	@Override
	public boolean allowFileReference(IContestObject obj, String property) {
		switch (obj.getType()) {
			case TEAM: {
				if ("desktop".equals(property) || "webcam".equals(property) || "audio".equals(property))
					return !this.getState().isFrozen();

				if ("tool_data".equals(property) || "key_log".equals(property))
					return true;
				return super.allowFileReference(obj, property);
			}
			case SUBMISSION: {
				ISubmission s = (ISubmission) obj;
				if ("reaction".equals(property)) {
					return this.isBeforeFreeze(s);
				}
				return super.allowFileReference(obj, property);
			}

			default:
				return super.allowFileReference(obj, property);
		}
	}
}