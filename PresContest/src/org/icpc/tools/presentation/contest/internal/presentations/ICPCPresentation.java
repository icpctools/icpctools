package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.icpc.tools.presentation.core.Presentation;

public class ICPCPresentation extends Presentation {
	private final String text;
	private Font font1, font2, font3;

	public ICPCPresentation() {
		super();

		Calendar c = new GregorianCalendar();
		text = "ICPC  " + c.get(Calendar.YEAR) + "  ";
		// mode = true;
	}

	@Override
	public void paint(Graphics2D g) {
		Dimension d = getSize();
		if (font1 == null) {
			Font f = g.getFont();

			final float dpi = 96;
			float size = (int) (d.getHeight() * 72f / dpi);
			font1 = f.deriveFont(Font.BOLD, size * 0.9f);
			font2 = f.deriveFont(Font.BOLD, size * 0.7f);
			font3 = f.deriveFont(Font.BOLD, size * 0.25f);
		}
		int len = text.length();
		int ind = (int) Math.floor(getTimeMs() * 2f / 1000f) % len;
		String s = text.charAt(ind) + "";

		g.setColor(Color.WHITE);

		int mode = ((int) Math.floor(getTimeMs() * 2f / 1000f) / len) % 3;
		if (mode == 0) {
			g.setFont(font1);
			FontMetrics fm = g.getFontMetrics();
			g.drawString(s, (d.width - fm.stringWidth(s)) / 2, (d.height - fm.getHeight()) / 2 + fm.getAscent());
		} else if (mode == 1) {
			g.setFont(font2);
			FontMetrics fm = g.getFontMetrics();
			if (ind % 2 == 0)
				g.drawString(s, 0, fm.getAscent());
			else
				g.drawString(s, d.width - fm.stringWidth(s), d.height - fm.getDescent());
		} else {
			g.setFont(font3);
			FontMetrics fm = g.getFontMetrics();
			s = text.substring(0, ind + 1);
			g.drawString(s, (d.width - fm.stringWidth(text.trim())) / 2, (d.height - fm.getHeight()) / 2 + fm.getAscent());
		}
	}
}