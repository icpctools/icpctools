package org.icpc.tools.presentation.core.transition;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.Transition;
import org.icpc.tools.presentation.core.transition.DirectionalTransition.Direction;

/**
 * A transition that removes the old presentation in sized black blocks and then adds the new one
 * in the same way.
 */
public class BlindsTransition extends Transition {
	protected int num = 10;
	protected Direction d = Direction.DOWN;

	public BlindsTransition() {
		// default size
	}

	public BlindsTransition(int num) {
		this.num = num;
	}

	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		paint(g, p1);

		Area clip = new Area();
		double h = height / (double) num;
		double w = width / (double) num;
		for (int i = 0; i < num; i++) {
			if (d == Direction.DOWN) {
				Rectangle r = new Rectangle(0, (int) (i * h), width, (int) (x * h));
				clip.add(new Area(r));
			} else if (d == Direction.UP) {
				int hh = (int) (x * h);
				Rectangle r = new Rectangle(0, (int) ((i + 1) * h) - hh, width, hh);
				clip.add(new Area(r));
			} else if (d == Direction.RIGHT) {
				Rectangle r = new Rectangle((int) (i * w), 0, (int) (x * w), height);
				clip.add(new Area(r));
			} else if (d == Direction.LEFT) {
				int ww = (int) (x * w);
				Rectangle r = new Rectangle((int) ((i + 1) * w) - ww, 0, ww, height);
				clip.add(new Area(r));
			}
		}
		g.setClip(clip);
		paint(g, p2);
	}

	@Override
	public void setProperty(String value) {
		Direction newDir = Direction.valueOf(value);
		if (newDir != null && (newDir == Direction.UP || newDir == Direction.DOWN || newDir == Direction.RIGHT
				|| newDir == Direction.LEFT))
			d = newDir;

		try {
			int n = Integer.parseInt(value);
			if (n < 2 || n > 50)
				return;
			num = n;
		} catch (Exception e) {
			Trace.trace(Trace.USER, "Invalid property: " + value);
		}
	}
}