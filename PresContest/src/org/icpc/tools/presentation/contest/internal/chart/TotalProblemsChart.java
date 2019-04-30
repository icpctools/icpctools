package org.icpc.tools.presentation.contest.internal.chart;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.presentation.contest.internal.ContestData;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.core.chart.AbstractChartPresentation;
import org.icpc.tools.presentation.core.chart.Series;
import org.icpc.tools.presentation.core.chart.Series.LinePoint;

public class TotalProblemsChart extends AbstractChartPresentation {
	private static final int MIN = 5;

	protected IContest contest = ContestData.getContest();
	protected int numValues;

	public TotalProblemsChart() {
		super(Type.LINE, "Total Problem Submissions", "# of submissions every 5 minutes");
		showValueLabels = false;
		setFont(ICPCFont.getMasterFont());
	}

	protected static String[] getHourLabels(int numValues, int min) {
		String[] labels = new String[numValues];
		if (60 / min < labels.length)
			labels[60 / min] = "1 hour";
		int count = 2;
		for (int i = 120 / min; i < numValues; i += 60 / min)
			labels[i] = count++ + " hours";

		return labels;
	}

	@Override
	protected void setupChart() {
		if (contest == null)
			return;

		numValues = contest.getContestTimeOfLastEvent() / 60000 / MIN;
		if (numValues < 1)
			numValues = 1;

		setHorizontalLabels(getHourLabels(numValues, MIN));
		setSeries(new Series(numValues, ICPCColors.FAILED_COLOR), new Series(numValues, ICPCColors.PENDING_COLOR),
				new Series(numValues, ICPCColors.SOLVED_COLOR));
		for (Series s : getSeries())
			s.setLinePoint(LinePoint.OVAL);
	}

	@Override
	protected void updateData() {
		if (contest == null)
			return;

		int[] solns = new int[numValues];
		int[] fails = new int[numValues];
		int[] pends = new int[numValues];
		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission s : submissions) {
			int rt = (int) Math.max(0, s.getContestTime() / 1000L / 60 / MIN);
			rt = Math.min(rt, numValues - 1);
			Status status = contest.getStatus(s);
			if (status == Status.SOLVED)
				solns[rt]++;
			else if (status == Status.FAILED)
				fails[rt]++;
			else
				pends[rt]++;
		}

		getSeries()[0].setValues(fails); // NPE
		getSeries()[1].setValues(pends);
		getSeries()[2].setValues(solns);

		getSeries()[0].setTitle("# of failed submissions");
		getSeries()[1].setTitle("# of pending submissions");
		getSeries()[2].setTitle("# of solved submissions");
	}
}