package org.icpc.tools.presentation.contest.internal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class Legend {
	private static final int MARGIN = 15;
	private static final int H_SPACING = 10;
	private static final int V_SPACING = 15;

	private static final String[] TEXT = new String[] { "First to Solve", "Solved Problem", "Rejected Submission",
			"Pending Submission" };

	protected static Font font;

	private static void init(Graphics2D g) {
		font = ICPCFont.getMasterFont().deriveFont(Font.BOLD, 24);
	}

	public static void drawLegend(Graphics2D g) {
		if (font == null)
			init(g);

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		Color c = new Color(255, 255, 255);
		g.setColor(new Color(0, 0, 0, 192));

		int fh = fm.getHeight();
		int fx = fh * 2;
		int h = MARGIN * 2 + V_SPACING * 3 + fh * 4;
		int w = MARGIN * 2 + fx + H_SPACING + fm.stringWidth(TEXT[2]);

		g.translate(-w, 0);
		// g.setColor(new Color(0, 0, 0, 210));
		// g.setColor(new Color(0, 0, 0, i));
		g.fillRect(0, 0, w, h);

		// Color c = Color.WHITE;
		g.setColor(c);
		g.drawRect(0, 0, w, h);

		g.translate(MARGIN, MARGIN);

		// draw a rounded rectangle outline around the FTS Legend item. The "outline" is produced by
		// drawing a filled full-size rectangle and then drawing a subsequent fill rectangle
		// slightly smaller
		g.setStroke(new BasicStroke(3));
		ShadedRectangle.drawRoundRect(g, 0, 0, fx, fh, ICPCColors.SOLVED_COLOR);
		g.setStroke(new BasicStroke(1));

		// fill the above rectangle with a smaller one in the current FTS color
		ShadedRectangle.drawRoundRect(g, 2, 2, fx - 4, fh - 4, ICPCColors.FIRST_TO_SOLVE_COLOR);
		g.setColor(c);
		g.drawString(TEXT[0], H_SPACING + fx, fm.getAscent() + 1);

		ShadedRectangle.drawRoundRect(g, 0, V_SPACING + fh, fx, fh, ICPCColors.SOLVED_COLOR);
		g.setColor(c);
		g.drawString(TEXT[1], H_SPACING + fx, V_SPACING + fh + fm.getAscent() + 1);

		ShadedRectangle.drawRoundRect(g, 0, V_SPACING * 2 + fh * 2, fx, fh, ICPCColors.FAILED_COLOR);
		g.setColor(c);
		g.drawString(TEXT[2], H_SPACING + fx, V_SPACING * 2 + fh * 2 + fm.getAscent() + 1);

		ShadedRectangle.drawRoundRect(g, 0, V_SPACING * 3 + fh * 3, fx, fh, ICPCColors.PENDING_COLOR);
		g.setColor(c);
		g.drawString(TEXT[3], H_SPACING + fx, V_SPACING * 3 + fh * 3 + fm.getAscent() + 1);

		g.translate(-MARGIN + w, -MARGIN);
	}
}