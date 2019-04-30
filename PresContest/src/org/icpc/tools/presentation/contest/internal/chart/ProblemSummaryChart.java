package org.icpc.tools.presentation.contest.internal.chart;

import java.awt.Color;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.presentation.contest.internal.ContestData;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.Utility;
import org.icpc.tools.presentation.core.chart.AbstractChartPresentation;
import org.icpc.tools.presentation.core.chart.Series;

public class ProblemSummaryChart extends AbstractChartPresentation {
	protected IContest contest = ContestData.getContest();

	public ProblemSummaryChart() {
		super(Type.BAR, "Attempts & Solutions by Problem", "# of submissions");
		setFont(ICPCFont.getMasterFont());
	}

	@Override
	protected void setupChart() {
		if (contest == null)
			return;

		IProblem[] problems = contest.getProblems();
		if (problems == null)
			return;

		int numProblems = problems.length;
		if (numProblems == 0)
			return;

		String[] labels = new String[numProblems];
		int u = 0;

		Color[] colorAttempt = new Color[numProblems];
		Color[] colorSolved = new Color[numProblems];
		for (int j = 0; j < numProblems; j++) {
			labels[j] = problems[j].getLabel();
			colorAttempt[j] = problems[j].getColorVal();
			if (colorAttempt[j] == null) {
				if (u == 0)
					colorAttempt[j] = ICPCColors.BLUE;
				else if (u == 1)
					colorAttempt[j] = ICPCColors.YELLOW;
				else
					colorAttempt[j] = ICPCColors.RED;
				u++;
				u %= 3;
			}
			colorSolved[j] = Utility.alphaDarker(colorAttempt[j], 200, 0.8f);
		}

		setHorizontalLabels(labels);
		setSeries(new Series(colorAttempt), new Series(colorSolved));
	}

	@Override
	protected void updateData() {
		if (contest == null || getSeries() == null)
			return;

		IProblem[] problems = contest.getProblems();
		if (problems == null)
			return;

		int numProblems = problems.length;
		if (numProblems == 0)
			return;

		int[] totalAttempts = new int[numProblems];
		int[] totalSolved = new int[numProblems];
		for (ITeam team : contest.getTeams()) {
			for (int j = 0; j < numProblems; j++) {
				IResult r = contest.getResult(team, j);
				totalAttempts[j] += r.getNumSubmissions();
				if (r.getStatus() == Status.SOLVED)
					totalSolved[j]++;
			}
		}

		getSeries()[0].setValues(totalAttempts);
		getSeries()[1].setValues(totalSolved);

		getSeries()[0].setTitle("# of submissions");
		getSeries()[1].setTitle("# of solved submissions");
	}
}