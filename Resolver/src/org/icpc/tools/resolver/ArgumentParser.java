package org.icpc.tools.resolver;

import java.util.ArrayList;
import java.util.List;

public class ArgumentParser {
	private static final String[] OPTIONS = new String[] { "--file", "--server", "--client", "--fast", "--full",
			"--citation", "--medals" };

	interface Argument {
		// tag interface
	}

	enum Option implements Argument {
		FILE, SERVER, CLIENT, FAST, FULL, CITATION, MEDALS
	}

	public class IntegerOption implements Argument {
		public int value;

		public IntegerOption(int i) {
			value = i;
		}
	}

	public class StringOption implements Argument {
		public String value;

		public StringOption(String s) {
			value = s;
		}
	}

	public class FloatOption implements Argument {
		public float value;

		public FloatOption(float f) {
			value = f;
		}
	}

	public void ensureParameters(List<Argument> list, int num) throws IllegalArgumentException {
		if (list.size() < num)
			throw new IllegalArgumentException("Missing parameters");

		for (int i = 0; i < num; i++) {
			if (list.get(i) instanceof Option)
				throw new IllegalArgumentException("Missing parameters");
		}
	}

	public List<Argument> parse(String[] args) throws IllegalArgumentException {
		List<Argument> list = new ArrayList<>();
		if (args == null)
			return new ArrayList<>(0);

		for (String s : args) {
			if (s == null)
				continue;

			if (s.startsWith("--")) {
				boolean found = false;
				for (int j = 0; j < OPTIONS.length; j++) {
					if (OPTIONS[j].equals(s)) {
						found = true;
						list.add(Option.values()[j]);
					}
				}
				if (!found) {
					throw new IllegalArgumentException("Unknown option " + s);
				}
			} else {
				try {
					float f = Float.parseFloat(s);

					try {
						int i = Integer.parseInt(s);
						list.add(new IntegerOption(i));
					} catch (Exception e) {
						list.add(new FloatOption(f));
					}
				} catch (Exception e) {
					list.add(new StringOption(s));
				}
			}
		}

		return list;
	}
}
