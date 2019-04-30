package org.icpc.tools.presentation.core.transition;

import java.awt.Graphics2D;

import org.icpc.tools.presentation.core.Presentation;

public class UncoverTransition extends DirectionalTransition {
	public UncoverTransition() {
		super(Direction.UP);
	}

	public UncoverTransition(Direction dir) {
		super(dir);
	}

	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		paint(g, p2);
		double xx = SmoothUtil.smooth(x);
		g.translate((int) (m[0] * xx * width), (int) (m[1] * xx * height));
		clipAndPaint(g, p1);
	}
}