package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IRun;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.internal.Problem;

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
					if (s.getContestTime() >= freezeTime && getState().getThawed() == null) {
						freeze.add(run);
						return;
					}
				}

				addIt(run);
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
	public boolean allowProperty(IContestObject obj, String property) {
		switch (obj.getType()) {
			case TEAM: {
				if (property.startsWith("desktop") || property.startsWith("webcam") || property.startsWith("audio")
						|| property.startsWith("tool_data") || property.startsWith("key_log")) {
					return this.getState().getStarted() != null && !this.getState().isFrozen();
				}
				return super.allowProperty(obj, property);
			}
			case SUBMISSION: {
				ISubmission s = (ISubmission) obj;
				if (property.startsWith("reaction")) {
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
				if (property.startsWith("desktop") || property.startsWith("webcam") || property.startsWith("audio")
						|| property.startsWith("tool_data") || property.startsWith("key_log"))
					return true;

				return super.canAccessProperty(type, property);
			}
			case SUBMISSION: {
				if (property.startsWith("reaction")) {
					return true;
				}
				return super.canAccessProperty(type, property);
			}
			default:
				return super.canAccessProperty(type, property);
		}
	}
}
