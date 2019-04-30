package org.icpc.tools.presentation.core.transition;

import java.awt.Color;
import java.awt.Graphics2D;

import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.Transition;

public class FadeThroughWhiteTransition extends Transition {
	@Override
	public TimeOverlap getTimeOverlap() {
		return TimeOverlap.MID;
	}

	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		if (x < 0.5) {
			paint(g, p1);
			g.setColor(new Color(255, 255, 255, (int) (x * 440f)));
		} else {
			paint(g, p2);
			g.setColor(new Color(255, 255, 255, 220 - (int) ((x - 0.5) * 440f)));
		}
		g.fillRect(0, 0, width, height);
	}
}