package org.icpc.tools.contest;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class SWTLauncher {
	private static final String[] SWT_JARS = new String[] { "swt-gtk-linux-x86_64.jar", "swt-win32-win32-x86_64.jar",
			"swt-cocoa-macosx-x86_64.jar", "swt-cocoa-macosx-aarch64.jar" };

	protected static URL findSWTJar() throws IOException {
		String osName = System.getProperty("os.name").toLowerCase();
		String osArch = System.getProperty("os.arch").toLowerCase();
		int x = 0;
		if (osName.contains("windows"))
			x = 1;
		else if (osName.contains("mac")) {
			if (!osArch.equals("aarch64"))
				x = 2;
			else
				x = 3;
		}

		return new File("lib" + File.separator + SWT_JARS[x]).toURI().toURL();
	}

	public static void main(String[] args) {
		if (args == null || args.length < 2) {
			System.err.println("Error: Missing SWT launcher arguments");
			return;
		}

		try {
			List<URL> urls = new ArrayList<URL>();

			URL swtURL = findSWTJar();
			urls.add(swtURL);

			File libFolder = new File("lib");
			File[] libs = libFolder.listFiles(new FileFilter() {
				@Override
				public boolean accept(File ff) {
					return !ff.getName().startsWith("swt-") && ff.getName().endsWith(".jar");
				}
			});
			for (File lib : libs) {
				urls.add(lib.toURI().toURL());
			}

			URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[0]));
			Class<?> c = cl.loadClass(args[0]);
			Method m = c.getMethod("main", String[].class);

			int size = args.length;
			String[] newAgs = new String[size - 1];
			System.arraycopy(args, 1, newAgs, 0, size - 1);
			m.invoke(null, (Object) newAgs);
			cl.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
