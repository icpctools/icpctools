package org.icpc.tools.contest.model.feed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.internal.SimpleMap;

public class JSONParser {
	protected static final char[] TOKENS = new char[] { '"', ':', ',', '[', ']', '{', '}' };
	protected static final int DEPTH_LIMIT = 20;

	enum Token {
		QUOTE, COLON, COMMA, ARRAY_START, ARRAY_END, OBJECT_START, OBJECT_END, OTHER_CHAR
	}

	private String s;
	private int ind;

	public static class JsonObject {
		public Map<String, Object> props = new SimpleMap();

		public boolean containsKey(String key) {
			return props.containsKey(key);
		}

		public boolean isNull(String key) {
			Object o = props.get(key);
			if (o == null)
				return true;

			String s = (String) o;
			return "null".equals(s);
		}

		public Object get(String key) {
			return props.get(key);
		}

		public String getString(String key) {
			String value = (String) props.get(key);
			return unescape(value);
		}

		public Object[] getArray(String key) {
			return (Object[]) props.get(key);
		}

		public JsonObject getJsonObject(String key) {
			return (JsonObject) props.get(key);
		}

		public boolean getBoolean(String key) {
			try {
				return (Boolean) props.get(key);
			} catch (Exception e) {
				return false;
			}
		}

		public int getInt(String key) {
			try {
				String value = (String) props.get(key);
				return Integer.parseInt(value);
			} catch (Exception e) {
				return -1;
			}
		}

		public long getLong(String key) {
			try {
				String value = (String) props.get(key);
				return Long.parseLong(value);
			} catch (Exception e) {
				return -1;
			}
		}

		public double getDouble(String key) {
			try {
				String value = (String) props.get(key);
				return Double.parseDouble(value);
			} catch (Exception e) {
				return -1;
			}
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("JSON [");
			boolean first = true;
			for (String s : props.keySet()) {
				if (!first)
					sb.append(",");
				sb.append(s + "=" + props.get(s));
				first = false;
			}
			sb.append("]");
			return sb.toString();
		}
	}

	public JSONParser(String s) {
		if (s == null)
			throw new IllegalArgumentException("Invalid JSON: null");
		this.s = s;
	}

	public JSONParser(File f) throws IOException {
		InputStream in = new FileInputStream(f);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		StringBuilder sb = new StringBuilder();
		String st = br.readLine();
		while (st != null) {
			sb.append(st);
			st = br.readLine();
		}
		s = sb.toString();
		in.close();
	}

