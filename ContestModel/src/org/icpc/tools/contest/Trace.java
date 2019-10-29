package org.icpc.tools.contest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class Trace {
	public static final byte INFO = 0;
	public static final byte USER = 1;
	public static final byte WARNING = 2;
	public static final byte ERROR = 3;

	private static final int NUM_LOGS_TO_RETAIN = 4;
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm.ss");
	private static final SimpleDateFormat logFormat = new SimpleDateFormat("yy.MM.dd_HH.mm.ss");

	private static File file;
	private static PrintWriter writer;

	private static List<Integer> previousErrors = new ArrayList<>();
	private static boolean hasOutputErrorMsg;
	private static String version;

	private static File getLogFile(String filename) {
		File logFolder = new File("logs");
		if (!logFolder.exists())
			logFolder.mkdir();

		// check for older files and delete
		File[] files = logFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".log");
			}
		});

		if (files.length > NUM_LOGS_TO_RETAIN) {
			Arrays.sort(files, new Comparator<File>() {
				@Override
				public int compare(File f1, File f2) {
					if (f1.lastModified() < f2.lastModified())
						return -1;
					if (f1.lastModified() > f2.lastModified())
						return 1;
					return 0;
				}
			});

			for (int i = 0; i < files.length - NUM_LOGS_TO_RETAIN; i++)
				files[i].delete();
		}

		return new File(logFolder, filename + "_" + logFormat.format(new Date()) + ".log");
	}

	public static byte[] getLogContents() {
		BufferedInputStream in = null;
		ByteArrayOutputStream out = null;
		try {
			in = new BufferedInputStream(new FileInputStream(file));
			out = new ByteArrayOutputStream();

			byte[] buf = new byte[8096];
			int n = in.read(buf);
			while (n >= 0) {
				if (n > 0)
					out.write(buf, 0, n);
				n = in.read(buf);
			}
			return out.toByteArray();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error reading logs", e);
		}
		return new byte[0];
	}

	public static String getLogContents2() {
		BufferedReader in = null;
		StringWriter sw = null;
		try {
			in = new BufferedReader(new FileReader(file));
			sw = new StringWriter();

			String s = in.readLine();
			while (s != null) {
				sw.append(s + "\n");
				s = in.readLine();
			}

			return sw.toString();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error reading logs", e);
		}
		return "";
	}

	private static String getVersion(String ver) {
		if (ver == null)
			return "dev";
		return ver;
	}

	public static String getVersion() {
		if (version == null)
			setVersion((InputStream) null);
		return version;
	}

	private static void setVersion(InputStream in) {
		Class<?> c = getCallerClass();
		Package pack = c.getPackage();
		String spec = pack.getSpecificationVersion();
		String impl = pack.getImplementationVersion();
		if (spec == null && impl == null)
			try {
				java.util.Properties prop = new java.util.Properties();
				prop.load(c.getResourceAsStream("/META-INF/MANIFEST.MF"));
				spec = prop.getProperty("Specification-Version");
				impl = prop.getProperty("Implementation-Version");
			} catch (Exception e) {
				// ignore
			}
		if (in != null && spec == null && impl == null)
			try {
				java.util.Properties prop = new java.util.Properties();
				prop.load(in);
				spec = prop.getProperty("Specification-Version");
				impl = prop.getProperty("Implementation-Version");
			} catch (Exception e) {
				// ignore
				e.printStackTrace();
			}

		version = getVersion(spec) + "." + getVersion(impl);
	}

	private static String getCallerClassName() {
		StackTraceElement[] stes = Thread.currentThread().getStackTrace();
		for (int i = 1; i < stes.length; i++) {
			String className = stes[i].getClassName();
			if (className.indexOf("Trace") < 0) {
				return className;
			}
		}
		return null;
	}

	private static Class<?> getCallerClass() {
		try {
			String name = getCallerClassName();
			return Class.forName(name);
		} catch (Exception e) {
			System.err.println("Missing caller class: " + e.getMessage());
			System.exit(1);
			return null;
		}
	}

	public static void initSysout(String name, InputStream in) {
		setVersion(in);
		System.out.println("--- " + name + " (" + version + ") ---");
	}

	public static void init(String name, String filename, String[] args) {
		String verAndBuild = getVersion();

		if (args != null && args.length == 1) {
			if ("--version".equals(args[0])) {
				System.out.println(name + " " + verAndBuild);
				System.exit(0);
			} else if ("--help".equals(args[0])) {
				try {
					Method m = null;
					try {
						Class<?> c = getCallerClass();
						m = c.getDeclaredMethod("showHelp", new Class<?>[0]);
					} catch (NoSuchMethodException ex) {
						System.out.println("Command doesn't provide help");
						System.exit(0);
					}

					m.invoke(null, new Object[0]);
				} catch (Exception e) {
					System.out.println("Command doesn't provide help: " + e.getMessage());
				}
				System.exit(0);
			}
		}

		System.out.println("--- " + name + " (" + verAndBuild + ") ---");
		if (filename == null)
			return;

		try {
			if (writer != null)
				writer.close();
		} catch (Exception e) {
			System.err.println("Could not close existing trace file");
			e.printStackTrace();
		}
		writer = null;

		try {
			file = getLogFile(filename);
			System.out.println("Log: " + file);
			writer = new PrintWriter(file);
			writer.println("---- " + name + " (" + verAndBuild + ") ----");
			writer.println("OS:  " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + "/"
					+ System.getProperty("os.version") + ")");
			writer.println("JRE: " + System.getProperty("java.vendor") + " (" + System.getProperty("java.version") + ")");
			writer.println("Folder: " + System.getProperty("user.dir"));
			writer.println("Locale/TZ: " + Locale.getDefault().toString() + " / "
					+ Calendar.getInstance().getTimeZone().getDisplayName());
			if (args != null) {
				writer.print("Args: ");
				for (String s : args)
					writer.print(s + " ");
				writer.println();
			}
			writer.println("Log started " + sdf.format(new Date()));
			writer.println();
			writer.flush();
		} catch (Exception e) {
			System.err.println("Could not set trace file");
			e.printStackTrace();
		}
	}

	public static void trace(byte type, String s) {
		trace(type, s, null);
	}

	public static void trace(byte type, String s, Throwable t) {
		if (type == ERROR && t != null) {
			int hash = 0;
			StackTraceElement[] stack = t.getStackTrace();
			if (stack != null && stack.length >= 1)
				hash = stack[0].hashCode();
			else
				hash = s.hashCode();
			if (previousErrors.contains(hash)) {
				if (hasOutputErrorMsg)
					return;
				hasOutputErrorMsg = true;
				System.err.println("Logging second instance of exception, will not log again");
				if (writer != null) {
					try {
						writer.println("Logging second instance of exception, will not log again");
						writer.flush();
					} catch (Exception e) {
						// ignore
					}
				}
			} else
				previousErrors.add(hash);
		}
		if (type != INFO || writer == null) {
			if (type == ERROR)
				System.err.println(s);
			else
				System.out.println(s);

			if (t != null) {
				outputException(t);
			}
		}

		StringBuffer sb = new StringBuffer();
		sb.append(sdf.format(new Date()));
		sb.append(" ");

		if (type == INFO)
			sb.append("I ");
		else if (type == WARNING)
			sb.append("W ");
		else if (type == ERROR)
			sb.append("E ");
		sb.append(s);

		if (writer != null) {
			try {
				Iterator<String> iterator = iterate(sb.toString());
				while (iterator.hasNext()) {
					writer.println(iterator.next());
				}
				if (t != null)
					t.printStackTrace(writer);
				writer.flush();
			} catch (Exception e) {
				System.err.println("Could not write to trace file");
				e.printStackTrace();
			}
		}
	}

	private static void outputException(Throwable t) {
		System.err.println("   " + t.getClass().getName() + ": " + t.getMessage());
		StackTraceElement[] stack = t.getStackTrace();
		for (StackTraceElement ste : stack) {
			if (ste.getClassName().startsWith("org.icpc.tools")) {
				System.err.println("   at " + ste.getClassName() + "." + ste.getMethodName() + "(" + ste.getFileName() + ":"
						+ ste.getLineNumber() + ")");
				break;
			}
		}
		Throwable cause = t.getCause();
		if (cause != null)
			outputException(cause);
	}

	private static Iterator<String> iterate(String s) {
		if (s == null)
			return null;
		List<String> list = new ArrayList<>();
		int index = s.indexOf("\n");
		String t = s;
		while (index >= 0) {
			list.add(t.substring(0, index));
			t = t.substring(index + 1);
			index = t.indexOf("\n");
		}
		list.add(t);
		return list.iterator();
	}
}