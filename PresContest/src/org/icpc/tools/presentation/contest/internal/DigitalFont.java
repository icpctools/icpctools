package org.icpc.tools.presentation.contest.internal;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

public class DigitalFont {
	protected static List<Polygon> digital = new ArrayList<>();
	protected static List<short[]> digital2 = new ArrayList<>();

	static {
		digital.add(new Polygon(new int[] { 6779, 5700, 2157, 1079 }, new int[] { -10204, -9111, -9111, -10204 }, 4)); // top
		// 874 to 6779, 6983
		// 2303, 5758
		digital.add(new Polygon(new int[] { 874, 874, 1968, 1968, 1283 },
				new int[] { -5714, -10000, -8907, -5991, -5306 }, 5)); // top left

		digital.add(new Polygon(new int[] { 5904, 5904, 6983, 6983, 6589 }, new int[] { -5991, -8907, -10000, -5714,
				-5306 }, 5)); // top right

		digital.add(new Polygon(new int[] { 5831, 6371, 5831, 2026, 1486, 2026 }, new int[] { -5642, -5102, -4562, -4562,
				-5102, -5642 }, 6)); // middle

		digital.add(new Polygon(new int[] { 1968, 1968, 874, 874, 1283 }, new int[] { -4213, -1297, -204, -4475, -4898 },
				5)); // bottom left

		digital.add(new Polygon(new int[] { 6983, 6983, 5904, 5904, 6589 },
				new int[] { -4475, -204, -1297, -4213, -4898 }, 5)); // bottom right

		digital.add(new Polygon(new int[] { 6779, 1079, 2157, 5700 }, new int[] { 0, 0, -1093, -1093 }, 4)); // bottom

		digital2.add(new short[] { 1, 1, 1, 0, 1, 1, 1 }); // 0
		digital2.add(new short[] { 0, 0, 1, 0, 0, 1, 0 });
		digital2.add(new short[] { 1, 0, 1, 1, 1, 0, 1 }); // 2
		digital2.add(new short[] { 1, 0, 1, 1, 0, 1, 1 });
		digital2.add(new short[] { 0, 1, 1, 1, 0, 1, 0 }); // 4
		digital2.add(new short[] { 1, 1, 0, 1, 0, 1, 1 });
		digital2.add(new short[] { 1, 1, 0, 1, 1, 1, 1 }); // 6
		digital2.add(new short[] { 1, 0, 1, 0, 0, 1, 0 });
		digital2.add(new short[] { 1, 1, 1, 1, 1, 1, 1 }); // 8
		digital2.add(new short[] { 1, 1, 1, 1, 0, 1, 0 });

		// colon (:) -10204
		digital.add(new Polygon(new int[] { 1210, 1210, 2303, 2303 }, new int[] { -7406, -8499, -8499, -7406 }, 4)); // top
		// digital.add(new Polygon(new int[] { 1210, 1210, 2303, 2303 }, new int[] { -1501, -1093,
		// -1093, 0 }, 4)); // bottom
		digital.add(new Polygon(new int[] { 1210, 1210, 2303, 2303 }, new int[] { -1501, -2491, -2491, -1501 }, 4)); // bottom

		// minus (-)
		digital.add(new Polygon(new int[] { 5219, 5758, 5219, 1414, 874, 1414 }, new int[] { -5642, -5102, -4562, -4562,
				-5102, -5642 }, 6));

		// plus
		digital.add(new Polygon(new int[] { 3863, 3324, 2770, 2770, 3324, 3863 }, new int[] { -5845, -5306, -5845, -6997,
				-7551, -6997 }, 6));

		digital.add(new Polygon(new int[] { 5758, 5219, 4067, 3513, 4067, 5219 }, new int[] { -5102, -4562, -4562, -5102,
				-5642, -5642 }, 6));

		digital.add(new Polygon(new int[] { 3119, 2566, 1414, 874, 1414, 2566 }, new int[] { -5102, -4562, -4562, -5102,
				-5642, -5642 }, 6));

		digital.add(new Polygon(new int[] { 3863, 3324, 2770, 2770, 3324, 3863 }, new int[] { -3193, -2653, -3193, -4359,
				-4898, -4359 }, 6));
	}

	/**
	 * Draw a string that looks like a digital clock.
	 *
	 * @param s the string
	 * @param x x-position of bottom left corner
	 * @param y y-position of bottom left corner
	 * @param h text height
	 * @param fg the foreground font color
	 * @param bg the color for squares that are 'off'
	 */
	public static void drawString(Graphics2D g, String s, int x, int y, int h, Color fg, Color bg) {
		float scale = h / 10240f;
		Graphics2D gg = (Graphics2D) g.create();
		gg.translate(x, y);
		gg.scale(scale, scale);

		char[] cc = s.toCharArray();
		for (char c : cc) {
			gg.translate(-874, 0);
			if (Character.isDigit(c)) {
				gg.setColor(fg);
				String cs = c + "";

				int b = Integer.parseInt(cs);

				short[] ss = digital2.get(b);
				int j = 0;
				for (short sh : ss) {
					if (sh == 1)
						gg.setColor(fg);
					else {
						if (bg != null)
							gg.setColor(bg);
					}

					Polygon p = digital.get(j);
					if (sh == 1 || bg != null)
						gg.fillPolygon(p);

					j++;
				}

				gg.translate(6109, 0);
			} else if (c == ':') {
				gg.setColor(fg);
				Polygon p = digital.get(7);
				gg.fillPolygon(p);
				p = digital.get(8);
				gg.fillPolygon(p);
				gg.translate(1092, 0);
			} else if (c == '.') {
				gg.setColor(fg);
				Polygon p = digital.get(8);
				gg.fillPolygon(p);

				gg.translate(1092, 0);
			} else if (c == '-') {
				gg.setColor(fg);
				Polygon p = digital.get(9);
				gg.fillPolygon(p);

				gg.translate(4884, 0);
			} else if (c == '+') {
				gg.setColor(fg);
				Polygon p = digital.get(10);
				gg.fillPolygon(p);
				p = digital.get(11);
				gg.fillPolygon(p);
				p = digital.get(12);
				gg.fillPolygon(p);
				p = digital.get(13);
				gg.fillPolygon(p);

				gg.translate(4884, 0);
			}
			gg.translate(1750, 0);
		}
		gg.dispose();
	}

	/**
	 * Returns the width of the given string at the given height.
	 *
	 * @param s the string
	 * @param h text height
	 */
	public static int stringWidth(String s, int h) {
		int w = 876 * (s.length() - 1); // spacing
		// digit: 874 to 6983
		// colon: 1210 - 2303
		// plus/minus: 874 - 5758
		for (char c : s.toCharArray()) {
			if (Character.isDigit(c))
				w += 6109;
			else if (c == ':' || c == '.')
				w += 1092;
			else if (c == '-' || c == '+')
				w += 4884;
		}

		return (int) (w * h / 10240.0);
	}
}