package org.icpc.tools.contest.model.internal.account;

import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IClarification;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IPerson;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IState;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Account;
import org.icpc.tools.contest.model.internal.Clarification;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.Judgement;
import org.icpc.tools.contest.model.internal.Organization;
import org.icpc.tools.contest.model.internal.Person;
import org.icpc.tools.contest.model.internal.Problem;
import org.icpc.tools.contest.model.internal.Submission;
import org.icpc.tools.contest.model.internal.Team;

/**
 * Public filter: information that everyone can see, regardless of role (including teams and
 * spectators) or whether they've logged in.
 *
 * This filter removes:
 * <ul>
 * <li>Accounts (but subclasses can set username to allow their own account through)</li>
 * <li>Hidden groups & teams (and all associated data)</li>
 * <li>Team backups, tool data, and key logs</li>
 * <li>Email and sex of all persons</li>
 * <li>Problems before contest start (and no test data count or package after)</li>
 * <li>Submission files and entry point, language, and reaction videos</li>
 * <li>Judgemewnts after the freeze (and max runtime always)</li>
 * <li>Clarifications (except broadcasts)</li>
 * <li>Awards, except for first to solve awards before the freeze</li>
 * <li>Test_data</li>
 * <li>Runs</li>
 * <li>Commentary</li>
 * </ul>
 */
public class PublicContest extends Contest implements IFilteredContest {
	private static final String EMAIL = "email";
	private static final String SEX = "sex";

	protected String username;

	protected List<IProblem> problems = new ArrayList<>();

	// objects seen during the freeze that should be sent on thaw
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
				super.add(obj);
				return;
			case ORGANIZATION:
				super.add(filterOrganization((IOrganization) obj));
				return;
			case JUDGEMENT_TYPE: {
				super.add(obj);
				return;
			}
			case PROBLEM: {
				IProblem p = (IProblem) obj;
				if (getState().getStarted() == null) {
					p = filterProblem(p);
					problems.add(p);
				} else
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
				if (state.getThawed() != null && !freeze.isEmpty()) {
					thaw();
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
				if (!isTeamHidden(team)) {
					ITeam t = filterTeam(team);
					super.add(t);
				}
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
				if (isTeamHidden(team))
					return;

				if (person.getEmail() != null || person.getSex() != null) {
					IPerson p = filterPerson(person);
					addIt(p);
					return;
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

				sub = filterSubmission(sub);
				super.add(sub);
				return;
			}
			case JUDGEMENT: {
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
					if (time >= freezeTime && getState().getThawed() == null) {
						freeze.add(j);
						return;
					}
				}

				j = filterJudgement(j);
				super.add(j);
				return;
			}
			case CLARIFICATION: {
				IClarification clar = (IClarification) obj;

				// everyone sees broadcasts
				if (clar.getFromTeamId() == null && clar.getToTeamId() == null) {
					clar = filterClarification(clar);
					super.add(clar);
				}

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

	protected void thaw() {
		for (IContestObject co : freeze) {
			// send objects back through add() to ensure filtering
			add(co);
		}
		freeze.clear();
	}

	/**
	 * Helper method for subclasses to add objects directly to the contest.
	 *
	 * @param obj
	 */
	protected void addIt(IContestObject obj) {
		super.add(obj);
	}

	/**
	 * Helper method for subclasses to filter problems.
	 */
	protected IProblem filterProblem(IProblem problem) {
		Problem p = (Problem) ((Problem) problem).clone();
		p.add("test_data_count", null);
		p.setPackage(null);
		return p;
	}

	/**
	 * Helper method for subclasses to filter persons.
	 */
	protected IPerson filterPerson(IPerson person) {
		Person p = (Person) ((Person) person).clone();
		p.add(EMAIL, null);
		p.add(SEX, null);
		return p;
	}

	/**
	 * Helper method for subclasses to filter clarifications.
	 */
	protected IClarification filterClarification(IClarification clar) {
		String rtId = clar.getReplyToId();
		if (getClarificationById(rtId) == null) {
			// strip reply_to_id if this account can't see it
			// (e.g. in case a response to a team was broadcast to everyone)
			Clarification c = (Clarification) ((Clarification) clar).clone();
			c.add("reply_to_id", null);
			return c;
		}
		return clar;
	}

	/**
	 * Helper method for subclasses to filter submissions.
	 */
	protected ISubmission filterSubmission(ISubmission sub) {
		Submission s = (Submission) ((Submission) sub).clone();
		String[] properties = new String[] { "language_id", "entry_point", "files", "reaction" };
		for (String property : properties) {
			if (!canAccessProperty(ContestType.SUBMISSION, property) || !allowProperty(s, property)) {
				s.add(property, null);
			}
		}
		return s;
	}

	/**
	 * Helper method to filter organizations.
	 */
	protected IOrganization filterOrganization(IOrganization org) {
		String hideCountryInfo = System.getProperty("hide-country-info");
		if (hideCountryInfo == null) {
			hideCountryInfo = System.getenv("HIDE_COUNTRY_INFO");
		}
		if (Boolean.parseBoolean(hideCountryInfo)) {
			Organization o = (Organization) ((Organization) org).clone();
			o.add("country", null);
			o.setCountryFlag(null);
			return o;
		}

		return org;
	}

	/**
	 * Helper method to filter team.
	 */
	protected ITeam filterTeam(ITeam team) {
		Team t = (Team) ((Team) team).clone();
		// all file references on non-timed object, only remove if they can never be accessed
		String[] properties = new String[] { "desktop", "webcam", "audio", "backup", "key_log", "tool_data" };
		for (String property : properties) {
			if (canAccessProperty(ContestType.TEAM, property)) {
				t.add(property, null);
			}
		}

		return t;
	}

	/**
	 * Helper method for subclasses to filter judgements.
	 */
	protected IJudgement filterJudgement(IJudgement jud) {
		Judgement j = (Judgement) ((Judgement) jud).clone();
		j.add("max_run_time", null);
		return j;
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

	@Override
	public boolean allowProperty(IContestObject obj, String property) {
		if (!canAccessProperty(obj.getType(), property))
			return false;

		return true;
	}

	@Override
	public boolean canAccessProperty(IContestObject.ContestType type, String property) {
		switch (type) {
			case TEAM: {
				if (property.startsWith("desktop") || property.startsWith("webcam") || property.startsWith("audio")
						|| property.startsWith("backup") || property.startsWith("tool_data")
						|| property.startsWith("key_log"))
					return false;
				return true;
			}
			case PROBLEM: {
				if (property.startsWith("package") || property.startsWith("test_data_count"))
					return false;
				return true;
			}
			case SUBMISSION: {
				if (property.startsWith("language_id") || property.startsWith("entry_point") || property.startsWith("files")
						|| property.startsWith("reaction"))
					return false;
				return true;
			}
			default:
				return true;
		}
	}
}
