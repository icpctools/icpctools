package org.icpc.tools.cds.video.containers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.video.VideoServingHandler;
import org.icpc.tools.cds.video.VideoStream;
import org.icpc.tools.cds.video.VideoStreamHandler.IStreamListener;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;

/**
 * HLS handler. https://localhost:8443/stream/11?_HLS_msn=10&_HLS_part=3&_HLS_skip=YES
 * https://localhost:8443/stream/11?_HLS_msn=10&_HLS_part=4&_HLS_skip=YES
 */
// TODO - cache HLS m3u8
// TODO - handle HLS query params
// TODO - no hardcoded stream
// TODO - multiplexing
// TODO - cache cleanup
public class HLSHandler extends VideoServingHandler {
	private static final String HEADER = "#EXTM3U";
	/*private static final String PARAM_MEDIA = "_HLS_msn";
	private static final String PARAM_PART = "_HLS_part";
	private static final String PARAM_SKIP = "_HLS_skip";
	*/
	// protected static final HLSFileCache fileCache = new HLSFileCache();

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
		System.out.println("hls index: " + request.getRequestURL());
		String query = request.getQueryString();
		if (query != null)
			System.out.println("  query: " + query);

		/*String media = request.getParameter(PARAM_MEDIA);
		String part = request.getParameter(PARAM_PART);
		String skip = request.getParameter(PARAM_SKIP);
		boolean doSkip = "YES".equals(skip);*/

		VideoStream vs = (VideoStream) store;
		String url = vs.getURL();

		InputStream in = null;
		try {
			URLConnection conn = null;
			if (query != null)
				conn = HTTPSSecurity.createURLConnection(new URL(url + "/stream.m3u8?" + query), null, null);
			else
				conn = HTTPSSecurity.createURLConnection(new URL(url + "/stream.m3u8"), null, null);

			conn.setConnectTimeout(15000);
			conn.setReadTimeout(10000);
			conn.setRequestProperty("Content-Type", "application/vnd.apple.mpegurl");
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
			parser.write(response.getOutputStream());

			HLSFileCache fileCache = (HLSFileCache) store.getObject();
			if (fileCache == null) {
				fileCache = new HLSFileCache();
				store.setObject(fileCache);
			}

			// cache files - segments + parts
			for (String s : parser.files()) {
				fileCache.cacheIt(url, s);
			}

			for (String s : parser.getPreload()) {
				System.out.println("  preload: " + s);
				fileCache.preload(s);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void start(ScheduledExecutorService executor) {
		// TODO cache cleanup
		// executor.scheduleAtFixedRate(() -> output(), 5000, 250, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response, int stream, IStore store, String path)
			throws IOException {
		if (path == null) {
			handleIndex(request, response, stream, store);
			return;
		}
		String name = path;
		System.out.println("hls: " + name);
		HLSFileCache fileCache = (HLSFileCache) store.getObject();
		if (fileCache == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Attempt to load from unindexed stream");
			return;
		}

		if (!fileCache.contains(name)) {
			if (fileCache.hasPreload(name)) {
				System.err.println("Preload unsupported: " + name);
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			System.err.println("Request to stream an invalid file: " + name);
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		try {
			fileCache.stream(name, response.getOutputStream());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*@Override
	protected void createReader(InputStream in, IStore stream, IStreamListener listener) throws IOException {
		// TODO

		HLSParser hls = (HLSParser) stream.getObject();
		if (hls == null)
			hls = new HLSParser();

		HLSParser parser = new HLSParser();
		parser.read(in);

		// make sure we have all the new files locally, and stream new ones to listener
		for (Segment seg : parser.playlist) {
			URL url = new URL(in, seg.file);
			pullFile(url, listener);
		}

		// update playlist for all clients
		stream.setObject(parser);

		// file old files for deletion
		for (Segment seg : hls.playlist) {
			boolean found = false;
			for (Segment seg2 : parser.playlist) {
				if (seg.file.equals(seg2.file)) {
					found = true;
					break;
				}
			}
			if (!found && !deleteMe.contains(seg.file))
				deleteMe.add(seg.file);
		}

		//
	}*/

	protected static String pullFile(URL url, IStreamListener listener) {
		InputStream in = null;
		try {
			URLConnection conn = HTTPSSecurity.createURLConnection(url, null, null);
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(10000);
			conn.setRequestProperty("Content-Type", "video/m2t");
			if (conn instanceof HttpURLConnection) {
				HttpURLConnection httpConn = (HttpURLConnection) conn;
				int httpStatus = httpConn.getResponseCode();
				if (httpStatus == HttpURLConnection.HTTP_NOT_FOUND)
					throw new IOException("404 Not found");
				else if (httpStatus == HttpURLConnection.HTTP_UNAUTHORIZED)
					throw new IOException("Not authorized (HTTP response code 401)");
			}

			in = conn.getInputStream();

			String f = "filename";
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));

			BufferedInputStream bin = new BufferedInputStream(in);
			byte[] b = new byte[1024 * 8];
			int n = bin.read(b);
			while (n != -1) {
				out.write(b, 0, n);
				listener.write(b, 0, n);
				n = bin.read(b);
			}

			bin.close();
			out.close();

			return f;
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not pull video");
			return null;
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (Exception e) {
					// ignore
				}
		}
	}

}