package org.icpc.tools.contest.model.feed;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.FileReference;

/**
 * A generic interface for sources of contest data, either file, URL, or other.
 */
public abstract class ContestSource {
	protected static ContestSource instance;

	private static final Object lock = new Object();
	private static final Object createLock = new Object();
	protected Contest contest;
	private ConnectionState lastState;
	private List<ContestSourceListener> listeners = new ArrayList<>(2);
	private Contest initialContest;

	public enum ConnectionState {
		INITIALIZING, INITIALZED, CONNECTING, CONNECTED, READING, RECONNECTING, FAILED, COMPLETE
	}

	private static final String[] STATE_LABELS = new String[] { "Initializing", "Configuration loaded", "Connecting",
			"Connected", "Reading", "Reconnecting", "Failed", "Complete" };

	public static String getStateLabel(ConnectionState state) {
		if (state == null)
			return "";
		return STATE_LABELS[state.ordinal()];
	}

	public interface ContestSourceListener {
		public void stateChanged(ConnectionState state);
	}

	public static ContestSource getInstance() {
		return instance;
	}

	protected ContestSource() {
		// instance = this;
	}

	public static ContestSource parseSource(String source, String arg1, String arg2) throws IOException {
		if (source == null)
			throw new IOException("No contest source");

		try {
			URL url = new URL(source);
			if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol()))
				return new RESTContestSource(url, arg1, arg2);
		} catch (Exception e) {
			// could not parse as a url, ignore
		}

		if (source.equals("ccs")) {
			try {
				return new CCSContestSource(arg1, Integer.parseInt(arg2));
			} catch (Exception e) {
				// ignore, try file instead
			}
		}
		File f = new File(source);
		if (f.exists()) {
			if (f.isDirectory())
				return new DiskContestSource(f);

			return new EventFeedContestSource(source);
		}

		throw new IOException("Could not parse or resolve contest source");
	}

	public static ContestSource[] parseMultiSource(String source, String arg1, String arg2) throws IOException {
		if (source == null)
			throw new IOException("No contest source");

		String[] ss = source.split("&");
		ContestSource[] cs = new ContestSource[ss.length];
		for (int i = 0; i < ss.length; i++)
			cs[i] = parseSource(ss[i], arg1, arg2);

		return cs;
	}

	public static ContestSource parseSource(String source) throws IOException {
		return parseSource(source, null, null);
	}

	public static ContestSource[] parseMultiSource(String source) throws IOException {
		return parseMultiSource(source, null, null);
	}

	public String getContestId() {
		return contest.getId();
	}

	/**
	 * Returns files from within the contest root, e.g. /submissions/15/files.
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public File getFile(String path) throws IOException {
		return null;
	}

	/**
	 * Returns files from within the contest root, e.g. /submissions/15/files.
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public File getFile(IContestObject obj, FileReference ref, String property) throws IOException {
		return null;
	}

	/**
	 * @throws IOException
	 */
	public String[] getDirectory(String path) throws IOException {
		return null;
	}

	public Contest loadContest(IContestListener listener) {
		if (contest != null) {
			if (listener != null)
				contest.addListener(listener);
			return contest;
		}

		synchronized (createLock) {
			if (contest != null) {
				if (listener != null)
					contest.addListener(listener);
				return contest;
			}

			if (initialContest != null)
				contest = initialContest;
			else
				contest = new Contest();

			if (listener != null)
				contest.addListener(listener);

			loadContestInBackground();

			waitForContestRead();
		}

		return contest;
	}

	public Contest getContest() {
		if (contest != null)
			return contest;

		return loadContest(null);
	}

	private void loadContestInBackground() {
		notifyListeners(ConnectionState.INITIALIZING);

		Thread thread = new Thread("Contest loader") {
			@Override
			public void run() {
				Trace.trace(Trace.INFO, "Loading contest from " + ContestSource.this.toString());
				try {
					initializeContestImpl();
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error initializing contest: " + e.getMessage(), e);
					notifyListeners(ConnectionState.FAILED);
					return;
				}
				notifyListeners(ConnectionState.INITIALZED);
				boolean reconnect = (ContestSource.this instanceof CCSContestSource)
						|| (ContestSource.this instanceof RESTContestSource);

				final IContestListener readListener = new IContestListener() {
					@Override
					public void contestChanged(IContest contest2, IContestObject obj, Delta delta) {
						notifyListeners(ConnectionState.READING);
						contest.removeListener(this);
					}
				};
				contest.addListener(readListener);
				do {
					try {
						// Trace.trace(Trace.INFO, "Loading contest from " +
						// ContestSource.this.toString());
						notifyListeners(ConnectionState.CONNECTING);
						loadContestImpl();
						reconnect = false; // normal termination - contest is finalized
						Trace.trace(Trace.INFO, "Contest loaded");
						notifyListeners(ConnectionState.COMPLETE);
					} catch (InterruptedException e) {
						// ignore
					} catch (Exception e) {
						Trace.trace(Trace.ERROR, "Error reading event feed: " + e.getMessage());
						if (!reconnect)
							notifyListeners(ConnectionState.FAILED);
					}

					if (reconnect) {
						notifyListeners(ConnectionState.RECONNECTING);
						Trace.trace(Trace.INFO, "Waiting to reconnect");
						// wait a few seconds to reconnect
						try {
							Thread.sleep(4000);
						} catch (Exception e) {
							// ignore
						}
					}
				} while (reconnect);
				// thread = null;
			}
		};
		thread.setPriority(Thread.NORM_PRIORITY + 1);
		thread.setDaemon(true);
		thread.start();
	}

	protected void initializeContestImpl() throws Exception {
		// do nothing
	}

	protected void loadContestImpl() throws Exception {
		// do nothing
	}

	public void close() throws Exception {
		// do nothing
	}

	/**
	 * Wait for the contest to be reading from a live source, or done/failed from any other source.
	 *
	 * @return <code>true</code> if the contest is or was successfully loaded, and
	 *         <code>false</code> otherwise
	 */
	public boolean waitForContestRead() {
		final int timeout = 2000;
		long time = System.currentTimeMillis();
		synchronized (lock) {
			while (true) {
				long dt = time + timeout - System.currentTimeMillis();
				if (dt <= 0)
					return false;

				if (lastState == ConnectionState.READING || lastState == ConnectionState.COMPLETE
						|| lastState == ConnectionState.FAILED)
					return true;

				try {
					lock.wait(dt);
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Wait for the contest to be loading.
	 *
	 * @return <code>true</code> if the contest is or was successfully loaded, and
	 *         <code>false</code> otherwise
	 */
	public boolean waitForContestLoad() {
		final int timeout = 2000;
		long time = System.currentTimeMillis();
		synchronized (lock) {
			while (true) {
				long dt = time + timeout - System.currentTimeMillis();
				if (dt <= 0)
					return false;

				if (lastState == ConnectionState.CONNECTED || lastState == ConnectionState.COMPLETE
						|| lastState == ConnectionState.FAILED)
					return true;

				try {
					lock.wait(dt);
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Wait for the contest to be loaded with the given timeout in ms.
	 *
	 * @param timeout a timeout in ms
	 * @return <code>true</code> if the contest was successfully loaded, and <code>false</code>
	 *         otherwise
	 */
	public boolean waitForContest(int timeout) {
		long time = System.currentTimeMillis();
		synchronized (lock) {
			while (true) {
				long dt = time + timeout - System.currentTimeMillis();
				if (dt <= 0)
					return false;

				if (lastState == ConnectionState.COMPLETE || lastState == ConnectionState.FAILED)
					return true;

				try {
					lock.wait(dt);
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	protected static void downloadAndCache(InputStream in, File file) throws IOException {
		if (file == null || in == null)
			throw new IOException("No input stream");

		// create folder
		File folder = file.getParentFile();
		if (!folder.exists())
			folder.mkdirs();

		File tempFile = null;
		BufferedInputStream bin = null;
		BufferedOutputStream bout = null;
		try {
			bin = new BufferedInputStream(in);
			tempFile = File.createTempFile("cache-", null, folder);
			bout = new BufferedOutputStream(new FileOutputStream(tempFile));

			byte[] b = new byte[1024 * 8];
			int n = bin.read(b);
			while (n != -1) {
				bout.write(b, 0, n);
				n = bin.read(b);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if (bin != null) {
				try {
					bin.close();
				} catch (Exception e) {
					// ignore
				}
			}
			if (bout != null) {
				try {
					bout.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		if (tempFile != null && tempFile.exists())
			synchronized (lock) {
				if (file.exists())
					tempFile.delete();
				else
					tempFile.renameTo(file);
			}
	}

	/**
	 * Set the contest start time to null or a value in ms since the epoch.
	 *
	 * @throws IOException
	 */
	public void setStartTime(Long time) throws IOException {
		// ignore
	}

	public void addListener(ContestSourceListener listener) {
		listeners.add(listener);
	}

	public void removeListener(ContestSourceListener listener) {
		listeners.remove(listener);
	}

	protected void notifyListeners(ConnectionState state) {
		lastState = state;
		for (ContestSourceListener l : listeners)
			l.stateChanged(state);

		synchronized (lock) {
			lock.notifyAll();
		}
	}

	public void setInitialContest(Contest c) {
		if (contest != null)
			return;
		initialContest = c;
	}

	public static class Validation {
		public boolean isValid = true;
		public List<String> messages = new ArrayList<>();

		public void ok(String m) {
			messages.add(m);
		}

		public void err(String m) {
			isValid = false;
			messages.add(m);
		}
	}

	public Validation validate() {
		return new Validation();
	}

	public void outputValidation() {
		Validation v = validate();
		if (v.isValid) {
			Trace.trace(Trace.INFO, "Contest source verified");
		} else {
			for (String m : v.messages) {
				Trace.trace(Trace.ERROR, m);
			}
			System.exit(1);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}