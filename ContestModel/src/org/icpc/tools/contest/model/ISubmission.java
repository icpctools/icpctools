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
	public String getProblemId();

	/**
	 * Returns the language that this submission is in.
	 *
	 * @return the language id
	 */
	public String getLanguageId();

	/**
	 * Returns the id of the team that made this submission.
	 *
	 * @return the team
	 */
	public String getTeamId();

	/**
	 * Returns the optional entry point this submission.
	 *
	 * @return the entry point
	 */
	public String getEntryPoint();

	/**
	 * Returns a zip of the files submitted in this submission.
	 *
	 * @return a zip of the files
	 */
	public File getFiles(boolean force);

	/**
	 * Returns the reaction video.
	 *
	 * @return the reaction video
	 */
	public File getReaction(boolean force);

	/**
	 * Return the URL to the reaction video
	 * 
	 * @return
	 */
	public String getReactionURL();

	/**
	 * Returns the contest time relative to the start of the contest, in ms.
	 *
	 * @return the contest time
	 */
	public int getContestTime();

	/**
	 * Returns the wall clock time, in ms since the epoch.
	 *
	 * @return the time
	 */
	public long getTime();
}