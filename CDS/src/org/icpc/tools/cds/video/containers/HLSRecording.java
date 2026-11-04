package org.icpc.tools.cds.video.containers;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.icpc.tools.cds.video.VideoHandler.IRecording;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;

/**
 * Records an HLS stream to a local folder. The remote index is polled periodically and any new
 * segments are appended to a local EVENT playlist, which is rewritten after every poll so that it
 * can be played while recording. When stopped, the playlist is ended.
 *
 * Segments are loaded through the stream's HLSFileCache, so they're shared with anyone watching the
 * stream and with any other recordings of the same stream.
 *
 * Only full media segments are recorded; low-latency parts and preload hints are ignored. If the
 * source is a multivariant (master) playlist, the first variant is recorded.
 */
public class HLSRecording implements IRecording {
	private static final String STREAM_INF = "#EXT-X-STREAM-INF:";
	private static final String TARGET_DURATION = "#EXT-X-TARGETDURATION:";
	private static final String MEDIA_SEQUENCE = "#EXT-X-MEDIA-SEQUENCE:";
	private static final String INF = "#EXTINF:";
	private static final String[] KEEP_TAGS = { INF, "#EXT-X-PROGRAM-DATE-TIME:" };
	private static final String DISCONTINUITY = "#EXT-X-DISCONTINUITY";

	private static final long MIN_POLL_MS = 1000;
	private static final long DEFAULT_POLL_MS = 2000;

	private final ScheduledExecutorService executor;
	private final HLSFileCache cache;
	private final File dir;
	private final File indexFile;
	private URI indexURI;
	// true if recording a variant of a multivariant playlist, which isn't cached
	private boolean variant;

	// media sequence number of the last recorded segment
	private long lastSequence = -1;
	// key of the last recorded segment
	private String lastKey;
	// local file names already in use
	private final Set<String> localNames = new HashSet<>();
	// lines of the local playlist after the header
	private final List<String> lines = new ArrayList<>();
	private String currentMap;
	private int numSegments;
	private int targetDuration = 1;
	private long pollMs = DEFAULT_POLL_MS;
	private boolean stopped;
	private ScheduledFuture<?> future;

	/**
	 * @param executor the executor to poll on
	 * @param cache the stream's file cache, or null to download files directly
	 * @param indexURI the remote index
	 * @param indexFile the local index file to write; segments are saved in the same folder
	 */
	public HLSRecording(ScheduledExecutorService executor, HLSFileCache cache, URI indexURI, File indexFile) {
		this.executor = executor;
		this.cache = cache;
		this.indexURI = indexURI;
		this.indexFile = indexFile;
		this.dir = indexFile.getParentFile();
	}

	public void start() throws IOException {
		if (!dir.exists() && !dir.mkdirs())
			throw new IOException("Could not create folder " + dir);

		writeIndex(false);
		synchronized (this) {
			future = executor.schedule(this::pollAndReschedule, 0, TimeUnit.MILLISECONDS);
		}
	}

