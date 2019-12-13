package org.icpc.tools.contest.model.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.ContestSource;

public class ArgumentParser {
	public interface OptionParser {
		/**
		 * Callback to allow parsing and validation of one command line option and its arguments.
		 * Clients will typically call expectOptions() or expectNoOptions() to validate the number
		 * and type of the arguments, then read the values.
		 *
		 * @param option the command line option, e.g. --option
		 * @param args the arguments supplied for the option
		 * @return true if the option was expected, false if the option wasn't (and this class will
		 *         provide an appropriate error
		 * @throws IllegalArgumentException
		 */
		public boolean setOption(String option, List<Object> args) throws IllegalArgumentException;

		/**
		 * Callback to display the command line help for the tool.
		 */
		public void showHelp();
	}

	/**
	 * Confirms that there were no arguments for the given option, and prints a consistent message
	 * if not.
	 *
	 * @param option the command line option
	 * @param args the user-provided arguments for the option
	 * @throws IllegalArgumentException
	 */
	public static void expectNoOptions(String option, List<Object> args) throws IllegalArgumentException {
		if (!args.isEmpty())
			throw new IllegalArgumentException("No allowed options for " + option);
	}

	private static String getOptionsString(String... options) {
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		for (String ss : options) {
			if (!first)
				sb.append(" ");
			else
				first = false;
			sb.append(ss);
		}
		return sb.toString();
	}

	/**
	 * Checks for expected arguments for the given option and does auto type-conversion where
	 * arguments are compatible. Expected arguments is of the form "name:type". Ending with an
	 * argument "*" allows the preceding argument to be repeated any number of times.
	 *
	 * @param option the command line option
	 * @param args the user-provided arguments for the option
	 * @param expectedArgs the expected arguments for the option
	 * @throws IllegalArgumentException
	 */
	public static void expectOptions(String option, List<Object> args, String... expectedArgs)
			throws IllegalArgumentException {
		boolean unlimited = "*".equals(expectedArgs[expectedArgs.length - 1]);
		if (!unlimited) {
			if (args.size() > expectedArgs.length) {
				throw new IllegalArgumentException(
						"Too many arguments for " + option + ", expects " + getOptionsString(expectedArgs));
			} else if (args.size() < expectedArgs.length) {
				throw new IllegalArgumentException(
						"Missing arguments. " + option + ", expects " + getOptionsString(expectedArgs));
			}
		}

		for (int i = 0; i < args.size(); i++) {
			int j = i;
			if (unlimited)
				j = expectedArgs.length - 2;
			int x = expectedArgs[j].indexOf(":");
			String name = expectedArgs[j].substring(0, x);
			String type = expectedArgs[j].substring(x + 1);
			Object o = args.get(i);
			if ("int".contentEquals(type) && !(o instanceof Integer))
				throw new IllegalArgumentException("Integer expected " + o + " for " + option + " " + name);
			else if ("float".contentEquals(type) && !(o instanceof Float)) {
				if (o instanceof Integer)
					args.set(i, new Float((Integer) o)); // auto-convert from int to float
				else
					throw new IllegalArgumentException("Float expected " + o + " for " + option + " " + name);
			} else if ("boolean".contentEquals(type) && !(o instanceof Boolean))
				throw new IllegalArgumentException("Boolean expected " + o + " for " + option + " " + name);
			else if ("string".contentEquals(type) && !(o instanceof String))
				args.set(i, o.toString()); // auto-convert to string
		}
	}

	private static void logOption(String option, List<Object> options) {
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		for (Object o : options) {
			if (!first)
				sb.append(" ");
			else
				first = false;
			sb.append(o);
		}

		Trace.trace(Trace.INFO, "Option found: " + option + " " + sb.toString());
	}

