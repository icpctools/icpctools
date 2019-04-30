package org.icpc.tools.presentation.contest.internal.tile;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;

public class TileScoreboardPresentation extends ScrollingTileScoreboardPresentation {
	private List<Integer> breaks = new ArrayList<>();

	@Override
	protected String getTitle() {
		return "Current Standings";
	}

	@Override
	protected void updateTeamTargets(ITeam[] teams, Point2D[] targets) {
		IContest contest = getContest();
		int size = teams.length;
		breaks.clear();
		int numSolved = 0;
		if (size > 0)
			numSolved = contest.getStanding(teams[0]).getNumSolved();
		for (int i = 0; i < size; i++) {
			targets[i].setLocation(0, i);
			int num = contest.getStanding(teams[i]).getNumSolved();
			if (num != numSolved) {
				numSolved = num;
				breaks.add(i);
			}
		}
	}

	@Override
	protected void paintTiles(Graphics2D g, int hScroll) {
		super.paintTiles(g, hScroll);

		// paint problem totals
		Graphics2D gg = (Graphics2D) g.create();
		gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		gg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		int numTeams = getContest().getOrderedTeams().length;
		int col = numTeams / rows;
		int xx = col * (tileDim.width + TILE_H_GAP);
		int yy = (int) ((numTeams - col * rows) * (tileDim.getHeight() + TILE_V_GAP));
		gg.translate(xx, yy);
		tileHelper.paintTileStats(gg);
		gg.dispose();

		// draw lines at each change in num solved
		int arc = tileDim.width / 40;
		g.setColor(Color.LIGHT_GRAY);
		g.setStroke(new BasicStroke(2f));
		for (Integer i : breaks) {
			int x = ((i / rows) * (tileDim.width + TILE_H_GAP));
			int y = ((i % rows) * (tileDim.height + TILE_V_GAP)) - (TILE_V_GAP + 1) / 2;
			g.drawLine(x + arc, y, x + tileDim.width - arc, y);
		}
	}
}