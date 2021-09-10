package org.icpc.tools.presentation.contest.internal;

import java.awt.Font;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;

public class ICPCFont {
	private static final int[] FONT_TYPES = new int[] { Font.TRUETYPE_FONT, Font.TRUETYPE_FONT, Font.TRUETYPE_FONT };
	private static final String[] FONT_NAMES = new String[] { "font/Helvetica-Plain.ttf", "font/Roboto-Regular.ttf",
			"font/TwitterColorEmoji-SVGinOT.ttf" };

	private static Font[] FONTS;

	private ICPCFont() {
		// no access, use static methods
	}

	public static Font[] getFonts() {
		if (FONTS != null)
			return FONTS;

		List<Font> list = new ArrayList<>();

		String overrideFonts = System.getenv("ICPC_FONT");
		if (overrideFonts != null) {
			String[] fontNames = overrideFonts.split(",");
			for (int i = 0; i < fontNames.length; i++) {
				Trace.trace(Trace.INFO, "Font override: " + fontNames[i]);
				list.add(new Font(fontNames[i], Font.PLAIN, 20));
			}
		}

		for (int i = 0; i < FONT_NAMES.length; i++) {
			Font f = getFontFromFile(FONT_TYPES[i], FONT_NAMES[i]);
			if (f != null)
				list.add(f);
		}

		list.add(new Font(Font.SANS_SERIF, Font.PLAIN, 20));

		FONTS = list.toArray(new Font[0]);
		return FONTS;
	}

	public static Font getMasterFont() {
		if (FONTS != null)
			return FONTS[0];

		getFonts();

		return FONTS[0];
	}

	public static Font deriveFont(int style, float size) {
		return getMasterFont().deriveFont(style, size);
	}

	public static Font[] deriveFonts(int style, float size) {
		Font[] f = getFonts();
		int len = f.length;
		Font[] fonts = new Font[len];

		for (int i = 0; i < len; i++) {
			fonts[i] = f[i].deriveFont(style, size);
		}
		return fonts;
	}

	private static Font getFontFromFile(int fontType, String fontName) {
		try (InputStream in = ICPCFont.class.getClassLoader().getResourceAsStream(fontName)) {
			return Font.createFont(fontType, in);
		} catch (Exception e) {
			// in case we are not in a jar, re-try as a file
			try {
				return Font.createFont(fontType, new File(fontName));
			} catch (Exception e1) {
				Trace.trace(Trace.ERROR, "Error loading font", e1);
				return null;
			}
		}
	}
}
