package org.icpc.tools.presentation.core.transition;

import java.awt.Color;
import java.awt.Graphics2D;

import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.Transition;

public class FlashTransition extends Transition {
	@Override
	public TimeOverlap getTimeOverlap() {
		return TimeOverlap.MID;
	}

	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		if (x < 0.25) {
			paint(g, p1);
			return;
		} else if (x > 0.75) {
			paint(g, p2);
			return;
		}
		if (x < 0.5) {
			paint(g, p1);
			// g.setColor(new Color(255, 255, 255, (int) ((x - 0.2) * 850.0)));
			g.setColor(new Color(255, 255, 255, (int) ((x - 0.25) * 1020.0)));
		} else {
			paint(g, p2);
			// g.setColor(new Color(255, 255, 255, 255 - (int) ((x - 0.5) * 850.0)));
			g.setColor(new Color(255, 255, 255, 255 - (int) ((x - 0.5) * 1020.0)));
		}
		g.fillRect(0, 0, width, height);
	}
}