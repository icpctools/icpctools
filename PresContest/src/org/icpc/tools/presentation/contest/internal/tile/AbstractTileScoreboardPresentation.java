package org.icpc.tools.presentation.contest.internal.tile;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.Recent;
import org.icpc.tools.contest.model.resolver.SelectType;
import org.icpc.tools.contest.model.resolver.SubmissionInfo;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.TeamUtil;
import org.icpc.tools.presentation.contest.internal.TeamUtil.Style;

public abstract class AbstractTileScoreboardPresentation extends AbstractICPCPresentation {
	protected static final int DEFAULT_COLUMNS = 2;
	protected static final int DEFAULT_ROWS = 14;
	protected static final int TILE_H_GAP = 5;
	protected static final int TILE_V_GAP = 3;

	protected int rows = DEFAULT_ROWS;
	protected int columns = DEFAULT_COLUMNS;
	protected Style overrideStyle;

	protected TeamTileHelper tileHelper;
	protected Dimension tileDim = null;

	private Point2D[] teamTargets;
	private double[] teamZoomTargets;
	protected final Map<String, TileAnimator> teamMap = new HashMap<>();
	private Runnable cacheRunnable;

	protected TeamTileHelper createTileHelper() {
		tileDim = new Dimension((width - (columns - 1) * TILE_H_GAP) / columns,
				(height - (rows - 1) * TILE_V_GAP) / rows);
		return new TeamTileHelper(tileDim, getContest(), overrideStyle);
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		if (d.width == 0 || d.height == 0)
			return;

		tileHelper = createTileHelper();

		cacheTiles();
		initTeamPosition();
	}

	protected void cacheTiles() {
		if (cacheRunnable != null)
			return;

		cacheRunnable = new Runnable() {
			@Override
			public void run() {
				tileHelper.cacheAllTiles();
				cacheRunnable = null;
			}
		};
		execute(cacheRunnable);
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();
		initTeamPosition();
	}

	/**
	 * Receives a parameter 'dt' (delta-time) specifying the amount of time, in milliseconds, that
	 * has elapsed since the last time the current presentation was updated; updates the Y position
	 * of all teams by moving each team a little bit, based on the elapsed time, toward its target
	 * location (where it belongs on the scoreboard based on current standings). Also applies
	 * "scrolling" to move the entire scoreboard up/down based on what team (row) has been selected.
	 *
	 * @param dt - the number of msec that have elapsed since the last call to this method
	 */
	@Override
	public void incrementTimeMs(long dt) {
		ITeam[] teams = getContest().getOrderedTeams();
		if (teams.length != teamTargets.length)
			initTeamPosition();
		updateTeamTargets(teams, teamTargets);
		updateTeamPosition(dt, teams);

		updateTeamZoomTargets(teams, teamZoomTargets);
		updateTeamZoomPosition(dt, teams);

		super.incrementTimeMs(dt);
	}

	/**
	 * Initializes a map of each team's vertical (Y) position on the scoreboard display so that it
	 * corresponds to the team's current position (standing) in the contest. The constructed map
	 * contains an entry for each team (using the team ID as the key); each entry contains two float
	 * elements: current scoreboard location, and speed. The method assigns a "speed" of zero for
	 * each team; the speed value is used later when moving teams around on the scoreboard display.
	 */
	protected void initTeamPosition() {
		IContest contest = getContest();
		if (contest == null)
			return;

		ITeam[] teams = contest.getOrderedTeams();
		int size = teams.length;
		if (teamTargets == null || teamTargets.length != size) {
			teamTargets = new Point2D.Double[size];
			for (int i = 0; i < size; i++) {
				teamTargets[i] = new Point2D.Double();
			}
		}
		updateTeamTargets(teams, teamTargets);

		if (teamZoomTargets == null || teamZoomTargets.length != size) {
			teamZoomTargets = new double[size];
		}
		updateTeamZoomTargets(teams, teamZoomTargets);

		for (int i = 0; i < size; i++) {
			teamMap.put(teams[i].getId(), new TileAnimator(teamTargets[i]));
		}
	}

	protected void updateTeamTargets(ITeam[] teams, Point2D[] targets) {
		for (int i = 0; i < teams.length; i++) {
			targets[i].setLocation(i / rows, i % rows);
		}
	}

