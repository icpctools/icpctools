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
	public int getNumPending();

	/**
	 * Return the number of failed submissions.
	 *
	 * @return the number of failed submissions
	 */
	public int getNumFailed();

	/**
	 * Return the number of solved submissions.
	 *
	 * @return the number of solved submissions
	 */
	public int getNumSolved();

	/**
	 * Return the total number of submissions.
	 *
	 * @return the total number of submissions
	 */
	public int getNumSubmissions();

	/**
	 * Return the time in ms of the first solution, or if the problem hasn't been solved, the time
	 * of the most recent submission.
	 *
	 * @return the submission time, in ms
	 */
	public int getContestTime();

	/**
	 * Returns the overall status of the submissions for this problem (i.e. SOLVED if at least one
	 * submission was a correct solution).
	 *
	 * @return the status
	 */
	public Status getStatus();
}