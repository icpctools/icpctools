package org.icpc.tools.presentation.contest.internal;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.icpc.tools.contest.Trace;

/**
 * Helper class for efficiently loading language support from properties files into a messages
 * class.
 */
public class NLS {
	private static final String EXTENSION = ".properties";
	private static String[] SUFFIXES;

	static {
		// build list of suffixes for loading resource bundles
		String locale = Locale.getDefault().toString();
		List<String> list = new ArrayList<>(4);
		list.add(EXTENSION);

		int ind = locale.lastIndexOf('_');
		while (ind >= 0) {
			list.add(1, '_' + locale + EXTENSION);
			locale = locale.substring(0, ind);
			ind = locale.lastIndexOf('_');
		}
		list.add(1, '_' + locale + EXTENSION);

		SUFFIXES = list.toArray(new String[0]);
	}

	private static String[] getMessagesFiles(String root) {
		String base = root.replace('.', '/');
		String[] files = new String[SUFFIXES.length];
		for (int i = 0; i < files.length; i++)
			files[i] = base + SUFFIXES[i];
		return files;
	}

	/**
	 * Helper method for adding a substitution variable to an NLS string.
	 *
	 * @param pattern
	 * @param sub
	 * @return
	 */
	public static String bind(String pattern, String sub) {
		return MessageFormat.format(pattern, sub);
	}

	/**
	 * Helper method for adding substitution variables to an NLS string.
	 *
	 * @param pattern
	 * @param sub
	 * @return
	 */
	public static String bind(String pattern, String sub1, String sub2) {
		return MessageFormat.format(pattern, sub1, sub2);
	}

	/**
	 * Look up message properties files based on the given class name, and set any fields in the
	 * class to the property value found. The current Locale is used when looking up the files and
	 * properties files may be sparse. The most locale-specific property is used for each field.
	 *
	 * For example, if the class is named test.Messages and has a public static field named title,
	 * and we're running in Locale en_US, it will look for property files named
	 * test/Messages_en_US.properties, test/Messages_en.properties, and test/Messages.properties. If
	 * there is a property named title, the value will be placed in the field.
	 *
	 * @param c the class to load messages for
	 */
	public static void initMessages(Class<?> c) {
		// find the properties file names to look for
		String[] messageFiles = getMessagesFiles(c.getSimpleName());

		// load the files in reverse order so that more specific locales are loaded last
		Properties p = new Properties();
		for (String file : messageFiles) {
			InputStream in = null;
			try {
				in = c.getResourceAsStream(file);
				if (in != null)
					p.load(in);
			} catch (Exception e) {
				Trace.trace(Trace.WARNING, "Could not load translation for " + file, e);
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}

		// set properties on the calling class
		Field[] fs = c.getFields();
		for (Field f : fs) {
			String val = p.getProperty(f.getName());
			if (val == null)
				Trace.trace(Trace.WARNING, "No translation available for " + c.getCanonicalName() + "." + f.getName());
			else if (!Modifier.isStatic(f.getModifiers()))
				Trace.trace(Trace.WARNING, "Non-static NLS field " + c.getCanonicalName() + "." + f.getName());
			else if (!f.canAccess(null))
				Trace.trace(Trace.WARNING,
						"Field not accessible for translation: " + c.getCanonicalName() + "." + f.getName());
			else {
				// for testing
				if (System.getProperty("ICPC_TOOLS_NLS") != null)
					val = "#" + val + "#";
				try {
					f.set(c, val);
				} catch (Exception e) {
					Trace.trace(Trace.WARNING, "Could not set translation for " + c.getCanonicalName() + "." + f.getName(),
							e);
				}
			}
		}
	}
}