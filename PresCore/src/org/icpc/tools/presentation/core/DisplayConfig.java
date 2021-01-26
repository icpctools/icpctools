package org.icpc.tools.presentation.core;

public class DisplayConfig {
	public enum Mode {
		TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MEDIUM, MEDIUM_BL, MIDDLE, ALMOST, FULL_WINDOW, FULL_SCREEN_MAX, FULL_SCREEN
	}

	public int device;
	public Mode mode = Mode.FULL_SCREEN;

	public int id = -1; // an identifier for when there are multiple multi-screens
	public int pos; // position in the grid. 0 is top left, counting to the right
	public int ww; // number of screens wide
	public int hh; // number of screens high

	private char[] PosStrs = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'm', 'l', 'w', 'x' };

	public DisplayConfig(String displayStr) {
		if (displayStr == null)
			return;

		device = Integer.parseInt(displayStr.charAt(0) + "") - 1;

		mode = Mode.FULL_SCREEN;
		if (displayStr.length() > 1) {
			char c = displayStr.charAt(1);
			if (!Character.isDigit(c)) {
				for (int i = 0; i < PosStrs.length; i++)
					if (PosStrs[i] == c)
						mode = Mode.values()[i];
			}
		}
	}

	/**
	 *
	 * display - 1, 1a. multiDisplay - 2@3x2, 1@2x2c
	 */
	public DisplayConfig(String display, String multiDisplay) throws IllegalArgumentException {
		if (display != null) {
			try {
				device = Integer.parseInt(display.charAt(0) + "") - 1;
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid display argument, should be a digit");
			}

			mode = Mode.FULL_SCREEN;
			if (display.length() == 2) {
				char c = display.charAt(1);
				if (!Character.isDigit(c)) {
					for (int i = 0; i < PosStrs.length; i++)
						if (PosStrs[i] == c)
							mode = Mode.values()[i];
				}
			}
		}

		if (multiDisplay == null) {
			id = -1;
			pos = 0;
			ww = 1;
			hh = 1;
		} else {
			try {
				pos = Integer.parseInt(multiDisplay.charAt(0) + "") - 1;
				ww = Integer.parseInt(multiDisplay.charAt(2) + "");
				hh = Integer.parseInt(multiDisplay.charAt(4) + "");
				if (multiDisplay.length() > 5)
					id = multiDisplay.charAt(5) - 'a';
				else
					id = 0;
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid multi-display argument, should be in p@wxh format");
			}
		}
	}

	public String getDisplay() {
		if (mode != Mode.FULL_SCREEN)
			return (device + 1) + "" + PosStrs[mode.ordinal()];
		return (device + 1) + "";
	}

	public String getMultiDisplay() {
		if (id == -1)
			return null;

		String idChar = "";
		if (id != 0)
			idChar = String.valueOf((char) (id + 'a'));
		return (pos + 1) + "@" + ww + "x" + hh + "" + idChar;
	}

	public static void main(String[] s) {
		System.out.println(new DisplayConfig("1"));
		System.out.println(new DisplayConfig("1a"));
		System.out.println(new DisplayConfig("1", "1@2x2"));
		System.out.println(new DisplayConfig("1b", "2@2x2d"));
		System.out.println(new DisplayConfig("1x", "4@3x3b"));
	}

	@Override
	public String toString() {
		return "DisplayConfig " + getDisplay() + " " + getMultiDisplay();
	}
}