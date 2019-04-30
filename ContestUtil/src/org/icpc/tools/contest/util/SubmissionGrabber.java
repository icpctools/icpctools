package org.icpc.tools.contest.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;

public class SubmissionGrabber {

	public static void main(String[] args) {
		SubmissionGrabber sg = new SubmissionGrabber();
		// for (int i = 1; i < 998; i++) {
		int i = 998;
		try {
			sg.read(i);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error " + i, e);
		}
		// }
	}

	private final String url = "https://192.168.1.207/submissionFiles/";
	private final String user = "balloon";
	private final String password = "bac0n";
	private final String dir = "c:" + File.separator + "icpcTest" + File.separator + "submissions" + File.separator;

	private HttpURLConnection createConnection(int run) throws IOException {
		try {
			return HTTPSSecurity.createConnection(new URL(url + run), user, password);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}
	}

	public void read(int run) throws IOException {
		try {
			HttpURLConnection conn = createConnection(run);

			conn.connect();
			InputStream in = conn.getInputStream();

			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dir + run + ".json"));

			BufferedInputStream bin = new BufferedInputStream(in);
			byte[] b = new byte[1024 * 8];
			int n = bin.read(b);
			while (n != -1) {
				out.write(b, 0, n);
				n = bin.read(b);
			}

			bin.close();
			out.close();
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}
	}
}