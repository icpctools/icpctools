package org.icpc.tools.contest.model.feed;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Timestamp {
	// parser for old spec times like: 1265335256.480
	private static final Pattern OLD_TIME_PATTERN = Pattern.compile("([0-9]+)(\\.([0-9]{1,}))?");

	// parser for times like: 2014-06-25T11:22:05.034+01:00
	private static final ThreadLocal<DateFormat> TIME_FORMAT = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		}
	};
	// parser for times like: 2014-06-25T11:22:05+01:00
	private static final ThreadLocal<DateFormat> TIME_FORMAT2 = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		}
	};

	public static long parseOld(String value) {
		Matcher match = OLD_TIME_PATTERN.matcher(value);
		if (!match.matches())
			return 0;

		long time = Long.parseLong(match.group(1)) * 1000;
		String s = match.group(3);
		if (s != null) {
			if (s.length() > 3)
				s = s.substring(0, 3);
			int weight = 100;
			for (int i = 0; i < s.length(); i++) {
				time += Character.digit(s.charAt(i), 10) * weight;
				weight /= 10;
			}
		}
		return time;
	}

	public static Long parse(String timeMs) throws ParseException {
		if (timeMs == null || "null".equals(timeMs))
			return null;

		try {
			return TIME_FORMAT.get().parse(timeMs).getTime();
		} catch (Exception e) {
			// ignore
		}
		return TIME_FORMAT2.get().parse(timeMs).getTime();
	}

	public static String format(Long timeMs) {
		if (timeMs == null)
			return "null";

		return TIME_FORMAT.get().format(new Date(timeMs));
	}

	public static String format(long timeMs) {
		return TIME_FORMAT.get().format(new Date(timeMs));
	}

	public static String now() {
		return format(System.currentTimeMillis());
	}

	/*public static void main(String[] args) {
		// String o = "2014-06-25T11:22:05.034+01";
		String o = "2014-06-25T11:22:05.034Z";
		long num = parse(o);
		System.out.println(o + " -> " + num + " -> ");
		System.out.println(format(num));
		num = parse(format(num));
		System.out.println(format(num));
	}*/
}