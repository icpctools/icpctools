package org.icpc.tools.cds.video.containers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.cds.video.containers.HLSFileCache.CachedFile;
import org.icpc.tools.contest.Trace;

/**
 * An HLS parser that is able to separate individual segments (files), provide a full list of
 * references files, output the identical file again, and do URL rewriting by adding prefixes to
 * relative URLs.
 *
 * https://datatracker.ietf.org/doc/html/draft-pantos-hls-rfc8216bis
 */
public class HLSParser {
	private static final String EXT = "#EXT";
	private static final String VERSION = "#EXT-X-VERSION:";
	private static final String X_GAP = "#EXT-X-GAP";
	private static final String X_MAP = "#EXT-X-MAP:";
	private static final String X_STREAM_INF = "#EXT-X-STREAM-INF:";
	private static final String X_BYTE_RANGE = "#EXT-X-BYTERANGE:";

	private static final String INF = "#EXTINF:";
	private static final String X_PART = "#EXT-X-PART:";

	private static final String PRELOAD_HINT = "#EXT-X-PRELOAD-HINT:";

	// #EXT-X-MAP:URI="60aab25693f9_init.mp4",BYTERANGE="720@0"
	// 60aab25693f9_seg7.mp4
	// #EXT-X-PART:DURATION=0.20000,URI="60aab25693f9_part23.mp4"
	// #EXT-X-PRELOAD-HINT:TYPE=PART,URI="b6d064eaa487_part10.mp4"
	// #EXT-X-BYTERANGE:2258821@2268117

	private static final String[] EMPTY = new String[0];

	protected URI uri;
	protected String version;
	protected String[] header;
	protected String[] footer;
	protected String[] footerParts;
	protected HLSSegment map;
	protected List<String> preload = new ArrayList<>();
	protected long readTime;

	protected String urlPrefix;

	protected HLSSegment[] playlist;

	public HLSParser(URI uri, String prefix) {
		this.uri = uri;
		urlPrefix = prefix;
	}

	public URI getURI() {
		return uri;
	}

	private String[] getURI(String s) {
		int i = s.indexOf("URI=");
		if (i < 0)
			return null;
		int j = s.indexOf("\"", i + 5);
		if (j < 0)
			return null;

		String uri2 = s.substring(i + 5, j);
		String edit = s.substring(0, i + 5) + urlPrefix + s.substring(i + 5);
		return new String[] { uri2, edit };
	}

	/**
	 * Returns the value of the quoted property from with the string. For instance, given the input
	 * string '...,x="y",...' and key 'x', it will return 'y'.
	 *
	 * @param s a string
	 * @param key a property key
	 * @return the value of the property, or null if it was not in the string
	 */
	private static String getValue(String s, String key) {
		int i = s.indexOf(key + "=");
		if (i < 0)
			return null;
		int j = s.indexOf("\"", i + key.length() + 2);
		if (j < 0)
			return null;

		return s.substring(i + key.length() + 2, j);
	}

