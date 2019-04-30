package org.icpc.tools.presentation.core.transition;

import java.awt.Graphics2D;

import org.icpc.tools.presentation.core.Presentation;

public class PushTransition extends DirectionalTransition {
	public PushTransition() {
		super(Direction.UP);
	}

	public PushTransition(Direction dir) {
		super(dir);
	}

	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		double sx = SmoothUtil.smooth(x);
		g.translate(m[0] * (int) (sx * width), m[1] * (int) (sx * height));
		clipAndPaint(g, p1);
		g.translate(-width * m[0], -height * m[1]);
		clipAndPaint(g, p2);
	}
}