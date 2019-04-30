package org.icpc.tools.presentation.core.transition;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.Transition;

/**
 * A transition that removes the old presentation in sized black blocks and then adds the new one
 * in the same way.
 */
public class DissolveThroughBlackTransition extends Transition {
	protected byte[][] map;
	protected int sq = 30;
	protected int w, h;

	public DissolveThroughBlackTransition() {
		// default size
	}

	public DissolveThroughBlackTransition(int sq) {
		this.sq = sq;
	}

	@Override
	public TimeOverlap getTimeOverlap() {
		return TimeOverlap.MID;
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
		int k = 0;
		if (x < 0.5) {
			paint(g, p1);
			k = (int) (x * 200f);
		} else {
			paint(g, p2);
			k = 100 - (int) ((x - 0.5) * 200f);
		}

		g.setColor(Color.BLACK);
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				if (map[i][j] < k) {
					g.fillRect(i * sq, j * sq, sq, sq);
				}
			}
		}
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