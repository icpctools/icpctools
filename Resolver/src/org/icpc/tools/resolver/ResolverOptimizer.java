package org.icpc.tools.resolver;

import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FreezeFilter;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.resolver.ResolverLogic;
import org.icpc.tools.contest.model.util.ArgumentParser;
import org.icpc.tools.contest.model.util.ArgumentParser.OptionParser;

public class ResolverOptimizer {
	static class Resolve {
		String teamId;
		String problemId;

		public Resolve(String teamId, String problemId) {
			this.teamId = teamId;
			this.problemId = problemId;
		}

		@Override
		public String toString() {
			return "Resolve[" + teamId + "," + problemId + "]";
		}
	}

	static class ImportantStep {
		String teamId;
		List<Integer> problemIds = new ArrayList<>(3);

		public ImportantStep(String teamId) {
			this.teamId = teamId;
		}

		@Override
		public String toString() {
			return "ImportantStep[" + teamId + "," + problemIds.size() + "]";
		}
	}

	static class DecisionPoint {
		String teamId;
		Contest contest; // state of the contest just before the decision is made
		List<Option> options = new ArrayList<>(3); // the options for next step
		int weight;

		public DecisionPoint(String teamId) {
			this.teamId = teamId;
		}

		@Override
		public String toString() {
			return "DecisionPoint[" + teamId + "," + options.size() + "]";
		}
	}

	static class Option {
		String problemId;
		DecisionPoint dp;
		boolean end;
		int weight;

		public Option(String problemId) {
			this.problemId = problemId;
		}

		@Override
		public String toString() {
			return "Option[" + problemId + "," + dp + "," + end + "]";
		}
	}

	static class PathStep {
		DecisionPoint dp;
		Option o;
		int weight;

		@Override
		public String toString() {
			return "Option[" + dp + "," + o + "," + weight + "]";
		}
	}

	private static List<ImportantStep> importantSteps = new ArrayList<>();
	private static List<DecisionPoint> dps = new ArrayList<>();

	private Contest contest;
	private Contest finalContest;
	private Option root;

	public static void main(String[] args) {
		ContestSource[] contestSource2 = ArgumentParser.parseMulti(args, new OptionParser() {
			@Override
			public boolean setOption(String option, List<Object> options) throws IllegalArgumentException {
				return false;
			}

			@Override
			public void showHelp() {
				System.out.println("Usage: optimizer.bat/sh contestURL user password [options]");
				System.out.println("   or: optimizer.bat/sh contestPath [options]");
			}
		});

		ContestSource contestSource = contestSource2[0];
		contestSource.outputValidation();

		Contest finalContest = contestSource.getContest();
		contestSource.waitForContest(10000);
		finalContest = ResolverLogic.filter(finalContest);

		// revert to start of the freeze
		Contest contest = finalContest.clone(new FreezeFilter(finalContest));

		// clean up unjudged and non-penalty submissions in the last hour that don't matter and will
		// mess with resolving
		cleanOutlierSubmissions(contest, finalContest);

		importantSteps.addAll(findImportantSubmissions(contest, finalContest));

		Trace.trace(Trace.USER, "Resolving " + countUnjudgedSubmissions(contest) + " pending submissions out of the "
				+ contest.getNumSubmissions() + " total submissions in the contest... ");

		long time = System.currentTimeMillis();

		ResolverOptimizer ro = new ResolverOptimizer(contest, finalContest);
		ro.resolveAllOptions();

		long resolveAllTime = System.currentTimeMillis() - time;
		Trace.trace(Trace.USER, "Resolution time: " + resolveAllTime + "ms");
		Trace.trace(Trace.USER, "Time/option: " + resolveAllTime / (dps.size() + 1) + "ms");

		ro.findBestPath();

		long bestPathTime = System.currentTimeMillis() - time;
		Trace.trace(Trace.USER, "Total time: " + bestPathTime + "ms");
		Trace.trace(Trace.USER, "Time/tree: " + bestPathTime / (dps.size() + 1) + "ms");
	}

	public ResolverOptimizer(Contest contest, Contest finalContest) {
		this.contest = contest;
		this.finalContest = finalContest;
	}

	private static void cleanOutlierSubmissions(Contest contest, Contest finalContest) {
		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission submission : submissions) {
			if (!contest.isJudged(submission)) {
				ITeam team = contest.getTeamById(submission.getTeamId());
				if (team == null)
					continue;

				int probIndex = contest.getProblemIndex(submission.getProblemId());
				IResult result = contest.getResult(team, probIndex);

				if (result.getStatus() != Status.SUBMITTED) {
					contest.updateSubmissionTo(submission, finalContest);
				}
			}
		}

