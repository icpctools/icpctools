package org.icpc.tools.contest.model;

import java.io.File;

import org.icpc.tools.contest.model.internal.FileReferenceList;

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
	 * Returns the id of the account that made this submission.
	 *
	 * @return the account id
	 */
	String getAccountId();

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
	FileReferenceList getFiles();

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
	FileReferenceList getReaction();

	/**
	 * Returns the reaction video.
	 *
	 * @return the reaction video
	 */
	File getReaction(boolean force);

	/**
	 * Return the URL to the reaction video
	 *
	 * @return
	 */
	String getReactionURL();

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