package org.icpc.tools.presentation.contest.internal.scoreboard;

import java.awt.Graphics2D;

import org.icpc.tools.presentation.contest.internal.nls.Messages;

/**
 * Contest leaderboard, shows the top teams.
 */
public class LeaderboardPresentation extends AbstractScoreboardPresentation {
	@Override
	protected String getTitle() {
		return Messages.titleLeaderboard;
	}

	@Override
	protected void paintLegend(Graphics2D g) {
		// no legend
	}
}