package org.icpc.tools.presentation.contest.internal.chart;

import java.awt.Color;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.TimeFilter;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.presentation.contest.internal.ContestData;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.core.chart.AbstractChartPresentation;
import org.icpc.tools.presentation.core.chart.Series;

public class ScoreboardChart extends AbstractChartPresentation {
	private static final int NUM_TEAMS = 4;
	private static final int MS_PER_MIN = 1000 * 60;
	private static final int MIN_PER_STEP = 5;

	protected IContest contest = ContestData.getContest();
	protected int numValues;

	private final Color[] COLORS = new Color[] { Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.YELLOW,
			Color.LIGHT_GRAY, Color.ORANGE, Color.PINK };

	public ScoreboardChart() {
		super(Type.LINE, "Scoreboard Standing", "Rank");
		showValueLabels = false;
		setFont(ICPCFont.getMasterFont());
	}

	@Override
	protected void setupChart() {
		if (contest == null || contest.getNumTeams() < NUM_TEAMS)
			return;

		numValues = (int) (contest.getContestTimeOfLastEvent() / MS_PER_MIN / MIN_PER_STEP);
		if (numValues < 1)
			numValues = 1;

		setHorizontalLabels(TotalProblemsChart.getHourLabels(numValues, MIN_PER_STEP));
		Series[] ser = new Series[NUM_TEAMS];
		for (int i = 0; i < NUM_TEAMS; i++) {
			ser[i] = new Series(numValues, COLORS[i]);
			ser[i].setLinePoint(null);
		}
		setSeries(ser);
		setData();
	}

	@Override
	protected void updateData() {
		// do nothing
	}

	private void setData() {
		int[][] teamData = new int[NUM_TEAMS][numValues];
		String[] tIds = new String[NUM_TEAMS];

		ITeam[] teams = contest.getOrderedTeams();

		// pick the current top X teams
		for (int i = 0; i < NUM_TEAMS; i++)
			tIds[i] = teams[i].getId();

		for (int i = 0; i < numValues; i++) {
			IContest c = ((Contest) contest).clone(new TimeFilter(contest, i * MIN_PER_STEP * 60000000));

			for (int j = 0; j < NUM_TEAMS; j++) {
				ITeam t = c.getTeamById(tIds[j]);
				teamData[j][i] = teams.length - c.getOrderOf(t);
				if (c.getStanding(t).getNumSolved() == 0)
					teamData[j][i] = 0;
			}
		}

		for (int i = 0; i < NUM_TEAMS; i++) {
			getSeries()[i].setValues(teamData[i]);
			getSeries()[i].setTitle(teams[i].getActualDisplayName());
		}
	}
}