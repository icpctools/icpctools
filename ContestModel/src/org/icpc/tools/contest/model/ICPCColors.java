package org.icpc.tools.contest.model;

import java.awt.Color;

public class ICPCColors {
	private static final Color PENDING_COLOR = new Color(230, 230, 0);
	private static final Color SOLVED_COLOR = new Color(0, 230, 0);
	private static final Color FAILED_COLOR = new Color(240, 0, 0);

	private static final int CCOUNT = 15;
	private static final Color[] PENDING = getColorsBetween(PENDING_COLOR, alphaDarker(PENDING_COLOR, 128, 0.5f), CCOUNT);
	private static final Color[] SOLVED = getColorsBetween(SOLVED_COLOR, alphaDarker(SOLVED_COLOR, 128, 0.5f), CCOUNT);
	private static final Color[] FAILED = getColorsBetween(FAILED_COLOR, alphaDarker(FAILED_COLOR, 128, 0.5f), CCOUNT);

	public static Color[] colorText = getColorsBetween(Color.darkGray, Color.white, CCOUNT);

	public static Color getStatusColor(Status status, long ms) {
		int k = (int) ((ms * 1.5 / 1000) % (ICPCColors.CCOUNT * 2)); // flash more than once per
		// second
		if (k > (ICPCColors.CCOUNT - 1))
			k = (ICPCColors.CCOUNT * 2 - 1) - k;

		if (status == Status.SOLVED)
			return ICPCColors.SOLVED[k];
		else if (status == Status.FAILED)
			return ICPCColors.FAILED[k];
		return ICPCColors.PENDING[k];
	}

	public static Color getStatusColor(Status status) {
		if (status == Status.SOLVED)
			return ICPCColors.SOLVED_COLOR;
		else if (status == Status.FAILED)
			return ICPCColors.FAILED_COLOR;
		return ICPCColors.PENDING_COLOR;
	}

	public static Color alphaDarker(Color c, int alpha, float f) {
		return new Color(Math.max((int) (c.getRed() * f), 0), Math.max((int) (c.getGreen() * f), 0),
				Math.max((int) (c.getBlue() * f), 0), alpha);
	}

	private static Color[] getColorsBetween(Color c1, Color c2, int steps) {
		Color[] colors = new Color[steps];
		float[] c1rgb = c1.getRGBComponents(null);
		float[] c2rgb = c2.getRGBComponents(null);

		float[] f = new float[4];
		for (int i = 0; i < steps; i++) {
			float x = (i / (float) (steps - 1));
			for (int j = 0; j < 4; j++) {
				f[j] = c1rgb[j] * (1f - x) + c2rgb[j] * x;
			}
			colors[i] = new Color(f[0], f[1], f[2], f[3]);
		}
		return colors;
	}

	/**
	 * Return a contrasting color to the given color.
	 *
	 * @param color
	 * @return a contrasting color
	 */
	public static Color getContrastColor(Color color) {
		if (color == null)
			return Color.WHITE;
		long y = (299 * color.getRed() + 587 * color.getGreen() + 114 * color.getBlue()) / 1000;
		return y >= 128 ? Color.BLACK : Color.WHITE;
	}
}