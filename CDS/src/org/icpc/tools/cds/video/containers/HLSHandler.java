package org.icpc.tools.cds.video.containers;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.icpc.tools.cds.service.ExecutorListener;
import org.icpc.tools.cds.video.VideoAggregator;
import org.icpc.tools.cds.video.VideoServingHandler;
import org.icpc.tools.cds.video.VideoStream;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HLS handler.
 *
 * https://datatracker.ietf.org/doc/html/draft-pantos-hls-rfc8216bis
 */
// TODO - handle HLS query params
// TODO - connection modes, e.g. eager, lazy
public class HLSHandler extends VideoServingHandler {
	private static final String HEADER = "#EXTM3U";
	/*private static final String PARAM_MEDIA = "_HLS_msn";
	private static final String PARAM_PART = "_HLS_part";
	private static final String PARAM_SKIP = "_HLS_skip";
	*/

	private static final NumberFormat nf = NumberFormat.getIntegerInstance();

	static {
		startBackgroundCleanup();
	}

	protected List<String> deleteMe = new ArrayList<>();

	class Store {
		HLSParser parser;
	}

	@Override
	protected String getName() {
		return "HLS";
	}

	@Override
	protected String getFileExtension() {
		return "m3u8";
	}

	@Override
	protected String getFileName() {
		return "index.m3u8";
	}

	@Override
	protected String getMimeType() {
		return "application/vnd.apple.mpegurl";
	}

	@Override
	protected boolean validate(InputStream in) throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(in));

			String head = br.readLine();
			return (head != null && head.equals(HEADER));
		} catch (Exception e) {
			return false;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	// @Override
	protected void handleIndex(HttpServletRequest request, HttpServletResponse response, int stream, IStore store) {
		String query = request.getQueryString();

		Trace.trace(Trace.INFO, "HLS index: " + request.getRequestURL() + " " + query);

		VideoStream vs = (VideoStream) store;
		HLSFileCache fileCache = (HLSFileCache) store.getObject();

		// most simple caching - if we've loaded the index we can serve it to anyone else who isn't
		// using query params. clear after 500ms
		if (query == null) {
			if (fileCache != null && fileCache.getParser() != null
					&& fileCache.getParser().readTime > System.currentTimeMillis() - 500) {
				response.setContentType(getMimeType());
				streamIndex(fileCache, response);
				return;
			}
		}

		String url = vs.getURL();

		long time = System.currentTimeMillis();
		InputStream in = null;
		try {
			String baseURL = url;
			if (!baseURL.endsWith(".m3u8")) {
				baseURL += "/stream.m3u8";
			}
			URI uri = null;
			if (query != null)
				uri = new URI(baseURL + "?" + query);
			else
				uri = new URI(baseURL);

			URLConnection conn = HTTPSSecurity.createURLConnection(uri.toURL(), null, null);
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(10000);
			// conn.setRequestProperty("Content-Type", "application/vnd.apple.mpegurl");
			// index.m3u8
			if (conn instanceof HttpURLConnection) {
				HttpURLConnection httpConn = (HttpURLConnection) conn;
				int httpStatus = httpConn.getResponseCode();
				if (httpStatus == HttpURLConnection.HTTP_NOT_FOUND)
					throw new IOException("404 Not found (" + url + ")");
				else if (httpStatus == HttpURLConnection.HTTP_UNAUTHORIZED)
					throw new IOException("Not authorized (HTTP response code 401)");
			}

			HLSParser parser = new HLSParser(uri, "");// stream + "/");

			in = conn.getInputStream();
			parser.read(in);
			response.setContentType(getMimeType());

			Trace.trace(Trace.INFO, "  Index success (" + (System.currentTimeMillis() - time) + "ms)");

			// System.out.println(" m3u8 time " + (System.currentTimeMillis() - time) + "ms");

			if (fileCache == null) {
				fileCache = new HLSFileCache(parser);
				store.setObject(fileCache);
			}

			// cache files - segments, parts, preloads
			parser.prefillCache(fileCache);

			BufferedOutputStream bout = new BufferedOutputStream(response.getOutputStream());
			parser.write(bout);
			bout.close();
			// System.out.println(" total time " + (System.currentTimeMillis() - time) + "ms");
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading HLS index", e);
		}
	}

	private static void cleanCaches() {
		VideoAggregator va = VideoAggregator.getInstance();
		int numStreams = va.getNumStreams();
		long[] cacheData = new long[5];
		for (int i = 0; i < numStreams; i++) {
			VideoStream vs = va.getStream(i);
			if (vs != null) {
				Object obj = vs.getObject();
				if (obj != null && obj instanceof HLSFileCache) {
					HLSFileCache fileCache = (HLSFileCache) obj;
					long[] data = fileCache.cleanCache();
					cacheData[0] += data[0];
					cacheData[1] += data[1];
					cacheData[2] += data[2];
					cacheData[3] += data[3];
					cacheData[4] += data[4];
				}
			}
		}

		if (cacheData[2] != 0 || cacheData[3] != 0 || cacheData[0] != cacheData[1]) {
			Trace.trace(Trace.INFO, "HLS cache: " + nf.format(cacheData[1] / 1024.0) + " kB. " + cacheData[2]
					+ " removed, " + cacheData[3] + " reduced, " + cacheData[4] + " entries.");
		}
	}

	protected static void startBackgroundCleanup() {
		ExecutorListener.getExecutor().scheduleAtFixedRate(() -> cleanCaches(), 20, 5, TimeUnit.SECONDS);
	}

	/**
	 * Parses a string byterange in the HTTP form "bytes=start-end" into a byte array, e.g.
	 * "bytes=512-1024" returns [512, 1024]. Open-ended ranges return -1 as the end, e.g.
	 * "bytes=512-" returns [512, -1].
	 *
	 * @param s an HTTP byte range
	 * @return the integer start and end values of the range
	 */
	protected static int[] parseRange(String ss) {
		String s = ss;
		if (s.startsWith("bytes="))
			s = ss.substring(6);

		int ind = s.indexOf("-");
		if (ind < 0)
			return null;

		// TODO: suffix ("bytes=-500") or multi-range requests aren't supported
		try {
			String end = s.substring(ind + 1);
			return new int[] { Integer.parseInt(s.substring(0, ind)), end.isEmpty() ? -1 : Integer.parseInt(end) };
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response, int stream, IStore store, String path)
			throws IOException {
		response.setHeader("Access-Control-Allow-Origin", "*");

		if (path == null || path.equals("index.m3u8")) {
			handleIndex(request, response, stream, store);
			return;
		}
		String name = path;
		HLSFileCache fileCache = (HLSFileCache) store.getObject();
		if (fileCache == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Attempt to load from unindexed stream");
			return;
		}

		int[] httpByterange = null;
		String range = request.getHeader("Range");
		if (range != null && range.length() > 0) {
			httpByterange = parseRange(range);
		}

		// any valid file should already be in the cache list
		fileCache.serveRange(name, httpByterange, response);
	}

	private static void streamIndex(HLSFileCache fileCache, HttpServletResponse response) {
		try {
			HLSParser parser = fileCache.getParser();

			BufferedOutputStream bout = new BufferedOutputStream(response.getOutputStream());
			parser.write(bout);
			bout.close();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error streaming HLS index from cache", e);
		}
	}
}