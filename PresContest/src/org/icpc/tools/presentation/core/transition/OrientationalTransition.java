package org.icpc.tools.presentation.core.transition;

import org.icpc.tools.presentation.core.Transition;

public abstract class OrientationalTransition extends Transition {
	public enum Direction {
		HORIZONTAL_IN, HORIZONTAL_OUT, VERTICAL_IN, VERTICAL_OUT
	}

	protected Direction dir;

	public OrientationalTransition() {
		this(Direction.HORIZONTAL_IN);
	}

	public OrientationalTransition(Direction dir) {
		this.dir = dir;
	}

	public void setDirection(Direction dir) {
		this.dir = dir;
	}

	@Override
	public void setProperty(String value) {
		Direction d = Direction.valueOf(value);
		if (d != null)
			dir = d;
	}
}