package org.icpc.tools.presentation.contest.internal.tile;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

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
	protected static final Color TILE_BG_LIGHT = new Color(200, 200, 200);
	private static final Color PROBLEM_BG = new Color(90, 90, 90);
	private static final Color PROBLEM_BG_LIGHT = new Color(160, 160, 160);

	private Font rankFont;
	private Font teamFont;
	private Font statusFont;
	private Font problemFont;
	private Font penaltyFont;

	private Dimension tileDim = null;
	private IContest contest;
	private boolean lightMode;

	private Map<String, BufferedImage> nameImages = new HashMap<>();
	private Map<String, SoftReference<BufferedImage>> resultImages = new HashMap<>();
	private Map<String, BufferedImage> problemImages = new HashMap<>();

	public TeamTileHelper(Dimension tileDim, IContest contest) {
		this.tileDim = tileDim;
		this.contest = contest;

		setup();
	}

	protected void setSize(Dimension d) {
		this.tileDim = d;
	}

	protected void setLightMode(boolean lightMode) {
		this.lightMode = lightMode;
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

	public void paintTile(Graphics2D g, int x, int y, ITeam team, int timeMs) {
		paintTile(g, x, y, 1.0, team, timeMs);
	}

	public void paintTile(Graphics2D g, int x, int y, double scale, ITeam team, int timeMs) {
		Graphics2D gg = (Graphics2D) g.create();
		gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

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
		BufferedImage img = nameImages.get(team.getId());
		if (img == null) {
			g.setFont(teamFont);
			TextHelper text = new TextHelper(g, team.getActualDisplayName());

			img = new BufferedImage(text.getWidth() + 2, text.getHeight() + 4, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D gg = (Graphics2D) img.getGraphics();
			gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			gg.setFont(teamFont);
			gg.setColor(lightMode ? Color.BLACK : Color.WHITE);
			text.setGraphics(gg);
			text.draw(1, 2);
			gg.dispose();

			nameImages.put(team.getId(), img);
		}

		if (img.getWidth() - 2 < maxwid) {
			g.drawImage(img, ww + tileDim.height + IN_TILE_GAP - 1, tileDim.height * 1 / 10 - 1, null);
			return;
		}

		String hash = team.getId() + maxwid;
		img = nameImages.get(hash);
		if (img == null) {
			g.setFont(teamFont);
			TextHelper text = new TextHelper(g, team.getActualDisplayName());

			img = new BufferedImage(text.getWidth() + 2, text.getHeight() + 4, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D gg = (Graphics2D) img.getGraphics();
			gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			gg.setFont(teamFont);
			gg.setColor(lightMode ? Color.BLACK : Color.WHITE);
			text.setGraphics(gg);
			text.drawFit(1, 2, maxwid);
			gg.dispose();

			nameImages.put(hash, img);
		}
		g.drawImage(img, ww + tileDim.height + IN_TILE_GAP - 1, tileDim.height * 1 / 10 - 2, null);
	}

	private void paintTileForeground(Graphics2D g, ITeam team, int timeMs) {
		g.setFont(rankFont);
		FontMetrics fm = g.getFontMetrics();
		int ww = fm.stringWidth("199");

		g.setFont(penaltyFont);
		fm = g.getFontMetrics();
		int maxwid = tileDim.width - tileDim.height - ww - IN_TILE_GAP * 3 - 2 - fm.stringWidth("1999");

		// draw rank & score
		g.setFont(rankFont);
		g.setColor(lightMode ? Color.BLACK : Color.WHITE);
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

		g.setColor(lightMode ? Color.DARK_GRAY : Color.LIGHT_GRAY);
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

		int y = tileDim.height * 7 / 10 - 3;
		int h = tileDim.height * 3 / 10;
		float w = (float) maxwid / (float) numProblems;
		int xx = ww + tileDim.height + IN_TILE_GAP;
		int arc = tileDim.width / 120;

		g.setFont(statusFont);
		fm = g.getFontMetrics();
		g.setFont(problemFont);
		FontMetrics fm2 = g.getFontMetrics();

		for (int i = 0; i < numProblems; i++) {
			IResult r = contest.getResult(team, i);
			if (r.getNumSubmissions() == 0) {
				String label = problems[i].getLabel();
				BufferedImage img = problemImages.get(label + w);
				if (img == null) {
					img = new BufferedImage((int) w, h, BufferedImage.TYPE_4BYTE_ABGR);
					Graphics2D gg = (Graphics2D) img.getGraphics();
					gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					paintProblem(gg, (int) w, h, arc, fm2, label);
					gg.dispose();
					problemImages.put(label + w, img);
				}
				g.drawImage(img, xx + (int) (w * i), y, null);
			} else if (ContestUtil.isRecent(contest, r)) {
				int k = (int) ((timeMs * 45.0 / 1000.0) % (ICPCColors.COUNT2 * 2));
				paintResult(g, k, r, xx + (int) (w * i), y, (int) w, h, arc, fm);
			} else {
				String hash = r.getNumSubmissions() + "-" + r.getContestTime() + " " + r.getStatus().name() + " " + w;
				SoftReference<BufferedImage> ref = resultImages.get(hash);
				BufferedImage img = null;
				if (ref != null)
					img = ref.get();
				if (img == null) {
					img = new BufferedImage((int) w, h, BufferedImage.TYPE_4BYTE_ABGR);
					Graphics2D gg = (Graphics2D) img.getGraphics();
					gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					paintResult(gg, r, (int) w, h, arc, fm);
					gg.dispose();
					resultImages.put(hash, new SoftReference<BufferedImage>(img));
				}
				g.drawImage(img, xx + (int) (w * i), y, null);
			}
		}
	}

	private void paintProblem(Graphics2D g, int w, int h, int arc, FontMetrics fm, String label) {
		g.setColor(lightMode ? PROBLEM_BG_LIGHT : PROBLEM_BG);
		g.fillRoundRect(0, 0, w - 3, h - 1, arc, arc);
		g.setColor(lightMode ? Color.DARK_GRAY : Color.LIGHT_GRAY);
		g.setFont(problemFont);
		g.drawString(label, (w - fm.stringWidth(label)) / 2 - 1, (h + fm.getAscent()) / 2 - 1);
	}

	private void paintResult(Graphics2D g, int kk, IResult r, int x, int y, int w, int h, int arc, FontMetrics fm) {
		Color c = null;
		if (ContestUtil.isRecent(contest, r)) {
			int k = kk;
			// flash more than once per second
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
		}

		g.setColor(c);
		g.fillRoundRect(x, y, w - 3, h - 1, arc, arc);
		g.setColor(lightMode ? Color.BLACK : Color.WHITE);
		String s = "";

		if (r.getNumSubmissions() > 0) {
			if (fm.stringWidth("9\u200A-\u200A999") > w - 5)
				s = r.getNumSubmissions() + "";
			else
				s = r.getNumSubmissions() + "\u200A-\u200A" + ContestUtil.getTime(r.getContestTime());

			g.setFont(statusFont);
			g.drawString(s, x + (w - fm.stringWidth(s)) / 2 - 1, y + (h + fm.getAscent()) / 2 - 1);

			if (r.isFirstToSolve()) {
				g.setColor(ICPCColors.SOLVED_COLOR);
				g.drawRoundRect(x, y, w - 3, h - 1, arc, arc);
			}
		}
	}

	private void paintResult(Graphics2D g, IResult r, int w, int h, int arc, FontMetrics fm) {
		Color c = null;
		if (r.getStatus() == Status.SOLVED) {
			if (r.isFirstToSolve())
				c = ICPCColors.FIRST_TO_SOLVE[5];
			else
				c = ICPCColors.SOLVED[5];
		} else if (r.getStatus() == Status.FAILED)
			c = ICPCColors.FAILED[5];
		else if (r.getStatus() == Status.SUBMITTED)
			c = ICPCColors.PENDING[5];

		g.setColor(c);
		g.fillRoundRect(0, 0, w - 3, h - 1, arc, arc);
		g.setColor(lightMode ? Color.BLACK : Color.WHITE);
		String s = "";

		if (r.getNumSubmissions() > 0) {
			if (fm.stringWidth("9\u200A-\u200A999") > w - 5)
				s = r.getNumSubmissions() + "";
			else
				s = r.getNumSubmissions() + "\u200A-\u200A" + ContestUtil.getTime(r.getContestTime());

			g.setFont(statusFont);
			g.drawString(s, (w - fm.stringWidth(s)) / 2 - 1, (h + fm.getAscent()) / 2 - 1);

			if (r.isFirstToSolve()) {
				g.setColor(ICPCColors.SOLVED_COLOR);
				g.drawRoundRect(0, 0, w - 3, h - 1, arc, arc);
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
				draw(g, lightMode ? PROBLEM_BG_LIGHT : PROBLEM_BG, xx + w * i, y - h * 5 / 4, w, h, arc2, "", fm);

			if (ps.getNumSolved() > 0)
				draw(g, ICPCColors.SOLVED[5], xx + w * i, y, w, h, arc2, ps.getNumSolved() + "", fm);
			else
				draw(g, lightMode ? PROBLEM_BG_LIGHT : PROBLEM_BG, xx + w * i, y, w, h, arc2, "", fm);
		}
	}

	private void draw(Graphics2D g, Color c, int x, int y, int w, int h, int arc, String s, FontMetrics fm) {
		g.setColor(c);
		g.fillRoundRect(x, y, w - 3, h - 1, arc, arc);
		g.setColor(lightMode ? Color.BLACK : Color.WHITE);
		g.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + (h + fm.getAscent()) / 2 - 1);
	}
}