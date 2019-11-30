package org.icpc.tools.contest.model;

/**
 * A clarification request or response.
 */
public interface IClarification extends IContestObject {
	/**
	 * The id of the clarification this is a reply to, or null if this is a question.
	 *
	 * @return the id
	 */
	String getReplyToId();

	/**
	 * The id of the team asking this question, may be null if sent from staff.
	 *
	 * @return the id
	 */
	String getFromTeamId();

	/**
	 * The id of the team receiving this reply, may be null if sent to staff. If both from and to
	 * are null, this is a broadcast from staff to all teams.
	 *
	 * @return the id
	 */
	String getToTeamId();

	/**
	 * The id of the associated problem, may be null.
	 *
	 * @return the id
	 */
	String getProblemId();

	/**
	 * The text of the question or reply.
	 *
	 * @return the text
	 */
	String getText();

	/**
	 * Returns the contest time relative to the start of the contest, in ms.
	 *
	 * @return the contest time
	 */
	int getContestTime();

	/**
	 * Returns the wall clock time, in ms since the epoch.
	 *
	 * @return the time
	 */
	long getTime();
}