		// remove non-penalty judgements
		submissions = finalContest.getSubmissions();
		for (ISubmission submission : submissions) {
			IJudgement[] sjs = finalContest.getJudgementsBySubmissionId(submission.getId());
			if (sjs != null) {
				for (IJudgement sj : sjs) {
					IJudgementType jt = finalContest.getJudgementTypeById(sj.getJudgementTypeId());
					if (jt != null && !jt.isSolved() && !jt.isPenalty() && !contest.isBeforeFreeze(submission)) {
						contest.add(sj);
					}
				}
			}
		}
	}

	private static int countUnjudgedSubmissions(Contest contest) {
		int count = 0;
		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission submission : submissions) {
			if (!contest.isJudged(submission))
				count++;
		}
		return count;
	}

	private static List<ImportantStep> findImportantSubmissions(Contest contest, Contest finalContest) {
		int count = 0;
		List<ImportantStep> iList = new ArrayList<>();
		ITeam[] teams = finalContest.getOrderedTeams();
		int NUM = 18; // 18
		// int NUM = Math.min(18, teams.length);
		// int NUM = Math.min(10, teams.length);
		IProblem[] problems = contest.getProblems();
		for (int i = 0; i < teams.length; i++) {
			List<Integer> list = new ArrayList<>();
			for (int j = 0; j < problems.length; j++) {
				IResult r1 = contest.getResult(teams[i], j);
				if (r1.getStatus() == Status.SUBMITTED) {
					list.add(j);
				}
			}
			if (list.size() > 1) {
				if (i < NUM) {
					ImportantStep is = new ImportantStep(teams[i].getId());
					is.problemIds = list;
					iList.add(is);
				}
				count += list.size() - 1;
			}
		}

		Trace.trace(Trace.USER, "Found " + count + " interesting resolution choices, picked " + iList.size());
		return iList;
	}

	private void resolveSubmissions(Option o, Resolve resolve) {
		ITeam team = contest.getTeamById(resolve.teamId);
		int oldRow = contest.getOrderOf(team);
		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission submission : submissions) {
			if (!contest.isJudged(submission) && submission.getTeamId().equals(resolve.teamId)
					&& submission.getProblemId().equals(resolve.problemId)) {
				contest.updateSubmissionTo(submission, finalContest);
			}
		}

		// weighting - maximum
		int newRow = contest.getOrderOf(team);
		if (oldRow != newRow && newRow == 1) {
			o.weight += (oldRow - newRow) + 10;
		}
	}

	protected void resolveAllOptions() {
		// kick off the first resolution to get to the first decision point
		root = new Option(null);
		Resolve resolve = getNextResolve(null);
		while (resolve != null) {
			resolveSubmissions(root, resolve);
			resolve = getNextResolve(root);
		}

		boolean done = false;
		while (!done) {
			done = true;
			DecisionPoint[] dpa = dps.toArray(new DecisionPoint[dps.size()]);
			for (DecisionPoint dp : dpa) {
				for (Option o : dp.options) {
					if (o.dp == null && !o.end) {
						done = false;
						resolve(dp, o);
					}
				}
			}
		}
		System.out.println("Decision points found: " + dps.size());
	}

	protected PathStep[] findBestPath(DecisionPoint dp, Option opt, boolean best) {
		if (opt.end)
			return null;

		PathStep[] bestPath = null;
		PathStep ps = new PathStep();
		ps.dp = dp;
		ps.o = opt;
		ps.weight = Integer.MIN_VALUE;
		for (Option o : opt.dp.options) {
			PathStep[] path = findBestPath(opt.dp, o, best);
			if (path != null) {
				if (bestPath == null || (best && path[0].weight > ps.weight) || (!best && path[0].weight < ps.weight)) {
					bestPath = path;
					ps.weight = path[0].weight;
				}
			}
		}
		if (ps.weight == Integer.MIN_VALUE)
			ps.weight = 0;
		ps.weight += opt.weight;

		if (bestPath == null) {
			PathStep[] path = new PathStep[1];
			path[0] = ps;
			return path;
		}

		if (dp == null)
			return bestPath;

		PathStep[] path = new PathStep[bestPath.length + 1];
		path[0] = ps;
		System.arraycopy(bestPath, 0, path, 1, bestPath.length);
		return path;
	}

	protected void findBestPath() {
		PathStep[] best = findBestPath(null, root, true);
		Trace.trace(Trace.USER, "Best path: " + best[0].weight);
		// for (PathStep ps : best)
		// System.out.println(" " + ps);
		output(best);

		best = findBestPath(null, root, false);
		Trace.trace(Trace.USER, "Worst path: " + best[0].weight);
		// for (PathStep ps : best)
		// System.out.println(" " + ps);
		output(best);
	}

	protected void output(PathStep[] list) {
		for (PathStep step : list) {
			if (step.dp != null)
				Trace.trace(Trace.USER, step.dp.teamId);
			Trace.trace(Trace.USER, "\t");
			IProblem p = contest.getProblemById(step.o.problemId);
			if (p != null)
				Trace.trace(Trace.USER, p.getLabel());

			Trace.trace(Trace.USER, "");
		}
	}

	protected void resolve(DecisionPoint dp, Option o) {
		contest = dp.contest.clone(false);

		// resolve the option
		Resolve resolve = new Resolve(dp.teamId, o.problemId);
		resolveSubmissions(o, resolve);

		// then keep going until we get to the next decision point or the end
		resolve = getNextResolve(o);
		while (resolve != null) {
			resolveSubmissions(o, resolve);
			resolve = getNextResolve(o);
		}
	}

	private Resolve getNextResolve(Option opt) {
		ITeam[] teams = contest.getOrderedTeams();
		IProblem[] problems = contest.getProblems();
		for (int i = teams.length - 1; i >= 0; i--) {
			ITeam team = teams[i];

			// check for predetermined steps first
			for (ImportantStep is : importantSteps) {
				if (is.teamId.equals(team.getId())) {
					List<Integer> list = new ArrayList<>();
					for (int j : is.problemIds) {
						IResult r1 = contest.getResult(team, j);
						if (r1.getStatus() == Status.SUBMITTED) {
							list.add(j);
						}
					}
					if (list.size() > 1) {
						// there's a decision point here!
						DecisionPoint dp = new DecisionPoint(team.getId());
						dp.contest = contest;
						for (Integer j : list)
							dp.options.add(new Option(problems[j].getId()));

						dps.add(dp);
						opt.dp = dp;
						return null;
					}
				}
			}

			// otherwise, default to pick left
			for (int j = 0; j < problems.length; j++) {
				IResult r1 = contest.getResult(team, j);
				if (r1.getStatus() == Status.SUBMITTED) {
					return new Resolve(team.getId(), problems[j].getId());
				}
			}
		}
		opt.end = true;
		return null;
	}
}