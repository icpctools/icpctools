package org.icpc.tools.presentation.admin.internal;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.icpc.tools.contest.Trace;

/**
 * Utility class to handle image resources.
 */
public class ImageResource {
	public static final String IMG_ICON = "icon";
	public static final String IMG_MISSING_PRESENTATION = "missing-presentation";
	public static final String IMG_MISSING_TRANSITION = "missing-transition";

	private static Map<String, Image> images = new HashMap<>();

	/**
	 * Cannot construct an ImageResource. Use static methods only.
	 */
	private ImageResource() {
		// do nothing
	}

	/**
	 * Dispose of image resources.
	 */
	protected static void dispose() {
		for (Image img : images.values()) {
			img.dispose();
		}
	}

	/**
	 * Return the image with the given key.
	 *
	 * @param key java.lang.String
	 * @return org.eclipse.swt.graphics.Image
	 */
	public static Image getImage(String key) {
		return images.get(key);
	}

	/**
	 * Initialize the image resources.
	 */
	protected static void initializeImageRegistry(Class<?> c, Display display) {
		registerImage(IMG_ICON, "adminIcon.png", c, display);
		registerImage(IMG_MISSING_PRESENTATION, "missingPres.png", c, display);
		registerImage(IMG_MISSING_TRANSITION, "missingTrans.png", c, display);
	}

	/**
	 * Register an image with the registry.
	 *
	 * @param key java.lang.String
	 * @param partialURL java.lang.String
	 */
	private static void registerImage(String key, String partialURL, Class<?> c, Display display) {
		InputStream in = null;
		try {
			in = c.getResourceAsStream("/images/" + partialURL);
			if (in != null)
				images.put(key, new Image(display, in));
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error registering image " + key, e);
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}
}