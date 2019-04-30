package org.icpc.tools.contest.model;

/**
 * A group. The id is typically a short label that represents the name, e.g. CE for Compile Error.
 */
public interface IJudgementType extends IContestObject {
	/**
	 * The name of the judgement type, e.g. Compile Error.
	 *
	 * @return the name
	 */
	public String getName();

	/**
	 * Returns true if this judgement causes penalty time.
	 *
	 * @return if this judgement represents a penalty
	 */
	public boolean isPenalty();

	/**
	 * Returns true if this judgement represents a correct solution.
	 *
	 * @return if this judgement represents a correct solution
	 */
	public boolean isSolved();
}