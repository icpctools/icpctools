package org.icpc.tools.presentation.contest.internal.chart;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.NDJSONFeedParser;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.presentation.contest.internal.ContestData;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.core.chart.AbstractChartPresentation;
import org.icpc.tools.presentation.core.chart.Series;

public class HistoricalComparisonChart extends AbstractChartPresentation {
	private static final Color[] COLORS = new Color[] { Color.MAGENTA, Color.BLUE, Color.YELLOW, Color.RED,
			Color.GREEN };
	private static final int MIN = 5;

	protected IContest contest = ContestData.getContest();
	protected long maxTime;
	protected int numValues;
	protected List<IContest> pastContests;
	protected List<Series> pastData = new ArrayList<>();

	public HistoricalComparisonChart() {
		super(Type.LINE, "Historical Comparison", "# of solutions");
		showValueLabels = false;
		setFont(ICPCFont.getMasterFont());
	}

	private static Contest loadContest(InputStream in) {
		Contest contest2 = new Contest();
		try {
			NDJSONFeedParser parser = new NDJSONFeedParser();
			parser.parse(contest2, in);
		} catch (Exception e) {
			return null;
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (Exception ex) {
				// ignore
			}
		}
		return contest2;
	}

	@Override
	public void init() {
		super.init();

		pastContests = new ArrayList<>();

		for (int year = 2013; year < 2017; year++) {
			try {
				String path = "/presentation/historical/events" + year + ".json";

				File f = ContestSource.getInstance().getFile(path);
				IContest contest2 = loadContest(new FileInputStream(f));
				if (contest2 != null) {
					pastContests.add(contest2);
					if (contest.getFreezeDuration() != null)
						maxTime = Math.max(maxTime, contest.getDuration() - contest.getFreezeDuration());
				}
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error loading past contest", e);
			}
		}

		numValues = (int) (maxTime / 60000 / MIN);
		if (numValues < 1)
			numValues = 1;
	}

	@Override
	protected void setupChart() {
		if (contest == null)
			return;

		setHorizontalLabels(TotalProblemsChart.getHourLabels(numValues, MIN));

		int size = pastContests.size();
		Series[] series = new Series[pastContests.size() + 1];
		for (int i = 0; i < size; i++) {
			series[i] = new Series(numValues, COLORS[i]);

			IContest contest2 = pastContests.get(i);
			int[] solns = new int[numValues];
			int[] total = new int[numValues];
			ISubmission[] submissions = contest2.getSubmissions();
			for (ISubmission s : submissions) {
				long time = s.getContestTime();
				if (time <= maxTime) {
					int rt = (int) Math.max(0, time / 1000L / 60 / MIN);
					rt = Math.min(rt, numValues - 1);
					if (contest2.isSolved(s))
						solns[rt]++;
					total[rt]++;
				}
			}

			for (int j = 1; j < numValues; j++)
				solns[j] += solns[j - 1];

			series[i].setValues(solns);
			series[i].setTitle("ICPC " + (2013 + i));
		}

		series[size] = new Series(numValues, COLORS[size]);
		// series[size].setLinePoint(LinePoint.OVAL);
		series[size].setTitle("Current");

		setSeries(series);
	}

	@Override
	protected void updateData() {
		if (contest == null)
			return;

		int[] solns = new int[numValues];
		for (int i = 0; i < numValues; i++)
			solns[i] = NO_DATA;

		int max = 0;
		int[] total = new int[numValues];
		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission s : submissions) {
			long time = s.getContestTime();
			if (time < maxTime) {
				int rt = (int) Math.max(0, time / 1000L / 60 / MIN);
				rt = Math.min(rt, numValues - 1);
				if (contest.isSolved(s))
					solns[rt]++;
				total[rt]++;
				max = Math.max(rt, max);
			}
		}

		for (int j = 1; j < max; j++)
			solns[j] += solns[j - 1];

		getSeries()[getSeries().length - 1].setValues(solns);

	}
}