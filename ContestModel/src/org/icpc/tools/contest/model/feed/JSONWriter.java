package org.icpc.tools.contest.model.feed;

import java.io.PrintWriter;

public class JSONWriter {
	protected PrintWriter pw;
	protected JSONEncoder je;

	public JSONWriter(PrintWriter pw) {
		this.pw = pw;
		je = new JSONEncoder(pw);
	}

	public String getContentType() {
		return IContentType.JSON;
	}

	public void writePrelude() {
		pw.write("{\n");
	}

	public void writeSeparator() {
		pw.write(",\n");
	}

	public void writePostlude() {
		pw.write("}");
	}

	public static String escape(String s) {
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
}