package org.icpc.tools.cds.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.contest.model.feed.JSONEncoder;

public class HttpHelper {
	private static final String OK_CHARS = new String("[]{},.~`?!@#$^&*()-_=+:|");

	public static void setThreadHost(HttpServletRequest request) {
		StringBuilder sb = new StringBuilder(request.getServerName());
		if (true) {
			int port = request.getLocalPort();
			if (port > 1000) {
				int th = port / 1000;
				port = th * 1000 + 80;
				sb.append(":" + port);
			}
		}
		JSONEncoder.setThreadHost(sb.toString());
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
				response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
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
						response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
						return;
					}
				}
			}
		} catch (Exception e) {
			// ignore, send anyway
		}

		String name = f.getName();
		if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
			response.setContentType("image/jpeg");
		else if (name.endsWith(".txt") || name.endsWith(".tsv") || name.endsWith(".yaml") || name.endsWith(".xml"))
			response.setContentType("text/plain");
		else if (name.endsWith(".png"))
			response.setContentType("image/png");

		response.setContentLength((int) f.length());
		response.setDateHeader("Last-Modified", lastModified);
		response.setHeader("ETag", "e" + lastModified);
		response.setHeader("Cache-Control", "max-age=1800"); // 30 minutes
		response.setHeader("Content-Disposition", "inline; filename=\"" + name + "\"");

		ServletOutputStream out = response.getOutputStream();
		BufferedInputStream bin = new BufferedInputStream(new FileInputStream(f));
		byte[] b = new byte[1024 * 8];
		int n = bin.read(b);
		while (n != -1) {
			out.write(b, 0, n);
			n = bin.read(b);
		}

		bin.close();
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
}