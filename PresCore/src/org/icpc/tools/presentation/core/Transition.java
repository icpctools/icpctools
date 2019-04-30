package org.icpc.tools.presentation.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * A transition between two presentations.
 */
public abstract class Transition {
	public enum TimeOverlap {
		NONE, // p1 should stop animating at the start, and p2 shouldn't start animation until end
		MID, // p1 should animate until 0.5, at which point p2 should start
		FULL // p1 and p2 time should overlap over the full transition
	}

	protected int width;
	protected int height;

	public void setSize(Dimension d) {
		width = d.width;
		height = d.height;
	}

	public void init() {
		// do nothing
	}

	public TimeOverlap getTimeOverlap() {
		return TimeOverlap.FULL;
	}

	/**
	 * Helper method to add a clip (to make sure presentation doesn't go outside of bounds) and
	 * paint a given presentation.
	 *
	 * @param g
	 * @param p
	 */
	protected void clipAndPaint(Graphics2D g, Presentation p) {
		g.setClip(0, 0, width, height);
		paint(g, p);
	}

	/**
	 * Helper method to paint a presentation safely, including use of a new graphics context to
	 * avoid the presentation changing the graphics object directly.
	 *
	 * @param g
	 * @param p
	 */
	protected void paint(Graphics2D g, Presentation p) {
		Graphics2D g2 = (Graphics2D) g.create();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, width, height);
		p.paint(g2);
		g2.dispose();
	}

	/**
	 * Paint the transition. Time goes from 0 (start of the transition, p1 on screen) to 1 (end of
	 * the transition, p2 on screen) over the duration of the transition.
	 *
	 * @param g
	 * @param time
	 * @param p1
	 * @param p2
	 */
	public abstract void paint(Graphics2D g, double time, Presentation p1, Presentation p2);

	public void setProperty(String value) {
		// do nothing
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}