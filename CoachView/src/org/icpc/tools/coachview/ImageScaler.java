package org.icpc.tools.coachview;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class ImageScaler {

	/**
	 * Returns a scaled instance of the provided {@code BufferedImage} made to fit within the given
	 * size without distortion.
	 *
	 * @param img the original image
	 * @param targetWidth the desired width of the scaled image
	 * @param targetHeight the desired height of the scaled image
	 * @return a scaled version of the original {@code BufferedImage}
	 */
	public static BufferedImage scaleImage(BufferedImage img, double targetWidth, double targetHeight) {
		return scaleImage(img, (int) targetWidth, (int) targetHeight);
	}

	public static BufferedImage scaleImage(BufferedImage img, int targetWidth, int targetHeight) {
		if (img == null)
			return null;

		int w = img.getWidth();
		int h = img.getHeight();
		float scale = Math.min(targetWidth / (float) w, targetHeight / (float) h);
		if (scale == 1f)
			return img;

		int nw = Math.round(w * scale);
		int nh = Math.round(h * scale);

		return getScaledImage(img, nw, nh);
	}

	/**
	 * Returns a scaled instance of the provided {@code BufferedImage}.
	 *
	 * @param img the original image
	 * @param targetWidth the desired width of the scaled image
	 * @param targetHeight the desired height of the scaled image
	 * @return a scaled version of the original {@code BufferedImage}
	 */
	private static BufferedImage getScaledImage(BufferedImage img, int targetWidth, int targetHeight) {
		if (img == null)
			return null;

		if (targetWidth == 0 || targetHeight == 0)
			return new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_4BYTE_ABGR);

		int type = img.getType();
		int sourceWidth = img.getWidth();
		int sourceHeight = img.getHeight();

		// Are we shrinking the image? If so, handle it ourselves
		if (targetWidth * 1.5 < sourceWidth && targetHeight * 1.5 < sourceHeight) {
			if (type == BufferedImage.TYPE_3BYTE_BGR || type == BufferedImage.TYPE_4BYTE_ABGR) {
				int depth = 3;
				if (img.getTransparency() != Transparency.OPAQUE)
					depth = 4;

				byte[] sourcePix = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();

				BufferedImage newImg = new BufferedImage(targetWidth, targetHeight, type);
				byte[] destPix = ((DataBufferByte) newImg.getRaster().getDataBuffer()).getData();

				// Total number of source pixels aggregated to each destination pixel
				double scale = (double) targetWidth * targetHeight / (sourceWidth * sourceHeight);

				// Per-destination-pixel sum of color components
				double[] sum = new double[depth];

				for (int dy = 0; dy < targetHeight; dy++) {
					// Compute range of source pixels in the Y direction
					double y = (double) dy * sourceHeight / targetHeight;
					int lowY = (int) y;
					// Weight of the top row in this span
					double firstYWeight = 1.0 - (y - lowY);

					y = (double) (dy + 1) * sourceHeight / targetHeight;
					int highY = (int) y;
					double lastYWeight = y - highY;
					// Handle last pixel specially, to prevent out-of-bounds
					if (highY >= sourceHeight) {
						highY = sourceHeight - 1;
						lastYWeight = 1.0;
					}

					for (int dx = 0; dx < targetWidth; dx++) {
						for (int c = 0; c < depth; c++)
							sum[c] = 0.0;

						// Compute range of source pixels in the X direction
						double x = (double) dx * sourceWidth / targetWidth;
						int lowX = (int) x;
						// Weight of the left column in this span
						double firstXWeight = 1.0 - (x - lowX);

						x = (double) (dx + 1) * sourceWidth / targetWidth;
						int highX = (int) x;
						double lastXWeight = x - highX;
						// Same as above, this will happen for last pixel in a row
						if (highX >= sourceWidth) {
							highX = sourceWidth - 1;
							lastXWeight = 1.0;
						}

						// Add up all the pixels that map to this one
						for (int sy = lowY; sy <= highY; sy++) {
							double yw = 1.0;
							if (sy == lowY)
								yw = firstYWeight;
							if (sy == highY)
								yw = lastYWeight;

							for (int sx = lowX; sx <= highX; sx++) {
								double xw = 1.0;
								if (sx == lowX)
									xw = firstXWeight;
								if (sx == highX)
									xw = lastXWeight;

								for (int c = 0; c < depth; c++) {
									int v = sourcePix[(sy * sourceWidth + sx) * depth + c] & 0xFF;
									sum[c] += xw * yw * v;
								}
							}
						}

						// Store the average in the destination image
						for (int c = 0; c < depth; c++) {
							int p = (int) Math.round(sum[c] * scale);
							if (p > 255)
								p = 255;
							destPix[(dy * targetWidth + dx) * depth + c] = (byte) p;
						}
					}
				}
				return newImg;
			}
		}

		if (img.getTransparency() == Transparency.OPAQUE)
			type = BufferedImage.TYPE_3BYTE_BGR;
		else
			type = BufferedImage.TYPE_4BYTE_ABGR;

		// if target is less than half the size of the original, resize by no more than 2 to reduce
		// down-sampling artifacts
		BufferedImage newImg = img;
		while (sourceWidth != targetWidth || sourceHeight != targetHeight) {
			if (sourceWidth > targetWidth)
				sourceWidth = Math.max(targetWidth, sourceWidth / 2);
			else
				sourceWidth = targetWidth;

			if (sourceHeight > targetHeight)
				sourceHeight = Math.max(targetHeight, sourceHeight / 2);
			else
				sourceHeight = targetHeight;

			BufferedImage tmp = new BufferedImage(sourceWidth, sourceHeight, type);
			Graphics2D g2 = tmp.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.drawImage(newImg, 0, 0, sourceWidth, sourceHeight, null);
			g2.dispose();

			newImg = tmp;
		}

		return newImg;
	}
}