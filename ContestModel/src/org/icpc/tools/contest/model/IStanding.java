package org.icpc.tools.contest.model;

/**
 * The current contest standings of a team.
 */
public interface IStanding {
	/**
	 * Return the number of problems that this team has solved.
	 *
	 * @return the number of problems solved
	 */
	int getNumSolved();

	/**
	 * Return the total time this team has (sum of solution times + penalty), in minutes.
	 *
	 * @return the total time of this team
	 */
	int getTime();

	/**
	 * Returns the time of the last (most recent) solution, in minutes.
	 *
	 * @return the time of last solution
	 */
	int getLastSolutionTime();

	/**
	 * Return the current rank of this team.
	 *
	 * @return the rank
	 */
	String getRank();
}