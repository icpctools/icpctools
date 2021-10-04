package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;

import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

public class ProblemSummary {
	private Rectangle r;
	private IProblem problem;
	private Font bigFont;
	private Font font;
	private Color color;
	private Color outlineColor;
	private Color brightColor;
	private Color darkBgColor;

	// private boolean recent;
	private int[] stats;

	public ProblemSummary(Rectangle r, IProblem p) {
		this.r = r;
		this.problem = p;
		if (problem != null) {
			Color pColor = problem.getColorVal();
			int rr = pColor.getRed();
			int gg = pColor.getGreen();
			int bb = pColor.getBlue();
			color = new Color(rr, gg, bb);
			if (Color.WHITE.equals(pColor))
				outlineColor = Color.DARK_GRAY;
			else
				outlineColor = org.icpc.tools.contest.model.ICPCColors.getContrastColor(pColor);

			brightColor = color.brighter();
			darkBgColor = new Color(rr / 3, gg / 3, bb / 3);
		}
		bigFont = ICPCFont.deriveFont(Font.BOLD, r.height * 5 / 6);
		font = ICPCFont.deriveFont(Font.BOLD, r.height / 8);
	}

	public IProblem getProblem() {
		return problem;
	}

	private static void draw(Graphics2D g, Color c, int x, int y, int w, int h, int arc, String s, FontMetrics fm) {
		g.setColor(c);
		g.fillRoundRect(x, y, w, h, arc, arc);
		g.setColor(Color.WHITE);
		g.drawString(s, x + (w - fm.stringWidth(s)) / 2, y + (h + fm.getAscent()) / 2 - 1);
	}

	public void setStats(int[] s) {
		stats = s;
	}

	public void paint(Graphics2D g) {
		g.translate(r.x, r.y);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		int ARC = (r.width + r.height) / 50;
		g.setColor(color);
		g.fillRoundRect(0, 0, r.width, r.height, ARC, ARC);

		if (problem != null) {
			Stroke stroke = g.getStroke();
			g.setStroke(new BasicStroke(1.5f));
			g.setColor(Color.LIGHT_GRAY);
			g.drawRoundRect(0, 0, r.width, r.height, ARC, ARC);
			g.setStroke(stroke);
		}

		g.setFont(bigFont);
		int margin = r.height / 20;
		if (problem != null) {
			FontMetrics fm = g.getFontMetrics();
			g.setColor(outlineColor);
			String s = problem.getLabel();
			for (int i = -1; i < 2; i++)
				for (int j = -1; j < 2; j++)
					if (i != 0 && j != 0)
						g.drawString(s, margin + i, margin + j + fm.getAscent());

			g.setColor(brightColor);
			g.drawString(s, margin, margin + fm.getAscent());
		}

		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		int margin2 = r.height / 15;
		int w = r.width * 3 / 9 - margin * 2;
		int x = r.width - margin - w - margin2;
		int y = margin + margin2;
		int h = (r.height - margin * 2 - margin2 * 3 - margin2 * 2) / 4;
		int arc2 = w / 10;

		if (stats == null) {
			g.setColor(Color.LIGHT_GRAY);
			String s = "Attempts";
			g.drawString(s, x + w - fm.stringWidth(s), y + fm.getAscent());
			s = "Accepted";
			g.drawString(s, x + w - fm.stringWidth(s), y + h + margin2 + fm.getAscent());
			s = "Rejected";
			g.drawString(s, x + w - fm.stringWidth(s), y + (h + margin2) * 2 + fm.getAscent());
			s = "First to Solve time";
			g.drawString(s, x + w - fm.stringWidth(s), y + (h + margin2) * 3 + fm.getAscent());
			return;
		}

		if (stats[0] > 0 || stats[1] > 0 || stats[2] > 0 || stats[3] > -1) {
			g.setColor(darkBgColor);
			g.fillRoundRect(x - margin2, y - margin2, w + margin2 * 2, h * 4 + margin2 * 3 + margin2 * 2, ARC, ARC);
		}

		if (stats[0] > 0)
			draw(g, ICPCColors.PENDING[5], x, y, w, h, arc2, stats[0] + "", fm);
		if (stats[1] > 0)
			draw(g, ICPCColors.SOLVED[5], x, y + h + margin2, w, h, arc2, stats[1] + "", fm);
		if (stats[2] > 0)
			draw(g, ICPCColors.FAILED[5], x, y + (h + margin2) * 2, w, h, arc2, stats[2] + "", fm);
		if (stats[3] > -1) {
			draw(g, ICPCColors.FIRST_TO_SOLVE[5], x, y + (h + margin2) * 3, w, h, arc2, ContestUtil.getTime(stats[3]), fm);
			g.setColor(ICPCColors.SOLVED_COLOR);
			Stroke stroke = g.getStroke();
			g.setStroke(new BasicStroke(1.5f));
			g.drawRoundRect(x, y + (h + margin2) * 3, w, h, arc2, arc2);
			g.setStroke(stroke);
		}
	}
}