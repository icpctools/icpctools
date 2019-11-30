package org.icpc.tools.contest.model;

/**
 * A result tracks the current state for a specific team working on a specific problem.
 */
public interface IResult {
	/**
	 * Return the total number of submissions.
	 *
	 * @return the total number of submissions
	 */
	int getNumSubmissions();

	/**
	 * Return the total number of submissions pending judgment.
	 *
	 * @return the number of submissions pending judgment
	 */
	int getNumPending();

	/**
	 * Return the number of submissions that have been judged.
	 *
	 * @return the number of judged submissions
	 */
	int getNumJudged();

	/**
	 * If this result was the first one to solve the problem.
	 *
	 * @return
	 */
	boolean isFirstToSolve();

	/**
	 * Return the time in ms of the solution, or if the problem hasn't been solved, the time of the
	 * most recent submission.
	 *
	 * @return the submission time, in ms
	 */
	int getContestTime();

	/**
	 * Return the penalty time for this problem, in minutes.
	 *
	 * @return the penalty time
	 */
	int getPenaltyTime();

	/**
	 * Returns the overall status of the submissions for this problem (i.e. SOLVED if at least one
	 * submission was a correct solution).
	 *
	 * @return the status
	 */
	Status getStatus();
}