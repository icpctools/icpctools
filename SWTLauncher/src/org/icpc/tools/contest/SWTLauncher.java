package org.icpc.tools.contest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class SWTLauncher {
	private static final String[] SWT_JARS = new String[] { "swt-gtk-linux-x86_64.jar", "swt-win32-win32-x86_64.jar",
			"swt-cocoa-macosx-x86_64.jar" };

	protected static URL findSWTJar() throws IOException {
		String osName = System.getProperty("os.name").toLowerCase();
		int x = 0;
		if (osName.contains("windows"))
			x = 1;
		else if (osName.contains("mac"))
			x = 2;

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

			String jarArg = args[0];
			String[] jars = jarArg.split(",");
			for (String jar : jars) {
				urls.add(new File("lib" + File.separator + jar).toURI().toURL());
			}

			URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[0]));
			Class<?> c = cl.loadClass(args[1]);
			Method m = c.getMethod("main", String[].class);

			int size = args.length;
			String[] newAgs = new String[size - 2];
			System.arraycopy(args, 2, newAgs, 0, size - 2);
			m.invoke(null, (Object) newAgs);
			cl.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
