package org.icpc.tools.cds.video.containers;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.service.ExecutorListener;
import org.icpc.tools.cds.video.VideoAggregator;
import org.icpc.tools.cds.video.VideoServingHandler;
import org.icpc.tools.cds.video.VideoStream;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;

/**
 * HLS handler. https://localhost:8443/stream/11?_HLS_msn=10&_HLS_part=3&_HLS_skip=YES
 * https://localhost:8443/stream/11?_HLS_msn=10&_HLS_part=4&_HLS_skip=YES
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

		System.out.println("HLS index: " + request.getRequestURL() + " " + query);
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
			URLConnection conn = null;
			if (query != null)
				conn = HTTPSSecurity.createURLConnection(new URL(url + "/stream.m3u8?" + query), null, null);
			else
				conn = HTTPSSecurity.createURLConnection(new URL(url + "/stream.m3u8"), null, null);

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

			HLSParser parser = new HLSParser();
			parser.setURLPrefix(stream + "/");

			in = conn.getInputStream();
			parser.read(in);
			response.setContentType(getMimeType());

			System.out.println("  Index success (" + (System.currentTimeMillis() - time) + "ms)");

			// System.out.println(" m3u8 time " + (System.currentTimeMillis() - time) + "ms");

			if (fileCache == null) {
				fileCache = new HLSFileCache();
				store.setObject(fileCache);
			}

			// cache files - segments + parts
			for (String s : parser.filesToDownload()) {
				fileCache.addToCache(url, s);
			}

			for (String s : parser.getPreload()) {
				fileCache.addPreload(s);
			}
			fileCache.setParser(parser);
			// System.out.println(" cache time " + (System.currentTimeMillis() - time) + "ms");

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

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response, int stream, IStore store, String path)
			throws IOException {
		if (path == null) {
			handleIndex(request, response, stream, store);
			return;
		}
		String name = path;
		HLSFileCache fileCache = (HLSFileCache) store.getObject();
		if (fileCache == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Attempt to load from unindexed stream");
			return;
		}

		if (!fileCache.contains(name)) {
			if (fileCache.hasPreload(name)) {
				try {
					VideoStream vs = (VideoStream) store;
					String url = vs.getURL();
					fileCache.preloadIt(url, name, response);
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error preloading HLS video", e);
				}

				return;
			}
			Trace.trace(Trace.ERROR, "Request to stream an invalid file: " + name);
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		try {
			fileCache.stream(name, response.getOutputStream());
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error streaming HLS video from cache", e);
		}
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