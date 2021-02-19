package org.icpc.tools.contest.model.feed;

import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Locale;

import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class JSONWriter extends Writer {
	private static final NumberFormat nf = NumberFormat.getInstance(Locale.US);
	private static final NumberFormat df = NumberFormat.getInstance(Locale.US);

	static {
		nf.setGroupingUsed(false);
		nf.setMaximumFractionDigits(0);
		df.setGroupingUsed(false);
	}

	private Writer w;

	public JSONWriter(Writer w) {
		this.w = w;
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

	private void writeValue(Object value) throws IOException {
		if (value == null)
			w.write("null");
		else if (value instanceof String)
			w.write("\"" + escape((String) value) + "\"");
		else if (value instanceof Boolean)
			w.write((Boolean) value ? "true" : "false");
		else if (value instanceof Double)
			w.write(df.format(((Double) value).doubleValue()));
		else if (value instanceof Integer)
			w.write(nf.format(((Integer) value).intValue()));
		else if (value instanceof Long)
			w.write(nf.format(((Long) value).longValue()));
		else if (value instanceof JsonObject)
			writeObject((JsonObject) value);
		else if (value instanceof Object[])
			writeArray((Object[]) value);
	}

	public void writeObject(JsonObject obj) throws IOException {
		w.write("{");
		boolean isFirst = true;
		for (String key : obj.props.keySet()) {
			if (!isFirst)
				w.write(",");
			isFirst = false;

			w.write("\"" + key + "\":");
			writeValue(obj.get(key));
		}
		w.write("}");
	}

	public void writeArray(Object[] obj) throws IOException {
		w.write("[");
		boolean isFirst = true;
		for (Object o : obj) {
			if (!isFirst)
				w.write(",");
			isFirst = false;

			writeValue(o);
		}
		w.write("]");
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		w.write(cbuf, off, len);
	}

	@Override
	public void flush() throws IOException {
		w.flush();
	}

	@Override
	public void close() throws IOException {
		w.close();
	}
}