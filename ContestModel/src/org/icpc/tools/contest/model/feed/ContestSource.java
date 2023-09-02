package org.icpc.tools.contest.model.feed;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
 *
 * The two primary contest sources (DiskContestSource and RESTContestSource) support five different
 * ways to load contest data:
 *
 * [1] A folder on disk following the Contest Archive Format.
 *
 * [2] A stand-alone event feed file, either JSON or XML.
 *
 * [3] A REST contest source with local caching in temp.
 *
 * [4] A REST contest feed with a local backing folder.
 *
 * [5] A local file or folder with ability to pull from REST resource references.
 *
 * A corresponding folder is usually created in temp to cache information and improve performance.
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
		void stateChanged(ConnectionState state);
	}

	public static ContestSource getInstance() {
		return instance;
	}

	protected ContestSource() {
		// instance = this;
	}

	/**
	 * Convenience method to obtain a contest source from three parameters: url [user] [password],
	 * folder, or file [user] [password].
	 *
	 * @param source
	 * @param user
	 * @param password
	 * @return
	 * @throws IOException
	 */
	public static ContestSource parseSource(String source, String user, String password) throws IOException {
		if (source == null)
			throw new IOException("No contest source");

		if (source.startsWith("http")) {
			String source2 = ContestAPIHelper.validateContestURL(source, user, password);
			return new RESTContestSource(source2, user, password);
		}

		File f = new File(source);
		if (f.exists())
			return new RESTContestSource(f, user, password);

		throw new IOException("Could not parse or resolve contest source");
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
			// try to connect right away, then add some incremental back-off
			private int[] DELAY = new int[] { 2, 5, 20 };

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
				boolean reconnect = (ContestSource.this instanceof RESTContestSource);

				final IContestListener readListener = new IContestListener() {
					@Override
					public void contestChanged(IContest contest2, IContestObject obj, Delta delta) {
						notifyListeners(ConnectionState.READING);
						contest.removeListener(this);
					}
				};
				contest.addListener(readListener);
				int i = 0;
				do {
					try {
						notifyListeners(ConnectionState.CONNECTING);
						loadContestImpl();
						if (reconnect && contest.isDoneUpdating())
							reconnect = false; // normal termination - contest is done updating
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
							Thread.sleep(1000 * DELAY[i]);
						} catch (Exception e) {
							// ignore
						}
						while (i < DELAY.length - 1)
							i++;
					} else {
						Trace.trace(Trace.INFO, "Contest loaded");
						notifyListeners(ConnectionState.COMPLETE);
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
		return waitForContest(new Ready() {
			@Override
			public boolean isReady(ConnectionState state) {
				return state == ConnectionState.READING;
			}
		}, 2000);
	}

	/**
	 * Wait for the contest to be loading.
	 *
	 * @return <code>true</code> if the contest is or was successfully loaded, and
	 *         <code>false</code> otherwise
	 */
	public boolean waitForContestLoad() {
		return waitForContest(new Ready() {
			@Override
			public boolean isReady(ConnectionState state) {
				return state == ConnectionState.CONNECTED || state == ConnectionState.READING;
			}
		}, 2000);
	}

	/**
	 * Wait for the contest to be loaded with the given timeout in ms. The method will return if the
	 * timeout has been hit, if the contest has been completely read (state = done updating), or if
	 * 2s pass with no new events.
	 *
	 * @param timeout a timeout in ms
	 * @return <code>true</code> if the contest was successfully loaded, and <code>false</code>
	 *         otherwise
	 */
	public boolean waitForContest(int timeout) {
		long endTime = System.currentTimeMillis() + timeout;
		boolean b = waitForContestLoad();
		if (!b)
			return false;

		int last = -1;
		int count = 0;
		int n = getContest().getNumObjects();
		while (count < 3 && !contest.isDoneUpdating()) {
			if (System.currentTimeMillis() > endTime)
				return false;

			if (last == n)
				count++;
			else
				count = 0;
			last = n;
			try {
				Thread.sleep(500);
			} catch (Exception e) {
				// ignore
			}

			n = getContest().getNumObjects();
		}
		return true;
	}

	interface Ready {
		public boolean isReady(ConnectionState state);
	}

	private boolean waitForContest(Ready r, int timeout) {
		long time = System.currentTimeMillis();
		synchronized (lock) {
			while (true) {
				long dt = time + timeout - System.currentTimeMillis();
				if (dt <= 0)
					return false;

				if (lastState == ConnectionState.COMPLETE || lastState == ConnectionState.FAILED
						|| (r != null && r.isReady(lastState)))
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

	/**
	 * Set the contest scoreboard thaw time to null or a value in ms since the epoch.
	 *
	 * @throws IOException
	 */
	public void setContestThawTime(Long time) throws IOException {
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

	/**
	 * Returns the current connection state.
	 *
	 * @return the connection state
	 */
	public ConnectionState getConnectionState() {
		return lastState;
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