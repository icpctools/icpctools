package org.icpc.tools.resolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.presentation.contest.internal.SubmissionInfo;
import org.icpc.tools.presentation.contest.internal.scoreboard.AbstractScoreboardPresentation;

public class ResolutionUtil {
	protected enum DelayType {
		SELECT_TEAM, SELECT_PROBLEM, SOLVED_MOVE, SOLVED_STAY, FAILED, DESELECT, SELECT_SUBMISSION
	}

	protected static final double[] DELAY_TIMES = new double[] { 1.3, 1.0, 2.25, 1.5, 0.85, 0.25, 0.45 };

	protected interface ResolutionStep {
		// tag interface, no methods
	}

	private ResolutionUtil() {
		// can't create
	}

	public static class PresentationStep implements ResolutionStep {
		enum Presentations {
			SPLASH, SCOREBOARD, JUDGE, TEAM_AWARD, TEAM_LIST
		}

		public Presentations p;

		public PresentationStep(Presentations pp) {
			this.p = pp;
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

	public static class TeamSelectionStep implements ResolutionStep {
		public List<ITeam> teams;
		public AbstractScoreboardPresentation.SelectType type;

		public TeamSelectionStep() {
			// no selection
		}

		public TeamSelectionStep(ITeam team) {
			this(team, AbstractScoreboardPresentation.SelectType.NORMAL);
		}

		public TeamSelectionStep(ITeam team, AbstractScoreboardPresentation.SelectType type) {
			teams = new ArrayList<>(1);
			teams.add(team);
			this.type = type;
		}

		public TeamSelectionStep(ITeam[] teams) {
			this.teams = Arrays.asList(teams);
			type = AbstractScoreboardPresentation.SelectType.TEAM_LIST;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (teams == null || teams.isEmpty())
				sb.append("Select team (none)");
			else if (teams.size() == 1) {
				ITeam team = teams.get(0);
				sb.append("Select team " + team.getId() + ": " + team.getActualDisplayName());
			} else {
				sb.append("Select teams ");
				int n = Math.min(10, teams.size());
				for (int i = 0; i < n; i++)
					sb.append(teams.get(i).getId() + " ");
				if (n < teams.size())
					sb.append("...");
			}
			if (type != null && type != AbstractScoreboardPresentation.SelectType.NORMAL)
				sb.append(" " + type.name());
			return sb.toString();
		}
	}

	public static class SubmissionSelectionStep implements ResolutionStep {
		public SubmissionInfo subInfo;

		public SubmissionSelectionStep(SubmissionInfo subInfo) {
			this.subInfo = subInfo;
		}

		@Override
		public String toString() {
			if (subInfo == null)
				return "Select submission (none)";
			return "Select submission " + subInfo.getProblemIndex() + " for team " + subInfo.getTeam().getId();
		}
	}

	public static class SubmissionSelectionStep2 implements ResolutionStep {
		public String submissionId;

		public SubmissionSelectionStep2(String submissionId) {
			this.submissionId = submissionId;
		}

		@Override
		public String toString() {
			return "Select submission id " + submissionId;
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

	public static class AwardStep implements ResolutionStep {
		public String teamId;
		public List<IAward> awards;

		public AwardStep(String teamId, List<IAward> awards) {
			this.teamId = teamId;
			this.awards = awards;
		}

		@Override
		public String toString() {
			if (awards == null)
				return "Award (none)";

			if (awards.size() == 1)
				return "Award (" + awards.get(0).getCitation() + ")";

			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (IAward a : awards) {
				if (first)
					first = !first;
				else
					sb.append(", ");
				sb.append(a.getCitation());
			}
			return "Awards (" + sb.toString() + ")";
		}
	}

	public static class TeamListStep implements ResolutionStep {
		public String title;
		public String subTitle;
		public ITeam[] teams;
		public Map<String, AbstractScoreboardPresentation.SelectType> selections;

		public TeamListStep(String title, String subTitle, ITeam[] teams,
				Map<String, AbstractScoreboardPresentation.SelectType> selections) {
			this.title = title;
			this.subTitle = subTitle;
			this.teams = teams;
			this.selections = selections;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Team List ");
			if (teams == null)
				sb.append("(none)");
			else
				sb.append(teams.length);
			sb.append(" " + title + " - " + subTitle);
			return sb.toString();
		}
	}

	public static class ScrollStep implements ResolutionStep {
		public int row;

		public ScrollStep(int row) {
			this.row = row;
		}

		@Override
		public String toString() {
			return "Scroll to row " + row;
		}
	}

	public static class ScrollTeamListStep implements ResolutionStep {
		public boolean top;

		public ScrollTeamListStep(boolean top) {
			this.top = top;
		}

		@Override
		public String toString() {
			if (top)
				return "Scroll team list down";
			return "Scroll team list up";
		}
	}

	protected static double getTotalTime(List<ResolutionStep> steps) {
		double time = 0;
		for (ResolutionStep step : steps) {
			if (step instanceof DelayStep) {
				DelayStep d = (DelayStep) step;
				time += ResolutionUtil.DELAY_TIMES[d.type.ordinal()];
			}
		}
		return time;
	}

	protected static long getTotalPauses(List<ResolutionStep> steps) {
		int count = 0;
		for (ResolutionStep step : steps) {
			if (step instanceof PauseStep) {
				count++;
			}
		}
		return count;
	}

	protected static long getTotalContests(List<ResolutionStep> steps) {
		int count = 0;
		for (ResolutionStep step : steps) {
			if (step instanceof ContestStateStep) {
				count++;
			}
		}
		return count;
	}

	protected static List<String> getTeamListIds(List<ResolutionStep> steps) {
		List<String> teamIds = new ArrayList<>();
		for (ResolutionStep step : steps) {
			if (step instanceof TeamListStep) {
				TeamListStep as = (TeamListStep) step;
				if (as.teams != null) {
					for (ITeam team : as.teams) {
						teamIds.add(team.getId());
					}
				}
			}
		}
		return teamIds;
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