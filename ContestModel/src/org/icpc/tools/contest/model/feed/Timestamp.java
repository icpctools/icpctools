package org.icpc.tools.contest.model.feed;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Timestamp {
	// parser for old spec times like: 1265335256.480
	private static final Pattern OLD_TIME_PATTERN = Pattern.compile("([0-9]+)(\\.([0-9]{1,}))?");

	// parser for times like: 2014-06-25T11:22:05.034+01:00, 2014-06-25T11:22:05+01, or
	// 2014-06-25T11:22:05.034Z
	public static final DateTimeFormatter TIME_FORMAT;
	static {
		TIME_FORMAT = DateTimeFormatter.ofPattern("[yyyy-MM-dd]'T'[HH:mm:ss][HH:mm:s][.SSS][.S][z][XXX][X]");
	}

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

	public static Long parse(String timeMs) throws DateTimeParseException {
		if (timeMs == null || "null".equals(timeMs))
			return null;

		return Instant.from(TIME_FORMAT.parse(timeMs)).toEpochMilli();
	}

	public static String format(Long timeMs) {
		if (timeMs == null)
			return "null";

		return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeMs), ZoneId.systemDefault())
				.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	public static String format(long timeMs) {
		return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeMs), ZoneId.systemDefault())
				.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	public static String now() {
		return format(System.currentTimeMillis());
	}

	/*public static void main(String[] args) {
		String[] s = new String[] { "2014-06-25T11:22:05.034Z", "2014-06-25T11:22:05.034-02",
				"2014-06-25T11:22:05.034+09", "2014-06-25T11:22:05.034-09:00", "2014-06-25T11:22:05.034-09:30",
				"2014-06-25T11:22:05-09:00", "2014-06-25T11:22:05-09:30", "2014-06-25T11:22:05.034+01:00",
				"2014-06-25T11:22:05.034+01", "2021-09-02T08:51:37.2-04:00", "2020-02-21T09:57:0.000-0500",
				"2021-06-12T07:54:58.8-04:00" };
		for (String o : s) {
			try {
				long num = parse(o);
				System.out.println(o + " -> " + num);
				System.out.println(format(num));
				long num2 = parse(format(num));
				System.out.println(format(num2));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}*/
}