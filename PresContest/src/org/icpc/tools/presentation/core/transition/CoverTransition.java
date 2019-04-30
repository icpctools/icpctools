package org.icpc.tools.presentation.core.transition;

import java.awt.Graphics2D;

import org.icpc.tools.presentation.core.Presentation;

public class CoverTransition extends DirectionalTransition {
	public CoverTransition() {
		super(Direction.UP);
	}

	public CoverTransition(Direction dir) {
		super(dir);
	}

	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		paint(g, p1);
		double sx = SmoothUtil.smooth(x);
		g.translate((int) (m[0] * (sx * width - width)), (int) (m[1] * (sx * height - height)));
		clipAndPaint(g, p2);
	}
}