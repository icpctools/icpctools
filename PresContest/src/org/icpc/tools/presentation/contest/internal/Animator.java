package org.icpc.tools.presentation.contest.internal;

public class Animator {
	public static class Movement {
		private final double acceleration;
		private final double maxSpeed;

		public Movement(double acceleration, double maxSpeed) {
			this.acceleration = acceleration;
			this.maxSpeed = maxSpeed;
		}
	}

	private Movement move;
	private double acc;

	private double xInitial;
	private double xTarget;

	private double vTarget;

	private double t1;
	private double tCoast;
	private double tScale = 1.0;

	private long t;

	public Animator(double x, Movement move) {
		this.xInitial = x;
		this.move = move;
		this.acc = move.acceleration;
		xTarget = xInitial;
	}

	public void incrementTimeMs(long dt) {
		t += dt;
	}

	/**
	 * Returns the projected movement time (only valid immediately after calling setTarget().
	 */
	protected double getMoveTime() {
		return t1 * 2 + tCoast;
	}

	public double getValue() {
		double tt = t * tScale / 1000.0;
		if (tt < t1) {
			return xInitial + (acc * tt * tt) / 2.0 + vTarget * tt;
		} else if (tt < t1 + tCoast) {
			tt = tt - t1;
			return xInitial + (acc * t1 * t1) / 2.0 + tt * move.maxSpeed * Math.signum(xTarget - xInitial)
					+ vTarget * (t1 + tt);
		} else if (tt < (t1 * 2 + tCoast)) {
			tt = (t1 * 2 + tCoast) - tt;
			return xTarget - (acc * tt * tt) / 2.0 + vTarget * (t1 * 2 + tCoast - tt);
		}
		return xTarget + vTarget * tt;
	}

	private double getVelocity() {
		double tt = t * tScale / 1000.0;
		if (tt < t1) {
			return (acc * tt) * tScale;
		} else if (tt < t1 + tCoast) {
			return move.maxSpeed * Math.signum(xTarget - xInitial) * tScale;
		} else if (tt < (t1 * 2 + tCoast)) {
			tt = (t1 * 2 + tCoast) - tt;
			return (acc * tt) * tScale;
		}
		return 0;
	}

	public void resetToTarget() {
		reset(xTarget, 0);
	}

	public void reset(double x) {
		reset(x, 0);
	}

	public void reset(double x, double v) {
		xInitial = x;
		xTarget = x;
		t1 = 0;
		tCoast = 0;
		vTarget = v;
		tScale = 1.0;
	}

	public void setTimeScale(double ts) {
		tScale = Math.min(1.0, Math.max(0.1, ts));
	}

	public double getTarget() {
		return xTarget;
	}

	public void setTarget(double x) {
		setTarget(x, 0);
	}

	public void setTarget(double x, double v) {
		if (x == xTarget && v == vTarget)
			return;

		double vInitial = getVelocity();

		// set the start and end points and reset time
		xInitial = getValue();
		xTarget = x;
		vTarget = v;
		t = 0;
		tScale = 1.0;
		acc = move.acceleration;
		if (xTarget < xInitial)
			acc = -move.acceleration;

		// adjust the curve if we were already moving
		if (vInitial != 0) {
			double dt = Math.abs(vInitial / move.acceleration); // time to stop
			double a2 = -Math.signum(vInitial) * move.acceleration; // acceleration needed to stop
			double xx = xInitial + vInitial * dt + a2 * dt * dt / 2.0; // value once stopped
			if (xx < Math.min(xInitial, xTarget) || xx > Math.max(xInitial, xTarget)) {
				// need to reverse direction. set zero time ahead of us
				t = -(int) (dt * 1000.0);
				if (Math.signum(xTarget - xx) != Math.signum(xTarget - xInitial))
					acc = -acc;
			} else {
				// already going in the right direction. set zero time behind us
				t = (int) (dt * 1000.0);
			}
			xInitial = xInitial - (acc * dt * dt) / 2.0 - t * v / 1000.0;
		}

		t1 = Math.sqrt(Math.abs(xTarget - xInitial) / move.acceleration);

		// if we exceed the max speed, insert a section of constant velocity
		double mt = move.maxSpeed / move.acceleration;
		if (t1 > mt) {
			// distance traveled while reaching and decelerating from max speed
			double d = (move.acceleration * mt * mt);
			tCoast = (Math.abs(xTarget - xInitial) - d) / move.maxSpeed;
			t1 = mt;
		} else
			tCoast = 0;
	}

	@Override
	public String toString() {
		return "Animator: " + getValue() + " " + xTarget;
	}
}