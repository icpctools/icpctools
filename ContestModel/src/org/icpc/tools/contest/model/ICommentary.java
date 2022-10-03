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
	 * Returns the ids of the team(s) that this commentary is for.
	 *
	 * @return the team id
	 */
	String[] getTeamIds();

	/**
	 * Returns the ids of the problem(s) that this commentary is for.
	 *
	 * @return the problem ids
	 */
	String[] getProblemIds();

	/**
	 * Returns the ids of the submission(s) that this commentary is for.
	 *
	 * @return the submission ids
	 */
	String[] getSubmissionIds();

	/**
	 * The message of the commentary.
	 *
	 * @return the message
	 */
	String getMessage();
}