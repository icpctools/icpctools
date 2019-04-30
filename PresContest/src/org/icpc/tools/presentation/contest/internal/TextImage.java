package org.icpc.tools.presentation.contest.internal;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

/**
 * Utility class from drawing text.
 */
public class TextImage {
	private static HashMap<Integer, Image> map = new HashMap<>();

	public static void drawString(Graphics2D g, String s, int x, int y) {
		Color c = g.getColor();
		Font f = g.getFont();
		int key = f.hashCode();
		if (s != null)
			key = key * 31 + s.hashCode();
		if (c != null)
			key = key * 31 + c.hashCode();

		Image image = map.get(key);
		if (image == null) {
			FontMetrics fm = g.getFontMetrics();
			image = createImage(fm.getStringBounds(s, g), c, f, s);
			map.put(key, image);
		}

		g.drawImage(image, x - 1, y - 1, null);
	}

	private static Image createImage(Rectangle2D rect, Color c, Font f, String s) {
		int w = (int) rect.getWidth() + 2 + 5;
		int h = (int) rect.getHeight() + 2;

		Image image = new BufferedImage(w, h, Transparency.TRANSLUCENT);
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		g.setFont(f);
		g.setColor(c);
		g.drawString(s, 1, g.getFontMetrics().getAscent() + 1);
		g.dispose();

		return image;
	}
}