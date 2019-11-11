package org.icpc.tools.presentation.contest.internal.tile;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
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
import org.icpc.tools.presentation.contest.internal.TeamUtil;
import org.icpc.tools.presentation.contest.internal.TeamUtil.Style;

public class TeamTileHelper {
	private static final int IN_TILE_GAP = 1;
	protected static final Color TILE_BG = new Color(50, 50, 50);
	private static final Color PROBLEM_BG = new Color(90, 90, 90);

	private Font rankFont;
	private Font teamFont;
	private Font problemFont;
	private Font penaltyFont;

	private Dimension tileDim = null;
	private IContest contest;
	private Style style;
	private boolean hasBg = false;

	private final Map<String, BufferedImage> tileImages = new HashMap<>();
	private boolean tileImagesCacheMiss;

	private BufferedImage tileBgImage;
	private long tileBgHash;

	public TeamTileHelper(Dimension tileDim, IContest contest) {
		this(tileDim, contest, null);
	}

	public TeamTileHelper(Dimension tileDim, IContest contest, Style style) {
		this.tileDim = tileDim;
		this.contest = contest;
		this.style = style;

		if (style == null)
			this.style = TeamUtil.getDefaultStyle(contest);

		setup();
	}

	public Style getStyle() {
		return style;
	}

	protected void setup() {
		final float dpi = 96;

		float size = tileDim.height * 36f * 0.95f / dpi;
		Font masterFont = ICPCFont.getMasterFont();
		teamFont = masterFont.deriveFont(Font.BOLD, size * 1.4f);
		rankFont = masterFont.deriveFont(Font.BOLD, size * 1.4f);
		problemFont = masterFont.deriveFont(Font.PLAIN, size * 0.7f);
		penaltyFont = masterFont.deriveFont(Font.BOLD, size * 0.85f);
	}

	public void cacheTile(ITeam team) {
		if (team == null)
			return;

		createBgTileImage();
		tileImages.put(team.getId(), createTileImage(team));
		tileImagesCacheMiss = false;
	}

	public void cacheAllTiles() {
		createBgTileImage();

		ITeam[] teams = contest.getOrderedTeams();
		for (ITeam t : teams)
			tileImages.put(t.getId(), createTileImage(t));
		tileImagesCacheMiss = false;
	}

	/**
	 * Returns true if all recently-painted tiles were found in the cache, and false otherwise.
	 *
	 * @return
	 */
	public boolean areTilesCached() {
		return !tileImagesCacheMiss;
	}

