package org.icpc.tools.presentation.contest.internal;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;

public class StatisticsGenerator {
	private Statistic[] stats = new Statistic[0];
	private final Queue<String> recentStack = new LinkedList<>();
	private static final Random rand = new Random(System.currentTimeMillis());

	protected static final NumberFormat percentFormat = NumberFormat.getPercentInstance();
	protected static final NumberFormat decimalFormat = NumberFormat.getNumberInstance();

	public StatisticsGenerator() {
		decimalFormat.setMaximumFractionDigits(1);
		decimalFormat.setMinimumFractionDigits(1);
	}

	public class Statistic {
		public String id;
		public String text;

		public Statistic(String id, String text) {
			this.id = id;
			this.text = text;
		}
	}

	/**
	 * Generates a new set of statistics based on the given contest.
	 *
	 * @param contest
	 */
	public void generate(IContest contest) {
		if (contest == null)
			return;

		int totalSubmissions = 0;
		int totalSolved = 0;
		int totalTime = 0;
		int teamsSolved = 0;

		int numTeams = contest.getNumTeams();
		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;

		int[] totalProblemSolved = new int[numProblems];
		int[] fastestProblemSolved = new int[numProblems];
		ITeam[] fastestProblemSolvedTeam = new ITeam[numProblems];

		for (int j = 0; j < numProblems; j++)
			fastestProblemSolved[j] = -1;

		int mostAttempts = -1;
		int mostAttemptsProblem = -1;
		int lastSolvedTime = -1;
		int lastSolvedProblem = -1;
		int totalPending = 0;

		for (ITeam team : contest.getTeams()) {
			boolean b = false;
			for (int j = 0; j < numProblems; j++) {
				IResult r = contest.getResult(team, j);
				totalSubmissions += r.getNumSubmissions();
				if (r.getStatus() == Status.SOLVED) {
					totalSolved++;
					b = true;
					totalProblemSolved[j]++;
					if (fastestProblemSolved[j] == -1 || r.getContestTime() < fastestProblemSolved[j]) {
						fastestProblemSolved[j] = r.getContestTime();
						fastestProblemSolvedTeam[j] = team;
					}
					if (r.getNumSubmissions() > mostAttempts) {
						mostAttempts = r.getNumSubmissions();
						mostAttemptsProblem = j;
					}
					if (r.getContestTime() > lastSolvedTime) {
						lastSolvedTime = r.getContestTime();
						lastSolvedProblem = j;
					}
					totalTime += r.getContestTime();
				}

				totalPending += r.getNumPending();
			}
			if (b)
				teamsSolved++;
		}

		List<Statistic> list = new ArrayList<>();

		// # teams that have solved a problem
		if (teamsSolved == 0)
			list.add(new Statistic("teamsSolved", "No teams have solved a problem"));
		else if (teamsSolved == 1)
			list.add(new Statistic("teamsSolved", "1 team has solved a problem!"));
		else
			list.add(new Statistic("teamsSolved", teamsSolved + " teams have solved a problem"));

		// % of teams that have solved a problem
		if (teamsSolved > 0)
			list.add(new Statistic("teamsSolved",
					percentFormat.format((float) teamsSolved / numTeams) + " of teams have solved a problem"));

		// total submissions
		if (totalSubmissions == 1)
			list.add(new Statistic("totalSubmissions", "There has been 1 problem submission"));
		else
			list.add(new Statistic("totalSubmissions", "There have been " + totalSubmissions + " problem submissions"));

		// total problems solved
		if (totalSolved == 1)
			list.add(new Statistic("totalNumProbs", "One problem has been solved"));
		else if (totalSolved > 1)
			list.add(new Statistic("totalNumProbs", "A total of " + totalSolved + " problems have been solved"));

		// total time
		if (totalTime > 0)
			list.add(new Statistic("totalTime", totalTime + " minutes have been spent on solved problems"));

		// average number of problems solved
		if (totalSolved > 1)
			list.add(new Statistic("avgSolved", "Teams have solved an average of "
					+ decimalFormat.format((float) totalSolved / numTeams) + " problems each"));

		for (int j = 0; j < numProblems; j++) {
			if (totalProblemSolved[j] > 0) {
				// teams per problem
				list.add(new Statistic("problemSolved" + j, percentFormat.format((float) totalProblemSolved[j] / numTeams)
						+ " of teams have solved Problem " + problems[j].getLabel()));

				// teams per problem
				// if (totalProblemSolved[j] == 1)
				// list.add(new Statistic("problemSolved" + j, "There is " + totalProblemSolved[j] +
				// " solution to problem " + contest.problems[j].name));
				// else
				// list.add(new Statistic("problemSolved" + j, "There are " + totalProblemSolved[j] +
				// " solutions to problem " + contest.problems[j].name));
				if (totalProblemSolved[j] == 1)
					list.add(
							new Statistic("problemSolved" + j, "Only one team has solved Problem " + problems[j].getLabel()));
				else
					list.add(new Statistic("problemSolved" + j,
							totalProblemSolved[j] + " teams have solved Problem " + problems[j].getLabel()));
			}
		}

		for (int j = 0; j < numProblems; j++) {
			// fastest solution
			if (fastestProblemSolved[j] >= 0)
				list.add(new Statistic("fastest" + j,
						fastestProblemSolvedTeam[j].getName() + " has solved Problem " + problems[j].getLabel()
								+ " in the fastest time of " + ContestUtil.getTimeInMin(fastestProblemSolved[j]) + " minutes"));
		}

		int count = 0;
		String s = "";
		for (int j = 0; j < numProblems; j++) {
			if (totalProblemSolved[j] == 0) {
				count++;
				if (s.length() > 0)
					s += ", ";
				s += problems[j].getLabel();
			}
		}

		if (count == numProblems)
			list.add(new Statistic("numProblemsSolved", "No problems have been solved"));
		else if (count == 0)
			list.add(new Statistic("numProblemsSolved", "All problems have been solved by at least one team"));
		else
			list.add(new Statistic("numProblemsSolved",
					(numProblems - count) + " out of " + numProblems + " problems have been solved by at least one team"));

		if (numProblems - count == 1)
			list.add(new Statistic("noSolutions", "There has been no solution to Problem " + s));
		else if (count > 0)
			list.add(new Statistic("noSolutions", "There have been no solutions to Problems " + s));

		if (mostAttempts > 1)
			list.add(new Statistic("mostAttempts", "The highest number of attempts before solving a problem is "
					+ mostAttempts + " on Problem " + problems[mostAttemptsProblem].getLabel()));

		if (lastSolvedProblem > -1)
			list.add(new Statistic("lastSolved",
					"The most recently solved problem is Problem " + problems[lastSolvedProblem].getLabel()));

		// total pending submissions
		list.add(new Statistic("totalPending", "There are " + totalPending + " pending submissions"));

		// TODO:
		// most recent/slowest solution ""?
		// xx perseverence
		// "The highest number of attempts before solving a problem is x, on problem y"
		// "x teams have solved at least y problems without any rejected submissions" (on any
		// problems)
		// "x teams have solved y problems" (y>0 && x>0)
		// "team x has the lowest average time to solve y problems"
		synchronized (stats) {
			stats = new Statistic[list.size()];
			list.toArray(stats);
		}
	}

	public String getStatistic() {
		synchronized (stats) {
			if (stats.length < 1)
				return "";
			while (recentStack.size() > stats.length - 5 && recentStack.size() > 0)
				recentStack.remove();

			int c = 0;
			while (true) {
				int n = rand.nextInt(stats.length);

				Statistic s = stats[n];
				if (c > 20 || !recentStack.contains(s.id)) {
					recentStack.offer(s.id);
					return s.text;
				}
				c++;
			}
		}
	}

	public void debug() {
		for (Statistic st : stats) {
			Trace.trace(Trace.USER, st.text);
		}
	}
}