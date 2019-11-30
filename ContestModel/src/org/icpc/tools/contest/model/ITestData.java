package org.icpc.tools.contest.model;

/**
 * Test data for a problem.
 */
public interface ITestData extends IContestObject {
	/**
	 * The id of the problem this is part of.
	 *
	 * @return the problem id
	 */
	String getProblemId();

	/**
	 * The ordinal of the test data.
	 *
	 * @return the ordinal
	 */
	int getOrdinal();

	/**
	 * <code>true</code> if this is sample (public) data, <code>false</code> otherwise.
	 *
	 * @return <code>true</code> if this is sample (public) data
	 */
	Boolean isSample();
}