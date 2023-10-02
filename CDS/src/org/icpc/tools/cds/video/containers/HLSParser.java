package org.icpc.tools.cds.video.containers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * An HLS parser that is able to separate individual segments (files), provide a full list of
 * references files, output the identical file again, and do URL rewriting by adding prefixes to
 * relative URLs.
 */
public class HLSParser {
	// #EXT-X-MAP:URI="60aab25693f9_init.mp4"
	// 60aab25693f9_seg7.mp4
	// #EXT-X-PART:DURATION=0.20000,URI="60aab25693f9_part23.mp4"
	// #EXT-X-PRELOAD-HINT:TYPE=PART,URI="b6d064eaa487_part10.mp4"

	private static final String[] EMPTY = new String[0];

	protected String[] header;
	protected String[] footer;
	protected String[] footerParts;
	protected String init;
	protected List<String> preload = new ArrayList<>();

	protected String urlPrefix;

	class Segment {
		String[] comments;
		String[] parts;
		String file;
	}

	protected Segment[] playlist;

	public HLSParser() {
		// todo
	}

	public void setURLPrefix(String s) {
		urlPrefix = s;
	}

	private String[] getURI(String s) {
		int i = s.indexOf("URI=");
		if (i < 0)
			return null;
		int j = s.indexOf("\"", i + 5);
		if (j < 0)
			return null;

		String uri = s.substring(i + 5, j);
		String edit = s.substring(0, i + 5) + urlPrefix + s.substring(i + 5);
		return new String[] { uri, edit };
	}

	public void read(InputStream in) {
		BufferedReader br = null;

		try {
			br = new BufferedReader(new InputStreamReader(in));

			String s = br.readLine();
			List<String> buf = new ArrayList<String>();
			List<String> filebuf = new ArrayList<String>();
			List<Segment> segments = new ArrayList<Segment>();
			while (s != null) {
				if (s.startsWith("#EXT-X-STREAM-INF:") || s.startsWith("#EXTINF:")) {
					if (header == null) {
						header = buf.toArray(EMPTY);
						buf.clear();
					}

					buf.add(s);
				} else if (s.startsWith("#EXT")) {
					if (s.startsWith("#EXT-X-MAP:")) {
						String[] ss = getURI(s);
						init = ss[0];
						buf.add(ss[1]);
					} else if (s.startsWith("#EXT-X-PART:")) {
						String[] ss = getURI(s);
						filebuf.add(ss[0]);
						buf.add(ss[1]);
					} else if (s.startsWith("#EXT-X-PRELOAD-HINT:")) {
						String[] ss = getURI(s);
						preload.add(ss[0]);
						buf.add(ss[1]);
					} else
						buf.add(s);
				} else { // file
					Segment seg = new Segment();
					seg.comments = buf.toArray(EMPTY);
					seg.parts = filebuf.toArray(EMPTY);
					seg.file = s;
					segments.add(seg);
					buf.clear();
					filebuf.clear();
				}
				s = br.readLine();
			}

			br.close();
			footer = buf.toArray(EMPTY);
			footerParts = filebuf.toArray(EMPTY);
			playlist = segments.toArray(new Segment[0]);
		} catch (Exception e) {
			// todo
			e.printStackTrace();
		}
	}

	public List<String> files() {
		List<String> list = new ArrayList<String>();

		if (init != null)
			list.add(init);

		for (Segment s : playlist) {
			list.add(s.file);
			for (String ss : s.parts) {
				list.add(ss);
			}
			list.add(s.file);
		}

		if (footerParts != null && footerParts.length > 0) {
			for (String ss : footerParts) {
				list.add(ss);
			}
		}

		return list;
	}

	public List<String> getPreload() {
		return preload;
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
				for (Segment seg : playlist) {
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
			// todo
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		write(bout);
		return bout.toString();
	}

	public static void main(String[] args) {
		HLSParser parser = new HLSParser();
		InputStream in = parser.getClass().getResourceAsStream("HLSexample2.m3u8");
		System.out.println("Reading");
		parser.read(in);
		System.out.println("Writing");
		parser.write(System.out);
	}
}