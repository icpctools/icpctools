package org.icpc.tools.presentation.contest.internal.presentations.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Calendar;

import org.icpc.tools.presentation.contest.internal.DigitalFont;
import org.icpc.tools.presentation.core.Presentation;

public class TestClockPresentation extends Presentation {
	private static final Color DARK_GRAY = new Color(8, 8, 8);

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
	}

	@Override
	public long getDelayTimeMs() {
		// paint 5 times a second
		return 200;
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		long ms = getTimeMs();

		int s = 0;
		int m = 0;
		int h = 0;

		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(ms);
		if (c.get(Calendar.YEAR) < 1990) {
			s = (int) Math.abs(Math.floor(ms / 1000f));
			m = (s / 60) % 60;
			h = s / 3600;
			s %= 60;
		} else {
			s = c.get(Calendar.SECOND);
			m = c.get(Calendar.MINUTE);
			h = c.get(Calendar.HOUR_OF_DAY);
		}

		StringBuilder sb = new StringBuilder();
		if (h < 10)
			sb.append("0");
		sb.append(h + ":");
		if (m < 10)
			sb.append("0");
		sb.append(m + ":");
		if (s < 10)
			sb.append("0");
		sb.append(s);

		int w = DigitalFont.stringWidth(sb.toString(), (int) (0.19f * width));
		DigitalFont.drawString(g, sb.toString(), (width - w) / 2, (int) (height * 0.6f), (int) (0.19f * width),
				Color.WHITE, DARK_GRAY);
	}
}