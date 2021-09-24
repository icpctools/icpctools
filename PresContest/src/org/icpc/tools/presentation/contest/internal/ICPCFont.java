package org.icpc.tools.presentation.contest.internal;

import java.awt.Font;
import java.io.File;
import java.io.InputStream;

import org.icpc.tools.contest.Trace;

public class ICPCFont {
	private static final int FONT_TYPE = Font.TRUETYPE_FONT;
	private static final String FONT_NAME = "font/Helvetica-Plain.ttf";

	private static Font MASTER_FONT;

	private ICPCFont() {
		// no access, use static methods
	}

	public static Font getMasterFont() {
		if (MASTER_FONT != null)
			return MASTER_FONT;

		String overrideFont = System.getenv("ICPC_FONT");
		if (overrideFont != null) {
			Trace.trace(Trace.INFO, "Font override: " + overrideFont);
			MASTER_FONT = new File(overrideFont).exists() ? getFontFromFile(FONT_TYPE, overrideFont)
					: new Font(overrideFont, Font.PLAIN, 20);
		} else
			MASTER_FONT = getFontFromFile(FONT_TYPE, FONT_NAME);

		return MASTER_FONT;
	}

	public static Font deriveFont(int style, float size) {
		return getMasterFont().deriveFont(style, size);
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
