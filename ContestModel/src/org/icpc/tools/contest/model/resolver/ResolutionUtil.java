package org.icpc.tools.contest.model.resolver;

import java.util.List;

import org.icpc.tools.contest.model.IAward;

public class ResolutionUtil {
	protected enum DelayType {
		SELECT_TEAM, SELECT_PROBLEM, SOLVED_MOVE, SOLVED_STAY, FAILED, DESELECT, SELECT_SUBMISSION
	}

	protected static final double[] DELAY_TIMES = new double[] { 1.3, 1.0, 2.25, 1.5, 0.85, 0.25, 0.45 };

	public interface ResolutionStep {
		// tag interface, no methods
	}

	private ResolutionUtil() {
		// can't create
	}

	public static class PauseStep implements ResolutionStep {
		public int num;

		public PauseStep() {
			// do nothing
		}

		@Override
		public String toString() {
			return "Pause " + num;
		}
	}

	public static class DelayStep implements ResolutionStep {
		public DelayType type;

		public DelayStep(DelayType type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return "Delay " + type.name();
		}
	}

	public static double getTotalTime(List<ResolutionStep> steps) {
		double time = 0;
		for (ResolutionStep step : steps) {
			if (step instanceof DelayStep) {
				DelayStep d = (DelayStep) step;
				time += ResolutionUtil.DELAY_TIMES[d.type.ordinal()];
			}
		}
		return time;
	}

	public static long getTotalPauses(List<ResolutionStep> steps) {
		int count = 0;
		for (ResolutionStep step : steps) {
			if (step instanceof PauseStep) {
				count++;
			}
		}
		return count;
	}

	protected static void numberThePauses(List<ResolutionStep> steps) {
		int num = 0;
		for (ResolutionStep step : steps) {
			if (step instanceof PauseStep) {
				PauseStep ps = (PauseStep) step;
				ps.num = num++;
			}
		}
	}

	public static boolean showScoreboardBefore(IAward award) {
		if (award.getParameters() != null && award.getParameters().containsKey("showScoreboardBefore")) {
			return Boolean.parseBoolean(award.getParameters().get("showScoreboardBefore"));
		}
		return true;
	}

	public static boolean shouldHighlight(IAward award) {
		if (award.getParameters() != null && award.getParameters().containsKey("highlight"))
			return Boolean.parseBoolean(award.getParameters().get("highlight"));
		return true;
	}

	public static boolean after(IAward award) {
		return award.getParameters() != null && award.getParameters().containsKey(ResolverLogic.AFTER)
				&& award.getParameters().get(ResolverLogic.AFTER).equals("true");
	}
}
