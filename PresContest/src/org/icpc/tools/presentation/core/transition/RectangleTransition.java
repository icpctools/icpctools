package org.icpc.tools.presentation.core.transition;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.Transition;

public class RectangleTransition extends Transition {
	private boolean out = false;

	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		if (out) {
			paint(g, p1);
			double w = width * SmoothUtil.smooth(x);
			double h = height * SmoothUtil.smooth(x);
			g.setClip(new Rectangle2D.Double((width - w) / 2, (height - h) / 2, w, h));
			paint(g, p2);
		} else {
			paint(g, p2);
			double w = width * (1.0 - SmoothUtil.smooth(x));
			double h = height * (1.0 - SmoothUtil.smooth(x));
			g.setClip(new Rectangle2D.Double((width - w) / 2, (height - h) / 2, w, h));
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