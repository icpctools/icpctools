package org.icpc.tools.presentation.core.transition;

import org.icpc.tools.presentation.core.Transition;

public abstract class DirectionalTransition extends Transition {
	public enum Direction {
		UP, DOWN, LEFT, RIGHT, UP_RIGHT, DOWN_RIGHT, DOWN_LEFT, UP_LEFT
	}

	protected int[] m;

	public DirectionalTransition() {
		this(Direction.UP);
	}

	public DirectionalTransition(Direction dir) {
		m = getDir(dir);
	}

	public void setDirection(Direction dir) {
		m = getDir(dir);
	}

	private static int[] getDir(Direction dir) {
		switch (dir) {
			case UP:
				return new int[] { 0, -1 };
			case UP_RIGHT:
				return new int[] { 1, -1 };
			case RIGHT:
				return new int[] { 1, 0 };
			case DOWN_RIGHT:
				return new int[] { 1, 1 };
			case DOWN:
				return new int[] { 0, 1 };
			case DOWN_LEFT:
				return new int[] { -1, 1 };
			case LEFT:
				return new int[] { -1, 0 };
			case UP_LEFT:
				return new int[] { -1, -1 };
			default:
				return new int[] { 0, 0 };
		}
	}

	@Override
	public void setProperty(String value) {
		Direction d = Direction.valueOf(value);
		if (d != null)
			m = getDir(d);
	}
}