package org.icpc.tools.contest.model;

import java.io.File;

/**
 * A problem submission by a team in the contest. The id is typically a small number.
 */
public interface ISubmission extends IContestObject {
	/**
	 * Returns the problem that this submission is for.
	 *
	 * @return the problem id
	 */
	String getProblemId();

	/**
	 * Returns the language that this submission is in.
	 *
	 * @return the language id
	 */
	String getLanguageId();

	/**
	 * Returns the id of the team that made this submission.
	 *
	 * @return the team id
	 */
	String getTeamId();

	/**
	 * Returns the id of the team member that made this submission.
	 *
	 * @return the team member id
	 */
	String getTeamMemberId();

	/**
	 * Returns the optional entry point this submission.
	 *
	 * @return the entry point
	 */
	String getEntryPoint();

	/**
	 * Returns a zip of the files submitted in this submission.
	 *
	 * @return a zip of the files
	 */
	File getFiles(boolean force);

	/**
	 * Returns the reaction video.
	 *
	 * @return the reaction video
	 */
	File getReaction(boolean force);

	/**
	 * Returns the reaction video.
	 *
	 * @return the reaction video
	 */
	File[] getReactions(boolean force);

	/**
	 * Return the URL to the reaction video
	 *
	 * @return
	 */
	String getReactionURL();

	/**
	 * Return the URLs to the reaction videos
	 *
	 * @return
	 */
	String[] getReactionURLs();

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