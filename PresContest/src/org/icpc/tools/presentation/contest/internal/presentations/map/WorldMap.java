package org.icpc.tools.presentation.contest.internal.presentations.map;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;

public class WorldMap {
	private static BufferedImage map;

	public static void load(Class<?> c) {
		if (map != null)
			return;

		ClassLoader cl = c.getClassLoader();
		try {
			map = ImageIO.read(cl.getResource("images/map.jpg"));
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading images", e);
		}
	}

	public static BufferedImage getMap() {
		return map;
	}

	public static void drawMap(Graphics2D g, int width, int height) {
		if (map == null)
			return;

		g.drawImage(map, 0, 0, width, height, 0, 0, map.getWidth(), map.getHeight(), null);
	}

	public static void drawMap(Graphics2D g, int x, int y, int width, int height, double sc) {
		if (map == null)
			return;

		g.drawImage(map, x, y, x + (int) (width * sc), y + (int) (height * sc), 0, 0, map.getWidth(), map.getHeight(),
				null);

		if (x + width * sc < width)
			g.drawImage(map, x + (int) (width * sc), y, x + (int) (width * sc) * 2, y + (int) (height * sc), 0, 0,
					map.getWidth(), map.getHeight(), null);

		if (x > 0)
			g.drawImage(map, x - (int) (width * sc), y, x, y + (int) (height * sc), 0, 0, map.getWidth(), map.getHeight(),
					null);
	}
}