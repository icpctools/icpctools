package org.icpc.tools.cds.video.containers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;

import jakarta.servlet.http.HttpServletResponse;

public class HLSFileCache {
	private static final long CACHE_TIME = 10 * 1000L; // 10s

	static class CachedFile {
		public String name;
		public byte[] b;
		public long storeTime;
		public long lastAccessTime;
		public int[] byterange;
	}

	private HLSParser parser;

	private List<CachedFile> list = new ArrayList<>();
	private List<String> preload = new ArrayList<>();
	// private List<String> lazyload = new ArrayList<>();

	public HLSFileCache(HLSParser parser) {
		this.parser = parser;
	}

	public int getSize() {
		return list.size();
	}

	public long cleanCache() {
		List<CachedFile> remove = new ArrayList<>(20);
		long size = 0;

		long now = System.currentTimeMillis();
		synchronized (list) {
			for (CachedFile cf : list) {
				// remove anything older than the cache time
				if (cf.storeTime < now - CACHE_TIME)
					remove.add(cf);
				else
					size += cf.b.length;
			}
		}

		for (CachedFile s : remove)
			list.remove(s);

		return size;
	}

	private CachedFile getFromCache(String name, int[] byterange) {
		if (name == null)
			return null;

		for (CachedFile cf : list) {
			if (cf.name.equals(name) && ((cf.byterange == null && byterange == null) || (cf.byterange != null
					&& byterange != null && cf.byterange[0] == byterange[0] && cf.byterange[1] == byterange[1])))
				return cf;
		}
		return null;
	}

	public boolean contains(String name, int[] byterange) {
		return getFromCache(name, byterange) != null;
	}

	public void addPreload(String name) {
		if (contains(name, null) || preload.contains(name))
			return;
		preload.add(name);
	}

	public boolean hasPreload(String name) {
		return preload.contains(name);
	}

	private void cacheImpl(String name, InputStream in, int[] byterange) throws IOException {
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
		cf.storeTime = System.currentTimeMillis();
		cf.byterange = byterange;
		list.add(cf);
	}

	public void stream(String name, OutputStream out, int[] byterange) throws IOException {
		CachedFile cf = getFromCache(name, byterange);
		if (cf == null)
			throw new IOException("File does not exist");

		cf.lastAccessTime = System.currentTimeMillis();

		BufferedOutputStream bout = new BufferedOutputStream(out);
		bout.write(cf.b, 0, cf.b.length);
		bout.close();
	}

	public boolean addToCache(String name, int[] byterange) {
		// already cached, don't grab them again
		if (contains(name, byterange))
			return true;

		synchronized (this) {
			if (contains(name, byterange))
				return true;

			long time = System.currentTimeMillis();
			String range = "";
			try {
				URI uri = parser.getURI().resolve(name);
				URLConnection conn = HTTPSSecurity.createURLConnection(uri.toURL(), null, null);
				conn.setConnectTimeout(15000);
				conn.setReadTimeout(10000);
				conn.setRequestProperty("Content-Type", "video/mp4");

				if (byterange != null) {
					// convert from HLS to HTTP byte range
					range = byterange[1] + "-" + (byterange[0] + byterange[1] - 1);
					conn.setRequestProperty("Range", "bytes=" + range);
				}

				if (conn instanceof HttpURLConnection) {
					HttpURLConnection httpConn = (HttpURLConnection) conn;
					int httpStatus = httpConn.getResponseCode();
					if (httpStatus == HttpURLConnection.HTTP_NOT_FOUND)
						throw new IOException("404 Not found (" + name + ")");
					else if (httpStatus == HttpURLConnection.HTTP_UNAUTHORIZED)
						throw new IOException("Not authorized (HTTP response code 401)");
				}

				cacheImpl(name, new BufferedInputStream(conn.getInputStream()), byterange);
				System.out.println(
						"  Cache success: " + name + " " + range + " (" + (System.currentTimeMillis() - time) + "ms)");
				return true;
			} catch (Exception e) {
				System.err
						.println("  Cache fail: " + name + " " + range + " (" + (System.currentTimeMillis() - time) + "ms)");
				Trace.trace(Trace.ERROR, "Could not cache HLS " + name, e);
				return false;
			}
		}
	}

	public HLSParser getParser() {
		return parser;
	}

	// treat preloads like lazy loads
	protected void preloadIt(String name, HttpServletResponse response) throws IOException {
		System.out.println("  Preload: " + name);
		if (!addToCache(name, null)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		stream(name, response.getOutputStream(), null);
	}
}