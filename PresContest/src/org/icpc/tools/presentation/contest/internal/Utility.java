package org.icpc.tools.presentation.contest.internal;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Utility class for common graphics functions.
 */
public class Utility {
	public static Color getColorBetween(Color c1, Color c2, float percent) {
		float[] c1rgb = c1.getRGBComponents(null);
		float[] c2rgb = c2.getRGBComponents(null);

		float[] f = new float[4];
		for (int j = 0; j < 4; j++) {
			f[j] = c1rgb[j] * (1f - percent) + c2rgb[j] * percent;
		}
		return new Color(f[0], f[1], f[2], f[3]);
	}

	public static Color getColorBetween(Color c1, Color c2, Color c3, float percent) {
		float[] c1rgb = c1.getRGBComponents(null);
		float[] c2rgb = c2.getRGBComponents(null);
		float[] c3rgb = c3.getRGBComponents(null);

		float[] f = new float[4];
		for (int j = 0; j < 4; j++) {
			if (percent <= 0.5) {
				f[j] = c1rgb[j] * (1f - percent * 2f) + c2rgb[j] * percent * 2f;
			} else {
				f[j] = c2rgb[j] * (1f - (percent - 0.5f) * 2f) + c3rgb[j] * (percent - 0.5f) * 2f;
			}
		}
		return new Color(f[0], f[1], f[2], f[3]);
	}

	public static Color[] getColorsBetween(Color c1, Color c2, int steps) {
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

	public static Color alpha(Color c, int alpha) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}

	public static Color darker(Color c, float f) {
		return new Color(Math.max((int) (c.getRed() * f), 0), Math.max((int) (c.getGreen() * f), 0),
				Math.max((int) (c.getBlue() * f), 0));
	}

	public static Color alphaDarker(Color c, int alpha, float f) {
		return new Color(Math.max((int) (c.getRed() * f), 0), Math.max((int) (c.getGreen() * f), 0),
				Math.max((int) (c.getBlue() * f), 0), alpha);
	}

	/**
	 * Draws a string with a black outline.
	 *
	 * @param g
	 * @param s
	 * @param x
	 * @param y
	 */
	public static void drawString3D(Graphics2D g, String s, float x, float y) {
		Color c = g.getColor();
		g.setColor(alpha(Color.BLACK, c.getAlpha() / 3));
		g.drawString(s, x - 1, y - 1);
		// g.drawString(s, x-1, y+1);
		// g.drawString(s, x+1, y-1);
		g.drawString(s, x + 1, y + 1);
		g.setColor(c);
		g.drawString(s, x, y);
	}

	/**
	 * Draws a string with a black outline.
	 *
	 * @param g
	 * @param s
	 * @param x
	 * @param y
	 */
	public static void drawString3D3(Graphics2D g, String s, float x, float y) {
		Color c = g.getColor();
		g.setColor(Color.BLACK);
		g.drawString(s, x - 2, y - 2);
		g.drawString(s, x - 2, y + 2);
		g.drawString(s, x + 2, y - 2);
		g.drawString(s, x + 2, y + 2);
		g.setColor(c);
		g.drawString(s, x, y);
	}

	/**
	 * Draws a string with a white outline.
	 *
	 * @param g
	 * @param s
	 * @param x
	 * @param y
	 */
	public static void drawString3DWhite(Graphics2D g, String s, float x, float y) {
		Color c = g.getColor();
		g.setColor(alpha(Color.WHITE, c.getAlpha() / 2));
		g.drawString(s, x - 1, y - 1);
		// g.drawString(s, x-1, y+1);
		// g.drawString(s, x+1, y-1);
		g.drawString(s, x + 1, y + 1);
		g.setColor(c);
		g.drawString(s, x, y);
	}
}