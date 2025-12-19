package org.icpc.tools.contest.model.internal;

/**
 * Helper class to deal with mime types and associated file extensions.
 */
public class MimeUtil {
	/**
	 * Return the mime-type based on filename.
	 *
	 * @param name
	 * @return
	 */
	public static String getMimeType(String name) {
		int ind = name.lastIndexOf(".");
		if (ind < 0)
			return null;

		String ext = name.substring(ind + 1).toLowerCase();

		if (ext.equals("zip"))
			return "application/zip";
		else if (ext.equals("png"))
			return "image/png";
		else if (ext.equals("jpg") || ext.equals("jpeg"))
			return "image/jpeg";
		else if (ext.equals("svg"))
			return "image/svg+xml";
		else if (ext.equals("ts"))
			return "video/mp2t";
		else if (ext.equals("m2ts"))
			return "video/m2ts";
		else if (ext.equals("ogg"))
			return "video/ogg";
		else if (ext.equals("flv"))
			return "video/x-flv";
		else if (ext.equals("txt") || ext.equals("log") || ext.equals("tsv") || ext.equals("yaml") || ext.equals("xml"))
			return "text/plain";
		else if (ext.equals("pdf"))
			return "application/pdf";
		else if (ext.equals("m4a") || ext.equals("mp4"))
			return "audio/mp4";
		else if (ext.equals("wav"))
			return "audio/wav";
		return null;
	}

	/**
	 * Return filename extensions based on mime-type.
	 *
	 * @param mimeType
	 * @return
	 */
	public static String getExtension(String mimeType) {
		if (mimeType == null)
			return null;

		switch (mimeType) {
			case ("application/zip"):
				return "zip";
			case ("image/png"):
				return "png";
			case ("image/jpeg"):
			case ("image/jpg"):
				return "jpg";
			case ("image/svg+xml"):
				return "svg";
			case ("video/mp2t"):
				return "ts";
			case ("video/m2ts"):
				return "m2ts";
			case ("video/ogg"):
				return "ogg";
			case ("video/x-flv"):
				return "flv";
			case ("text/plain"):
				return "txt";
			case ("application/pdf"):
				return "pdf";
			case ("audio/mp4"):
				return "mp4";
			case ("audio/wav"):
				return "wav";
		}
		return null;
	}
}