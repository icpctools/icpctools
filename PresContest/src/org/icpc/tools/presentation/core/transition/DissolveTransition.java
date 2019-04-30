package org.icpc.tools.presentation.core.transition;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.Transition;

/**
 * A transition that removes the old presentation in sized black blocks and then adds the new one
 * in the same way.
 */
public class DissolveTransition extends Transition {
	protected byte[][] map;
	protected int sq = 30;
	protected int w, h;

	public DissolveTransition() {
		// default size
	}

	public DissolveTransition(int sq) {
		this.sq = sq;
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		if (width <= 0 || height <= 0)
			return;

		w = width / sq;
		h = height / sq;

		map = new byte[w][h];

		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				map[i][j] = (byte) (Math.random() * 100f);
			}
		}
	}

	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		paint(g, p1);

		Area clip = new Area();
		int k = (int) (x * 200f);
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				if (map[i][j] < k) {
					Rectangle r = new Rectangle(i * sq, j * sq, sq, sq);
					clip.add(new Area(r));
				}
			}
		}
		g.setClip(clip);
		paint(g, p2);
	}

	@Override
	public void setProperty(String value) {
		try {
			int num = Integer.parseInt(value);
			if (num < 2 || num > 100)
				return;
			sq = num;
			setSize(new Dimension(width, height));
		} catch (Exception e) {
			Trace.trace(Trace.USER, "Invalid property: " + value);
		}
	}
}