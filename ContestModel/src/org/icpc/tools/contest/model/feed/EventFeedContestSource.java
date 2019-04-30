package org.icpc.tools.contest.model.feed;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.internal.Contest;

/**
 * A contest source that reads from a Contest Data Package exploded on disk.
 */
public class EventFeedContestSource extends ContestSource {
	private final File root;

	public EventFeedContestSource(File file) {
		root = file;

		if (root == null || !root.exists() || root.isDirectory())
			throw new IllegalArgumentException("File must be a valid event feed");
		instance = this;
	}

	public EventFeedContestSource(String file) {
		this(new File(file));
	}

	public File getFile() {
		return root;
	}

	public static Contest loadContest(File file, IContestListener listener) throws Exception {
		EventFeedContestSource source = new EventFeedContestSource(file);
		return source.loadContest(listener);
	}

	@Override
	protected void initializeContestImpl() throws Exception {
		File file = getFile();
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			notifyListeners(ConnectionState.CONNECTED);
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Could not read event feed", e);
			throw e;
		}
		try {
			if (file.getName().endsWith("json")) {
				NDJSONFeedParser parser = new NDJSONFeedParser();
				parser.parse(contest, in);
			} else {
				XMLFeedParser parser = new XMLFeedParser();
				parser.parse(contest, in);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (Exception ex) {
				// ignore
			}
		}
	}

	@Override
	public String toString() {
		return "FileContestSource[" + root + "]";
	}
}