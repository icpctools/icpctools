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
	public Long getStarted();

	/**
	 * The time the contest ended.
	 *
	 * @return the end time
	 */
	public Long getEnded();

	/**
	 * The time the contest was frozen.
	 *
	 * @return the freeze time
	 */
	public Long getFrozen();

	/**
	 * The time the contest was thawed.
	 *
	 * @return the thaw time
	 */
	public Long getThawed();

	/**
	 * The time the contest was finalized.
	 *
	 * @return the finalization time
	 */
	public Long getFinalized();

	/**
	 * The time the contest was closed for updates.
	 *
	 * @return the end of updates time
	 */
	public Long getEndOfUpdates();

	/**
	 * Returns true if the contest is running.
	 *
	 * @return
	 */
	public boolean isRunning();

	/**
	 * Returns true if the contest is frozen.
	 *
	 * @return
	 */
	public boolean isFrozen();

	/**
	 * Returns true if the contest is final.
	 *
	 * @return
	 */
	public boolean isFinal();

	/**
	 * Returns true if the contest is done all updates.
	 *
	 * @return
	 */
	public boolean isDoneUpdating();
}