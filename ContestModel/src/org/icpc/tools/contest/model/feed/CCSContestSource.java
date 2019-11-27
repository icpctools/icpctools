package org.icpc.tools.contest.model.feed;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.StringTokenizer;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.internal.FileReference;

public class CCSContestSource extends DiskContestSource {
	private String user;
	private String password;
	private String host;
	private int port;
	private String startTime;
	private String submissionFiles;
	private XMLFeedParser parser;

	/**
	 * Create a CCS contest source with a temporary cache.
	 *
	 * @param host
	 * @param port
	 */
	public CCSContestSource(String host, int port) {
		super(host + port);
		this.host = host;
		this.port = port;

		instance = this;
	}

	/**
	 * Create a CCS contest source with a local disk cache.
	 *
	 * @param host
	 * @param port
	 * @param folder
	 */
	public CCSContestSource(String host, int port, File folder) {
		super(folder);
		this.host = host;
		this.port = port;

		instance = this;
	}

	/**
	 * Create a CCS contest source with a disk cache.
	 */
	public CCSContestSource(String eventFeed, String user, String password, String submissionFiles, String startTime,
			File folder) {
		super(folder);
		int index = eventFeed.indexOf(":");
		try {
			host = eventFeed.substring(0, index);
			port = Integer.parseInt(eventFeed.substring(index + 1, eventFeed.length()));
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not parse event feed");
		}

		this.user = user;
		this.password = password;
		this.startTime = startTime;
		this.submissionFiles = submissionFiles;

		instance = this;
	}

	@Override
	public File getFile(String path) throws IOException {
		File f = super.getFile(path);
		if (f.exists())
			return f;

		// for submissions, try to cache submissions
		if (path == null || !(path.startsWith("/submissions/") && path.endsWith("/files")) || submissionFiles == null)
			return null;

		String submissionId = path.substring(13, path.length() - 13 - 6);
		String url = submissionFiles.replace("{0}", submissionId);

		try {
			HttpURLConnection conn = HTTPSSecurity.createConnection(new URL(url), user, password);
			conn.connect();

			if (conn.getResponseCode() != 200)
				throw new IOException(conn.getResponseCode() + ":" + conn.getResponseMessage());

			InputStream in = null;
			try {
				in = conn.getInputStream();
				downloadAndCache(in, f);
				return f;
			} finally {
				in.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException("500: Could not connect: " + e.getMessage());
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}
	}

	@Override
	public File getFile(IContestObject obj, FileReference ref, String property) throws IOException {
		if (super.isCache())
			return null;

		// TODO - use submission file interface
		File localFile = super.getFile(obj, ref, property);
		if (localFile.exists())
			return localFile;
		return null;
	}

	private InputStream connectToSocket() throws IOException {
		Socket s = null;
		try {
			s = new Socket();
			s.connect(new InetSocketAddress(host, port), 5000);
			return s.getInputStream();
		} catch (Exception e) {
			throw new IOException("Connection error (" + host + ":" + port + ")", e);
		}
	}

	@Override
	protected void loadContestImpl() throws Exception {
		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(connectToSocket());
			if (super.isCache())
				notifyListeners(ConnectionState.CONNECTED);
			parser = new XMLFeedParser();
			parser.parse(contest, in);
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (Exception ex) {
				// ignore
			}
		}
	}

	@Override
	public void close() throws Exception {
		super.close();
		if (parser != null)
			parser.close();
	}

	/**
	 * Returns an input stream to the submission file content for the given submission id.
	 *
	 * @param submissionId a valid submission id
	 * @return an input stream to the submission file content
	 * @throws IOException
	 */
	public InputStream getSubmissionFile(String submissionId) throws IOException {
		if (submissionFiles == null)
			throw new IOException("Submission file service not configured");

		try {
			String url2 = submissionFiles.replace("{0}", submissionId);
			HttpURLConnection conn = HTTPSSecurity.createConnection(new URL(url2), user, password);
			conn.connect();

			if (conn.getResponseCode() != 200)
				throw new IOException(conn.getResponseCode() + ": " + conn.getResponseMessage());
			return conn.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException("500: Could not connect: " + e.getMessage());
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}
	}

	/**
	 * Get the start time of a contest. Time is in s since Jan 1, 1970 (the Unix epoch), or null to
	 * clear.
	 *
	 * @param time
	 * @throws IOException
	 */
	public Double getStartTime() throws IOException {
		if (startTime == null)
			throw new IOException("Start time service not configured");

		try {
			HttpURLConnection conn = HTTPSSecurity.createConnection(new URL(startTime), user, password);
			conn.setRequestMethod("GET");

			if (conn.getResponseCode() != 200)
				throw new IOException(conn.getResponseCode() + ": " + conn.getResponseMessage());

			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String s = br.readLine();
			while (s != null) {
				sb.append(s);
				s = br.readLine();
			}
			String val = null;
			StringTokenizer st = new StringTokenizer(sb.toString(), " {:,\n\r", false);
			s = st.nextToken();
			if (!("starttime".equals(s) || "\"starttime\"".equals(s)))
				throw new IOException("Parsing error: no starttime: " + s);

			val = st.nextToken();
			if (val.startsWith("\"") && val.endsWith("\""))
				val = val.substring(1, val.length() - 2);

			if ("undefined".equals(val))
				return null;
			return Double.parseDouble(val);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException("500: Could not connect: " + e.getMessage());
			// throw e;
		} catch (Exception e) {
			throw new IOException("Error getting start time", e);
		}
	}

	/**
	 * Set or clear the start time of a contest. Time is in ms since Jan 1, 1970 (the Unix epoch),
	 * or null to clear.
	 *
	 * @param time
	 * @throws IOException
	 */
	@Override
	public void setStartTime(Long time) throws IOException {
		if (startTime == null)
			throw new IOException("Start time service not configured");

		try {
			HttpURLConnection conn = HTTPSSecurity.createConnection(new URL(startTime), user, password);
			conn.setRequestMethod("PUT");
			conn.setDoOutput(true);

			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			bw.write("{ \"starttime\":");

			if (time == null || time <= 0)
				bw.write("\"undefined\" }\n");
			else
				bw.write((int) Math.floor(time / 1000.0) + " }\n");
			bw.close();
			if (conn.getResponseCode() != 200)
				throw new IOException(conn.getResponseCode() + ": " + conn.getResponseMessage());
			Trace.trace(Trace.INFO, "Start time successfully set on CCS");
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException("500: Could not connect: " + e.getMessage());
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}
	}

	@Override
	public Validation validate() {
		Validation v = new Validation();
		try {
			Socket s = new Socket();
			s.connect(new InetSocketAddress(host, port), 5000);
			InputStream in = s.getInputStream();
			in.read();
			in.close();
			v.ok("Connected successfully");
		} catch (Exception e) {
			v.err("Connection failure: " + e.getMessage());
		}

		if (submissionFiles != null) {
			try {
				getFile("/submissions/0/files");
				v.ok("Submission files: OK");
			} catch (Exception e) {
				v.err("Submission files: FAIL - " + e.getMessage());
			}
		} else
			v.ok("No submission file API configured");

		if (startTime != null) {
			try {
				getStartTime();
				v.ok("Start time: OK");
			} catch (Exception e) {
				v.err("Start time: FAIL - " + e.getMessage());
			}
		} else
			v.ok("No start time API configured");
		return v;
	}

	@Override
	public String toString() {
		return "CCSContestSource[" + host + ":" + port + "]";
	}
}