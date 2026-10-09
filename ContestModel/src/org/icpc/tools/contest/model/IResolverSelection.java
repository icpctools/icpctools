package org.icpc.tools.contest.model;

public interface IResolverSelection extends IContestObject {
	/**
	 * Returns the selected team ids.
	 *
	 * @return the team ids
	 */
	String[] getTeamIds();

	/**
	 * Returns the selected problem id, may be null.
	 *
	 * @return the problem id
	 */
	String getProblemId();
}