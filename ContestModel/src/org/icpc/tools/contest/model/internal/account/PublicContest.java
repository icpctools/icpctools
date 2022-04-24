package org.icpc.tools.contest.model.internal.account;

import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IClarification;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IPerson;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IState;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Account;
import org.icpc.tools.contest.model.internal.Contest;

/**
 * Public filter: information that everyone can see, regardless of role (including teams and
 * spectators) or whether they've logged in.
 *
 * This filter removes:
 * <ul>
 * <li>Accounts (but subclasses can set username to allow their own account through)</li>
 * <li>Problems before contest start</li>
 * <li>Hidden groups & teams (and all associated data)</li>
 * <li>Clarifications (except broadcasts)</li>
 * <li>Judgemewnts and commentary after the freeze</li>
 * <li>Test_data</li>
 * <li>Runs</li>
 * <li>Commentary</li>
 * </ul>
 */
public class PublicContest extends Contest {
	protected String username;

	protected List<IProblem> problems = new ArrayList<IProblem>();

	public PublicContest() {
		// do nothing
	}

	@Override
	public void add(IContestObject obj) {
		IContestObject.ContestType cType = obj.getType();

		switch (cType) {
			// all of these are fully public
			case CONTEST:
			case LANGUAGE:
			case JUDGEMENT_TYPE:
			case MAP_INFO:
			case START_STATUS:
			case AWARD:
			case ORGANIZATION:
				super.add(obj);
				return;
			case PROBLEM: {
				if (getState().getStarted() == null)
					problems.add((IProblem) obj);
				else
					super.add(obj);
				return;
			}
			case STATE: {
				IState state = (IState) obj;
				super.add(state);
				// TODO out of order!
				if (state.getStarted() != null && getProblems().length == 0) {
					for (IProblem p : problems) {
						super.add(p);
					}
				}
				return;
			}
			case GROUP: {
				IGroup group = (IGroup) obj;
				if (!group.isHidden())
					super.add(group);
				return;
			}
			case TEAM: {
				ITeam team = (ITeam) obj;
				if (!isTeamHidden(team))
					super.add(team);
				return;
			}
			case ACCOUNT: {
				// everyone only sees their own account (public has no account)
				IAccount account = (IAccount) obj;
				if (username == null || !username.equals(account.getUsername()))
					return;

				if (account.getPassword() != null) {
					account = (IAccount) ((Account) account).clone();
					((Account) account).add("password", null);
				}
				super.add(account);
				return;
			}
			case PERSON: {
				IPerson person = (IPerson) obj;
				ITeam team = getTeamById(person.getTeamId());
				if (!isTeamHidden(team))
					super.add(person);
				return;
			}
			case SUBMISSION: {
				ISubmission sub = (ISubmission) obj;

				// hide submissions from outside the contest time
				int time = sub.getContestTime();
				if (time < 0 || time >= getDuration())
					return;

				// hide submissions from hidden teams
				ITeam team = getTeamById(sub.getTeamId());
				if (isTeamHidden(team))
					return;

				super.add(sub);
				return;
			}
			case JUDGEMENT: {
				IJudgement j = (IJudgement) obj;

				ISubmission s = getSubmissionById(j.getSubmissionId());
				if (s == null)
					return;

				// hide judgements for submissions outside the contest time
				int time = s.getContestTime();
				if (time < 0 || time >= getDuration())
					return;

				// or during the freeze
				if (getFreezeDuration() != null) {
					int freezeTime = getDuration() - getFreezeDuration();
					if (time >= freezeTime)
						return;
				}

				// don't show judgements for hidden teams
				if (isJudgementHidden(j))
					return;

				super.add(j);
				return;
			}
			case CLARIFICATION: {
				IClarification clar = (IClarification) obj;

				// everyone sees broadcasts
				if (clar.getFromTeamId() == null && clar.getToTeamId() == null)
					super.add(clar);

				return;
			}

			// no test data, runs, or commentary
			default:
				return;
		}
	}

	/**
	 * Helper method for subclasses to add objects directly to the contest.
	 *
	 * @param obj
	 */
	protected void addIt(IContestObject obj) {
		super.add(obj);
	}

	protected boolean isJudgementHidden(IJudgement j) {
		if (j == null)
			return false;

		ISubmission sub = getSubmissionById(j.getSubmissionId()); // TODO duplication?
		if (sub == null)
			return false;

		ITeam team = getTeamById(sub.getTeamId());
		return isTeamHidden(team);
	}
}