	private BufferedImage createTileImage(ITeam team) {
		BufferedImage img = new BufferedImage(tileDim.width, tileDim.height, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

		// draw rank & score
		g.setFont(rankFont);
		FontMetrics fm = g.getFontMetrics();
		int ww = fm.stringWidth("199");

		g.setFont(teamFont);
		fm = g.getFontMetrics();

		// draw image & name
		IOrganization org = contest.getOrganizationById(team.getOrganizationId());
		if (org != null) {
			BufferedImage logoImg = org.getLogoImage(tileDim.height - 10, tileDim.height - 10, true, true);
			if (logoImg != null) {
				g.drawImage(logoImg, ww + (tileDim.height - logoImg.getWidth()) / 2,
						(tileDim.height - logoImg.getHeight()) / 2, null);
				logoImg.flush();
			}
		}

		paintName(g, team, ww);
		/*float n = 1f;
		while (fm.stringWidth(team.getName()) > tileDim.width - tileDim.height - ww - IN_TILE_GAP) {
			Font f = teamFont.deriveFont(AffineTransform.getScaleInstance(n, 1.0));
			g.setFont(f);
			fm = g.getFontMetrics();
			n -= 0.025f;
		}
		int yy = tileDim.height * 7 / 10;
		g.setColor(Color.WHITE);
		g.drawString(team.getName(), ww + tileDim.height + IN_TILE_GAP, (yy + fm.getAscent()) / 2 - 2);
		 */
		g.dispose();
		return img;
	}

	private void paintBgTileImage(Graphics2D g) {
		createBgTileImage();

		g.drawImage(tileBgImage, 0, 0, null);
	}

	private void createBgTileImage() {
		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;

		if (tileBgImage != null && numProblems == tileBgHash)
			return;

		tileBgHash = numProblems;

		BufferedImage img = new BufferedImage(tileDim.width, tileDim.height, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		// fill background
		int arc = tileDim.width / 40;
		if (hasBg) {
			g.setColor(TILE_BG);
			g.fillRoundRect(0, 0, tileDim.width, tileDim.height, arc, arc);
		}

		// draw rank & score
		g.setFont(rankFont);
		FontMetrics fm = g.getFontMetrics();
		int ww = fm.stringWidth("199");

		// draw a rounded-rectangle representation for each problem
		g.setFont(penaltyFont);
		fm = g.getFontMetrics();

		if (numProblems > 0) {
			int y = tileDim.height * 7 / 10;
			int h = tileDim.height * 3 / 10;
			int w = (tileDim.width - ww - tileDim.height - IN_TILE_GAP * 2 - fm.stringWidth("1999")) / numProblems;
			int xx = ww + tileDim.height + IN_TILE_GAP;
			int arc2 = tileDim.width / 90;

			g.setFont(problemFont);
			fm = g.getFontMetrics();

			for (int i = 0; i < numProblems; i++) {
				g.setColor(PROBLEM_BG);
				g.fillRoundRect(xx + w * i, y, w - 3, h - 1, arc2, arc2);
				g.setColor(Color.BLACK);
				String s = problems[i].getLabel();
				g.drawString(s, xx + w * i + (w - fm.stringWidth(s)) / 2 - 2, y + (h + fm.getAscent()) / 2 - 2);
			}
		}

		if (hasBg) {
			g.setColor(TILE_BG);
			g.drawRoundRect(0, 0, tileDim.width - 1, tileDim.height - 1, arc, arc);
		}

		g.dispose();
		tileBgImage = img;
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

		paintBgTileImage(gg);

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

		BufferedImage img = tileImages.get(team.getId());
		if (img != null)
			gg.drawImage(img, 0, 0, null);
		else
			tileImagesCacheMiss = true;

		gg.clipRect(0, 0, tileDim.width, tileDim.height);
		paintTileForeground(gg, team, img == null, timeMs);

		gg.dispose();
	}

	private void paintName(Graphics2D g, ITeam team, int ww) {
		g.setFont(teamFont);
		FontMetrics fm = g.getFontMetrics();

		String s = TeamUtil.getTeamName(style, contest, team);
		float n = 1f;
		while (fm.stringWidth(s + " 10") > tileDim.width - tileDim.height - ww - IN_TILE_GAP * 2 - 2) {
			Font f = teamFont.deriveFont(AffineTransform.getScaleInstance(n, 1.0));
			g.setFont(f);
			fm = g.getFontMetrics();
			n -= 0.0125f;
		}
		g.setColor(Color.WHITE);
		g.drawString(s, ww + tileDim.height + IN_TILE_GAP, (tileDim.height * 7 / 10 + fm.getAscent()) / 2 - 2);
	}

	private void paintTileForeground(Graphics2D g, ITeam team, boolean includeName, long timeMs) {
		// draw rank & score
		g.setFont(rankFont);
		g.setColor(Color.WHITE);
		FontMetrics fm = g.getFontMetrics();
		int ww = fm.stringWidth("199");

		IStanding standing = contest.getStanding(team);
		String s = standing.getRank();
		g.drawString(s, (ww - fm.stringWidth(s)) / 2, (tileDim.height + fm.getAscent()) / 2);

		// draw name
		if (includeName)
			paintName(g, team, ww);

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
					tileDim.height * 17 / 20 + fm.getAscent() / 2 - 2);
		}

		// draw a rounded-rectangle representation for each problem
		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;
		if (numProblems == 0)
			return;

		int y = tileDim.height * 7 / 10;
		int h = tileDim.height * 3 / 10;
		int w = (tileDim.width - ww - tileDim.height - IN_TILE_GAP * 2 - fm.stringWidth("1999")) / numProblems;
		int xx = ww + tileDim.height + IN_TILE_GAP;
		int arc2 = tileDim.width / 90;

		g.setFont(problemFont);
		fm = g.getFontMetrics();

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
						c = ICPCColors.FIRST_TO_SOLVE_COLOR;
					else
						c = ICPCColors.SOLVED_COLOR;
				} else if (r.getStatus() == Status.FAILED)
					c = ICPCColors.FAILED_COLOR;
				else if (r.getStatus() == Status.SUBMITTED)
					c = ICPCColors.PENDING_COLOR;
			}

			if (c != null) {
				g.setColor(c);
				g.fillRoundRect(xx + w * i, y, w - 3, h - 1, arc2, arc2);
				g.setColor(Color.BLACK);
				s = "";// problems[i].getLabel();

				if (r.getNumSubmissions() > 0) {
					if (fm.stringWidth("9\u200A-\u200A999") > w - 5)
						s = r.getNumSubmissions() + "";
					else
						s = r.getNumSubmissions() + "\u200A-\u200A" + ContestUtil.getTime(r.getContestTime());
				}

				g.drawString(s, xx + w * i + (w - fm.stringWidth(s)) / 2 - 2, y + (h + fm.getAscent()) / 2 - 2);
			}
		}
	}

	public void paintTileStats(Graphics2D g) {
		paintBgTileImage(g);

		g.setFont(rankFont);
		g.setColor(Color.WHITE);
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
		int w = (tileDim.width - ww - tileDim.height - IN_TILE_GAP * 2 - fm.stringWidth("1999")) / numProblems;
		int xx = ww + tileDim.height + IN_TILE_GAP;
		int arc2 = tileDim.width / 90;

		g.setFont(problemFont);
		fm = g.getFontMetrics();

		for (int i = 0; i < numProblems; i++) {
			IProblemSummary ps = contest.getProblemSummary(i);
			if (ps.getNumPending() > 0)
				draw(g, ICPCColors.PENDING_COLOR, xx + w * i, y - h * 5 / 4, w, h, arc2, ps.getNumPending() + "", fm);
			else
				draw(g, PROBLEM_BG, xx + w * i, y - h * 5 / 4, w, h, arc2, "", fm);
			if (ps.getNumSolved() > 0)
				draw(g, ICPCColors.SOLVED_COLOR, xx + w * i, y, w, h, arc2, ps.getNumSolved() + "", fm);
			else
				draw(g, PROBLEM_BG, xx + w * i, y, w, h, arc2, "", fm);
		}
	}

	private static void draw(Graphics2D g, Color c, int x, int y, int w, int h, int arc, String s, FontMetrics fm) {
		g.setColor(c);
		g.fillRoundRect(x, y, w - 3, h - 1, arc, arc);
		g.setColor(Color.BLACK);
		g.drawString(s, x + (w - fm.stringWidth(s)) / 2 - 2, y + (h + fm.getAscent()) / 2 - 2);
	}
}