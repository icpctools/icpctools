package org.icpc.tools.contest.model.internal.account;

import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IClarification;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IPerson;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IState;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Account;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.Person;

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
 * <li>Awards, except for first to solve awards before the freeze</li>
 * <li>Test_data</li>
 * <li>Runs</li>
 * <li>Commentary</li>
 * </ul>
 */
public class PublicContest extends Contest {
	private static final String EMAIL = "email";
	private static final String SEX = "sex";

	protected String username;

	protected List<IProblem> problems = new ArrayList<>();
	// TODO - simplified judgement types deferred
	// protected Map<String, String> judgementTypeMap = new HashMap<>();

	protected List<IContestObject> freeze = new ArrayList<>();

	public PublicContest() {
		// do nothing
	}

	@Override
	public void add(IContestObject obj) {
		IContestObject.ContestType cType = obj.getType();
		if (obj instanceof IDelete) {
			if (cType.equals(IContestObject.ContestType.PROBLEM) && !problems.isEmpty()) {
				IProblem remove = null;
				for (IProblem p : problems) {
					if (p.getId().equals(obj.getId()))
						remove = p;
				}
				if (remove != null)
					problems.remove(remove);
				return;
			}
			super.add(obj);
			return;
		}

		switch (cType) {
			// all of these are fully public
			case CONTEST:
			case LANGUAGE:
			case MAP_INFO:
			case RESOLVE_INFO:
			case START_STATUS:
			case ORGANIZATION:
				super.add(obj);
				return;
			case JUDGEMENT_TYPE: {
				/*IJudgementType jt = (IJudgementType) obj;
				if (jt.getSimplifiedId() == null) {
					judgementTypeMap.remove(jt.getId());
					super.add(obj);
				} else
					judgementTypeMap.put(jt.getId(), jt.getSimplifiedId());*/
				super.add(obj);
				return;
			}
			case PROBLEM: { // TODO - access block - problem package, test data
				// TODO - teams should not get sample data
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
					problems.clear();
				}
				if (state.getThawed() != null) {
					for (IContestObject co : freeze) {
						super.add(co);
					}
					freeze.clear();
				}
				return;
			}
			case GROUP: {
				IGroup group = (IGroup) obj;
				if (!group.isHidden())
					super.add(group);
				return;
			}
			case TEAM: { // TODO - access block tool-data, keylog
				// TODO - backups
				// TODO - teams should not have access to webcam, audio, etc
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
			case PERSON: { // TODO - access block strip sex, email
				IPerson person = (IPerson) obj;
				ITeam team = getTeamById(person.getTeamId());
				if (isTeamHidden(team))
					return;

				if (person.getEmail() != null) {
					person = (IPerson) ((Person) person).clone();
					((Person) person).add(EMAIL, null);
					((Person) person).add(SEX, null);
				}
				super.add(person);
				return;
			}
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
			case JUDGEMENT: { // TODO - access block - max runtime
				IJudgement j = (IJudgement) obj;

				ISubmission s = getSubmissionById(j.getSubmissionId());
				if (s == null)
					return;

				// hide judgements for submissions outside the contest time
				long time = s.getContestTime();
				if (time < 0 || time >= getDuration())
					return;

				// don't show judgements for hidden teams
				if (isJudgementHidden(j))
					return;

				// or during the freeze
				if (getFreezeDuration() != null) {
					long freezeTime = getDuration() - getFreezeDuration();
					if (time >= freezeTime) {
						freeze.add(j);
						return;
					}
				}

				// TODO - max run time

				/*String jtId = j.getJudgementTypeId();
				if (judgementTypeMap.containsKey(jtId)) {
					// TODO not for actual team
					IJudgement sj = (IJudgement) ((Judgement) j).clone();
					((Judgement) sj).add("judgement_type_id", judgementTypeMap.get(jtId));
					super.add(sj);
					// super.add(j);
				} else*/
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
			case AWARD: {
				IAward award = (IAward) obj;
				if (award.getAwardType() != IAward.FIRST_TO_SOLVE)
					return;

				IState state = getState();
				if (state.isFrozen())
					return;

				super.add(obj);
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