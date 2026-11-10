package org.icpc.tools.contest.model;

public interface IResolverPresentation extends IContestObject {
	public enum Presentations {
		SPLASH, SCOREBOARD, JUDGE, TEAM_AWARD, TEAM_LIST, TEAM_PHOTOS, MESSAGE
	}

	/**
	 * Returns the kind.
	 *
	 * @return the kind
	 */
	String getKind();

	/**
	 * Returns the selected team id.
	 *
	 * @return the team id
	 */
	String getTeamId();

	/**
	 * Returns the award id, may be null.
	 *
	 * @return the award id
	 */
	String getAwardId();

	/**
	 * Returns the message, may be null.
	 *
	 * @return the message
	 */
	String getMessage();

	/**
	 * Returns the scroll, may be null.
	 *
	 * @return the scroll
	 */
	Boolean getScroll();

	/**
	 * Returns the submission ids, may be null.
	 *
	 * @return the submission id
	 */
	String[] getSubmissionIds();
}