	protected void updateTeamZoomTargets(ITeam[] teams, double[] targets) {
		IContest contest = getContest();
		for (int i = 0; i < teams.length; i++) {
			targets[i] = 1.0;
			ITeam team = teams[i];
			Recent recent = ((Contest) contest).getRecent(team);
			if (recent != null) {
				long age = System.currentTimeMillis() - recent.time;
				if (age > 0 && age < 30000) {
					targets[i] = 1.1 - age * 0.1 / 30000.0;
				}
			}
		}
	}

	/**
	 * Computes the desired screen position (the "target position") for each team based on the
	 * team's current standing (order in the contest standings), then moves the team a little bit in
	 * the direction of its target position (that is, to update the team's current position based on
	 * its speed). The new current position of the team is stored into the global map "teamMap".
	 *
	 * @param dt - the elapsed time, in seconds, since the last time the team's position was
	 *           updated. (Note that this does not mean the team will arrive at its desired position
	 *           in that time; only that it moves for the specified amount of time toward its target
	 *           position based on its current speed.)
	 */
	protected void updateTeamPosition(long dt, ITeam[] teams) {
		IContest contest = getContest();
		if (contest == null)
			return;

		int size = teams.length;
		for (int i = 0; i < size; i++) {
			ITeam team = teams[i];

			// get the team's current location
			String id = team.getId();
			TileAnimator current = teamMap.get(id);

			// update the team's target location and time
			if (current == null) {
				current = new TileAnimator(teamTargets[i]);
				teamMap.put(id, current);
			} else {
				current.setTarget(teamTargets[i]);
				current.incrementTimeMs(dt);
			}
		}
	}

	/**
	 * Computes the desired screen position (the "target position") for each team based on the
	 * team's current standing (order in the contest standings), then moves the team a little bit in
	 * the direction of its target position (that is, to update the team's current position based on
	 * its speed). The new current position of the team is stored into the global map "teamMap".
	 *
	 * @param dt - the elapsed time, in seconds, since the last time the team's position was
	 *           updated. (Note that this does not mean the team will arrive at its desired position
	 *           in that time; only that it moves for the specified amount of time toward its target
	 *           position based on its current speed.)
	 */
	protected void updateTeamZoomPosition(long dt, ITeam[] teams) {
		IContest contest = getContest();
		if (contest == null)
			return;

		int size = teams.length;
		for (int i = 0; i < size; i++) {
			ITeam team = teams[i];

			// get the team's current location
			String id = team.getId();
			TileAnimator current = teamMap.get(id);

			if (current != null) {
				current.setZoomTarget(teamZoomTargets[i]);
				current.incrementTimeMs(dt);
			}
		}
	}

	/**
	 * Default implementation, will typically be overridden by subclasses.
	 */
	protected void paintImpl(Graphics2D g) {
		IContest contest = getContest();
		if (contest == null)
			return;

		ITeam[] teams = contest.getOrderedTeams();
		for (int i = teams.length - 1; i >= 0; i--) {
			TileAnimator anim = teamMap.get(teams[i].getId());
			if (anim != null) {
				Point2D p = anim.getValue();
				int x = (int) (p.getX() * (tileDim.width + TILE_H_GAP));
				int y = (int) (p.getY() * (tileDim.height + TILE_V_GAP));

				tileHelper.paintTile(g, x, y, teams[i], getTimeMs());
			}
		}
	}

	@Override
	public void paint(Graphics2D g) {
		IContest c = getContest();
		if (c == null || c.getNumTeams() == 0)
			return;

		paintImpl(g);

		if (!tileHelper.areTilesCached())
			cacheTiles();
	}

	public void setScrollToRow(int i) {
		// ignore
	}

	public void setShowLegend(boolean b) {
		// ignore
	}

	public void setShowRunInfo(boolean b) {
		// ignore
	}

	public void setFocusOnTeam(int id) {
		// ignore
	}

	public boolean getShowRunInfo() {
		return false;
	}

	public void setSelectedTeam(ITeam team, SelectType type) {
		// ignore
	}

	public boolean setHighlightedRun(SubmissionInfo submission) {
		return false;
	}

	@Override
	public void setProperty(String value) {
		if (value.startsWith("rows:")) {
			try {
				rows = Integer.parseInt(value.substring(5));
				setSize(getSize());
			} catch (Exception e) {
				// ignore
			}
		} else if (value.startsWith("columns:")) {
			try {
				columns = Integer.parseInt(value.substring(8));
				setSize(getSize());
			} catch (Exception e) {
				// ignore
			}
		} else if (value.startsWith("style:")) {
			try {
				String s = value.substring(6);
				overrideStyle = TeamUtil.getStyleByString(s);
				setSize(getSize());
			} catch (Exception e) {
				// ignore
			}
		}
	}
}