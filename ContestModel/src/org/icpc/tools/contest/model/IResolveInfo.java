package org.icpc.tools.contest.model;

import java.util.List;

public interface IResolveInfo extends IContestObject {
	public static class PredeterminedStep {
		public String teamLabel;
		public String problemLabel;

		public PredeterminedStep(String teamLabel, String problemLabel) {
			this.teamLabel = teamLabel;
			this.problemLabel = problemLabel;
		}

		@Override
		public String toString() {
			return "Step: " + teamLabel + " " + problemLabel;
		}
	}

	/**
	 * Returns the current number of clicks (what step we're on).
	 *
	 * @return the number of clicks
	 */
	int getClicks();

	/**
	 * Returns the resolution speed factor. Normal speed is 1, <1 is faster and >1 is slower.
	 *
	 * @return the relative animation speed
	 */
	double getSpeedFactor();

	/**
	 * Returns the scroll speed factor. Normal speed is 1, >1 is faster and <1 is slower.
	 *
	 * @return the relative scrolling speed
	 */
	double getScrollSpeedFactor();

	/**
	 * Returns true if animation is currently paused.
	 *
	 * @return true if animation is paused
	 */
	boolean isAnimationPaused();

	/**
	 * Returns the row to switch to single steps.
	 *
	 * @return the single step row
	 */
	int getSingleStepRow();

	/**
	 * Returns the UI row offset.
	 *
	 * @return the UI row offset
	 */
	int getRowOffset();

	/**
	 * Returns the predetermined steps.
	 *
	 * @return the predetermined steps
	 */
	List<PredeterminedStep> getPredeterminedSteps();
}