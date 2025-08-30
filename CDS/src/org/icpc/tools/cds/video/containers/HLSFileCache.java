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
import java.util.concurrent.TimeUnit;

import org.icpc.tools.cds.service.ExecutorListener;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;

public class HLSFileCache {
	private static final long CACHE_TIME = 10 * 1000L; // 10s
	private static final long MEMORY_TIME = 2 * 60 * 1000L; // 2 minutes

	static class CachedFile {
		public String name;
		public byte[] b;
		public int downloaded = -1;
		public long storeTime;
		public long lastAccessTime;
		public int[] byterange;
		public boolean keep;

		@Override
		public String toString() {
			if (byterange != null) {
				return name + " " + byterange[1] + "-" + (byterange[0] + byterange[1] - 1);
			}
			return name;
		}
	}

	private HLSParser parser;

	private List<CachedFile> list = new ArrayList<>();

	public HLSFileCache(HLSParser parser) {
		this.parser = parser;
	}

	public int getSize() {
		return list.size();
	}

	public long cleanCache() {
		Trace.trace(Trace.INFO, "Cleaning cache");
		List<CachedFile> remove = new ArrayList<>(20);
		long size = 0;

		long now = System.currentTimeMillis();
		synchronized (list) {
			for (CachedFile cf : list) {
				if (!cf.keep && cf.lastAccessTime < now - CACHE_TIME) {
					if (cf.storeTime < now - MEMORY_TIME) {
						remove.add(cf);
						Trace.trace(Trace.INFO, "Removing " + cf);
					} else if (cf.b != null) {
						// remove the local cached copy
						cf.downloaded = 0;
						cf.b = null;
						// size += cf.b.length;
						Trace.trace(Trace.INFO, "Removing data from " + cf);
					} else {
						size += cf.b.length;
					}
				} else {
					size += cf.b.length;
				}
			}
		}

		synchronized (list) {
			for (CachedFile s : remove)
				list.remove(s);
		}

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

	private static void cacheUnknownLength(CachedFile cf, InputStream in, OutputStream out) throws IOException {
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

		cf.b = bout.toByteArray();
		cf.downloaded = cf.b.length;
	}

	// Optimized caching when we know the size of the file in advance
	private static void cache(CachedFile cf, InputStream in, OutputStream out) throws IOException {
		int length = cf.b.length;
		BufferedInputStream bin = new BufferedInputStream(in);
		int n = bin.read(cf.b, 0, length);
		int i = 0;
		while (n != -1) {
			if (out != null) {
				try {
					out.write(cf.b, i, n);
				} catch (Exception e) {
					// ignore
				}
			}
			i += n;
			cf.downloaded = i;
			if (i == length)
				break;

			n = bin.read(cf.b, i, length - i);
		}

		bin.close();
		cf.downloaded = length;
	}

	private void stream(CachedFile cf, OutputStream out) {
		int count = 0;
		while (cf.downloaded != -1 && !(cf.b != null && cf.downloaded == cf.b.length) && count < 10) {
			// another thread is downloading, wait a bit
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				// ignore
			}
			count++;
		}
		if (cf.b == null || cf.downloaded != cf.b.length) {
			// the other thread failed to download, let's try again
			serveFile(cf, out);
			return;
		}
		try {
			byte[] b = cf.b;
			cf.lastAccessTime = System.currentTimeMillis();

			BufferedOutputStream bout = new BufferedOutputStream(out);
			bout.write(b, 0, b.length);
			bout.close();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error streaming HLS video from cache " + cf, e);
		}
	}

	public CachedFile addToCache(String name, int[] byterange, boolean downloadNow) {
		CachedFile cf = getFromCache(name, byterange);
		if (cf == null) {
			synchronized (list) {
				cf = getFromCache(name, byterange);

				if (cf == null) {
					cf = new CachedFile();
					cf.name = name;
					cf.byterange = byterange;
					list.add(cf);
				}
			}
		}
		cf.storeTime = System.currentTimeMillis();
		Trace.trace(Trace.INFO, "Added to cache list: " + cf + " " + downloadNow);

		if (downloadNow) {
			final CachedFile cf2 = cf;
			ExecutorListener.getExecutor().schedule(() -> {
				boolean download = false;
				synchronized (cf2) {
					if (cf2.downloaded == -1) {
						download = true;
						cf2.downloaded = 0;
					}
				}
				if (download) {
					streamAndCache(cf2, null);
				}
			}, 0, TimeUnit.SECONDS);
		}
		return cf;
	}

	public void serveFile(CachedFile cf, OutputStream out) {
		// only the first thread should attempt to download the file
		boolean download = false;
		synchronized (cf) {
			if (cf.downloaded == -1) {
				download = true;
				cf.downloaded = 0;
			}
		}
		if (download) {
			streamAndCache(cf, out);
		} else {
			stream(cf, out);
		}
	}

	private void streamAndCache(CachedFile cf, OutputStream out) {
		long time = System.currentTimeMillis();
		String range = "";
		try {
			URI uri = parser.getURI().resolve(cf.name);
			URLConnection conn = HTTPSSecurity.createURLConnection(uri.toURL(), null, null);
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(10000);
			conn.setRequestProperty("Content-Type", "video/mp4");

			if (cf.byterange != null) {
				// convert from HLS to HTTP byte range
				range = cf.byterange[1] + "-" + (cf.byterange[0] + cf.byterange[1] - 1);
				conn.setRequestProperty("Range", "bytes=" + range);
			}

			if (conn instanceof HttpURLConnection) {
				HttpURLConnection httpConn = (HttpURLConnection) conn;
				int httpStatus = httpConn.getResponseCode();
				if (httpStatus == HttpURLConnection.HTTP_NOT_FOUND)
					throw new IOException("404 Not found (" + cf.name + ")");
				else if (httpStatus == HttpURLConnection.HTTP_UNAUTHORIZED)
					throw new IOException("Not authorized (HTTP response code 401)");
			}

			int length = conn.getContentLength();
			if (length > 0 || cf.byterange != null) {
				cf.b = new byte[length > 0 ? length : cf.byterange[0]];
				cache(cf, conn.getInputStream(), out);
			} else {
				cacheUnknownLength(cf, conn.getInputStream(), out);
			}
			cf.lastAccessTime = System.currentTimeMillis();

			Trace.trace(Trace.INFO,
					"  Cache success: " + cf.name + " " + range + " (" + (System.currentTimeMillis() - time) + "ms)");
		} catch (Exception e) {
			Trace.trace(Trace.ERROR,
					"  Cache fail: " + cf.name + " " + range + " (" + (System.currentTimeMillis() - time) + "ms)");
			cf.downloaded = -1;
		}
	}

	public HLSParser getParser() {
		return parser;
	}
}