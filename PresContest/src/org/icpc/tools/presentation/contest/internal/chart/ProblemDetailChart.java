package org.icpc.tools.presentation.contest.internal.chart;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.presentation.contest.internal.ContestData;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.core.chart.AbstractChartPresentation;
import org.icpc.tools.presentation.core.chart.Series;
import org.icpc.tools.presentation.core.chart.Series.LinePoint;

public class ProblemDetailChart extends AbstractChartPresentation {
	private static final int MIN = 5;
	protected int problemNum = 0;
	protected int targetProblem = -1;
	protected IContest contest = ContestData.getContest();
	private int numMin;

	public ProblemDetailChart() {
		super(Type.LINE, "Problem Submissions", "# of submissions every 5 minutes");
		showValueLabels = false;
		setFont(ICPCFont.getMasterFont());
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();
		if (contest != null) {
			problemNum++;
			problemNum %= contest.getProblems().length;
		}
	}

	@Override
	protected void setupChart() {
		if (contest == null)
			return;

		if (targetProblem >= 0)
			problemNum = targetProblem;

		IProblem problem = contest.getProblems()[problemNum];
		setTitle("Problem " + problem.getLabel() + " Submissions");

		numMin = (int) (contest.getContestTimeOfLastEvent() / 60000 / MIN);
		if (numMin < 1)
			numMin = 1;

		setHorizontalLabels(TotalProblemsChart.getHourLabels(numMin, MIN));
		setSeries(new Series(numMin, ICPCColors.FAILED_COLOR), new Series(numMin, ICPCColors.PENDING_COLOR),
				new Series(numMin, ICPCColors.SOLVED_COLOR));

		getSeries()[0].setTitle("Failed submissions");
		getSeries()[1].setTitle("Pending submissions");
		getSeries()[2].setTitle("Solved submissions");

		for (Series s : getSeries())
			s.setLinePoint(LinePoint.RECT);
	}

	@Override
	protected void updateData() {
		if (contest == null)
			return;

		if (targetProblem >= 0)
			problemNum = targetProblem;

		IProblem problem = contest.getProblems()[problemNum];

		int[] fails = new int[numMin];
		int[] solns = new int[numMin];
		int[] pends = new int[numMin];
		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission s : submissions) {
			if (problem.equals(contest.getProblemById(s.getProblemId()))) {
				int rt = (int) Math.max(0, s.getContestTime() / 1000L / 60 / MIN);
				rt = Math.min(rt, numMin - 1);
				Status status = contest.getStatus(s);
				if (status == Status.SOLVED)
					solns[rt]++;
				else if (status == Status.FAILED)
					fails[rt]++;
				else
					pends[rt]++;
			}
		}

		getSeries()[0].setValues(fails);
		getSeries()[1].setValues(pends);
		getSeries()[2].setValues(solns);
	}

	@Override
	public void setProperty(String value) {
		if (value == null || value.isEmpty())
			return;
		if (value.startsWith("lightMode:"))
			return;

		try {
			targetProblem = Integer.parseInt(value);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error setting properties", e);
		}
	}
}