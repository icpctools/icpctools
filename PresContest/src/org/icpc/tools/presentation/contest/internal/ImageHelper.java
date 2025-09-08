package org.icpc.tools.presentation.contest.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.internal.SVGUtil;
import org.w3c.dom.svg.SVGDocument;

public class ImageHelper {
	/**
	 * Loads and returns the image at the given path, or throws an exception to say why it could not
	 * be loaded.
	 *
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public static BufferedImage loadImage(String path) throws Exception {
		if (path == null)
			throw new IOException("Invalid path");

		InputStream in = null;
		try {
			File f = ContestSource.getInstance().getFile(path);
			if (f == null)
				throw new IOException("No image found");

			BufferedImage img = ImageIO.read(f);
			if (img == null)
				throw new IOException("Unknown image type");
			return img;
		} catch (Exception e) {
			throw e;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Loads and returns a resized svg or png from the given path, or throws an exception to say why
	 * it could not be loaded.
	 *
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public static BufferedImage loadImage(String path, double width, double height) throws Exception {
		return loadImage(path, (int) width, (int) height);
	}

	/**
	 * Loads and returns a resized svg or png from the given path, or throws an exception to say why
	 * it could not be loaded.
	 *
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public static BufferedImage loadImage(String path, int width, int height) throws Exception {
		if (path == null)
			throw new IOException("Invalid path");

		InputStream in = null;
		try {
			File f = ContestSource.getInstance().getFile(path);
			if (f == null) {
				f = ContestSource.getInstance().getFile(path + ".png");
				if (f == null) {
					throw new IOException("Could not find image");
				}
			}

			if (f.getName().endsWith(".svg")) {
				SVGDocument svg = SVGUtil.loadSVG(f);
				if (svg == null)
					throw new IOException("Could not read svg");
				return SVGUtil.convertSVG(svg, width, height);
			}

			BufferedImage img = ImageIO.read(f);
			if (img == null)
				throw new IOException("Could not read png");

			return ImageScaler.scaleImage(img, width, height);
		} catch (Exception e) {
			throw e;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}
}