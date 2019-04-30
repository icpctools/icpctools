package org.icpc.tools.presentation.contest.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.model.feed.ContestSource;

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
}