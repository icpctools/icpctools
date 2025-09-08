package org.icpc.tools.cds.presentations;

import java.io.File;

import org.icpc.tools.cds.CDSConfig;

public class PresentationFilesHelper {
	private static final String[] ALLOWED_FILES = new String[] { "" };
	private static final String[] ALLOWED_PREFIX = new String[] { "promo", "fts", "build", "photo", "historical",
			"presentations-", "coachview-", "presentationAdmin", "balloonUtil", "ccs", "first-solution" };

	/**
	 * Return the given config file, which may not exist. Returns null if the folder or CDS config
	 * are not setup, or the path is not allowed.
	 *
	 * @return
	 */
	public static File getFile(String path) {
		if (path == null || !path.startsWith("/"))
			return null;

		String file = path.substring(1);

		boolean allowed = false;
		for (String s : ALLOWED_FILES) {
			if (s.equals(file))
				allowed = true;
		}
		for (String s : ALLOWED_PREFIX) {
			if (file.startsWith(s))
				allowed = true;
		}

		if (!allowed)
			return null;

		File folder = CDSConfig.getFolder();
		if (folder == null)
			return null;

		return new File(folder, "present" + File.separator + file);
	}
}