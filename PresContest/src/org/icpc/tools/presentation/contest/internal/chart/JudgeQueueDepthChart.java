package org.icpc.tools.presentation.contest.internal.chart;

import java.awt.BasicStroke;
import java.awt.Paint;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.presentation.contest.internal.ContestData;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.nls.Messages;
import org.icpc.tools.presentation.core.chart.AbstractChartPresentation;
import org.icpc.tools.presentation.core.chart.Series;

public class JudgeQueueDepthChart extends AbstractChartPresentation {
	private int INTERVAL = 1000; // 1s interval
	private int numPoints;

	protected IContest contest = ContestData.getContest();

	class TimeSeries extends Series {
		public TimeSeries(Paint[] paint) {
			super(paint);
		}

		@Override
		public String getValueLabel(int i) {
			return "";
		}
	}

	public JudgeQueueDepthChart() {
		super(Type.LINE, Messages.judgeQueueDepth, Messages.time);
		setFont(ICPCFont.getMasterFont());
		showValueLabels = false;
	}

	@Override
	protected void setupChart() {
		if (contest == null)
			return;

		long dur = contest.getDuration();
		numPoints = (int) (dur / INTERVAL);
		if (numPoints < 1)
			numPoints = 1;

		String[] labels = new String[numPoints];
		if (3_600_000 / INTERVAL < numPoints)
			labels[3_600_000 / INTERVAL] = "1 hour";
		int count = 2;
		for (int i = 7_200_000 / INTERVAL; i < numPoints; i += 3_600_000 / INTERVAL)
			labels[i] = count++ + " hours";

		setHorizontalLabels(labels);
		setVerticalAxisTitle("Depth");
		Series s = new Series(numPoints, ICPCColors.SOLVED_COLOR);
		s.setStroke(new BasicStroke(1f));
		setSeries(s);

		getSeries()[0].setTitle("Queued Submissions");
	}

	@Override
	protected void updateData() {
		if (contest == null || getSeries() == null)
			return;

		long now = contest.getContestTimeOfLastEvent();
		int ms2 = (int) (Math.floor(now / (double) INTERVAL));

		int[] arr = new int[numPoints];
		ISubmission[] subs = contest.getSubmissions();
		for (ISubmission s : subs) {
			int ms1 = (int) (Math.ceil(s.getContestTime() / (double) INTERVAL));
			IJudgement[] js = contest.getJudgementsBySubmissionId(s.getId());
			if (js != null && js.length > 0) {
				IJudgement j = js[js.length - 1];
				if (j.getEndContestTime() != null)
					ms2 = (int) (Math.floor(j.getEndContestTime() / (double) INTERVAL));
			}
			if (ms1 > numPoints)
				ms1 = numPoints - 1;
			if (ms2 > numPoints)
				ms2 = numPoints - 1;

			for (int i = ms1; i < ms2; i++)
				arr[i]++;
		}
		getSeries()[0].setValues(arr);
	}
}