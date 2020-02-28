package org.icpc.tools.contest.model;

public interface IStartStatus extends IContestObject {
	/**
	 * Returns the countdown label.
	 *
	 * @return the label
	 */
	String getLabel();

	/**
	 * Returns the countdown status.
	 *
	 * @return the status
	 */
	int getStatus();
}