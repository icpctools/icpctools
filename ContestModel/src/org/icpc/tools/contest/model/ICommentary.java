package org.icpc.tools.contest.model;

/**
 * A commentary.
 */
public interface ICommentary extends IContestObject {
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

	/**
	 * Returns the ids of the problems that this commentary is for.
	 *
	 * @return the team id
	 */
	String[] getProblemIds();

	/**
	 * Returns the ids of the teams that this commentary is for.
	 *
	 * @return the team id
	 */
	String[] getTeamIds();

	/**
	 * The message of the commentary.
	 *
	 * @return the message
	 */
	String getMessage();
}