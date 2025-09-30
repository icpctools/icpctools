package org.icpc.tools.cds.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;

import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.internal.MimeUtil;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HttpHelper {
	private static final String OK_CHARS = new String("[]{},.~`?!@#$^&*()-_=+:|");

	public static void setThreadHost(HttpServletRequest request) {
		JSONEncoder.setThreadHost("https://" + request.getServerName() + ":" + request.getServerPort());
	}

	public static void sendFile(HttpServletRequest request, HttpServletResponse response, File f) throws IOException {
		if (f == null || !f.exists()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// last-modified
		long lastModified = f.lastModified() / 1000 * 1000;
		try {
			long ifModifiedSince = request.getDateHeader("If-Modified-Since");
			if (ifModifiedSince != -1 && ifModifiedSince >= lastModified) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			}
		} catch (Exception e) {
			// ignore, send anyway
		}

		// etags
		try {
			Enumeration<String> ifNoneMatch = request.getHeaders("If-None-Match");
			if (ifNoneMatch != null) {
				String current = "e" + lastModified;
				while (ifNoneMatch.hasMoreElements()) {
					String val = ifNoneMatch.nextElement();
					if (current.equals(val)) {
						response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						return;
					}
				}
			}
		} catch (Exception e) {
			// ignore, send anyway
		}

		setCommonHeaders(f.getName(), response);

		response.setContentLength((int) f.length());
		response.setDateHeader("Last-Modified", lastModified);
		response.setHeader("ETag", "e" + lastModified);

		ServletOutputStream out = response.getOutputStream();
		BufferedInputStream bin = new BufferedInputStream(new FileInputStream(f));
		byte[] b = new byte[1024 * 8];
		int n = bin.read(b);
		while (n != -1) {
			out.write(b, 0, n);
			n = bin.read(b);
		}

		bin.close();
		out.flush();
	}

	public static void streamFile(HttpServletRequest request, HttpServletResponse response, File f) throws IOException {
		if (f == null || !f.exists()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		File tempFile = new File(f.getParentFile(), f.getName() + "-temp");
		if (!tempFile.exists()) {
			// not streaming anymore, we can just send the file
			sendFile(request, response, f);
			return;
		}

		setCommonHeaders(f.getName(), response);

		long start = System.currentTimeMillis();
		boolean streaming = true;

		ServletOutputStream out = response.getOutputStream();
		BufferedInputStream bin = new BufferedInputStream(new FileInputStream(f));
		byte[] b = new byte[1024 * 8];
		while (streaming) {
			if (!tempFile.exists()) {
				streaming = false;
				try {
					Thread.sleep(400);
				} catch (Exception e) {
					// ignore
				}
			}

			int n = bin.read(b);
			while (n != -1) {
				out.write(b, 0, n);
				n = bin.read(b);
			}

			// give up after 240s
			if (start + 240000L < System.currentTimeMillis())
				streaming = false;
		}

		bin.close();
		out.flush();
	}

	private static void setCommonHeaders(String name, HttpServletResponse response) {
		response.setContentType(MimeUtil.getMimeType(name));

		response.setHeader("Cache-Control", "max-age=1800"); // 30 minutes
		response.setHeader("Content-Disposition", "inline; filename=\"" + name + "\"");
	}

	/**
	 * Sanitize a string to avoid cross-site scripting attacks.
	 *
	 * @param s
	 * @return a sanitized version where all non-valid characters are changed to '-'.
	 */
	public static String sanitize(String s) {
		StringBuilder sb = new StringBuilder();
		for (char c : s.toCharArray()) {
			if (Character.isAlphabetic(c) || Character.isDigit(c) || Character.isWhitespace(c) || OK_CHARS.indexOf(c) >= 0)
				sb.append(c);
			else
				sb.append("-");
		}
		return sb.toString();
	}

	/**
	 * Strip any non-valid characters from a string to avoid cross-site scripting attacks.
	 *
	 * @param s
	 * @return a sanitized version where all non-valid characters are removed.
	 */
	public static String strip(String s) {
		StringBuilder sb = new StringBuilder();
		for (char c : s.toCharArray()) {
			if (Character.isAlphabetic(c) || Character.isDigit(c) || Character.isWhitespace(c) || OK_CHARS.indexOf(c) >= 0)
				sb.append(c);
		}
		return sb.toString();
	}

	public static String sanitizeHTML(String s) {
		if (s == null || s.isEmpty())
			return "";

		int len = s.length();
		StringBuilder sb = new StringBuilder(len + 10);

		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			switch (c) {
				case '<':
					sb.append("&lt;");
					break;
				case '>':
					sb.append("&gt;");
					break;
				case '&':
					sb.append("&amp;");
					break;
				case '\'':
					sb.append("&apos;");
					break;
				case '"':
					sb.append("&quot;");
					break;
				default:
					if (c < 0x0020 || c > 0x007e) {
						String t = Integer.toString(c);
						sb.append("&#" + t + ";");
					} else
						sb.append(c);
			}
		}
		return sb.toString();
	}
}
