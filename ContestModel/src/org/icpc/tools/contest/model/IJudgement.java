package org.icpc.tools.contest.model;

/**
 * A submission judgement.
 */
public interface IJudgement extends IContestObject {
	/**
	 * Returns the contest time relative to the start of the contest, in ms.
	 *
	 * @return the contest time
	 */
	public int getStartContestTime();

	/**
	 * Returns the wall clock time, in ms since the epoch.
	 *
	 * @return the time
	 */
	public long getStartTime();

	/**
	 * Returns the submission that this judgement is for.
	 *
	 * @return the submission id
	 */
	public String getSubmissionId();

	/**
	 * Returns the judgement type id.
	 *
	 * @return the judgement type id
	 */
	public String getJudgementTypeId();

	/**
	 * Returns the maximum run time, in ms.
	 *
	 * @return the maximum run time
	 */
	public int getMaxRunTime();

	/**
	 * Returns the contest time relative to the start of the contest, in ms.
	 *
	 * @return the contest time
	 */
	public Integer getEndContestTime();

	/**
	 * Returns the wall clock time, in ms since the epoch.
	 *
	 * @return the time
	 */
	public Long getEndTime();
}