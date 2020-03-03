package org.icpc.tools.contest.model.util;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ICPCColors;

public class Balloon {
	private static BufferedImage balloonImage;
	private static BufferedImage solvedImage;
	private static BufferedImage failedImage;

	public static void load(Class<?> c) {
		if (balloonImage != null)
			return;

		ClassLoader cl = c.getClassLoader();
		try {
			balloonImage = ImageIO.read(cl.getResource("images/balloon.gif"));
			solvedImage = ImageIO.read(cl.getResource("images/balloonSolved.gif"));
			failedImage = ImageIO.read(cl.getResource("images/balloonFailed.gif"));
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading images", e);
		}
	}

	public static BufferedImage getBalloonSolvedImage() {
		return solvedImage;
	}

	public static BufferedImage getBalloonFailedImage() {
		return failedImage;
	}

	public static BufferedImage getBalloonImage(Color c) {
		int balloonColor = -1;
		int highlightColor = -1;
		int outlineColor = 255 << 24;
		if (c != null) {
			balloonColor = 255 << 24 | c.getRed() << 16 | c.getGreen() << 8 | c.getBlue();
			Color cc = c.brighter();
			highlightColor = 255 << 24 | cc.getRed() << 16 | cc.getGreen() << 8 | cc.getBlue();
			outlineColor = ICPCColors.getContrastColor(c).getRGB();
		}
		BufferedImage img = new BufferedImage(balloonImage.getWidth(), balloonImage.getHeight(),
				BufferedImage.TYPE_4BYTE_ABGR);
		for (int x = 0; x < balloonImage.getWidth(); x++) {
			for (int y = 0; y < balloonImage.getHeight(); y++) {
				int rgb = balloonImage.getRGB(x, y);
				if (rgb != -1) {
					if (rgb == -65536) // red
						rgb = balloonColor;
					else if (rgb == -14650) // highlight
						rgb = highlightColor;
					else if (rgb == -16777216) // outline
						rgb = outlineColor;
					img.setRGB(x, y, rgb);
				}
			}
		}
		return img;
	}
}