package org.icpc.tools.coachview;

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

		try {
			InputStream in = new ICPCFont().getClass().getClassLoader().getResourceAsStream("font/HELV.PFB");
			MASTER_FONT = Font.createFont(Font.TYPE1_FONT, in);
			in.close();
		} catch (Exception e) {
			// in case we are not in a jar, re-try as a file
			try {
				MASTER_FONT = Font.createFont(Font.TYPE1_FONT, new File("font/HELV.PFB"));
			} catch (Exception ex) {
				Trace.trace(Trace.ERROR, "Error setting master font", ex);
			}
		}

		return MASTER_FONT;
	}
}