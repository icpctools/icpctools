package org.icpc.tools.cds.video.containers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;

public class HLSFileCache {
	static class CachedFile {
		public String name;
		public byte[] b;
		public long lastAccess;
	}

	private Map<String, CachedFile> map = new HashMap<>();
	private List<String> preload = new ArrayList<>();

	public HLSFileCache() {
		// create
	}

	public void cleanCache() {
		List<String> remove = new ArrayList<>(100);

		long now = System.currentTimeMillis();
		synchronized (map) {
			for (String s : map.keySet()) {
				CachedFile cf = map.get(s);
				// older than 20s
				if (cf.lastAccess < now - 20 * 1000L)
					remove.add(s);
			}
		}

		for (String s : remove) {
			map.remove(s);
		}
	}

	public boolean contains(String name) {
		return map.containsKey(name);
	}

	public void preload(String name) {
		if (map.containsKey(name) || preload.contains(name))
			return;
		preload.add(name);
	}

	public boolean hasPreload(String name) {
		return preload.contains(name);
	}

	public void cache(String name, InputStream in) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		BufferedInputStream bin = new BufferedInputStream(in);
		byte[] b = new byte[1024 * 8];
		int n = bin.read(b);
		while (n != -1) {
			bout.write(b, 0, n);
			n = bin.read(b);
		}

		bin.close();
		bout.close();

		CachedFile cf = new CachedFile();
		cf.name = name;
		cf.b = bout.toByteArray();
		cf.lastAccess = System.currentTimeMillis();
		map.put(name, cf);
	}

	public void stream(String name, OutputStream out) throws IOException {
		CachedFile cf = map.get(name);
		if (cf == null)
			throw new IOException("File does not exist");

		cf.lastAccess = System.currentTimeMillis();

		BufferedOutputStream bout = new BufferedOutputStream(out);
		bout.write(cf.b, 0, cf.b.length);
		bout.close();
	}

	protected void cacheIt(String url, String name) {
		// TODO: what are these gap files and do they matter?
		if ("gap.mp4".equals(name))
			return;

		// already cached, don't grab them again
		if (contains(name))
			return;

		try {
			URLConnection conn = HTTPSSecurity.createURLConnection(new URL(url + "/" + name), null, null);
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(10000);
			conn.setRequestProperty("Content-Type", "video/mp4");

			if (conn instanceof HttpURLConnection) {
				HttpURLConnection httpConn = (HttpURLConnection) conn;
				int httpStatus = httpConn.getResponseCode();
				if (httpStatus == HttpURLConnection.HTTP_NOT_FOUND)
					throw new IOException("404 Not found (" + url + ")");
				else if (httpStatus == HttpURLConnection.HTTP_UNAUTHORIZED)
					throw new IOException("Not authorized (HTTP response code 401)");
			}

			cache(name, conn.getInputStream());
			System.out.println("  Cache success: " + name);
		} catch (Exception e) {
			System.err.println("  Cache fail: " + name);
			Trace.trace(Trace.ERROR, "Could not cache HLS " + url + "/" + name, e);
		}
	}

	protected void preloadIt(String url, String name) {
		// TODO: what are these gap files and do they matter?
		if ("gap.mp4".equals(name))
			return;

		// already cached, don't grab them again
		if (contains(name))
			return;

		try {
			URLConnection conn = HTTPSSecurity.createURLConnection(new URL(url + "/" + name), null, null);
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(10000);
			conn.setRequestProperty("Content-Type", "video/mp4");

			if (conn instanceof HttpURLConnection) {
				HttpURLConnection httpConn = (HttpURLConnection) conn;
				int httpStatus = httpConn.getResponseCode();
				if (httpStatus == HttpURLConnection.HTTP_NOT_FOUND)
					throw new IOException("404 Not found (" + url + ")");
				else if (httpStatus == HttpURLConnection.HTTP_UNAUTHORIZED)
					throw new IOException("Not authorized (HTTP response code 401)");
			}

			cache(name, conn.getInputStream());
			System.out.println("  Cache success: " + name);
		} catch (Exception e) {
			System.err.println("  Cache fail: " + name);
			Trace.trace(Trace.ERROR, "Could not cache HLS " + url + "/" + name, e);
		}
	}
}