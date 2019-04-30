package org.icpc.tools.contest.model;

/**
 * A run (testcase data judgement).
 */
public interface IRun extends IContestObject {
	/**
	 * The id of the judgement that this run belongs to.
	 *
	 * @return the judgement id
	 */
	public String getJudgementId();

	/**
	 * The judgement type (result) of this run.
	 *
	 * @return the id
	 */
	public String getJudgementTypeId();

	/**
	 * Return the ordinal of the run, used for sorting.
	 *
	 * @return the ordinal
	 */
	public int getOrdinal();

	/**
	 * Returns the run time, in ms.
	 *
	 * @return the run time
	 */
	public int getRunTime();

	/**
	 * Returns the contest time relative to the start of the contest, in ms.
	 *
	 * @return the contest time
	 */
	public int getContestTime();

	/**
	 * Returns the wall clock time, in ms since the epoch.
	 *
	 * @return the time
	 */
	public long getTime();
}