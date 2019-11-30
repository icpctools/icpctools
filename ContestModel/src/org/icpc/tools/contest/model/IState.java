package org.icpc.tools.contest.model;

/**
 * The contest state.
 */
public interface IState extends IContestObject {
	/**
	 * The time the contest started.
	 *
	 * @return the start time
	 */
	Long getStarted();

	/**
	 * The time the contest ended.
	 *
	 * @return the end time
	 */
	Long getEnded();

	/**
	 * The time the contest was frozen.
	 *
	 * @return the freeze time
	 */
	Long getFrozen();

	/**
	 * The time the contest was thawed.
	 *
	 * @return the thaw time
	 */
	Long getThawed();

	/**
	 * The time the contest was finalized.
	 *
	 * @return the finalization time
	 */
	Long getFinalized();

	/**
	 * The time the contest was closed for updates.
	 *
	 * @return the end of updates time
	 */
	Long getEndOfUpdates();

	/**
	 * Returns true if the contest is running.
	 *
	 * @return
	 */
	boolean isRunning();

	/**
	 * Returns true if the contest is frozen.
	 *
	 * @return
	 */
	boolean isFrozen();

	/**
	 * Returns true if the contest is final.
	 *
	 * @return
	 */
	boolean isFinal();

	/**
	 * Returns true if the contest is done all updates.
	 *
	 * @return
	 */
	boolean isDoneUpdating();
}