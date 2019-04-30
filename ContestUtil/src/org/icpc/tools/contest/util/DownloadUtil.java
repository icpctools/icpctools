package org.icpc.tools.contest.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.StringTokenizer;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.RESTContestSource;

public class DownloadUtil {
	protected static String user;
	protected static String password;

	public static void main(String[] args) {
		Trace.init("ICPC Download Util", "downloadUtil", args);

		if (args == null || args.length != 4) {
			Trace.trace(Trace.ERROR, "Usage: [url] [user] [password] [num]");
			return;
		}

		try {
			String url = args[0];
			user = args[1];
			password = args[2];
			int num = Integer.parseInt(args[3]);
			download(url, num);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error launching", e);
		}
	}

	protected static void download(String url, int num) {
		for (int i = 0; i < num; i++) {
			try {
				getResource(url, i + 1);
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error downloading", e);
			}
		}
	}

	protected static void getResource(String url, int num) throws IOException {
		RESTContestSource feed = new RESTContestSource(url + num, user, password);
		feed.outputValidation();
		HttpURLConnection conn = feed.createConnection("");

		Trace.trace(Trace.USER, "Downloading: " + num);
		conn.connect();

		// Content-Disposition: inline; filename="name.png"
		String contentDisp = conn.getHeaderField("Content-Disposition");
		String name = num + "";
		if (contentDisp != null) {
			StringTokenizer st = new StringTokenizer(contentDisp, ";");
			while (st.hasMoreTokens()) {
				String s = st.nextToken().trim();
				if (s.startsWith("filename=")) {
					s = s.substring(9);
					if (s.startsWith("\"") || s.startsWith("'"))
						s = s.substring(1);
					if (s.endsWith("\"") || s.endsWith("'"))
						s = s.substring(0, s.length() - 1);
					name = s;
				}
			}
		}

		File localFile = new File("c:\\icpcTest\\tempPath\\" + name);
		long localTime = -1;
		if (localFile.exists()) {
			localTime = localFile.lastModified();
			// conn.setIfModifiedSince(localTime);
		}

		long remoteTime = conn.getLastModified();
		if (localTime != remoteTime) {
			if (localFile.exists())
				localFile.delete();

			if (!localFile.getParentFile().exists())
				localFile.getParentFile().mkdirs();
			FileOutputStream out = new FileOutputStream(localFile);

			InputStream in = conn.getInputStream();
			byte[] buf = new byte[1024 * 8];
			int n = in.read(buf);
			while (n >= 0) {
				if (n > 0)
					out.write(buf, 0, n);
				n = in.read(buf);
			}

			in.close();
			out.close();
			if (remoteTime > 0)
				localFile.setLastModified(remoteTime);
		}
	}
}