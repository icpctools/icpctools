package org.icpc.tools.presentation.core.transition;

import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;

import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.Transition;

public class ClockTransition extends Transition {
	private double mult = -1.0;

	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		paint(g, p1);
		Area a = new Area(new Arc2D.Double(-width, -height, width * 3, height * 3, 90,
				mult * 360.0 * SmoothUtil.smooth(x), Arc2D.PIE));
		g.setClip(a);
		paint(g, p2);
	}

	@Override
	public void setProperty(String value) {
		if ("-".equals(value))
			mult = 1.0;
		else
			mult = -1.0;
	}
}