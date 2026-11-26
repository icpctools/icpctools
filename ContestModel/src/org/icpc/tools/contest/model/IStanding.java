package org.icpc.tools.contest.model;

/**
 * The current contest standings of a team.
 */
public interface IStanding extends ITeamResult {
	/**
	 * Return the current rank of this team.
	 *
	 * @return the rank
	 */
	int getRank();
}