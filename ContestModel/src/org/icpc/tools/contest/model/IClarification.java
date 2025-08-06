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
	 * The id of the team making the clarification, may be null if sent from staff.
	 *
	 * @return the id
	 */
	String getFromTeamId();

	/**
	 * The id of the team(s) receiving this reply, may be null.
	 *
	 * @return the ids
	 */
	String[] getToTeamIds();

	/**
	 * The id of the group(s) receiving this reply, may be null.
	 *
	 * @return the ids
	 */
	String[] getToGroupIds();

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
	 * Returns true if this is a broadcast (sent to everyone)
	 *
	 * @return true if this is a broadcast
	 */
	boolean isBroadcast();

	/**
	 * Returns the contest time relative to the start of the contest, in ms.
	 *
	 * @return the contest time
	 */
	long getContestTime();

	/**
	 * Returns the wall clock time, in ms since the epoch.
	 *
	 * @return the time
	 */
	long getTime();
}