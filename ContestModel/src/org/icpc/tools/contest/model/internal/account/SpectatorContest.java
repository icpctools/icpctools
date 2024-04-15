package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
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
				if (property.startsWith("desktop") ||
						property.startsWith("webcam") ||
						property.startsWith("audio") ||
						property.startsWith("tool_data") ||
						property.startsWith("key_log"))
					return this.getState().getStarted() != null && !this.getState().isFrozen();

				return super.allowFileReference(obj, property);
			}
			case SUBMISSION: {
				ISubmission s = (ISubmission) obj;
				if (property.startsWith("reaction")) {
					return this.isBeforeFreeze(s);
				}
				return super.allowFileReference(obj, property);
			}

			default:
				return super.allowFileReference(obj, property);
		}
	}
}
