package org.icpc.tools.presentation.contest.internal.tile;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IProblemSummary;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.Recent;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.TextHelper;

public class TeamTileHelper {
	private static final int IN_TILE_GAP = 3;
	protected static final Color TILE_BG = new Color(50, 50, 50);
	private static final Color PROBLEM_BG = new Color(90, 90, 90);

	private Font rankFont;
	private Font teamFont;
	private Font statusFont;
	private Font problemFont;
	private Font penaltyFont;

	private Dimension tileDim = null;
	private IContest contest;

	public TeamTileHelper(Dimension tileDim, IContest contest) {
		this.tileDim = tileDim;
		this.contest = contest;

		setup();
	}

	protected void setSize(Dimension d) {
		this.tileDim = d;
	}

	protected void setup() {
		final float dpi = 96;

		float size = tileDim.height * 36f * 0.95f / dpi;
		teamFont = ICPCFont.deriveFont(Font.BOLD, size * 1.4f);
		rankFont = ICPCFont.deriveFont(Font.BOLD, size * 1.4f);
		statusFont = ICPCFont.deriveFont(Font.PLAIN, size * 0.7f);
		problemFont = ICPCFont.deriveFont(Font.PLAIN, size * 0.45f);
		penaltyFont = ICPCFont.deriveFont(Font.BOLD, size * 0.85f);
	}

	public void paintTile(Graphics2D g, int x, int y, ITeam team, long timeMs) {
		paintTile(g, x, y, 1.0, team, timeMs);
	}

	public void paintTile(Graphics2D g, int x, int y, double scale, ITeam team, long timeMs) {
		Graphics2D gg = (Graphics2D) g.create();
		gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		gg.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		if (scale != 1.0) {
			gg.translate(x + tileDim.width / 2, y + tileDim.height / 2);
			gg.scale(scale, scale);
			gg.translate(-tileDim.width / 2, -tileDim.height / 2);
		} else
			gg.translate(x, y);

		// paint recent change color
		Recent recent = ((Contest) contest).getRecent(team);
		if (recent != null) {
			long age = timeMs - recent.time;
			if (age > 0 && age < 30000) {
				Color c = ICPCColors.getStatusColor(recent.status, age);
				if (c != null) {
					gg.setColor(c);
					int arc = tileDim.width / 40;
					gg.fillRoundRect(0, 0, tileDim.width, tileDim.height, arc, arc);
				}
			}
		}

		// draw image & name
		gg.setFont(rankFont);
		FontMetrics fm = gg.getFontMetrics();
		int ww = fm.stringWidth("199");

		gg.clipRect(0, 0, tileDim.width, tileDim.height);

		IOrganization org = contest.getOrganizationById(team.getOrganizationId());
		if (org != null) {
			BufferedImage logoImg = org.getLogoImage(tileDim.height - 10, tileDim.height - 10, true, true);
			if (logoImg != null) {
				gg.drawImage(logoImg, ww + (tileDim.height - logoImg.getWidth()) / 2,
						(tileDim.height - logoImg.getHeight()) / 2, null);
				logoImg.flush();
			}
		}

		paintTileForeground(gg, team, timeMs);

		gg.dispose();
	}

	private void paintName(Graphics2D g, ITeam team, int ww, int maxwid) {
		g.setFont(teamFont);
		FontMetrics fm = g.getFontMetrics();

		String s = team.getActualDisplayName();

		g.setColor(Color.WHITE);
		TextHelper text = new TextHelper(g, s);
		text.drawFit(ww + tileDim.height + IN_TILE_GAP, (tileDim.height * 7 / 10 - fm.getAscent()) / 2 - 1, maxwid);
	}

