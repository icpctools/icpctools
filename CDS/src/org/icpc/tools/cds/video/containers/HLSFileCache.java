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

import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;

public class HLSFileCache {
	private static final long CACHE_TIME = 20 * 1000L; // 20s

	static class CachedFile {
		public String name;
		public byte[] b;
		public long lastAccess;
	}

	private HLSParser parser;

	private Map<String, CachedFile> map = new HashMap<>();
	private List<String> preload = new ArrayList<>();
	// private List<String> lazyload = new ArrayList<>();

	public HLSFileCache() {
		// create
	}

	public long cleanCache() {
		List<String> remove = new ArrayList<>(100);
		long size = 0;

		long now = System.currentTimeMillis();
		synchronized (map) {
			for (String s : map.keySet()) {
				CachedFile cf = map.get(s);
				// older than 20s
				if (cf.lastAccess < now - CACHE_TIME)
					remove.add(s);
				else
					size += cf.b.length;
			}
		}

		for (String s : remove)
			map.remove(s);

		return size;
	}

	public boolean contains(String name) {
		return map.containsKey(name);
	}

	public void addPreload(String name) {
		if (map.containsKey(name) || preload.contains(name))
			return;
		preload.add(name);
	}

	public boolean hasPreload(String name) {
		return preload.contains(name);
	}

	private void cacheImpl(String name, InputStream in) throws IOException {
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

	public boolean addToCache(String url, String name) {
		// already cached, don't grab them again
		if (contains(name))
			return true;

		synchronized (this) {
			if (contains(name))
				return true;

			long time = System.currentTimeMillis();
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

				cacheImpl(name, new BufferedInputStream(conn.getInputStream()));
				System.out.println("  Cache success: " + name + " (" + (System.currentTimeMillis() - time) + "ms)");
				return true;
			} catch (Exception e) {
				System.err.println("  Cache fail: " + name + " (" + (System.currentTimeMillis() - time) + "ms)");
				Trace.trace(Trace.ERROR, "Could not cache HLS " + url + "/" + name, e);
				return false;
			}
		}
	}

	public HLSParser getParser() {
		return parser;
	}

	public void setParser(HLSParser parser) {
		this.parser = parser;
	}

	// treat preloads like lazy loads
	protected void preloadIt(String url, String name, HttpServletResponse response) throws IOException {
		System.out.println("  Preload: " + name);
		if (!addToCache(url, name)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		stream(name, response.getOutputStream());
	}
}