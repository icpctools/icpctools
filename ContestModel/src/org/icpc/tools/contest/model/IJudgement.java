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
	int getStartContestTime();

	/**
	 * Returns the wall clock time, in ms since the epoch.
	 *
	 * @return the time
	 */
	long getStartTime();

	/**
	 * Returns the submission that this judgement is for.
	 *
	 * @return the submission id
	 */
	String getSubmissionId();

	/**
	 * Returns the judgement type id.
	 *
	 * @return the judgement type id
	 */
	String getJudgementTypeId();

	/**
	 * Returns the maximum run time, in ms.
	 *
	 * @return the maximum run time
	 */
	int getMaxRunTime();

	/**
	 * Returns the contest time relative to the start of the contest, in ms.
	 *
	 * @return the contest time
	 */
	Integer getEndContestTime();

	/**
	 * Returns the wall clock time, in ms since the epoch.
	 *
	 * @return the time
	 */
	Long getEndTime();

	/**
	 * Returns the score, for scoring contests.
	 *
	 * @return the score
	 */
	Double getScore();
}