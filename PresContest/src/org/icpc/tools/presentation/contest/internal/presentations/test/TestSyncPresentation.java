package org.icpc.tools.presentation.contest.internal.presentations.test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;

import org.icpc.tools.presentation.core.Presentation;

/**
 * Timing test presentation.
 */
public class TestSyncPresentation extends Presentation {
	private long avgMilliDiff;
	private int avgMilliDiffCount;

	@Override
	public long getRepeat() {
		return 8000L;
	}

	@Override
	public void paint(Graphics2D g) {
		paintSyncLine(g);
		paintTimeDiff(g);
		paintOval(g);
	}

	private void paintOval(Graphics2D g2) {
		float t = (getRepeatTimeMs() % 2000L) / 500f;

		Graphics2D g = (Graphics2D) g2.create();
		Dimension d = getSize();

		float wf = 2f * width / (width + height);
		float xx = 0f;
		float yy = 0f;
		if (t < wf) {
			xx = t / wf;
		} else if (t < 2.0f) {
			xx = 1.0f;
			yy = (t - wf) / (2 - wf);
		} else if (t < 2.0f + wf) {
			xx = 1.0f - (t - 2.0f) / wf;
			yy = 1.0f;
		} else {
			yy = 1.0f - (t - 2.0f - wf) / (2 - wf);
		}

		float x = (d.width - 50) * xx;
		float y = (d.height - 50) * yy;

		g.setColor(isLightMode() ? Color.ORANGE : Color.YELLOW);

		g.translate(x, y);
		g.fillOval(0, 0, 50, 50);

		g.dispose();
	}

	private void paintSyncLine(Graphics2D g) {
		long ms = getTimeMs();
		int x = (int) (ms % 1000) * width / 1000;

		g.setColor(isLightMode() ? Color.LIGHT_GRAY : Color.DARK_GRAY);
		g.fillRect(x, 0, 10, height);
	}

	private void paintTimeDiff(Graphics2D g) {
		long ms = getTimeMs();
		long localMs = System.currentTimeMillis();
		long milliDiff = localMs - ms;
		avgMilliDiff = (avgMilliDiff * avgMilliDiffCount + milliDiff) / (avgMilliDiffCount + 1);
		if (avgMilliDiffCount < 10) {
			avgMilliDiffCount++;
		}
		double a = Math.atan(avgMilliDiff / 1000.0);

		String s = "" + avgMilliDiff;
		int sw = g.getFontMetrics().stringWidth(s), sh = g.getFontMetrics().getHeight();

		g.setColor(isLightMode() ? Color.LIGHT_GRAY : Color.DARK_GRAY);
		g.setStroke(new BasicStroke(2));
		int x = width - 30, y = height - 50;
		g.draw(new Line2D.Double(x, y, x + Math.sin(a) * 25, y - Math.cos(a) * 25));
		g.drawString(s, x - sw / 2, y + sh);
	}
}