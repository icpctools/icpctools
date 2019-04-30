package org.icpc.tools.presentation.contest.internal.tile;

import java.awt.geom.Point2D;

import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator2D;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;

public class TileAnimator extends Animator2D {
	protected static final Movement ZOOM_ANIM = new Movement(1.0, 2.0);
	protected static final Movement TEAM_X_ANIM = new Movement(1.75, 5.0);
	protected static final Movement TEAM_Y_ANIM = new Movement(0.4, 1.75);
	private Animator zAnim;

	public TileAnimator(Point2D p) {
		super(p, TEAM_X_ANIM, TEAM_Y_ANIM);
		zAnim = new Animator(1.0, ZOOM_ANIM);
	}

	@Override
	public void incrementTimeMs(long dt) {
		super.incrementTimeMs(dt);
		zAnim.incrementTimeMs(dt);
	}

	@Override
	public void reset(Point2D p) {
		super.reset(p);
		zAnim.reset(1.0);
	}

	public void setZoomTarget(double z) {
		zAnim.setTarget(z);
	}

	public double getZoom() {
		return zAnim.getValue();
	}
}