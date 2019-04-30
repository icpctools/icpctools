package org.icpc.tools.presentation.core.transition;

import java.awt.Graphics2D;

import org.icpc.tools.presentation.core.Presentation;

public class WipeTransition extends DirectionalTransition {
	public WipeTransition() {
		super(Direction.UP);
	}

	public WipeTransition(Direction dir) {
		super(dir);
	}

	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		paint(g, p1);
		double sx = SmoothUtil.smooth(x);
		int xx = (int) (m[0] * (sx * width - width));
		int yy = (int) (m[1] * (sx * height - height));
		g.setClip(xx, yy, width, height);
		paint(g, p2);
	}
}