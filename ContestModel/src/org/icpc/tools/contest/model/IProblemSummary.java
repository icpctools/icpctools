package org.icpc.tools.contest.model;

/**
 * Summary information of all team's submissions on a specific problem.
 */
public interface IProblemSummary {
	/**
	 * Return the total number of submissions pending judgment.
	 *
	 * @return the number of submissions pending judgment
	 */
	int getNumPending();

	/**
	 * Return the time in ms of the most recent submission, or 0 if there is none.
	 *
	 * @return the submission time, in ms
	 */
	long getPendingContestTime();

	/**
	 * Return the number of failed submissions.
	 *
	 * @return the number of failed submissions
	 */
	int getNumFailed();

	/**
	 * Return the time in ms of the most recent failed submission, or 0 if there is none.
	 *
	 * @return the submission time, in ms
	 */
	long getFailedContestTime();

	/**
	 * Return the number of solved submissions.
	 *
	 * @return the number of solved submissions
	 */
	int getNumSolved();

	/**
	 * Return the time in ms of the most recent accepted submission, or 0 if there is none.
	 *
	 * @return the submission time, in ms
	 */
	long getSolvedContestTime();

	/**
	 * Return the total number of submissions.
	 *
	 * @return the total number of submissions
	 */
	int getNumSubmissions();
}