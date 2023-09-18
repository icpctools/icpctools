package org.icpc.tools.contest.model;

public interface IPause extends IContestObject {
	/**
	 * Returns the pause start time.
	 *
	 * @return the start
	 */
	Long getStart();

	/**
	 * Returns the pause end time.
	 *
	 * @return the end
	 */
	Long getEnd();
}