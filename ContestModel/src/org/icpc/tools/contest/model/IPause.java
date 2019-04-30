package org.icpc.tools.contest.model;

public interface IPause extends IContestObject {
	/**
	 * Returns the pause start time.
	 *
	 * @return the start
	 */
	public long getStart();

	/**
	 * Returns the pause end time.
	 *
	 * @return the end
	 */
	public long getEnd();
}