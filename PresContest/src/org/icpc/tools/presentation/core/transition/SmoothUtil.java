package org.icpc.tools.presentation.core.transition;

public class SmoothUtil {
	private static final float[] SMOOTH = new float[1000];

	static {
		for (int i = 0; i < 1000; i++) {
			SMOOTH[i] = (float) smoothImpl(i / 1000.0);
		}
	}

	private static double smoothImpl(double x) {
		return f(x) / (f(x) + f(1 - x));
	}

	private static double f(double x) {
		if (x < 0)
			return -Math.exp(1.0 / x);
		if (x == 0)
			return 0;
		return Math.exp(-1.0 / x);
	}

	/**
	 * Smooth function. Pass in a number from 0 to 1 and it will return a 'smoothed' number between
	 * 0 and 1. The function starts moving slowly, picks up speed, and then slows down again before
	 * hitting 1.
	 *
	 * @param x
	 * @return
	 */
	public static float smooth(double x) {
		if (x <= 0)
			return 0;
		if (x >= 1)
			return 1;
		return SMOOTH[(int) (x * 1000.0)];
	}
}