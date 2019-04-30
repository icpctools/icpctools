package org.icpc.tools.presentation.contest.internal.presentations.old;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Dimension2D;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ShadedRectangle;
import org.icpc.tools.presentation.contest.internal.Utility;

public class LeaderTickerPresentation extends AbstractTickerPresentation {
	public static final int BORDER2 = 2;
	protected Font titleFont;
	protected boolean showLeader = false;

	public class LeaderTicker implements ITicker {
		protected int rank;
		protected String name;
		protected int solved;
		protected int penalty;

		public LeaderTicker(int rank, String name, int solved, int penalty) {
			this.rank = rank;
			this.name = name;
			this.solved = solved;
			this.penalty = penalty;
		}

		@Override
		public float getWidth(Graphics2D g) {
			g.setFont(titleFont);
			FontMetrics fm = g.getFontMetrics();
			if (rank < 12)
				return fm.stringWidth(rank + ": " + name) + 20;
			return fm.stringWidth(rank + ": " + name) + 150;
		}

		@Override
		public void paint(Graphics2D g, float x, float time2) {
			int height3 = getSize().height;
			g.setFont(titleFont);
			FontMetrics fm = g.getFontMetrics();
			g.setColor(Color.WHITE);
			int cube = fm.getHeight();
			g.drawString(rank + ": " + name, x, BORDER2 + fm.getAscent());
			Color c = ICPCColors.SOLVED_COLOR;
			g.translate(x, 0);
			ShadedRectangle.drawRoundRect(g, 0, height3 - BORDER2 - cube, cube * 3 / 2, cube, c);

			g.setColor(Color.WHITE);
			Utility.drawString3D(g, solved + "", cube * 3f / 4 - fm.stringWidth(solved + "") / 2f,
					height3 - BORDER2 - fm.getDescent() - 1);
			g.drawString(penalty + "", cube * 3f / 2 + 7, height3 - BORDER2 - fm.getDescent() - 1);
			g.translate(-x, 0);
		}

		@Override
		public void paint(Graphics2D g, Dimension2D d, float count) {
			paint(g, 0, count);
		}
	}

	public LeaderTickerPresentation() {
		setSpeed(12);
		// verticalTicker = true;
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		final float dpi = 96;
		float rowHeight = (height - BORDER2 * 2) / 2f;
		float inch = rowHeight / dpi;
		int size = (int) (inch * 72f); // * 0.95f);
		titleFont = new Font("Tahoma", Font.BOLD, size);
	}

	@Override
	public void paintBackground(Graphics2D g) {
		// g.setColor(Color.BLACK);
		// g.fillRect(0, 0, (int)width, height + BORDER * 2);

		// g.setColor(Color.GRAY);
		// g.drawRect(0, 0, width, height);

		/*g.setColor(Color.WHITE);
		g.fillRect(0, 0, 60, (int)height + BORDER * 2);*/
	}

	@Override
	public void paintForeground(Graphics2D g) {
		if (!showLeader)
			return;

		int WID = 70;
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, WID, height);

		g.setFont(titleFont);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(Color.BLACK);
		String s = "Top 12";
		g.drawString(s, WID - BORDER - fm.stringWidth(s), BORDER2 + fm.getAscent());
		s = "Status";
		g.drawString(s, WID - BORDER - fm.stringWidth(s), height - BORDER2 - fm.getDescent());

		// g.setClip(oldClip);
	}

	@Override
	protected void newContent() {
		IContest sc = getContest();
		ITeam[] teams = sc.getOrderedTeams();
		for (int i = 0; i < 12; i++) {
			IStanding standing = sc.getStanding(teams[i]);
			append(new LeaderTicker(i + 1, teams[i].getName(), standing.getNumSolved(), standing.getTime()));
		}
	}
}