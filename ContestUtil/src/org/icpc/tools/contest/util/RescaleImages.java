package org.icpc.tools.contest.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;

/**
 * Utility class to resize larger images down to slightly bigger than 1920x1080.
 */
public class RescaleImages {
	private static final int SIZE = (int) (1920 * 1.25f);
	private static final Object obj = new Object();

	public static void main(String[] args) {
		Trace.init("ICPC Image Resizer", "imageResizer", args);

		if (args == null || args.length < 2) {
			Trace.trace(Trace.ERROR, "Missing/incorrect args");
			System.exit(0);
			return;
		}

		boolean overwrite = false;
		if (args.length > 2 && "x".equals(args[2]))
			overwrite = true;

		final File to = new File(args[1]);
		if (!to.exists())
			to.mkdirs();
		else if (to.listFiles().length > 0 && !overwrite) {
			Trace.trace(Trace.ERROR, "Output folder is not empty");
			System.exit(0);
			return;
		}

		File root = new File(args[0]);
		final File[] files = root.listFiles();

		final int split = 4;
		Thread[] threads = new Thread[split];
		for (int i = 0; i < 4; i++) {
			final int ii = i;
			threads[i] = new Thread("Image processor") {
				@Override
				public void run() {
					int num = files.length;
					// int d = num / split;
					int start = (ii * num) / split;
					int end = (num * (ii + 1)) / split;

					File[] ff = new File[end - start];
					for (int j = start; j < end; j++) {
						ff[j - start] = files[j];
					}
					process(ff, to);
				}
			};
			threads[i].setDaemon(true);
			threads[i].start();
		}

		try {
			for (Thread t : threads)
				t.join();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error launching", e);
		}

		Trace.trace(Trace.USER, "Done");
	}

	protected static void process(File[] files, File to) {
		for (File imgFile : files) {
			if (imgFile.isDirectory())
				continue;

			File toFile = new File(to, imgFile.getName());

			if (toFile.exists() && toFile.lastModified() == imgFile.lastModified())
				Trace.trace(Trace.USER, "Skipping: " + imgFile);
			else {
				if (toFile.exists()) {
					Trace.trace(Trace.USER, "Replacing: " + imgFile + " to " + toFile);
					toFile.delete();
				} else
					Trace.trace(Trace.USER, "Copying: " + imgFile + " to " + toFile);

				try {
					FileInputStream in = new FileInputStream(imgFile);
					BufferedImage img = null;

					synchronized (obj) {
						img = ImageIO.read(in);
					}

					if (SIZE != img.getWidth() || SIZE != img.getHeight())
						img = ImageScaler.scaleImage(img, SIZE, SIZE);

					ImageIO.write(img, "jpg", toFile);

					if (!toFile.exists())
						throw new IOException("New image didn't write");

					toFile.setLastModified(imgFile.lastModified());

					in.close();
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error scaling image", e);
				}
			}
		}
	}
}