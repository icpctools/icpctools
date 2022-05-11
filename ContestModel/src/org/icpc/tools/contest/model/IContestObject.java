package org.icpc.tools.contest.model;

import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.internal.Account;
import org.icpc.tools.contest.model.internal.Award;
import org.icpc.tools.contest.model.internal.Clarification;
import org.icpc.tools.contest.model.internal.Commentary;
import org.icpc.tools.contest.model.internal.Group;
import org.icpc.tools.contest.model.internal.Info;
import org.icpc.tools.contest.model.internal.Judgement;
import org.icpc.tools.contest.model.internal.JudgementType;
import org.icpc.tools.contest.model.internal.Language;
import org.icpc.tools.contest.model.internal.MapInfo;
import org.icpc.tools.contest.model.internal.Organization;
import org.icpc.tools.contest.model.internal.Pause;
import org.icpc.tools.contest.model.internal.Person;
import org.icpc.tools.contest.model.internal.Problem;
import org.icpc.tools.contest.model.internal.Run;
import org.icpc.tools.contest.model.internal.StartStatus;
import org.icpc.tools.contest.model.internal.State;
import org.icpc.tools.contest.model.internal.Submission;
import org.icpc.tools.contest.model.internal.Team;

public interface IContestObject {
	enum ContestType {
		CONTEST, LANGUAGE, GROUP, ORGANIZATION, TEAM, STATE, RUN, SUBMISSION, JUDGEMENT, CLARIFICATION, AWARD, JUDGEMENT_TYPE, TEST_DATA, PROBLEM, PAUSE, PERSON, MAP_INFO, START_STATUS, COMMENTARY, ACCOUNT
	}

	String[] ContestTypeNames = new String[] { "contests", "languages", "groups", "organizations", "teams", "state",
			"runs", "submissions", "judgements", "clarifications", "awards", "judgement-types", "testdata", "problems",
			"pause", "persons", "map-info", "start-status", "commentary", "accounts" };

	static String getTypeName(ContestType type) {
		return ContestTypeNames[type.ordinal()];
	}

	static ContestType getTypeByName(String typeName) {
		for (int i = 0; i < ContestTypeNames.length; i++) {
			if (ContestTypeNames[i].equals(typeName))
				return ContestType.values()[i];
		}
		return null;
	}

	static boolean isSingleton(ContestType type) {
		return type != null && (ContestType.STATE.equals(type) || ContestType.MAP_INFO.equals(type));
	}

	static IContestObject createByName(String typeName) {
		return createByType(getTypeByName(typeName));
	}

	static IContestObject createByType(ContestType type) {
		if (type == null)
			return null;

		switch (type) {
			case CONTEST:
				return new Info();
			case STATE:
				return new State();
			case GROUP:
				return new Group();
			case ORGANIZATION:
				return new Organization();
			case TEAM:
				return new Team();
			case PERSON:
				return new Person();
			case ACCOUNT:
				return new Account();
			case JUDGEMENT_TYPE:
				return new JudgementType();
			case LANGUAGE:
				return new Language();
			case PROBLEM:
				return new Problem();
			case SUBMISSION:
				return new Submission();
			case JUDGEMENT:
				return new Judgement();
			case RUN:
				return new Run();
			case CLARIFICATION:
				return new Clarification();
			case COMMENTARY:
				return new Commentary();
			case AWARD:
				return new Award();
			case START_STATUS:
				return new StartStatus();
			case PAUSE:
				return new Pause();
			case MAP_INFO:
				return new MapInfo();
			default:
				// don't create unrecognized elements
				return null;
		}
	}

	ContestType getType();

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