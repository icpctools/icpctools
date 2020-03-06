package org.icpc.tools.presentation.contest.internal;

import java.awt.geom.Point2D;

import org.icpc.tools.presentation.contest.internal.Animator.Movement;

public class Animator2D {
	// for now, just implement as two separate 1D Animators
	private Animator xAnim;
	private Animator yAnim;

	public Animator2D(Point2D p, Movement move) {
		xAnim = new Animator(p.getX(), move);
		yAnim = new Animator(p.getY(), move);
	}

	public Animator2D(Point2D p, Movement xMove, Movement yMove) {
		xAnim = new Animator(p.getX(), xMove);
		yAnim = new Animator(p.getY(), yMove);
	}

	public Animator2D(double x, Movement xMove, double y, Movement yMove) {
		xAnim = new Animator(x, xMove);
		yAnim = new Animator(y, yMove);
	}

	public void incrementTimeMs(long dt) {
		xAnim.incrementTimeMs(dt);
		yAnim.incrementTimeMs(dt);
	}

	public Point2D getValue() {
		return new Point2D.Double(xAnim.getValue(), yAnim.getValue());
	}

	public void reset(Point2D p) {
		xAnim.reset(p.getX());
		yAnim.reset(p.getY());
	}

	public void resetToTarget() {
		xAnim.resetToTarget();
		yAnim.resetToTarget();
	}

	public void setTarget(Point2D p) {
		setTarget(p.getX(), p.getY());
	}

	public void setTarget(double x, double y) {
		xAnim.setTarget(x);
		yAnim.setTarget(y);

		double xTime = xAnim.getMoveTime();
		double yTime = yAnim.getMoveTime();
		if (xTime != 0 && yTime != 0 && xTime != yTime) {
			if (xTime > yTime)
				yAnim.setTimeScale(yTime / xTime);
			else
				xAnim.setTimeScale(xTime / yTime);
		}
	}

	public double getXTarget() {
		return xAnim.getTarget();
	}

	public double getYTarget() {
		return yAnim.getTarget();
	}
}