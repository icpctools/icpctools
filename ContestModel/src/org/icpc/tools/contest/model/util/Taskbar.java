package org.icpc.tools.contest.model.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import javax.imageio.ImageIO;

public class Taskbar {
	public static void setTaskbarImage(InputStream in) {
		try {
			BufferedImage image = ImageIO.read(in);
			if (image != null)
				Taskbar.setTaskbarImage(image);
		} catch (IOException e) {
			// could not set icon
		}
	}

	public static void setTaskbarImage(Image iconImage) {
		// call java.awt.Taskbar.getTaskbar().setIconImage() (Java 9+) or
		// for Mac Java 8 call com.apple.eawt.Application.getApplication().setDockIconImage()
		// without direct dependencies
		try {
			Class<?> c = Class.forName("java.awt.Taskbar");
			Method m = c.getDeclaredMethod("getTaskbar");
			Object o = m.invoke(null);
			m = c.getDeclaredMethod("setIconImage", Image.class);
			m.invoke(o, iconImage);
			return;
		} catch (Exception e) {
			// ignore
		}

		if (!System.getProperty("os.name").contains("Mac"))
			return;

		try {
			Class<?> c = Class.forName("com.apple.eawt.Application");
			Method m = c.getDeclaredMethod("getApplication");
			Object o = m.invoke(null);
			m = c.getDeclaredMethod("setDockIconImage", Image.class);
			m.invoke(o, iconImage);
		} catch (Exception e) {
			// ignore
		}
	}
}