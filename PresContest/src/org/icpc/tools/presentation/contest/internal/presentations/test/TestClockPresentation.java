package org.icpc.tools.presentation.contest.internal.presentations.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Calendar;

import org.icpc.tools.presentation.contest.internal.DigitalFont;
import org.icpc.tools.presentation.core.Presentation;

public class TestClockPresentation extends Presentation {
	private static final Color DARK_GRAY = new Color(12, 12, 12);
	private static final Color LIGHT_GRAY = new Color(250, 250, 250);

	public Color getTextBackgroundColor() {
		return isLightMode() ? LIGHT_GRAY : DARK_GRAY;
	}

	public Color getTextForegroundColor() {
		return isLightMode() ? Color.BLACK : Color.WHITE;
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
	}

	private boolean showLocalTime;

	@Override
	public void setProperty(String value) {
		if (value == null || value.isEmpty())
			return;
		if ("localTimeOn".equals(value))
			showLocalTime = true;
		else if ("localTimeOff".equals(value))
			showLocalTime = false;
		else
			super.setProperty(value);
	}

	@Override
	public long getDelayTimeMs() {
		if (showLocalTime) {
			// paint ASAP
			return 0;
		}

		// paint 20 times a second
		return 50;
	}

	private static String timeString(long ms) {
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

		return sb.toString();
	}

	@Override
	public void paint(Graphics2D g) {
		if (showLocalTime) {
			drawSyncLine(g);
			drawServerTimeDiff(g);
		}
		drawClock(g);
	}

	public void drawClock(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		// g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
		// RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		long ms = getTimeMs();

		String s = timeString(ms);
		int w = DigitalFont.stringWidth(s, (int) (0.19f * width));
		DigitalFont.drawString(g, s, (width - w) / 2, (int) (height * 0.6f), (int) (0.19f * width),
				getTextForegroundColor(), getTextBackgroundColor());
	}

	long avgMilliDiff;
	int avgMilliDiffCount;
	long firstNanoDiffMs;
	int firstNanoDiffCount;

	public void drawServerTimeDiff(Graphics2D g) {
		long serverMillis = this.getTimeMs();
		long milliDiff = System.currentTimeMillis() - serverMillis;
		avgMilliDiff = (avgMilliDiff * avgMilliDiffCount + milliDiff) / (avgMilliDiffCount + 1);
		if (avgMilliDiffCount < 10) {
			avgMilliDiffCount++;
		}
		int x = 25;
		int y = 3 * height / 17;
		int h = 2 * height / 17;
		DigitalFont.drawString(g, "" + avgMilliDiff, x, y, h, getTextForegroundColor(), getTextBackgroundColor());
		g.setColor(getTextForegroundColor());
		g.drawString("Server time diff [ms]", x, y + g.getFontMetrics().getHeight());

		long nanoDiffMs = System.nanoTime() / (int) 1e6 - System.currentTimeMillis();
		if (firstNanoDiffCount < 10) {
			firstNanoDiffMs = (firstNanoDiffMs * firstNanoDiffCount + nanoDiffMs) / (firstNanoDiffCount + 1);
			firstNanoDiffCount++;
		}
		final long nanoDiffThresholdMs = 10;
		if (Math.abs(nanoDiffMs - firstNanoDiffMs) > nanoDiffThresholdMs) {
			x = width / 3 + 25;
			DigitalFont.drawString(g, "" + (nanoDiffMs - firstNanoDiffMs), x, y, h, getTextForegroundColor(),
					getTextBackgroundColor());
			g.setColor(getTextForegroundColor());
			g.drawString("nanoTime diff [ms]", x, y + g.getFontMetrics().getHeight());
		}

		long localMs = System.currentTimeMillis();
		String s = timeString(localMs);
		int w = DigitalFont.stringWidth(s, h);
		x = width - 25 - w;
		DigitalFont.drawString(g, s, x, y, h, getTextForegroundColor(), getTextBackgroundColor());
		g.setColor(getTextForegroundColor());
		g.drawString("Local time", x, y + g.getFontMetrics().getHeight());
	}

	public void drawSyncLine(Graphics2D g) {
		long serverMillis = getTimeMs();
		int x = (int) (System.currentTimeMillis() % 1000) * width / 1000;
		g.setColor(getTextBackgroundColor());
		int h = 2 * this.height / 17;
		g.fillRect(x, 0, width / 100, 2 * h);
		x = (int) (serverMillis % 1000) * width / 1000;
		g.setColor(Color.GREEN);
		g.fillRect(x, 0, width / 100, 2 * h);
	}
}
