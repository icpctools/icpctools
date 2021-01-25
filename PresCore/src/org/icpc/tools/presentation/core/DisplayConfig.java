package org.icpc.tools.presentation.core;

public class DisplayConfig {
	public enum Mode {
		TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MEDIUM, MEDIUM_BL, MIDDLE, ALMOST, FULL_WINDOW, FULL_SCREEN_MAX, FULL_SCREEN
	}

	public int device;
	public Mode mode = Mode.FULL_SCREEN;

	public int id = -1; // an identifier for when there are multiple multi-screens
	public int pp; // position in the grid. 0 is top left, counting to the right
	public int ww; // number of screens wide
	public int hh; // number of screens high

	private char[] PosStrs = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'm', 'l', 'w', 'x' };

	public DisplayConfig() {
		// default full screen on primary device
	}

	public DisplayConfig(String displayStr) {
		if (displayStr == null)
			return;

		device = Integer.parseInt(displayStr.charAt(0) + "") - 1;

		mode = Mode.FULL_SCREEN;
		int n = 1;
		if (displayStr.length() > 1) {
			char c = displayStr.charAt(1);
			if (!Character.isDigit(c)) {
				n++;
				for (int i = 0; i < PosStrs.length; i++)
					if (PosStrs[i] == c)
						mode = Mode.values()[i];
			}
		}

		if (displayStr.length() > 3) {
			id = 0;
			ww = Integer.parseInt(displayStr.charAt(n) + "");
			hh = Integer.parseInt(displayStr.charAt(n + 1) + "");
			pp = Integer.parseInt(displayStr.charAt(n + 2) + "");
			if (displayStr.length() > 5)
				id = displayStr.charAt(n + 3) - 'a';
		} else {
			id = -1;
			pp = 0;
			ww = 1;
			hh = 1;
		}
	}

	public String toDisplayString() {
		char modeChar = '\0';
		if (mode != Mode.FULL_SCREEN)
			modeChar = PosStrs[mode.ordinal()];
		if (id != -1) {
			String idChar = "";
			if (id != 0)
				idChar = String.valueOf((char) (id + 'a'));
			return (device + 1) + "" + modeChar + "" + ww + "" + hh + "" + pp + idChar;
		}

		return (device + 1) + "" + modeChar;
	}

	public static void main(String[] s) {
		System.out.println(new DisplayConfig("1").toDisplayString());
		System.out.println(new DisplayConfig("1a").toDisplayString());
		System.out.println(new DisplayConfig("1220").toDisplayString());
		System.out.println(new DisplayConfig("1b221").toDisplayString());
		System.out.println(new DisplayConfig("1x334b").toDisplayString());
	}

	@Override
	public String toString() {
		return "DisplayConfig " + toDisplayString();
	}
}