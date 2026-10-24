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
import org.icpc.tools.contest.model.internal.MimeUtil;

import jakarta.servlet.http.HttpServletResponse;

/**
 * HLS cache. Keeps a list of file names and byte ranges, each of which may or may not have their
 * data cached.
 *
 * When new files/ranges are added to the cache, the first segment is downloaded on a background
 * thread and cached. Any byte range can be served at any point, with any new segments streamed and
 * cached as needed.
 *
 * Cached file data is periodically cleared to reduce memory use - if they're requested at this
 * point they'll just be downloaded/streamed again. After a longer period the file data is removed
 * entirely if it's not in the current HLS index, unless the playlist has ended (e.g. on-demand
 * video) since clients won't reload the index.
 */
public class HLSFileCache {
	private static final long CACHE_TIME = 17 * 1000L; // 17s
	private static final long MEMORY_TIME = 2 * 60 * 1000L; // 2 minutes

	static class CachedFile {
		public String name;
		public byte[] b;
		public int downloaded = -1;
		public long storeTime;
		public long lastAccessTime;
		// [length, start], or null for the entire file
		public int[] byterange;
		public boolean keep;
		// the content type and length as reported by the source
		public String contentType;
		public long length;

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

	private long lastCacheSize = 0;

	// true if the playlist has ended, so entries are never removed (only their data)
	private volatile boolean endList;

	public HLSFileCache(HLSParser parser) {
		this.parser = parser;
	}

	public void setEndList(boolean endList) {
		this.endList = endList;
	}

	/**
	 * Cleans the cache. Returns an array: [lastCacheSize, newCacheSize, removed, reduced]
	 *
	 * @return
	 */
	public long[] cleanCache() {
		List<CachedFile> remove = new ArrayList<>(20);
		long[] cacheData = new long[5];
		cacheData[0] = lastCacheSize;

		long now = System.currentTimeMillis();
		synchronized (list) {
			for (CachedFile cf : list) {
				if (!cf.keep && cf.lastAccessTime < now - CACHE_TIME) {
					if (!endList && cf.storeTime < now - MEMORY_TIME) {
						remove.add(cf);
						cacheData[2]++;
					} else if (cf.b != null && isDownloaded(cf)) {
						// remove the local cached copy (it will be downloaded again if needed)
						cf.b = null;
						cf.downloaded = -1;
						cacheData[3]++;
					}
				} else if (cf.b != null) {
					cacheData[1] += cf.b.length;
				}
			}
		}

		synchronized (list) {
			for (CachedFile s : remove)
				list.remove(s);
		}

		cacheData[4] = list.size();
		lastCacheSize = cacheData[1];

		return cacheData;
	}

	private CachedFile getFromCache(String name, int[] byterange) {
		if (name == null)
			return null;

		for (CachedFile cf : list) {
			if (cf.name.equals(name) && HLSParser.byterangeMatches(cf.byterange, byterange))
				return cf;
		}
		return null;
	}

	private static int getStart(CachedFile cf) {
		return cf.byterange[1];
	}

	private static int getEnd(CachedFile cf) {
		return cf.byterange[1] + cf.byterange[0] - 1;
	}

	/**
	 * Returns the cached ranges (sorted by start) that together cover the HTTP byte range [start,
	 * end], or null if the range isn't completely covered.
	 */
	private static List<CachedFile> getCoveringRanges(List<CachedFile> sortedRanges, int start, int end) {
		if (start < 0 || end < start)
			return null;

		List<CachedFile> covering = new ArrayList<>();
		int pos = start;
		for (CachedFile cf : sortedRanges) {
			if (getStart(cf) > pos)
				return null; // gap

			if (getEnd(cf) >= pos) {
				covering.add(cf);
				pos = getEnd(cf) + 1;
				if (pos > end)
					return covering;
			}
		}
		return null;
	}