	private void paintTileForeground(Graphics2D g, ITeam team, long timeMs) {
		g.setFont(rankFont);
		FontMetrics fm = g.getFontMetrics();
		int ww = fm.stringWidth("199");

		g.setFont(penaltyFont);
		fm = g.getFontMetrics();
		int maxwid = tileDim.width - tileDim.height - ww - IN_TILE_GAP * 3 - 2 - fm.stringWidth("1999");

		// draw rank & score
		g.setFont(rankFont);
		g.setColor(Color.WHITE);
		fm = g.getFontMetrics();

		IStanding standing = contest.getStanding(team);
		String s = standing.getRank();
		g.drawString(s, (ww - fm.stringWidth(s)) / 2, (tileDim.height + fm.getAscent()) / 2);

		// draw name
		paintName(g, team, ww, maxwid - 3);

		g.setFont(teamFont);
		fm = g.getFontMetrics();
		if (standing.getNumSolved() > 0) {
			s = standing.getNumSolved() + "";
			g.drawString(s, tileDim.width - IN_TILE_GAP * 2 - fm.stringWidth(s),
					(tileDim.height * 7 / 10 + fm.getAscent()) / 2 - 2);
		}

		g.setColor(Color.LIGHT_GRAY);
		g.setFont(penaltyFont);
		fm = g.getFontMetrics();
		if (standing.getTime() > 0) {
			s = standing.getTime() + "";
			g.drawString(s, tileDim.width - IN_TILE_GAP * 2 - fm.stringWidth(s),
					tileDim.height * 17 / 20 + fm.getAscent() / 2 - 3);
		}

		// draw a rounded-rectangle representation for each problem
		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;
		if (numProblems == 0)
			return;

		int y = tileDim.height * 7 / 10 - 2;
		int h = tileDim.height * 3 / 10;
		float w = (float) maxwid / (float) numProblems;
		// int www = (int) w;
		int xx = ww + tileDim.height + IN_TILE_GAP;
		int arc = tileDim.width / 120;

		g.setFont(statusFont);
		fm = g.getFontMetrics();
		g.setFont(problemFont);
		FontMetrics fm2 = g.getFontMetrics();

		for (int i = 0; i < numProblems; i++) {
			IResult r = contest.getResult(team, i);
			Color c = null;
			if (ContestUtil.isRecent(contest, r)) {
				// flash more than once per second
				int k = (int) ((timeMs * 45.0 / 1000.0) % (ICPCColors.COUNT2 * 2));
				if (k > (ICPCColors.COUNT2 - 1))
					k = (ICPCColors.COUNT2 * 2 - 1) - k;

				if (r.getStatus() == Status.SOLVED) {
					if (r.isFirstToSolve())
						c = ICPCColors.FIRST_TO_SOLVE3[k];
					else
						c = ICPCColors.SOLVED3[k];
				} else if (r.getStatus() == Status.FAILED)
					c = ICPCColors.FAILED3[k];
				else if (r.getStatus() == Status.SUBMITTED)
					c = ICPCColors.PENDING3[k];
			} else {
				if (r.getStatus() == Status.SOLVED) {
					if (r.isFirstToSolve())
						c = ICPCColors.FIRST_TO_SOLVE[5];
					else
						c = ICPCColors.SOLVED[5];
				} else if (r.getStatus() == Status.FAILED)
					c = ICPCColors.FAILED[5];
				else if (r.getStatus() == Status.SUBMITTED)
					c = ICPCColors.PENDING[5];
				else {
					g.setColor(PROBLEM_BG);
					g.fillRoundRect(xx + (int) (w * i), y, (int) w - 3, h - 1, arc, arc);
					g.setColor(Color.LIGHT_GRAY);
					s = problems[i].getLabel();
					g.setFont(problemFont);
					g.drawString(s, (int) (xx + w * i + (w - fm2.stringWidth(s)) / 2) - 1,
							y + (h + fm2.getAscent()) / 2 - 1);
				}
			}

			if (c != null) {
				g.setColor(c);
				g.fillRoundRect(xx + (int) (w * i), y, (int) w - 3, h - 1, arc, arc);
				g.setColor(Color.WHITE);
				s = "";

				if (r.getNumSubmissions() > 0) {
					if (fm.stringWidth("9\u200A-\u200A999") > w - 5)
						s = r.getNumSubmissions() + "";
					else
						s = r.getNumSubmissions() + "\u200A-\u200A" + ContestUtil.getTime(r.getContestTime());

					g.setFont(statusFont);
					g.drawString(s, (int) (xx + w * i + (w - fm.stringWidth(s)) / 2) - 1, y + (h + fm.getAscent()) / 2 - 1);

					if (r.isFirstToSolve()) {
						g.setColor(ICPCColors.SOLVED_COLOR);
						g.drawRoundRect(xx + (int) (w * i), y, (int) w - 3, h - 1, arc, arc);
					}
				}
			}
		}
	}

	public void paintTileStats(Graphics2D g) {
		g.setFont(rankFont);
		FontMetrics fm = g.getFontMetrics();
		int ww = fm.stringWidth("199");

		g.setFont(penaltyFont);
		fm = g.getFontMetrics();

		// draw a rounded-rectangle representation for each problem
		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;
		if (numProblems == 0)
			return;

		int y = tileDim.height * 7 / 10;
		int h = tileDim.height * 3 / 10;
		int maxwid = tileDim.width - tileDim.height - ww - IN_TILE_GAP * 3 - 2 - fm.stringWidth("1999");
		int w = maxwid / numProblems;
		int xx = ww + tileDim.height + IN_TILE_GAP;
		int arc2 = tileDim.width / 120;

		g.setFont(statusFont);
		fm = g.getFontMetrics();

		for (int i = 0; i < numProblems; i++) {
			IProblemSummary ps = contest.getProblemSummary(i);
			if (ps.getNumPending() > 0)
				draw(g, ICPCColors.PENDING[5], xx + w * i, y - h * 5 / 4, w, h, arc2, ps.getNumPending() + "", fm);
			else
				draw(g, PROBLEM_BG, xx + w * i, y - h * 5 / 4, w, h, arc2, "", fm);

			if (ps.getNumSolved() > 0)
				draw(g, ICPCColors.SOLVED[5], xx + w * i, y, w, h, arc2, ps.getNumSolved() + "", fm);
			else
				draw(g, PROBLEM_BG, xx + w * i, y, w, h, arc2, "", fm);
		}
	}

	private static void draw(Graphics2D g, Color c, int x, int y, int w, int h, int arc, String s, FontMetrics fm) {
		g.setColor(c);
		g.fillRoundRect(x, y, w - 3, h - 1, arc, arc);
		g.setColor(Color.WHITE);
		g.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + (h + fm.getAscent()) / 2 - 1);
	}
}