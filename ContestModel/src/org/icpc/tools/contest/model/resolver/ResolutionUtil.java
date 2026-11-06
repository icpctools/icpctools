package org.icpc.tools.contest.model.resolver;

import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Contest;

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

	public static class PresentationStep implements ResolutionStep {
		public enum Presentations {
			SPLASH, SCOREBOARD, JUDGE, TEAM_AWARD, TEAM_LIST, TEAM_LIST_PHOTO, MESSAGE
		}

		public Presentations p;
		public String message;
		public String teamId;
		public Boolean scroll;

		public PresentationStep(Presentations pp) {
			this.p = pp;
		}

		public PresentationStep withMessage(String newMessage) {
			message = newMessage;
			return this;
		}

		public PresentationStep withTeamId(String newTeamId) {
			teamId = newTeamId;
			return this;
		}

		public PresentationStep withScroll(Boolean newScroll) {
			scroll = newScroll;
			return this;
		}

		@Override
		public String toString() {
			return "Show presentation " + p.name();
		}
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

	public static class ContestStateStep implements ResolutionStep {
		public Contest contest;

		public ContestStateStep(Contest contest) {
			this.contest = contest;
		}

		@Override
		public String toString() {
			return "Change contest state";
		}
	}

	public static class ToJudgeStep implements ResolutionStep {
		public String[] submissionIds;

		public ToJudgeStep(String[] submissionIds) {
			this.submissionIds = submissionIds;
		}

		@Override
		public String toString() {
			if (submissionIds == null)
				return "Submissions to judge: (none)";
			return "Submissions to judge: " + submissionIds.length;
		}
	}

	public static class ListAwardStep implements ResolutionStep {
		public IAward award;
		public ITeam[] teams;
		public Map<String, SelectType> selections;
		public boolean photos;
		public boolean after;

		public ListAwardStep(IAward award, ITeam[] teams, Map<String, SelectType> selections, boolean photos,
				boolean after) {
			this.award = award;
			this.teams = teams;
			this.selections = selections;
			this.photos = photos;
			this.after = after;
		}

		@Override
		public String toString() {
			return "Team List " + award.getId();
		}

		public boolean showScoreboardBefore() {
			if (award.getParameters() != null && award.getParameters().containsKey("showScoreboardBefore")) {
				return Boolean.parseBoolean(award.getParameters().get("showScoreboardBefore"));
			}
			return true;
		}

		public boolean shouldHighlight() {
			if (award.getParameters() != null && award.getParameters().containsKey("highlight"))
				return Boolean.parseBoolean(award.getParameters().get("highlight"));
			return true;
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
}
