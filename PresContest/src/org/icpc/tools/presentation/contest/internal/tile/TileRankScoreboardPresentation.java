package org.icpc.tools.presentation.contest.internal.tile;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.nls.Messages;

public class TileRankScoreboardPresentation extends ScrollingTileScoreboardPresentation {
	private static final double RANK_GAP = 0.25;

	class Group {
		double y;
		int numSolved;
		int numRows;
	}

	private List<Group> groups = new ArrayList<>();
	private Font numFont;
	private Font solvedFont;

	public TileRankScoreboardPresentation() {
		super(Direction.VERTICAL, Header.LEFT);
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		final float dpi = 96;
		float size = tileDim.height * 36f * 0.95f / dpi;
		Font masterFont = ICPCFont.getMasterFont();
		numFont = masterFont.deriveFont(Font.BOLD, size * 1.9f);
		solvedFont = masterFont.deriveFont(Font.PLAIN, size * 0.85f);
	}

	@Override
	protected int getNumRows() {
		return super.getNumRows() + 3;
	}

	@Override
	protected void updateTeamTargets(ITeam[] teams, Point2D[] targets) {
		if (teams == null || teams.length == 0)
			return;

		double y = 0;
		int size = teams.length;
		IContest contest = getContest();
		groups.clear();
		int i = 0;
		while (i < size) {
			Group g = new Group();
			g.y = y;
			g.numSolved = contest.getStanding(teams[i]).getNumSolved();

			// count number of teams with same score
			int groupSize = 0;
			int j = i;
			while (j < size && g.numSolved == contest.getStanding(teams[j]).getNumSolved()) {
				j++;
				groupSize++;
			}

			// lay out group
			int numRows = (groupSize + columns - 1) / columns;
			for (j = 0; j < groupSize; j++)
				targets[i + j].setLocation(j / numRows, y + (j % numRows));

			g.numRows = numRows;
			groups.add(g);

			y += (numRows + RANK_GAP);
			i += groupSize;
		}
	}

	@Override
	protected void paintHeader(Graphics2D g) {
		int vScrollv = (int) (vScroll.getValue() * height);
		g.translate(0, -vScrollv);

		g.setFont(solvedFont);
		FontMetrics fm2 = g.getFontMetrics();
		g.setFont(numFont);
		FontMetrics fm = g.getFontMetrics();
		int i = 0;
		while (i < groups.size()) {
			Group gr = groups.get(i);
			int arc = tileDim.width / 40;

			g.setColor(Color.DARK_GRAY);
			g.fillRoundRect(0, (int) (gr.y * (tileDim.height + TILE_V_GAP)), margin - TILE_H_GAP,
					gr.numRows * (tileDim.height + TILE_V_GAP) - TILE_V_GAP, arc, arc);

			String s = gr.numSolved + "";
			g.setColor(Color.WHITE);
			g.setFont(numFont);
			g.drawString(s, (margin - TILE_H_GAP - fm.stringWidth(s)) / 2,
					fm.getAscent() + (int) (gr.y * (tileDim.height + TILE_V_GAP)) + TILE_V_GAP * 2);

			if (gr.numRows > 1) {
				s = Messages.numSolved;
				g.setColor(Color.LIGHT_GRAY);
				g.setFont(solvedFont);
				g.drawString(s, (margin - TILE_H_GAP - fm2.stringWidth(s)) / 2,
						fm.getAscent() + fm2.getAscent() + (int) (gr.y * (tileDim.height + TILE_V_GAP)) + TILE_V_GAP * 4);
			}

			i++;
		}
	}

	@Override
	protected void paintBackground(Graphics2D g) {
		int arc = tileDim.width / 70;
		g.setColor(TeamTileHelper.TILE_BG);
		for (Group gr : groups) {
			for (int i = 0; i < gr.numRows; i += 2) {
				for (int j = 0; j < columns; j++) {
					g.fillRoundRect((tileDim.width + TILE_H_GAP) * j,
							(int) ((tileDim.getHeight() + TILE_V_GAP) * (i + gr.y)), tileDim.width + TILE_H_GAP / 2,
							tileDim.height + TILE_V_GAP / 2, arc, arc);
				}
			}
		}
	}

	@Override
	protected String getTitle() {
		return Messages.titleTeamRank;
	}
}