package org.icpc.tools.presentation.contest.internal.scoreboard;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;

/**
 * Group/region leaderboard.
 */
public class GroupLeaderboardPresentation extends AbstractScoreboardPresentation {
	protected List<String> recentGroups = new ArrayList<>();
	protected String currentGroupId;
	protected String currentGroupName;

	public GroupLeaderboardPresentation() {
		super();
		teamsPerScreen = 10;
	}

	@Override
	public void aboutToShow() {
		IContest contest2 = getContest();
		currentGroupId = null;
		currentGroupName = "<n/a>";

		if (contest2 != null) {
			ITeam[] teams = contest2.getTeams();
			for (int i = 0; i < teams.length; i++) {
				String[] groupIds = teams[i].getGroupIds();
				if (!contest2.isTeamHidden(teams[i]) && groupIds != null) {
					for (String groupId : groupIds) {
						if (!recentGroups.contains(groupId)) {
							currentGroupId = groupId;
							currentGroupName = contest2.getGroupById(currentGroupId).getName();
							recentGroups.add(currentGroupId);
							i += teams.length;
						}
					}
				}
			}
		}

		if (currentGroupId == null) {
			if (recentGroups.size() == 0)
				return;

			currentGroupId = recentGroups.get(0);
			currentGroupName = contest2.getGroupById(currentGroupId).getName();
			recentGroups.remove(0);
		}

		super.aboutToShow();
	}

	@Override
	protected String getTitle() {
		if (currentGroupName == null)
			return "Leaders";

		return currentGroupName + " Leaders";
	}

	@Override
	protected double[] getTeamYTargets(ITeam[] teams) {
		int size = teams.length;
		int count = 0;
		double[] targets = new double[size];
		for (int i = 0; i < size; i++) {
			ITeam team = teams[i];
			String[] groupIds = team.getGroupIds();
			for (String groupId : groupIds) {
				if (currentGroupId == null || currentGroupId.equalsIgnoreCase(groupId)) {
					targets[i] = count++;
					continue;
				}
				targets[i] = teamsPerScreen * 2;
			}

		}
		return targets;
	}

	@Override
	protected void paintLegend(Graphics2D g) {
		// no legend
	}
}