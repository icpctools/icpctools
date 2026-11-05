package org.icpc.tools.contest.model.resolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IJudgement;
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

	public static class MessageStep implements ResolutionStep {
		public String text;

		public MessageStep(String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return "Message " + text;
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

	public static class JudgementStep implements ResolutionStep {
		public IJudgement[] judgements;

		public JudgementStep(IJudgement[] judgements) {
			this.judgements = judgements;
		}

		@Override
		public String toString() {
			return "Judgement step";
		}
	}

	/**
	 * A selection in the resolver. withX() methods change the existing object (clearing any old
	 * data, e.g. if you select a team then the previous problem and submission id are reset) and
	 * then return a clone(), so you can do:
	 * <ol>
	 * <li>steps.add(step.withProblem(p));</li>
	 * <li>steps.add(step.withSubmissionId(subId));</li>
	 * </ol>
	 * and not worry about creating new objects each time.
	 */
	public static class SelectionStep implements ResolutionStep {
		public List<ITeam> teams;
		public SelectType type;
		public String problemId;
		public SubmissionInfo subInfo;
		public String submissionId;

		// team
		public SelectionStep() {
			// no selection
		}

		@Override
		protected SelectionStep clone() {
			SelectionStep ss = new SelectionStep();
			ss.teams = teams;
			ss.type = type;
			ss.problemId = problemId;
			ss.subInfo = subInfo;
			ss.submissionId = submissionId;
			return ss;
		}

		public SelectionStep withTeam(ITeam team) {
			return withTeam(team, SelectType.NORMAL);
		}

		public SelectionStep withTeam(ITeam team, SelectType newType) {
			teams = new ArrayList<>(1);
			teams.add(team);
			type = newType;
			problemId = null;
			subInfo = null;
			submissionId = null;
			return clone();
		}

		public SelectionStep withTeams(ITeam[] newTeams) {
			teams = Arrays.asList(newTeams);
			type = SelectType.TEAM_LIST;
			problemId = null;
			subInfo = null;
			submissionId = null;
			return clone();
		}

		public SelectionStep withProblem(SubmissionInfo newSubInfo) {
			if (newSubInfo != null)
				problemId = newSubInfo.getProblemId();
			else
				problemId = null;
			subInfo = newSubInfo;
			submissionId = null;
			return clone();
		}

		public SelectionStep withSubmissionId(String newSubId) {
			submissionId = newSubId;
			return clone();
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
			if (type != null && type != SelectType.NORMAL)
				sb.append(", " + type.name());

			sb.append(", " + problemId);
			sb.append(", " + submissionId);
			return sb.toString();
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

	public static long getTotalContests(List<ResolutionStep> steps) {
		int count = 0;
		for (ResolutionStep step : steps) {
			if (step instanceof ContestStateStep) {
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
