package org.icpc.tools.presentation.contest.internal;

import org.icpc.tools.presentation.contest.internal.Animator.Movement;

public class Animator3D {
	// for now, just implement as three separate 1D Animators
	private Animator xAnim;
	private Animator yAnim;
	private Animator zAnim;

	public Animator3D(double x, double y, double z, Movement move) {
		xAnim = new Animator(x, move);
		yAnim = new Animator(y, move);
		zAnim = new Animator(z, move);
	}

	public Animator3D(double x, Movement xMove, double y, Movement yMove, double z, Movement zMove) {
		xAnim = new Animator(x, xMove);
		yAnim = new Animator(y, yMove);
		zAnim = new Animator(z, zMove);
	}

	public void incrementTimeMs(long dt) {
		xAnim.incrementTimeMs(dt);
		yAnim.incrementTimeMs(dt);
		zAnim.incrementTimeMs(dt);
	}

	public double getXValue() {
		return xAnim.getValue();
	}

	public double getYValue() {
		return yAnim.getValue();
	}

	public double getZValue() {
		return zAnim.getValue();
	}

	public void reset(double x, double y, double z) {
		xAnim.reset(x);
		yAnim.reset(y);
		zAnim.reset(z);
	}

	public void setTarget(double x, double y, double z) {
		xAnim.setTarget(x);
		yAnim.setTarget(y);
		zAnim.setTarget(z);

		double xTime = xAnim.getMoveTime();
		double yTime = yAnim.getMoveTime();
		double zTime = zAnim.getMoveTime();
		double max = Math.max(Math.max(xTime, yTime), zTime);
		if (xTime != 0)
			xAnim.setTimeScale(xTime / max);
		if (yTime != 0)
			yAnim.setTimeScale(yTime / max);
		if (zTime != 0)
			zAnim.setTimeScale(zTime / max);
	}
}