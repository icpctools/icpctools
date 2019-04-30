package org.icpc.tools.presentation.contest.internal.chart;

import java.awt.Color;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.presentation.contest.internal.ContestData;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.core.chart.AbstractChartPresentation;
import org.icpc.tools.presentation.core.chart.Series;
import org.icpc.tools.presentation.core.chart.Series.LinePoint;

public class ProblemComparisonChart extends AbstractChartPresentation {
	private static final int MIN = 5;
	protected IContest contest = ContestData.getContest();
	private int numMin;

	public ProblemComparisonChart() {
		super(Type.LINE, "Problem Submissions", "# of submissions every 5 minutes");
		showValueLabels = false;
		setFont(ICPCFont.getMasterFont());
	}

	@Override
	protected void setupChart() {
		if (contest == null)
			return;

		setTitle("Submissions by Problem");

		numMin = contest.getContestTimeOfLastEvent() / 60000 / MIN;
		if (numMin < 1)
			numMin = 1;

		setHorizontalLabels(TotalProblemsChart.getHourLabels(numMin, MIN));

		IProblem[] problems = contest.getProblems();
		int size = problems.length;
		Series[] s = new Series[size];
		for (int i = 0; i < size; i++) {
			Color c = problems[i].getColorVal();
			if (Color.black.equals(c))
				c = Color.DARK_GRAY;
			s[i] = new Series(numMin, c);
			s[i].setTitle("Problem " + problems[i].getLabel());
			s[i].setLinePoint(LinePoint.RECT);
		}
		setSeries(s);
	}

	@Override
	protected void updateData() {
		if (contest == null)
			return;

		IProblem[] problems = contest.getProblems();
		int size = problems.length;
		int[][] data = new int[size][numMin];

		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission s : submissions) {
			String pId = s.getProblemId();
			for (int i = 0; i < size; i++) {
				if (problems[i].getId().equals(pId)) {
					int rt = (int) Math.max(0, s.getContestTime() / 1000L / 60 / MIN);
					rt = Math.min(rt, numMin - 1);
					data[i][rt]++;
				}
			}
		}

		for (int i = 0; i < size; i++)
			getSeries()[i].setValues(data[i]);
	}
}