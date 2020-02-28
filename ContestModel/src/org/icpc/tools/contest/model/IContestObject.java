package org.icpc.tools.contest.model;

import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.internal.Award;
import org.icpc.tools.contest.model.internal.Clarification;
import org.icpc.tools.contest.model.internal.Commentary;
import org.icpc.tools.contest.model.internal.Deletion;
import org.icpc.tools.contest.model.internal.Group;
import org.icpc.tools.contest.model.internal.Info;
import org.icpc.tools.contest.model.internal.Judgement;
import org.icpc.tools.contest.model.internal.JudgementType;
import org.icpc.tools.contest.model.internal.Language;
import org.icpc.tools.contest.model.internal.Organization;
import org.icpc.tools.contest.model.internal.Pause;
import org.icpc.tools.contest.model.internal.Problem;
import org.icpc.tools.contest.model.internal.Run;
import org.icpc.tools.contest.model.internal.StartStatus;
import org.icpc.tools.contest.model.internal.State;
import org.icpc.tools.contest.model.internal.Submission;
import org.icpc.tools.contest.model.internal.Team;
import org.icpc.tools.contest.model.internal.TeamMember;

public interface IContestObject {
	enum ContestType {
		CONTEST, LANGUAGE, GROUP, ORGANIZATION, TEAM, STATE, RUN, SUBMISSION, JUDGEMENT, CLARIFICATION, AWARD, JUDGEMENT_TYPE, TEST_DATA, PROBLEM, PAUSE, TEAM_MEMBER, PRINTER, START_STATUS, COMMENTARY, DELETE
	}

	String[] ContestTypeNames = new String[] { "contests", "languages", "groups", "organizations", "teams", "state",
			"runs", "submissions", "judgements", "clarifications", "awards", "judgement-types", "testdata", "problems",
			"pause", "team-members", "printers", "start-status", "commentary", "delete" };

	static String getTypeName(ContestType type) {
		return ContestTypeNames[type.ordinal()];
	}

	static ContestType getTypeByName(String typeName) {
		for (int i = 0; i < ContestTypeNames.length - 1; i++) {
			if (ContestTypeNames[i].equals(typeName))
				return ContestType.values()[i];
		}
		return null;
	}

	static IContestObject createByName(String typeName) {
		return createByType(getTypeByName(typeName));
	}

	static IContestObject createByType(ContestType type) {
		if (type == null)
			return null;

		if (ContestType.CONTEST.equals(type))
			return new Info();
		else if (ContestType.STATE.equals(type))
			return new State();
		else if (ContestType.TEAM.equals(type))
			return new Team();
		else if (ContestType.TEAM_MEMBER.equals(type))
			return new TeamMember();
		else if (ContestType.PROBLEM.equals(type))
			return new Problem();
		else if (ContestType.GROUP.equals(type))
			return new Group();
		else if (ContestType.ORGANIZATION.equals(type))
			return new Organization();
		else if (ContestType.JUDGEMENT_TYPE.equals(type))
			return new JudgementType();
		else if (ContestType.SUBMISSION.equals(type))
			return new Submission();
		else if (ContestType.JUDGEMENT.equals(type))
			return new Judgement();
		else if (ContestType.RUN.equals(type))
			return new Run();
		else if (ContestType.LANGUAGE.equals(type))
			return new Language();
		else if (ContestType.AWARD.equals(type))
			return new Award();
		else if (ContestType.CLARIFICATION.equals(type))
			return new Clarification();
		else if (ContestType.START_STATUS.equals(type))
			return new StartStatus();
		else if (ContestType.PAUSE.equals(type))
			return new Pause();
		else if (ContestType.COMMENTARY.equals(type))
			return new Commentary();
		else if (ContestType.DELETE.equals(type))
			return new Deletion();

		// don't import unrecognized elements
		return null;
	}

	ContestType getType();

	Object getProperty(String s);

	Map<String, Object> getProperties();

	/**
	 * The unique id.
	 *
	 * @return the id
	 */
	String getId();

	/**
	 * Validate this contest object within the scope of the given contest (to check for id
	 * references). Returns a list of errors, or null if there weren't any.
	 */
	List<String> validate(IContest contest);

	default Object resolveFileReference(String url) {
		return null;
	}
}