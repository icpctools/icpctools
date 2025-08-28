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

	protected CachedFile getFromCache(String name, int[] byterange) {
		if (name == null)
			return null;

		for (CachedFile cf : list) {
			if (cf.name.equals(name) && HLSParser.byterangeMatches(cf.byterange, byterange))
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

	private static byte[] cacheImpl(InputStream in, OutputStream out) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		BufferedInputStream bin = new BufferedInputStream(in);
		byte[] b = new byte[1024 * 8];
		int n = bin.read(b);
		while (n != -1) {
			bout.write(b, 0, n);
			if (out != null) {
				try {
					out.write(b, 0, n);
				} catch (Exception e) {
					// ignore
				}
			}
			n = bin.read(b);
		}

		bin.close();
		bout.close();

		return bout.toByteArray();
	}

	// Optimized caching when we know the size of the file in advance
	private static byte[] cacheImpl(InputStream in, OutputStream out, int length) throws IOException {
		byte[] cache = new byte[length];
		BufferedInputStream bin = new BufferedInputStream(in);
		int n = bin.read(cache, 0, length);
		int i = 0;
		while (n != -1) {
			if (out != null) {
				try {
					out.write(cache, i, n);
				} catch (Exception e) {
					// ignore
				}
			}
			i += n;
			if (i == length)
				break;

			n = bin.read(cache, i, length - i);
		}

		bin.close();

		return cache;
	}

	public void stream(CachedFile cf, OutputStream out) {
		try {
			cf.lastAccessTime = System.currentTimeMillis();

			BufferedOutputStream bout = new BufferedOutputStream(out);
			bout.write(cf.b, 0, cf.b.length);
			bout.close();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error streaming HLS video from cache", e);
		}
	}

	public boolean addToCache(String name, int[] byterange) {
		return streamAndCache(name, byterange, null);
	}

	public boolean streamAndCache(String name, int[] byterange, OutputStream out) {
		CachedFile cf = getFromCache(name, byterange);
		if (cf != null) {
			// already cached, just send it
			stream(cf, out);
			return true;
		}

		synchronized (this) {
			cf = getFromCache(name, byterange);
			if (cf != null) {
				stream(cf, out);
				return true;
			}

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

				int length = conn.getContentLength();

				cf = new CachedFile();
				cf.name = name;
				if (length > 0 || byterange != null) {
					cf.b = cacheImpl(conn.getInputStream(), out, length > 0 ? length : byterange[0]);
				} else {
					cf.b = cacheImpl(conn.getInputStream(), out);
				}
				cf.storeTime = System.currentTimeMillis();
				cf.byterange = byterange;
				list.add(cf);

				Trace.trace(Trace.INFO,
						"  Cache success: " + name + " " + range + " (" + (System.currentTimeMillis() - time) + "ms)");
				return true;
			} catch (Exception e) {
				Trace.trace(Trace.ERROR,
						"  Cache fail: " + name + " " + range + " (" + (System.currentTimeMillis() - time) + "ms)");
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

		CachedFile cf = getFromCache(name, null);
		if (cf != null)
			stream(cf, response.getOutputStream());
	}
}