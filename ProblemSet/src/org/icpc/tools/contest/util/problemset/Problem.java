package org.icpc.tools.contest.util.problemset;

import org.eclipse.swt.graphics.RGB;

public class Problem {
	protected String letter;
	protected String shortName;
	protected String color;
	private String rgb;

	private RGB rgbValue;

	public String getRGB() {
		return rgb;
	}

	public void setRGB(String rgb) {
		this.rgb = rgb;
		rgbValue = null;
	}

	public RGB getRGBVal() {
		if (rgbValue != null)
			return rgbValue;

		if (rgb == null || (rgb.length() != 3 && rgb.length() != 6))
			return null;

		try {
			if (rgb.length() == 3) {
				int r = Integer.parseInt(rgb.substring(0, 1) + rgb.substring(0, 1), 16);
				int g = Integer.parseInt(rgb.substring(1, 2) + rgb.substring(1, 2), 16);
				int b = Integer.parseInt(rgb.substring(2, 3) + rgb.substring(2, 3), 16);
				if (r < 0 || r > 255)
					return null;
				if (g < 0 || g > 255)
					return null;
				if (b < 0 || b > 255)
					return null;
				rgbValue = new RGB(r, g, b);
			} else {
				int r = Integer.parseInt(rgb.substring(0, 2), 16);
				int g = Integer.parseInt(rgb.substring(2, 4), 16);
				int b = Integer.parseInt(rgb.substring(4, 6), 16);
				if (r < 0 || r > 255)
					return null;
				if (g < 0 || g > 255)
					return null;
				if (b < 0 || b > 255)
					return null;
				rgbValue = new RGB(r, g, b);
			}
		} catch (Exception e) {
			return null;
		}
		return rgbValue;
	}
}