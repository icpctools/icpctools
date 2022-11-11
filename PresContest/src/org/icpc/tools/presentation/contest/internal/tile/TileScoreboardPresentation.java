package org.icpc.tools.presentation.contest.internal.tile;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.nls.Messages;

public class TileScoreboardPresentation extends ScrollingTileScoreboardPresentation {
	private List<Integer> breaks = new ArrayList<>();
	protected final Animator initScroll = new Animator(1, new Movement(0.4, 0.5));

	@Override
	protected String getTitle() {
		return Messages.titleCurrentStandings;
	}

	@Override
	public void incrementTimeMs(long dt) {
		initScroll.incrementTimeMs(dt);

		super.incrementTimeMs(dt);
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();

		if (getRepeatTimeMs() < 5000)
			initScroll.reset(1);
		else
			initScroll.reset(columns);
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

		if (getRepeatTimeMs() < 5000)
			initScroll.reset(1);
		else
			initScroll.setTarget(columns);
	}

	@Override
	protected void paintImpl(Graphics2D g) {
		preRenderRandom();

		double cols = initScroll.getValue();
		setColumns(cols);

		tileHelper.setApproximateRendering(Math.abs(cols - initScroll.getTarget()) > 1e-3);

		super.paintImpl(g);
	}

	private void preRenderRandom() {
		double cols = 1.000 + Math.random() * (columns - 1);
		setColumns(cols);

		TeamTileHelper tileHelper2 = createTileHelper();
		tileHelper2.joinCaches(tileHelper);
		tileHelper2.setLightMode(isLightMode());
		tileHelper2.setSize(tileHelper.getSize());
		tileHelper2.setApproximateRendering(cols > 1.001 && cols < 1.999);

		renderPool.getExecutor().submit(() -> {
			ITeam[] teams = getContest().getOrderedTeams();

			BufferedImage dummy = new BufferedImage(100, 100, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D gg = dummy.createGraphics();
			final int N_PRE_PER_FRAME = 11;
			for (int i = 0; i < N_PRE_PER_FRAME; i++) {
				int randomTopRow = (int) (Math.random() * Math.min(2 * rows, teams.length));
				ITeam team = teams[randomTopRow];

				tileHelper2.paintTile(gg, 0, 0, 1.0, team, (int) getRepeatTimeMs(), true);
			}
			gg.dispose();
		});
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
		int arc = tileDim.width / 90;
		g.setColor(isLightMode() ? Color.DARK_GRAY : Color.LIGHT_GRAY);
		g.setStroke(new BasicStroke(2f));
		for (Integer i : breaks) {
			int x = ((i / rows) * (tileDim.width + TILE_H_GAP));
			int y = ((i % rows) * (tileDim.height + TILE_V_GAP)) - (TILE_V_GAP + 1) / 2;
			g.drawLine(x + arc, y, x + tileDim.width - arc, y);
		}
	}
}