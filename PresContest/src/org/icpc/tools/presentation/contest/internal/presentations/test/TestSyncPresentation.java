package org.icpc.tools.presentation.contest.internal.presentations.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

import org.icpc.tools.presentation.core.Presentation;

/**
 * Timing test presentation.
 */
public class TestSyncPresentation extends Presentation {
	@Override
	public long getRepeat() {
		return 8000L;
	}

	@Override
	public void paint(Graphics2D g2) {
		float t = (getRepeatTimeMs() % 2000L) / 500f;

		Graphics2D g = (Graphics2D) g2.create();
		Dimension d = getSize();

		float xx = 0f;
		float yy = 0f;
		if (t < 1.0f) {
			xx = t;
		} else if (t < 2.0f) {
			xx = 1.0f;
			yy = t - 1.0f;
		} else if (t < 3.0f) {
			xx = 1.0f - (t - 2.0f);
			yy = 1.0f;
		} else {
			yy = 1.0f - (t - 3.0f);
		}

		float x = (d.width - 50) * xx;
		float y = (d.height - 50) * yy;

		g.translate(x, y);
		g.setColor(Color.YELLOW);

		g.fillOval(0, 0, 50, 50);
		g.dispose();
	}
}