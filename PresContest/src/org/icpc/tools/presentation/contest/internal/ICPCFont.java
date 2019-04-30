package org.icpc.tools.presentation.contest.internal;

import java.awt.Font;
import java.io.File;
import java.io.InputStream;

import org.icpc.tools.contest.Trace;

public class ICPCFont {
	private static Font MASTER_FONT;

	private ICPCFont() {
		// no access, use static methods
	}

	public static Font getMasterFont() {
		if (MASTER_FONT != null)
			return MASTER_FONT;

		String fontName = System.getenv("ICPC_FONT");
		if (fontName != null) {
			MASTER_FONT = new Font(fontName, 20, Font.PLAIN);
			if (MASTER_FONT != null) {
				Trace.trace(Trace.USER, "Font override: " + fontName);
				return MASTER_FONT;
			}
		}
		try {
			InputStream in = new ICPCFont().getClass().getClassLoader().getResourceAsStream("font/HELV.PFB");
			MASTER_FONT = Font.createFont(Font.TYPE1_FONT, in);
			in.close();
		} catch (Exception e) {
			// in case we are not in a jar, re-try as a file
			try {
				MASTER_FONT = Font.createFont(Font.TYPE1_FONT, new File("font/HELV.PFB"));
			} catch (Exception e1) {
				Trace.trace(Trace.ERROR, "Error loading font", e1);
			}
		}

		return MASTER_FONT;
	}
}