	/**
	 * Serves the HTTP byte range [start, end] or null (the entire file) for the named file from the
	 * cache. Ranges that don't match the playlist exactly are assembled from one or more cached
	 * ranges.
	 */
	public void serveRange(String name, int[] httpByterange, HttpServletResponse response) throws IOException {
		CachedFile whole = null;
		List<CachedFile> ranges = new ArrayList<>();
		synchronized (list) {
			for (CachedFile cf : list) {
				if (cf.name.equals(name)) {
					if (cf.byterange == null)
						whole = cf;
					else
						ranges.add(cf);
				}
			}
		}

		if (whole != null) {
			serveWholeFile(whole, httpByterange, response);
			return;
		}

		if (ranges.isEmpty()) {
			Trace.trace(Trace.ERROR, "Request to stream an invalid file: " + name);
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		ranges.sort((a, b) -> Integer.compare(getStart(a), getStart(b)));

		int start, end;
		if (httpByterange != null) {
			start = httpByterange[0];
			end = httpByterange[1];
		} else {
			// otherwise, send everything we have
			start = getStart(ranges.get(0));
			end = -1;
		}
		if (end < 0) // open-ended range
			end = getEnd(ranges.get(ranges.size() - 1));

		// if this matches exactly, just serve it (streaming while it downloads if necessary)
		for (CachedFile cf : ranges) {
			if (getStart(cf) == start && getEnd(cf) == end) {
				// the headers need the origin's total length, so the first download can't be streamed
				if (cf.length == 0 && load(cf) == null) {
					Trace.trace(Trace.ERROR, "Could not load " + cf);
					response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
					return;
				}
				setRangeHeaders(cf, response, start, end);
				serveFile(cf, response.getOutputStream());
				return;
			}
		}

		List<CachedFile> covering = getCoveringRanges(ranges, start, end);
		if (covering == null) {
			Trace.trace(Trace.ERROR, "Request to stream an invalid range: " + name + " / " + start + "-" + end);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		// make sure the first range is available before responding (this also tells us the total
		// length)
		byte[] b = load(covering.get(0));
		if (b == null || b.length != covering.get(0).byterange[0]) {
			Trace.trace(Trace.ERROR, "Could not load " + covering.get(0) + " to serve range " + start + "-" + end);
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			return;
		}

		// send the headers right away, then stream each range as it's available
		setRangeHeaders(covering.get(0), response, start, end);
		try (BufferedOutputStream bout = new BufferedOutputStream(response.getOutputStream())) {
			for (int i = 0; i < covering.size(); i++) {
				CachedFile cf = covering.get(i);
				if (i > 0) {
					bout.flush();
					b = load(cf);
					if (b == null || b.length != cf.byterange[0])
						throw new IOException("Could not load " + cf);
				}
				int from = Math.max(start, getStart(cf));
				int to = Math.min(end, getEnd(cf));
				bout.write(b, from - getStart(cf), to - from + 1);
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error streaming HLS video from cache " + name + " " + start + "-" + end, e);
		}
	}

	private void serveWholeFile(CachedFile cf, int[] httpByterange, HttpServletResponse response) throws IOException {
		cf.lastAccessTime = System.currentTimeMillis();
		if (httpByterange == null) {
			setFileHeaders(cf, response);
			if (cf.b != null && cf.downloaded == cf.b.length)
				response.setContentLength(cf.b.length);

			serveFile(cf, response.getOutputStream());
			return;
		}

		byte[] b = load(cf);
		if (b == null) {
			Trace.trace(Trace.ERROR, "Could not load " + cf);
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			return;
		}

		int start = httpByterange[0];
		int end = httpByterange[1] < 0 ? b.length - 1 : Math.min(httpByterange[1], b.length - 1);
		if (start > end) {
			Trace.trace(Trace.ERROR, "Request to stream an invalid range: " + cf + " / " + start + "-" + end);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		setRangeHeaders(cf, response, start, end);
		try (BufferedOutputStream bout = new BufferedOutputStream(response.getOutputStream())) {
			bout.write(b, start, end - start + 1);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error streaming HLS video from cache " + cf + " " + start + "-" + end, e);
		}
	}

	private static void setFileHeaders(CachedFile cf, HttpServletResponse response) {
		String contentType = cf.contentType;
		if (contentType == null)
			contentType = MimeUtil.getMimeType(cf.name);
		if (contentType != null)
			response.setContentType(contentType);
		response.setHeader("Accept-Ranges", "bytes");
	}

	private static void setRangeHeaders(CachedFile cf, HttpServletResponse response, int start, int end) {
		setFileHeaders(cf, response);
		response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
		response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + (cf.length > 0 ? cf.length : "*"));
		response.setContentLength(end - start + 1);
	}

	/**
	 * Remember the content type and total length of the file from the origin's response headers.
	 */
	private static void storeFileInfo(CachedFile cf, URLConnection conn) {
		String contentType = conn.getContentType();
		if (contentType != null)
			cf.contentType = contentType;

		// e.g. "bytes 0-1023/146515"
		String contentRange = conn.getHeaderField("Content-Range");
		if (contentRange != null) {
			int ind = contentRange.lastIndexOf("/");
			if (ind >= 0) {
				try {
					cf.length = Long.parseLong(contentRange.substring(ind + 1).trim());
				} catch (NumberFormatException e) {
					// unknown length ("*")
				}
			}
		} else if (cf.byterange == null && conn.getContentLengthLong() >= 0) {
			cf.length = conn.getContentLengthLong();
		}
	}

	private static void cacheUnknownLength(CachedFile cf, InputStream in, OutputStream out) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		BufferedInputStream bin = new BufferedInputStream(in);
		byte[] b = new byte[1024 * 8];
		int n = bin.read(b);
		while (n != -1) {
			bout.write(b, 0, n);
			cf.lastAccessTime = System.currentTimeMillis();
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
			cf.lastAccessTime = System.currentTimeMillis();
			if (i == length)
				break;

			n = bin.read(cf.b, i, length - i);
		}

		bin.close();
		cf.downloaded = length;
	}

	private static boolean isDownloaded(CachedFile cf) {
		byte[] b = cf.b;
		return b != null && cf.downloaded == b.length;
	}

	/**
	 * Wait for another thread to finish (or fail) downloading the file, up to the given time.
	 */
	private static void waitForDownload(CachedFile cf, long maxWait) {
		long end = System.currentTimeMillis() + maxWait;
		while (cf.downloaded != -1 && !isDownloaded(cf) && System.currentTimeMillis() < end) {
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				// ignore
			}
		}
	}

	private void stream(CachedFile cf, OutputStream out) {
		waitForDownload(cf, 1000);
		if (!isDownloaded(cf)) {
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

	/**
	 * Returns the complete contents of a cached file, downloading it or waiting for another thread
	 * to download it if necessary. Returns null if the file could not be loaded.
	 */
	private byte[] load(CachedFile cf) {
		// try again once if another thread's download fails
		for (int attempt = 0; attempt < 2; attempt++) {
			boolean download = false;
			synchronized (cf) {
				if (cf.downloaded == -1) {
					download = true;
					cf.downloaded = 0;
				}
			}
			if (download)
				streamAndCache(cf, null);
			else
				waitForDownload(cf, 30000);

			byte[] b = cf.b;
			if (b != null && cf.downloaded == b.length) {
				cf.lastAccessTime = System.currentTimeMillis();
				return b;
			}
		}
		return null;
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

	private void serveFile(CachedFile cf, OutputStream out) {
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
		cf.lastAccessTime = time;
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
			storeFileInfo(cf, conn);

			int length = conn.getContentLength();
			if (length > 0 || cf.byterange != null) {
				cf.b = new byte[length > 0 ? length : cf.byterange[0]];
				cache(cf, conn.getInputStream(), out);
			} else {
				cacheUnknownLength(cf, conn.getInputStream(), out);
			}
			cf.lastAccessTime = System.currentTimeMillis();
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