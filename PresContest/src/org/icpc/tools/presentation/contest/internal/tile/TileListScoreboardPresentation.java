package org.icpc.tools.presentation.contest.internal.tile;

import java.awt.geom.Point2D;

import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.nls.Messages;

public class TileListScoreboardPresentation extends ScrollingTileScoreboardPresentation {
	@Override
	protected String getTitle() {
		return Messages.titleAlphabeticListOfTeams;
	}

	@Override
	protected void updateTeamTargets(ITeam[] teams, Point2D[] targets) {
		if (teams == null || teams.length == 0)
			return;

		int size = teams.length;
		int[] sort = new int[size];
		String[] names = new String[size];
		for (int i = 0; i < size; i++) {
			sort[i] = i;
			names[i] = teams[i].getActualDisplayName();
		}

		for (int i = 0; i < size - 1; i++) {
			for (int j = i + 1; j < size; j++) {
				if (names[sort[i]].compareTo(names[sort[j]]) > 0) {
					int t = sort[i];
					sort[i] = sort[j];
					sort[j] = t;
				}
			}
		}

		for (int i = 0; i < size; i++)
			targets[sort[i]].setLocation(0, i);
	}
}