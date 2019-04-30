package org.icpc.tools.presentation.contest.internal.scoreboard;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;

/**
 * List of group leaders.
 */
public class AllGroupsLeaderboardPresentation extends AbstractScoreboardPresentation {
	protected List<String> groupIds = new ArrayList<>();

	public AllGroupsLeaderboardPresentation() {
		super();
		teamsPerScreen = 10;
	}

	@Override
	public void aboutToShow() {
		groupIds.clear();

		IContest contest = getContest();
		if (contest == null)
			return;

		for (ITeam team : contest.getTeams()) {
			String[] groupIds2 = team.getGroupIds();
			if (groupIds2 != null && !contest.isTeamHidden(team)) {
				for (String groupId : groupIds2)
					if (!groupIds.contains(groupId))
						groupIds.add(groupId);
			}
		}

		super.aboutToShow();
	}

	@Override
	protected String getTitle() {
		return "All Group Leaders";
	}

	@Override
	protected double[] getTeamYTargets(ITeam[] teams) {
		List<String> found = new ArrayList<>(groupIds.size());

		int size = teams.length;
		int count = 0;
		double[] targets = new double[size];
		for (int i = 0; i < size; i++) {
			ITeam team = teams[i];
			String[] groupIds2 = team.getGroupIds();
			if (groupIds2 != null) {
				for (String groupId : groupIds2) {
					if (!found.contains(groupId)) {
						found.add(groupId);
						targets[i] = count++;
						continue;
					}

					targets[i] = teamsPerScreen * 2;
				}
			}
		}
		return targets;
	}

	@Override
	protected void paintLegend(Graphics2D g) {
		// g2.translate(width - 25, height - 230);
		// Legend.drawLegend(g2, time);
		// g2.translate(25 - width, -(height - 230));
	}
}