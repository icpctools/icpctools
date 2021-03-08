package org.icpc.tools.presentation.contest.internal.chart;

import java.awt.Color;
import java.awt.Paint;
import java.text.NumberFormat;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.presentation.contest.internal.ContestData;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.Utility;
import org.icpc.tools.presentation.contest.internal.nls.Messages;
import org.icpc.tools.presentation.core.chart.AbstractChartPresentation;
import org.icpc.tools.presentation.core.chart.Series;

public class JudgementChart extends AbstractChartPresentation {
	protected static final NumberFormat sFormat = NumberFormat.getNumberInstance();
	protected static final NumberFormat msFormat = NumberFormat.getNumberInstance();
	static {
		sFormat.setMinimumFractionDigits(1);
		sFormat.setMaximumFractionDigits(1);
		sFormat.setGroupingUsed(false);
		msFormat.setMaximumFractionDigits(0);
		msFormat.setGroupingUsed(false);
	}

	protected IContest contest = ContestData.getContest();

	class TimeSeries extends Series {
		public TimeSeries(Paint[] paint) {
			super(paint);
		}

		@Override
		public String getValueLabel(int i) {
			double d = getValue(i);
			if (d == 0)
				return "";
			if (d > 950)
				return sFormat.format(d / 1000.0) + "s";
			return msFormat.format(d) + "ms";
		}
	}

	public JudgementChart() {
		super(Type.BAR, Messages.judgementTimeByProblem, "Time");
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
		Color[] colorAttempt = new Color[numProblems];
		Color[] colorSolved = new Color[numProblems];
		for (int j = 0; j < numProblems; j++) {
			labels[j] = problems[j].getLabel();
			colorAttempt[j] = problems[j].getColorVal();
			colorSolved[j] = Utility.alphaDarker(colorAttempt[j], 200, 0.8f);
		}

		setHorizontalLabels(labels);
		setSeries(new TimeSeries(colorAttempt), new TimeSeries(colorSolved));
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

		double[] attemptTime = new double[numProblems];
		double[] solvedTime = new double[numProblems];
		int[] attemptCount = new int[numProblems];
		int[] solvedCount = new int[numProblems];
		IContest clone = ((Contest) contest).clone(false);

		ISubmission[] submissions = clone.getSubmissions();
		for (ISubmission s : submissions) {
			IJudgement[] sjs = clone.getJudgementsBySubmissionId(s.getId());
			if (sjs != null && sjs.length > 0) {
				IJudgement sj = sjs[sjs.length - 1];
				if (sj != null && sj.getEndTime() != null) {
					int problemIndex = clone.getProblemIndex(s.getProblemId());
					if (clone.isSolved(s)) {
						solvedTime[problemIndex] += sj.getEndTime() - sj.getStartTime();
						solvedCount[problemIndex]++;
					} else {
						attemptTime[problemIndex] += sj.getEndTime() - sj.getStartTime();
						attemptCount[problemIndex]++;
					}
				}
			}
		}
		for (int j = 0; j < numProblems; j++) {
			if (attemptCount[j] > 0)
				attemptTime[j] /= attemptCount[j];
			if (solvedCount[j] > 0)
				solvedTime[j] /= solvedCount[j];
		}

		getSeries()[0].setValues(solvedTime);
		getSeries()[1].setValues(attemptTime);
		getSeries()[0].setTitle(Messages.solutionJudgementTime);
		getSeries()[1].setTitle(Messages.attemptJudgementTime);
	}
}