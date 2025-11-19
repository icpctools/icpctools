package org.icpc.tools.contest.model.feed;

import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Locale;

import org.icpc.tools.contest.model.internal.NetworkUtil;

public class JSONEncoder {
	private static final NumberFormat nf = NumberFormat.getInstance(Locale.US);
	private static final NumberFormat df = NumberFormat.getInstance(Locale.US);

	static {
		nf.setGroupingUsed(false);
		nf.setMaximumFractionDigits(0);
		df.setGroupingUsed(false);
	}

	private static final ThreadLocal<String> local = new ThreadLocal<>();
	private static final ThreadLocal<String> local2 = new ThreadLocal<>();
	private static String DEFAULT_HOST = null;

	private boolean first = true;
	private PrintWriter pw;

	public JSONEncoder(PrintWriter pw) {
		this.pw = pw;
	}

	private static String escape(String s) {
		if (s == null || s.isEmpty())
			return "";

		int len = s.length();
		StringBuilder sb = new StringBuilder(len + 10);

		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			switch (c) {
				case '\\':
				case '"':
					sb.append('\\');
					sb.append(c);
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\t':
					sb.append("\\t");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\r':
					sb.append("\\r");
					break;
				default:
					if (c < 0x0020 || c > 0x007e) {
						String t = "000" + Integer.toHexString(c);
						sb.append("\\u" + t.substring(t.length() - 4));
					} else
						sb.append(c);
			}
		}
		return sb.toString();
	}

	public void reset() {
		first = true;
	}

	public void writeSeparator() {
		if (!first) {
			pw.write(",");
			first = true;
		}

		pw.write("\n");
	}

	public void open() {
		if (!first) {
			pw.write(",");
			first = true;
		}
		pw.write("{");
	}

	public void openChild(String name) {
		if (!first) {
			pw.write(",");
			first = true;
		}
		pw.write("\"" + name + "\":{");
	}

	public void openArray() {
		if (!first) {
			pw.write(",");
			first = true;
		}
		pw.write("[");
	}

	public void openChildArray(String name) {
		if (!first) {
			pw.write(",");
			first = true;
		}
		pw.write("\"" + name + "\":[");
	}

	public void encode(String name) {
		if (!first)
			pw.write(",");
		else
			first = false;
		pw.write("\"" + name + "\":null");
	}

	public void encode(String name, Integer value) {
		if (!first)
			pw.write(",");
		else
			first = false;
		pw.write("\"" + name + "\":" + nf.format(value.intValue()));
	}

	public void encode(String name, int value) {
		if (!first)
			pw.write(",");
		else
			first = false;
		pw.write("\"" + name + "\":" + nf.format(value));
	}

	public void encode(String name, long value) {
		if (!first)
			pw.write(",");
		else
			first = false;
		pw.write("\"" + name + "\":" + nf.format(value));
	}

	public void encode(String name, Double value) {
		if (!first)
			pw.write(",");
		else
			first = false;
		pw.write("\"" + name + "\":" + df.format(value.doubleValue()));
	}

	public void encode(String name, double value) {
		if (!first)
			pw.write(",");
		else
			first = false;
		pw.write("\"" + name + "\":" + df.format(value));
	}

	public void encode(String name, Boolean value) {
		if (!first)
			pw.write(",");
		else
			first = false;
		pw.write("\"" + name + "\":" + value.booleanValue());
	}

	public void encode(String name, boolean value) {
		if (!first)
			pw.write(",");
		else
			first = false;
		pw.write("\"" + name + "\":" + value);
	}

	public void encode(String name, String value) {
		if (!first)
			pw.write(",");
		else
			first = false;
		if (value == null)
			pw.write("\"" + name + "\":null");
		else
			pw.write("\"" + name + "\":\"" + escape(value) + "\"");
	}

	public void encodeString(String name, String value) {
		if (!first)
			pw.write(",");
		else
			first = false;
		pw.write("\"" + name + "\":\"" + value + "\"");
	}

	public void encodePrimitive(String name, String value) {
		if (!first)
			pw.write(",");
		else
			first = false;
		pw.write("\"" + name + "\":" + value);
	}

	public static void setThreadHost(String host) {
		local.set(host);
	}

	public static void setAccountToken(String token) {
		local2.set(token);
	}

	public String replace(String s) {
		String host = local.get();
		if (host == null) {
			if (DEFAULT_HOST == null) {
				try {
					DEFAULT_HOST = NetworkUtil.getLocalAddress();
				} catch (Exception e) {
					// ignore exception
					DEFAULT_HOST = "https://cds";
				}
			}
			host = DEFAULT_HOST;
		}
		String t = s.replace("<host>", host);

		int ind = t.indexOf("\"href\":\"");
		if (ind > 0) {
			int ind2 = t.indexOf("\"", ind + 10);
			if (ind2 > 0 && local2.get() != null) {
				t = t.substring(0, ind2) + "?token=" + local2.get() + t.substring(ind2);
			}
		}

		return t;
	}

	public void encodeValue(int value) {
		if (!first)
			pw.write(",");
		else
			first = false;
		pw.write(value + "");
	}

	public void encodeValue(String value) {
		if (!first)
			pw.write(",");
		else
			first = false;
		pw.write("\"" + escape(value) + "\"");
	}

	public void encodeNull() {
		if (!first)
			pw.write(",");
		else
			first = false;
		pw.write("null");
	}

	public void close() {
		pw.write("}");
		first = false;
	}

	public void closeArray() {
		pw.write("]");
		first = false;
	}
}