	private static ContestSource[] parseSource(List<Object> args) {
		ContestSource[] source = null;
		try {
			if (args.size() == 3) {
				expectOptions("Contest source", args, "url:string", "user:string", "password:string");
				source = ContestSource.parseMultiSource((String) args.get(0), (String) args.get(1), (String) args.get(2));
			} else if (args.size() == 1) {
				expectOptions("Contest source", args, "url:string");
				source = ContestSource.parseMultiSource((String) args.get(0));
			} else
				Trace.trace(Trace.ERROR, "Invalid contest source");
		} catch (IOException e) {
			Trace.trace(Trace.ERROR, "Invalid contest source: " + e.getMessage());
			System.exit(1);
		}

		return source;
	}

	private static ContestSource[] setOption(OptionParser parser, String option, List<Object> list)
			throws IllegalArgumentException {
		if (option == null) {
			if (!list.isEmpty())
				return parseSource(list);

			throw new IllegalArgumentException("Options without argument");
		}

		logOption(option, list);
		if (!parser.setOption(option, list))
			throw new IllegalArgumentException("Unrecognized option " + option);
		return null;
	}

	/**
	 * Parse the command line options provided by the user, allowing a callback for each option (and
	 * it's argument). Expects command lines in the form: contestSource --optionA arg --optionB arg1
	 * arg 2 --optionC
	 *
	 * Contest source is optional, must be specified before any options, and can either be a folder
	 * or Contest API (url user password). After this there may be multiple options, which may each
	 * have arguments or not. The callback is responsible for validating the arguments for each
	 * option.
	 *
	 * @param args the command line arguments
	 * @param argUser
	 * @return a contest source, or null if none was provided by the user
	 * @throws IllegalArgumentException
	 */
	public static ContestSource parse(String[] args, OptionParser parser) throws IllegalArgumentException {
		ContestSource[] cs = parseMulti(args, parser);
		if (cs == null || cs.length == 0)
			return null;
		return cs[0];
	}

	/**
	 * Parse the command line options provided by the user, allowing a callback for each option (and
	 * it's argument). Expects command lines in the form: contestSource --optionA arg --optionB arg1
	 * arg 2 --optionC
	 *
	 * Contest source is optional, must be specified before any options, and can either be a folder
	 * or Contest API (url user password). After this there may be multiple options, which may each
	 * have arguments or not. The callback is responsible for validating the arguments for each
	 * option.
	 *
	 * Supports providing multiple contests through the initial url|url format.
	 *
	 * @param args the command line arguments
	 * @param argUser
	 * @return a contest source, or null if none was provided by the user
	 * @throws IllegalArgumentException
	 */
	public static ContestSource[] parseMulti(String[] args, OptionParser parser) throws IllegalArgumentException {
		if (args == null || args.length == 0)
			return null;

		List<String> used = new ArrayList<String>();
		ContestSource[] source = null;
		try {
			String option = null;
			List<Object> list = new ArrayList<>(2);
			for (String s : args) {
				if (s == null)
					continue;

				if (s.startsWith("--")) {
					if ("--help".equals(s)) {
						parser.showHelp();
						System.exit(0);
					}
					if (used.contains(s))
						throw new IllegalArgumentException("Duplicate option " + s);
					used.add(s);

					ContestSource[] cs = setOption(parser, option, list);
					if (cs != null)
						source = cs;

					option = s.toLowerCase();
					list = new ArrayList<>(2);
				} else {
					try {
						int i = Integer.parseInt(s);
						list.add(i);
					} catch (Exception e) {
						try {
							float f = Float.parseFloat(s);
							list.add(f);
						} catch (Exception ex) {
							if ("true".equalsIgnoreCase(s))
								list.add(Boolean.TRUE);
							else if ("false".equalsIgnoreCase(s))
								list.add(Boolean.FALSE);
							else
								list.add(s);
						}
					}
				}
			}

			ContestSource[] cs = setOption(parser, option, list);
			if (cs != null)
				source = cs;

			return source;
		} catch (IllegalArgumentException e) {
			Trace.trace(Trace.ERROR, e.getMessage());
			Trace.trace(Trace.USER, "");
			parser.showHelp();
			System.exit(2);
			return null;
		}
	}
}