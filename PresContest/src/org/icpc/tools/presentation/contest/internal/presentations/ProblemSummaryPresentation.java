package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.Utility;

public class ProblemSummaryPresentation extends AbstractICPCPresentation {
	private static final int ARC = 10;
	// private static final int BORDER = 20;
	private static final int OUTER_MARGIN = 20;
	private static final int INNER_MARGIN = 20;
	private static final int SPACING = 20;
	private static final int INNER_SPACING = 10;

	private Font font1;
	private Font font2;
	private Font font3;
	private float boxWidth = 50;
	private float boxHeight = 50;
	private boolean[] updatedRecently;

	private static final int CCOUNT = 15;
	private final Color[] colors = Utility.getColorsBetween(Color.white, new Color(255, 255, 255, 0), CCOUNT);
	private final Map<String, Color> pColors = new HashMap<>();
	private final Map<String, Color> poColors = new HashMap<>();

	private BufferedImage[] buf;

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		boxWidth = (width - OUTER_MARGIN * 2 - INNER_MARGIN * 2 - SPACING * 3) / 4;
		boxHeight = (height - 78 - OUTER_MARGIN * 2 - INNER_MARGIN * 2 - SPACING * 2) / 3;
		final float dpi = 96;
		float inch = boxHeight * 72f / dpi;
		font1 = ICPCFont.getMasterFont().deriveFont(Font.BOLD, inch * 0.95f);
		font2 = ICPCFont.getMasterFont().deriveFont(Font.BOLD, inch * 0.33f);
		font3 = ICPCFont.getMasterFont().deriveFont(Font.BOLD, inch * 0.22f);
	}

	@Override
	public void paint(Graphics2D g) {
		IContest sc = getContest();
		if (sc == null)
			return;

		IProblem[] problems = sc.getProblems();
		if (problems == null)
			return;

		int numProblems = problems.length;
		if (numProblems == 0)
			return;

		// g.setPaint(new GradientPaint(0, 0, Color.BLACK, 0, height*3, Color.DARK_GRAY));
		// g.fillRoundRect(BORDER / 2, BORDER / 2, width * 4 - BORDER, height * 3 - BORDER, ARC,
		// ARC);

		// g.setColor(Color.WHITE);
		g.setColor(ICPCColors.BLUE);
		g.setStroke(new BasicStroke(2));
		// g.drawRoundRect(BORDER / 2, BORDER / 2, width * 4 - BORDER, height * 3 - BORDER, ARC,
		// ARC);
		// g.drawRoundRect(OUTER_MARGIN, OUTER_MARGIN, screen.width - OUTER_MARGIN * 2, screen.height
		// - OUTER_MARGIN * 2 - 78, ARC, ARC);

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		int[] totalAttempts = new int[numProblems];
		int[] totalSolved = new int[numProblems];
		int[] fastestSoln = new int[numProblems];
		updatedRecently = new boolean[numProblems];

		for (ITeam team : sc.getTeams()) {
			for (int j = 0; j < numProblems; j++) {
				IResult r = sc.getResult(team, j);
				if (ContestUtil.isRecent(sc, r))
					updatedRecently[j] = true;

				if (r.getStatus() == Status.SOLVED) {
					totalSolved[j]++;
					if (r.getContestTime() > 0 && fastestSoln[j] == 0 || r.getContestTime() < fastestSoln[j])
						fastestSoln[j] = r.getContestTime();
				}
				if (r.getStatus() != Status.UNATTEMPTED)
					totalAttempts[j] += r.getNumSubmissions();
			}
		}

		g.setFont(font1);

		for (int j = 0; j < 12; j++) {
			int xx = (j % 4);
			int yy = (j / 4);
			int x = (int) (boxWidth + SPACING) * xx + OUTER_MARGIN + INNER_MARGIN;
			int y = (int) (boxHeight + SPACING) * yy + OUTER_MARGIN + INNER_MARGIN;

			paintRect(g, x, y, j, problems);

			if (j != 11 && j < problems.length) {
				String t = totalAttempts[j] + "";
				String u = "";
				if (totalAttempts[j] > 0)
					u = totalSolved[j] + "";
				String v = "";
				if (fastestSoln[j] > 0)
					v = ContestUtil.getTime(fastestSoln[j]);

				// g.setColor(ICPCColors.PROBLEM_COLORS[j]);
				g.setColor(Color.white);
				g.setFont(font2);
				FontMetrics fm = g.getFontMetrics();
				if (updatedRecently[j]) {
					int xxy = (int) (getTimeMs() * CCOUNT * 500f + numProblems) % (CCOUNT * 2); // flash
					// once
					// per
					// second
					if (xxy > (CCOUNT - 1))
						xxy = (CCOUNT * 2 - 1) - xxy;
					g.setColor(colors[xxy]);
				}
				g.drawString(t, x + boxWidth - INNER_SPACING - fm.stringWidth(t), y + INNER_SPACING + fm.getAscent());
				// TextImage.drawString(g, t, x + (int)boxWidth - INNER_SPACING - fm.stringWidth(t), y
				// + INNER_SPACING + fm.getAscent(), g.getColor(), g.getFont());
				g.setColor(Color.white);
				g.drawString(u, x + boxWidth - INNER_SPACING - fm.stringWidth(u),
						y + INNER_SPACING + fm.getAscent() + fm.getHeight());
				// TextImage.drawString(g, u, x + (int)boxWidth - INNER_SPACING - fm.stringWidth(t), y
				// + INNER_SPACING + fm.getAscent() + fm.getHeight(), g.getColor(), g.getFont());
				g.drawString(v, x + boxWidth - INNER_SPACING - fm.stringWidth(v),
						y + INNER_SPACING + fm.getAscent() + fm.getHeight() * 2);
				// TextImage.drawString(g, v, x + (int)boxWidth - INNER_SPACING - fm.stringWidth(t), y
				// + INNER_SPACING + fm.getAscent() + fm.getHeight()*2, g.getColor(), g.getFont());
			}
		}
	}

	private void paintRect(Graphics2D g2, int x, int y, int j, IProblem[] problems) {
		if (buf == null)
			buf = new BufferedImage[12];

		// if (j != 11 && j < problems.length)
		if (j >= problems.length && j != 11)
			return;

		if (buf[j] == null) {
			int w = (int) boxWidth;
			int h = (int) boxHeight;
			buf[j] = new BufferedImage(w, h, Transparency.OPAQUE);

			Graphics2D g = buf[j].createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			g.setColor(Color.black);
			g.fillRect(0, 0, w, h);

			Color probColor = null;
			Color pColor = null;
			Color poColor = null;
			if (j != 11) {
				String problemId = problems[j].getId();
				probColor = problems[j].getColorVal();
				pColor = pColors.get(problemId);
				poColor = poColors.get(problemId);
				if (pColor == null) {
					pColor = new Color(probColor.getRed(), probColor.getGreen(), probColor.getBlue(), 164);
					pColors.put(problemId, pColor);
					poColor = org.icpc.tools.contest.model.ICPCColors.getContrastColor(probColor);
					poColors.put(problemId, poColor);
				}
			}

			if (j == 11)
				g.setColor(Color.black);
			else
				g.setColor(pColor);
			g.fillRoundRect(1, 1, w - 2, h - 2, ARC, ARC);

			if (j == 11)
				g.setColor(Color.LIGHT_GRAY);
			else {
				if (Color.black.equals(probColor))
					g.setColor(Color.LIGHT_GRAY);
				else
					g.setColor(probColor);
			}
			g.setStroke(new BasicStroke(2.5f));
			g.drawRoundRect(1, 1, w - 3, h - 3, ARC, ARC);
			g.setStroke(new BasicStroke(1));

			if (j != 11) {
				g.setFont(font1);
				FontMetrics fm = g.getFontMetrics();
				if (Color.white.equals(probColor))
					g.setColor(Color.BLACK);
				else if (Color.black.equals(probColor))
					g.setColor(Color.LIGHT_GRAY);
				else
					g.setColor(poColor);

				String s = problems[j].getLabel();
				for (int i = -2; i < 3; i++) {
					for (int jj = -2; jj < 3; jj++) {
						g.drawString(s, INNER_SPACING + i, fm.getAscent() + INNER_SPACING - jj);
					}
				}

				g.setColor(probColor);
				g.drawString(s, INNER_SPACING, fm.getAscent() + INNER_SPACING);
			}

			if (j == 11) {
				String t = "attempts";
				String u = "solutions";
				String v = "fastest sol'n";
				g.setFont(font2);
				FontMetrics fm2 = g.getFontMetrics();
				g.setFont(font3);
				FontMetrics fm = g.getFontMetrics();
				g.setColor(Color.white);
				g.drawString(t, w - INNER_SPACING - fm.stringWidth(t), INNER_SPACING + fm2.getAscent());
				g.drawString(u, w - INNER_SPACING - fm.stringWidth(u), INNER_SPACING + fm2.getAscent() + fm2.getHeight());
				g.drawString(v, w - INNER_SPACING - fm.stringWidth(v),
						INNER_SPACING + fm2.getAscent() + fm2.getHeight() * 2);
			}

			g.dispose();
		}

		g2.drawImage(buf[j], x, y, null);
	}
}