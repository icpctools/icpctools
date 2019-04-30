package org.icpc.tools.contest.model.feed;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RelativeTime {
	// parser for old spec contest times like: 118.480
	private static final Pattern OLD_TIME_PATTERN = Pattern.compile("([0-9]+)(\\.([0-9]{1,}))?");

	// parser for contest times like: 1:22:05.034
	private static final Pattern TIME_PATTERN = Pattern.compile("-?([0-9]+):([0-9]{2}):([0-9]{2})(\\.[0-9]{3})?");

	public static int parseOld(String value) {
		Matcher match = OLD_TIME_PATTERN.matcher(value);
		if (!match.matches())
			return 0;

		int contestTime = Integer.parseInt(match.group(1)) * 1000;
		String s = match.group(3);
		if (s != null) {
			if (s.length() > 3)
				s = s.substring(0, 3);
			int weight = 100;
			for (int i = 0; i < s.length(); i++) {
				contestTime += Character.digit(s.charAt(i), 10) * weight;
				weight /= 10;
			}
		}
		return contestTime;
	}

	public static int parse(String contestTime) throws ParseException {
		Matcher match = TIME_PATTERN.matcher(contestTime);
		if (!match.matches())
			throw new ParseException("Invalid contest time string: " + contestTime, 0);

		int h = Integer.parseInt(match.group(1));
		int m = Integer.parseInt(match.group(2));
		int s = Integer.parseInt(match.group(3));
		if (m > 59 || s > 59)
			throw new ParseException("Invalid contest time string: " + contestTime, 0);
		int ms = 0;
		if (match.group(4) != null) {
			ms = Integer.parseInt(match.group(4).substring(1));
		}
		if (contestTime.startsWith("-"))
			return -(h * 60 * 60 * 1000 + m * 60 * 1000 + s * 1000 + ms);

		return h * 60 * 60 * 1000 + m * 60 * 1000 + s * 1000 + ms;
	}

	public static String format(Integer contestTimeMs) {
		if (contestTimeMs == null)
			return "null";
		return format(contestTimeMs.intValue());
	}

	public static String format(int contestTimeMs) {
		int ms = contestTimeMs;
		StringBuilder sb = new StringBuilder();
		if (contestTimeMs < 0) {
			ms = -contestTimeMs;
			sb.append("-");
		}

		int s = ms / 1000;
		if (s > 0)
			ms -= s * 1000;
		int m = s / 60;
		if (m > 0)
			s -= m * 60;
		int h = m / 60;
		if (h > 0)
			m -= h * 60;

		sb.append(h + ":");
		if (m < 10)
			sb.append("0");
		sb.append(m + ":");
		if (s < 10)
			sb.append("0");
		sb.append(s);
		sb.append(".");
		if (ms < 10)
			sb.append("00");
		else if (ms < 100)
			sb.append("0");
		sb.append(ms);
		return sb.toString();
	}

	public static void main(String[] args) throws Exception {
		String o = "1:22:05.034";
		// String o = "2:09:05.134";
		// String o = "2:09:05";
		int num = parse(o);
		System.out.println(o + " -> " + num + " -> " + format(num));
	}
}