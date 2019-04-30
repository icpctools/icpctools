package org.icpc.tools.presentation.contest.internal.scoreboard;

import java.awt.Graphics2D;

/**
 * Contest leaderboard, shows the top teams.
 */
public class LeaderboardPresentation extends AbstractScoreboardPresentation {
	@Override
	protected String getTitle() {
		return "Current Medal Contenders";
	}

	@Override
	protected void paintLegend(Graphics2D g) {
		// no legend
	}
}