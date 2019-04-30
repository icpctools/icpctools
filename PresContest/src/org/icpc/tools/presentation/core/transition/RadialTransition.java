package org.icpc.tools.presentation.core.transition;

import java.awt.Graphics2D;
import java.awt.geom.Arc2D;

import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.Transition;

public class RadialTransition extends Transition {
	private boolean out = false;

	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		if (out) {
			paint(g, p1);
			double r = Math.max(width, height) * 1.2 * SmoothUtil.smooth(x);
			g.setClip(new Arc2D.Double(width / 2 - r / 2, height / 2 - r / 2, r, r, 0, 360.0, Arc2D.OPEN));
			paint(g, p2);
		} else {
			paint(g, p2);
			double r = Math.max(width, height) * 1.2 * (1.0 - SmoothUtil.smooth(x));
			g.setClip(new Arc2D.Double(width / 2 - r / 2, height / 2 - r / 2, r, r, 0, 360.0, Arc2D.OPEN));
			paint(g, p1);
		}
	}

	@Override
	public void setProperty(String value) {
		if ("IN".equals(value))
			out = false;
		else if ("OUT".equals(value))
			out = true;
	}
}