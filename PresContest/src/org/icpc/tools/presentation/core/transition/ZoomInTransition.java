package org.icpc.tools.presentation.core.transition;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import org.icpc.tools.presentation.core.Presentation;

public class ZoomInTransition extends DirectionalTransition {
	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		paint(g, p1);
		double sx = SmoothUtil.smooth(x);
		double stx = width / 2.0 + width / 2.0 * m[0];
		double sty = height / 2.0 + height / 2.0 * m[1];
		int xx = (int) (stx * (1.0 - sx));
		int yy = (int) (sty * (1.0 - sx));
		g.translate(xx, yy);

		AffineTransform trans = AffineTransform.getScaleInstance(sx, sx);
		g.transform(trans);
		clipAndPaint(g, p2);
	}
}