	public JSONParser(InputStream in) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String st = br.readLine();
		while (st != null) {
			sb.append(st);
			st = br.readLine();
		}
		s = sb.toString();
	}

	private Token nextToken() {
		char c = s.charAt(ind++);
		while (true) {
			for (int i = 0; i < TOKENS.length; i++) {
				if (c == TOKENS[i])
					return Token.values()[i];
			}
			if (ind >= s.length())
				throw new IllegalArgumentException("Unexpected end");

			if (c != ' ' && c != '\r' && c != '\n')
				return Token.OTHER_CHAR;

			c = s.charAt(ind++);
		}
	}

	private String readValue() {
		int st = ind;
		char c = s.charAt(st);
		while (c != '"' || s.charAt(ind - 1) == '\\') {
			ind++;
			if (ind >= s.length())
				throw new IllegalArgumentException("Unexpected value");
			c = s.charAt(ind);
		}
		return unescape(s.substring(st, ind++));
	}

	private Object readUntilNextToken() {
		int st = ind - 1;
		char c = s.charAt(st);
		while (true) {
			for (int i = 0; i < TOKENS.length; i++) {
				if (c == TOKENS[i]) {
					int in = ind;
					ind = st;
					String ss = s.substring(in - 1, st).trim();
					if ("true".equals(ss))
						return Boolean.TRUE;
					else if ("false".equals(ss))
						return Boolean.FALSE;
					else if ("null".equals(ss))
						return null;

					return ss;
				}
			}
			// TODO: fail if not whitespace
			if (st >= s.length())
				throw new IllegalArgumentException("Unexpected char");
			c = s.charAt(++st);
		}
	}

	private boolean readAttr(JsonObject obj) {
		Token t = nextToken();
		if (t != Token.QUOTE)
			throw new IllegalArgumentException("Unexpected " + t);

		String key = readValue();

		t = nextToken();
		if (t != Token.COLON)
			throw new IllegalArgumentException("Unexpected " + t);

		t = nextToken();

		Object value = null;
		if (t == Token.QUOTE) {
			// quoted value
			value = readValue();
		} else if (t == Token.OBJECT_START) {
			ind--;
			value = readObject();
		} else if (t == Token.ARRAY_START) {
			ind--;
			value = readArray();
		} else { // simple value
			value = readUntilNextToken();
		}
		if (value != null)
			obj.props.put(key, value);
		return false;
	}

	public Object[] readArray() {
		List<Object> list = new ArrayList<>();

		Token t = nextToken();
		if (t != Token.ARRAY_START)
			throw new IllegalArgumentException("Expected array start");

		t = nextToken();

		while (t != Token.ARRAY_END) {
			Object value = null;
			if (t == Token.QUOTE) {
				// quoted value
				value = readValue();
			} else if (t == Token.OBJECT_START) {
				ind--;
				value = readObject();
			} else if (t == Token.ARRAY_START) {
				ind--;
				value = readArray();
			} else { // simple value
				value = readUntilNextToken();
			}
			list.add(value);
			t = nextToken();
			if (t == Token.COMMA)
				t = nextToken();
		}

		return list.toArray(new Object[list.size()]);
	}

	public JsonObject readObject() {
		JsonObject obj = new JsonObject();

		int lastInd = ind;
		Token t = nextToken();
		if (t != Token.OBJECT_START) {
			ind = lastInd;
			throw new IllegalArgumentException("Expected object start");
		}

		while (t != Token.OBJECT_END) {
			readAttr(obj);
			t = nextToken();
			if (t != Token.COMMA && t != Token.OBJECT_END)
				throw new IllegalArgumentException("Unexpected " + t);
		}

		return obj;
	}

	public static JsonObject getOrReadObject(Object o) {
		if (o instanceof String) {
			JSONParser rdr = new JSONParser((String) o);
			return rdr.readObject();
		}
		return (JsonObject) o;
	}

	public static Object[] getOrReadArray(Object o) {
		if (o instanceof String) {
			JSONParser rdr = new JSONParser((String) o);
			return rdr.readArray();
		}
		return (Object[]) o;
	}

	public static final void main(String[] s) {
		String ss = null;
		/*ss = "{ \"x\": \"6\", \"clients\": [42, \"test\", [5], { \"test\": \"value\" }, 47, 139]}";
		// ss = "{\"type\":\"stop\",\"clients\":[1436190600]}";
		System.out.println(ss);
		JSONParser rr = new JSONParser(ss);
		JsonObject jo = rr.readObject();
		Object[] obj = jo.getArray("clients");
		System.out.println(obj.length);
		for (Object o : obj)
			System.out.println("  " + o + " - " + o.getClass());*/

		try {
			System.gc();
			Thread.sleep(1500);
		} catch (Exception e) {
			// ignore
		}

		long time = System.currentTimeMillis();
		for (int i = 0; i < 8; i++) {
			try {
				InputStream in = new FileInputStream(new File("/Users/deboer/ICPC/2018/cdp/event-feed.json"));
				BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
				ss = br.readLine();
				while (ss != null) {
					JSONParser r = new JSONParser(ss);
					r.readObject();
					ss = br.readLine();
				}
			} catch (Exception e) {
				System.err.println(ss);
				e.printStackTrace();
			}
		}
		long time2 = System.currentTimeMillis();
		System.out.println();
		System.out.println("  Time: " + (time2 - time) + "ms");
	}

	/*
	 * Converts encoded &#92;uxxxx to unicode chars
	 * and changes special saved chars to their original forms
	 */
	protected static String unescape(String val) {
		if (val == null || !val.contains("\\"))
			return val;

		char[] in = val.toCharArray();
		StringBuilder sb = new StringBuilder(in.length);

		int i = 0;
		while (i < in.length) {
			char c = in[i++];
			if (c == '\\') {
				c = in[i++];
				if (c == 'u') {
					// read the xxxx
					String s = new String(in, i, 4);
					sb.append((char) Integer.parseUnsignedInt(s, 16));
					i += 4;
				} else {
					if (c == 't')
						c = '\t';
					else if (c == 'r')
						c = '\r';
					else if (c == 'b')
						c = '\b';
					else if (c == 'n')
						c = '\n';
					else if (c == 'f')
						c = '\f';
					sb.append(c);
				}
			} else
				sb.append(c);
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "Parsing: " + ind + "/" + s.charAt(ind) + " in " + s;
	}
}