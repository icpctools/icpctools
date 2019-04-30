package org.icpc.tools.presentation.core.transition;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;

import org.icpc.tools.presentation.core.Presentation;

public class SplitTransition extends OrientationalTransition {
	public SplitTransition() {
		super();
	}

	public SplitTransition(Direction dir) {
		super(dir);
	}

	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		paint(g, p1);
		double sx = SmoothUtil.smooth(x);
		if (dir == Direction.VERTICAL_OUT) {
			int xx = (int) (sx * width);
			g.setClip((width - xx) / 2, 0, xx, height);
		} else if (dir == Direction.VERTICAL_IN) {
			int xx = (int) (sx * width / 2.0);
			Area a1 = new Area(new Rectangle(0, 0, xx, height));
			Area a2 = new Area(new Rectangle(width - xx, 0, xx, height));
			a1.add(a2);
			g.setClip(a1);
		} else if (dir == Direction.HORIZONTAL_OUT) {
			int xx = (int) (sx * height);
			g.setClip(0, (height - xx) / 2, width, xx);
		} else if (dir == Direction.HORIZONTAL_IN) {
			int xx = (int) (sx * height / 2.0);
			Area a1 = new Area(new Rectangle(0, 0, width, xx));
			Area a2 = new Area(new Rectangle(0, height - xx, width, xx));
			a1.add(a2);
			g.setClip(a1);
		}
		paint(g, p2);
	}
}