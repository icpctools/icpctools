package org.icpc.tools.cds.video.containers;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.icpc.tools.cds.service.ExecutorListener;
import org.icpc.tools.cds.video.VideoAggregator;
import org.icpc.tools.cds.video.VideoServingHandler;
import org.icpc.tools.cds.video.VideoStream;
import org.icpc.tools.cds.video.containers.HLSFileCache.CachedFile;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HLS handler.
 *
 * https://datatracker.ietf.org/doc/html/draft-pantos-hls-rfc8216bis
 */
// TODO - cache HLS m3u8
// TODO - handle HLS query params
// TODO - multiplexing m3u8
// TODO - connection modes, e.g. eager, lazy
public class HLSHandler extends VideoServingHandler {
	private static final String HEADER = "#EXTM3U";
	/*private static final String PARAM_MEDIA = "_HLS_msn";
	private static final String PARAM_PART = "_HLS_part";
	private static final String PARAM_SKIP = "_HLS_skip";
	*/

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
		/*String media = request.getParameter(PARAM_MEDIA);
		String part = request.getParameter(PARAM_PART);
		String skip = request.getParameter(PARAM_SKIP);
		boolean doSkip = "YES".equals(skip);*/

		VideoStream vs = (VideoStream) store;
		HLSFileCache fileCache = (HLSFileCache) store.getObject();

		// most simple caching - if we've loaded the index we can serve it to anyone else who isn't
		// using query params. clear after 500ms
		if (query == null) {
			if (fileCache != null && fileCache.getParser() != null
					&& fileCache.getParser().readTime > System.currentTimeMillis() - 500) {
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
		// long cacheSize = 0;
		for (int i = 0; i < numStreams; i++) {
			VideoStream vs = va.getStream(i);
			if (vs != null) {
				Object obj = vs.getObject();
				if (obj != null && obj instanceof HLSFileCache) {
					HLSFileCache fileCache = (HLSFileCache) obj;
					// cacheSize +=
					fileCache.cleanCache();
				}
			}
		}

		// System.out.println("Cleaned cache, remaining size: " + cacheSize + " bytes");
	}

	protected static void startBackgroundCleanup() {
		ExecutorListener.getExecutor().scheduleAtFixedRate(() -> cleanCaches(), 20, 5, TimeUnit.SECONDS);
	}

	/**
	 * Parses a string byterange in the HTTP form "bytes=start-end" into a byte array, e.g.
	 * "bytes=512-1024" returns [512, 1024].
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

		return new int[] { Integer.parseInt(s.substring(0, ind)), Integer.parseInt(s.substring(ind + 1)) };
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response, int stream, IStore store, String path)
			throws IOException {
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
		int[] hlsByterange = null;
		String range = request.getHeader("Range");
		if (range != null && range.length() > 0) {
			httpByterange = parseRange(range);

			// convert from HTTP byte range [start, end] to HLS byte range [length, start]
			hlsByterange = new int[2];
			hlsByterange[0] = httpByterange[1] - httpByterange[0] + 1;
			hlsByterange[1] = httpByterange[0];
		}

		// any valid file should already be in the cache list
		CachedFile cf = fileCache.getFromCache(name, hlsByterange);
		if (cf == null) {
			Trace.trace(Trace.ERROR, "Request to stream an invalid file: " + name + " / " + range);
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (cf.byterange != null) {
			response.setStatus(206);
			response.setHeader("Content-Range", "bytes " + cf.byterange[0] + "-" + cf.byterange[1] + "/*");
		}

		if (cf.byterange != null) {
			response.setContentLength(cf.byterange[0]);
		} else if (cf.b != null) {
			response.setContentLength(cf.b.length);
		}

		fileCache.serveFile(cf, response.getOutputStream());
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