	/**
	 * Parses a byterange string from the spec form "<length>[@<start>]" into an int array with
	 * [length, start]. e.g. input "1024@512" -> output [1024, 512]
	 *
	 * @param a byterange string
	 * @return an integer array containing the length and start
	 */
	private static int[] parseByteRange(String s) {
		if (s == null || s.length() == 0)
			return null;

		int[] br = new int[2];
		try {
			int ind = s.indexOf("@");
			if (ind < 0) {
				br[0] = Integer.parseInt(s);
			} else {
				br[0] = Integer.parseInt(s.substring(0, ind));
				br[1] = Integer.parseInt(s.substring(ind + 1, s.length()));
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not parse byterange " + s, e);
		}
		return br;
	}

	public void read(InputStream in) {
		BufferedReader br = null;

		try {
			br = new BufferedReader(new InputStreamReader(in));

			String s = br.readLine();
			List<String> buf = new ArrayList<String>();
			List<String> filebuf = new ArrayList<String>();
			List<HLSSegment> segments = new ArrayList<HLSSegment>();
			boolean gap = false;
			int[] byterange = null;
			while (s != null) {
				if (s.startsWith(X_STREAM_INF) || s.startsWith(INF)) {
					if (header == null) {
						header = buf.toArray(EMPTY);
						buf.clear();
					}
					byterange = null;

					buf.add(s);
				} else if (s.startsWith(EXT)) {
					if (s.startsWith(X_MAP)) {
						String[] ss = getURI(s);
						HLSSegment seg = new HLSSegment();
						seg.file = ss[0];
						seg.byterange = parseByteRange(getValue(s, "BYTERANGE"));
						map = seg;
						buf.add(ss[1]);
					} else if (s.startsWith(X_PART)) {
						String[] ss = getURI(s);
						filebuf.add(ss[0]);
						buf.add(ss[1]);
					} else if (s.startsWith(PRELOAD_HINT)) {
						String[] ss = getURI(s);
						preload.add(ss[0]);
						buf.add(ss[1]);
					} else if (s.equals(X_GAP)) {
						gap = true;
						buf.add(s);
					} else if (s.startsWith(X_BYTE_RANGE)) {
						byterange = parseByteRange(s.substring(X_BYTE_RANGE.length()));
						buf.add(s);
					} else if (s.equals(VERSION)) {
						version = s.substring(VERSION.length());
						buf.add(s);
					} else
						buf.add(s);
				} else { // file
					HLSSegment seg = new HLSSegment();
					seg.comments = buf.toArray(EMPTY);
					seg.parts = filebuf.toArray(EMPTY);
					seg.file = s;
					seg.gap = gap;
					seg.byterange = byterange;
					segments.add(seg);
					buf.clear();
					filebuf.clear();
					gap = false;
				}
				s = br.readLine();
			}

			br.close();
			footer = buf.toArray(EMPTY);
			footerParts = filebuf.toArray(EMPTY);
			playlist = segments.toArray(new HLSSegment[0]);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error parsing HLS index", e);
		}

		readTime = System.currentTimeMillis();
	}

	public void prefillCache(HLSFileCache fileCache) {
		// add all files to the cache list, but download only the map, footer, and up to 2 files
		if (map != null) {
			CachedFile cf = fileCache.addToCache(map.file, map.byterange, true);
			cf.keep = true;
		}

		boolean first = true;
		for (HLSSegment s : playlist) {
			if (s.gap)
				continue;

			fileCache.addToCache(s.file, s.byterange, first);
			first = false;
			for (String ss : s.parts) {
				fileCache.addToCache(ss, null, false);
			}
		}

		if (footerParts != null && footerParts.length > 0) {
			for (String ss : footerParts) {
				fileCache.addToCache(ss, null, false);
			}
		}

		for (String s : preload) {
			fileCache.addToCache(s, null, false);
		}
	}

	protected static boolean byterangeMatches(int[] a, int[] b) {
		return ((a == null && b == null) || (a != null && b != null && a[0] == b[0] && a[1] == b[1]));
	}

	public void write(OutputStream out) {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(out));

			if (header != null) {
				for (String s : header) {
					bw.write(s);
					bw.newLine();
				}
			}

			if (playlist != null) {
				for (HLSSegment seg : playlist) {
					for (String s : seg.comments) {
						bw.write(s);
						bw.newLine();
					}

					if (urlPrefix != null)
						bw.write(urlPrefix + seg.file);
					else
						bw.write(seg.file);
					bw.newLine();
				}
			}

			if (footer != null) {
				for (String s : footer) {
					bw.write(s);
					bw.newLine();
				}
			}

			bw.close();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error writing HLS index", e);
		}
	}

	@Override
	public String toString() {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		write(bout);
		return bout.toString();
	}

	public static void main(String[] args) {
		HLSParser parser = new HLSParser(URI.create("/"), null);
		InputStream in = parser.getClass().getResourceAsStream("HLSexample2.m3u8");
		System.out.println("Reading");
		parser.read(in);
		System.out.println("Writing");
		parser.write(System.out);
	}
}