	private void pollAndReschedule() {
		synchronized (this) {
			if (stopped)
				return;

			poll();

			if (!stopped)
				future = executor.schedule(this::pollAndReschedule, pollMs, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public void stop() {
		synchronized (this) {
			if (stopped)
				return;

			if (future != null)
				future.cancel(false);

			// pick up anything new since the last poll
			poll();
			stopped = true;
			writeIndex(true);
		}
	}

	@Override
	public synchronized boolean isEmpty() {
		return numSegments == 0;
	}

	private void poll() {
		try {
			HLSParser parser = readIndex(indexURI);
			if (parser.playlist == null)
				return;

			// multivariant playlist: switch to recording the first variant
			if (parser.playlist.length > 0 && isVariant(parser.playlist[0])) {
				indexURI = indexURI.resolve(parser.playlist[0].file);
				variant = true;
				Trace.trace(Trace.INFO, "Recording HLS variant " + indexURI);
				parser = readIndex(indexURI);
				if (parser.playlist == null)
					return;
			}

			updatePollInterval(parser);

			long mediaSequence = getMediaSequence(parser);
			long lastInPlaylist = mediaSequence + parser.playlist.length - 1;
			boolean changed = false;
			if (lastSequence < 0) {
				// the live window contains older segments; start with the most recent one
				lastSequence = lastInPlaylist - 1;
			} else if (lastInPlaylist < lastSequence) {
				// sequence went backwards: either a stale index (ignore it) or the stream restarted
				for (HLSSegment seg : parser.playlist) {
					if (seg.file != null && key(seg).equals(lastKey))
						return;
				}
				Trace.trace(Trace.INFO, "HLS stream restarted while recording: " + indexURI);
				lastSequence = mediaSequence - 1;
				lines.add(DISCONTINUITY);
				changed = true;
			}

			for (int i = 0; i < parser.playlist.length; i++) {
				long sequence = mediaSequence + i;
				if (sequence <= lastSequence)
					continue;

				HLSSegment seg = parser.playlist[i];
				if (seg.gap || seg.file == null) {
					lastSequence = sequence;
					continue;
				}

				String key = key(seg);
				if (parser.map != null && !key(parser.map).equals(currentMap)) {
					String mapName = download(parser.map);
					lines.add("#EXT-X-MAP:URI=\"" + mapName + "\"");
					currentMap = key(parser.map);
				}

				String name = download(seg);
				lastSequence = sequence;
				lastKey = key;
				numSegments++;
				changed = true;

				for (String s : seg.comments) {
					if (s.equals(DISCONTINUITY))
						lines.add(s);
					else {
						for (String tag : KEEP_TAGS) {
							if (s.startsWith(tag)) {
								lines.add(s);
								if (tag.equals(INF))
									updateTargetDuration(s);
							}
						}
					}
				}
				lines.add(name);
			}

			if (changed)
				writeIndex(false);
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Error recording HLS stream " + indexURI + ": " + e.getMessage());
		}
	}

	private static boolean isVariant(HLSSegment seg) {
		if (seg.comments == null)
			return false;
		for (String s : seg.comments) {
			if (s.startsWith(STREAM_INF))
				return true;
		}
		return false;
	}

	private static long getMediaSequence(HLSParser parser) {
		if (parser.header != null) {
			for (String s : parser.header) {
				if (s.startsWith(MEDIA_SEQUENCE)) {
					try {
						return Long.parseLong(s.substring(MEDIA_SEQUENCE.length()).trim());
					} catch (NumberFormatException e) {
						// ignore
					}
				}
			}
		}
		return 0;
	}

	private void updatePollInterval(HLSParser parser) {
		if (parser.header == null)
			return;

		for (String s : parser.header) {
			if (s.startsWith(TARGET_DURATION)) {
				try {
					int td = Integer.parseInt(s.substring(TARGET_DURATION.length()).trim());
					pollMs = Math.max(MIN_POLL_MS, td * 1000L / 2);
				} catch (NumberFormatException e) {
					// ignore
				}
			}
		}
	}

	private void updateTargetDuration(String inf) {
		// #EXTINF:<duration>,[<title>]
		String s = inf.substring(INF.length());
		int ind = s.indexOf(",");
		if (ind >= 0)
			s = s.substring(0, ind);
		try {
			targetDuration = Math.max(targetDuration, (int) Math.round(Double.parseDouble(s.trim())));
		} catch (NumberFormatException e) {
			// ignore
		}
	}

	private String key(HLSSegment seg) {
		String key = indexURI.resolve(seg.file).toString();
		if (seg.byterange != null)
			key += "@" + seg.byterange[1] + "-" + seg.byterange[0];
		return key;
	}

	private static URLConnection openConnection(URI uri) throws IOException {
		URLConnection conn = HTTPSSecurity.createURLConnection(uri.toURL(), null, null);
		conn.setConnectTimeout(15000);
		conn.setReadTimeout(10000);
		return conn;
	}

	private static int checkStatus(URLConnection conn, URI uri) throws IOException {
		if (!(conn instanceof HttpURLConnection))
			return HttpURLConnection.HTTP_OK;

		int status = ((HttpURLConnection) conn).getResponseCode();
		if (status == HttpURLConnection.HTTP_NOT_FOUND)
			throw new IOException("404 Not found (" + uri + ")");
		else if (status == HttpURLConnection.HTTP_UNAUTHORIZED)
			throw new IOException("Not authorized (HTTP response code 401)");
		else if (status >= 400)
			throw new IOException("HTTP response code " + status + " (" + uri + ")");
		return status;
	}

	private static HLSParser readIndex(URI uri) throws IOException {
		URLConnection conn = openConnection(uri);
		checkStatus(conn, uri);
		HLSParser parser = new HLSParser(uri, "");
		try (InputStream in = conn.getInputStream()) {
			parser.read(in);
		}
		return parser;
	}

	/**
	 * Saves a segment into the recording folder and returns the local file name.
	 */
	private String download(HLSSegment seg) throws IOException {
		URI uri = indexURI.resolve(seg.file);
		String name = localName(uri, seg.byterange);
		File f = new File(dir, name);
		File temp = new File(dir, name + ".part");

		// the cache's file names are relative to the main index, so variants aren't cached
		if (cache != null && !variant) {
			// use the same name as the index if it resolves to the same file, so that it's shared
			String cacheName = seg.file;
			if (!uri.equals(cache.getParser().getURI().resolve(cacheName)))
				cacheName = uri.toString();

			byte[] b = cache.getContents(cacheName, seg.byterange);
			if (b == null)
				throw new IOException("Could not load " + uri);
			Files.write(temp.toPath(), b);
		} else
			downloadDirect(uri, seg.byterange, temp);

		Files.move(temp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
		localNames.add(name);
		return name;
	}

	private static void downloadDirect(URI uri, int[] br, File temp) throws IOException {
		URLConnection conn = openConnection(uri);
		if (br != null)
			conn.setRequestProperty("Range", "bytes=" + br[1] + "-" + (br[1] + br[0] - 1));
		int status = checkStatus(conn, uri);

		try (InputStream in = conn.getInputStream();
				OutputStream out = new BufferedOutputStream(new FileOutputStream(temp))) {
			if (br != null && status != HttpURLConnection.HTTP_PARTIAL) {
				// server ignored the range request, extract the range ourselves
				skipFully(in, br[1]);
				copy(in, out, br[0]);
			} else
				in.transferTo(out);
		} catch (IOException e) {
			temp.delete();
			throw e;
		}
	}

	private static void skipFully(InputStream in, long n) throws IOException {
		long remaining = n;
		while (remaining > 0) {
			long skipped = in.skip(remaining);
			if (skipped <= 0) {
				if (in.read() == -1)
					throw new IOException("Unexpected end of stream");
				skipped = 1;
			}
			remaining -= skipped;
		}
	}

	private static void copy(InputStream in, OutputStream out, long n) throws IOException {
		byte[] b = new byte[8192];
		long remaining = n;
		while (remaining > 0) {
			int r = in.read(b, 0, (int) Math.min(b.length, remaining));
			if (r == -1)
				throw new IOException("Unexpected end of stream");
			out.write(b, 0, r);
			remaining -= r;
		}
	}

	/**
	 * Returns a unique, safe local file name for the given remote file.
	 */
	private String localName(URI uri, int[] byterange) {
		String path = uri.getPath();
		String name = path == null ? "" : path.substring(path.lastIndexOf('/') + 1);
		name = name.replaceAll("[^A-Za-z0-9._-]", "_");
		if (name.isEmpty() || name.startsWith(".") || name.equals(indexFile.getName()))
			name = "segment" + name;

		if (byterange != null) {
			int ind = name.lastIndexOf('.');
			if (ind > 0)
				name = name.substring(0, ind) + "_" + byterange[1] + name.substring(ind);
			else
				name = name + "_" + byterange[1];
		}

		String unique = name;
		int count = 2;
		while (localNames.contains(unique)) {
			unique = count + "_" + name;
			count++;
		}
		return unique;
	}

	private void writeIndex(boolean end) {
		// write to a temp file and move, so readers never see a partial index
		File temp = new File(dir, indexFile.getName() + ".tmp");
		try (BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8))) {
			bw.write("#EXTM3U");
			bw.newLine();
			bw.write("#EXT-X-VERSION:7");
			bw.newLine();
			bw.write("#EXT-X-PLAYLIST-TYPE:EVENT");
			bw.newLine();
			bw.write(TARGET_DURATION + targetDuration);
			bw.newLine();
			bw.write("#EXT-X-MEDIA-SEQUENCE:0");
			bw.newLine();
			for (String s : lines) {
				bw.write(s);
				bw.newLine();
			}
			if (end) {
				bw.write("#EXT-X-ENDLIST");
				bw.newLine();
			}
		} catch (IOException e) {
			Trace.trace(Trace.ERROR, "Error writing HLS recording index " + indexFile, e);
			return;
		}

		try {
			Files.move(temp.toPath(), indexFile.toPath(), StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			Trace.trace(Trace.ERROR, "Error writing HLS recording index " + indexFile, e);
		}
	}

	@Override
	public String toString() {
		return indexFile.toString();
	}
}
