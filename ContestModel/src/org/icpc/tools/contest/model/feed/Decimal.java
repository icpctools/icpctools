package org.icpc.tools.contest.model.feed;

import java.text.NumberFormat;
import java.util.Locale;

public class Decimal {
	private static final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);

	static {
		nf.setMaximumFractionDigits(3);
		nf.setGroupingUsed(false);
	}

	public static int parse(String time) {
		if (time == null)
			return 0;

		return (int) Math.round(Double.parseDouble(time) * 1000.0);
	}

	public static String format(int timeMs) {
		return nf.format(timeMs / 1000.0);
	}

	public static void main(String[] args) throws Exception {
		String o = "0.1";
		int num = parse(o);
		System.out.println(o + " -> " + num + " -> " + format(num));
		o = "1.01";
		num = parse(o);
		System.out.println(o + " -> " + num + " -> " + format(num));
		o = "1.019";
		num = parse(o);
		System.out.println(o + " -> " + num + " -> " + format(num));
		o = "1.0";
		num = parse(o);
		System.out.println(o + " -> " + num + " -> " + format(num));
	}
}