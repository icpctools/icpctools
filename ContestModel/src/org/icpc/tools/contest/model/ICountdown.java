package org.icpc.tools.contest.model;

public interface ICountdown extends IContestObject {
	/**
	 * Returns the countdown status.
	 *
	 * @return the status
	 */
	boolean[] getStatus();
}