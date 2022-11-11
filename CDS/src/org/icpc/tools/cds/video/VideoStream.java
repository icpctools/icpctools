package org.icpc.tools.cds.video;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.icpc.tools.cds.video.VideoAggregator.ConnectionMode;
import org.icpc.tools.cds.video.VideoAggregator.Stats;
import org.icpc.tools.cds.video.VideoAggregator.Status;
import org.icpc.tools.cds.video.VideoHandler.IStore;
import org.icpc.tools.cds.video.VideoHandler.IStreamListener;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;

public class VideoStream implements IStore {
	private final ThreadPoolExecutor executor;
	private final VideoHandler handler;
	private final Stats stats = new Stats();

	private interface ReadThread extends Runnable {
		void terminate();
	}

	private interface StreamOperation {
		void execute(VideoStreamListener listener) throws IOException;
	}

	public enum StreamType {
		DESKTOP, WEBCAM, AUDIO, OTHER
	}

	private String name;
	private StreamType type;
	private String teamId;
	private String url;
	private Status status = Status.UNKNOWN;
	private ConnectionMode mode = ConnectionMode.LAZY;
	private ReadThread thread;
	private List<VideoStreamListener> listeners = new ArrayList<>(3);

	public VideoStream(VideoAggregator videoAggregator, String name, String url, StreamType type, String teamId) {
		this.executor = videoAggregator.executor;
		this.handler = VideoAggregator.handler;
		this.name = name;
		this.url = url;
		this.type = type;
		this.teamId = teamId;
	}

	public String getName() {
		return name;
	}

	public String getURL() {
		return url;
	}

	public Status getStatus() {
		return status;
	}

	public StreamType getType() {
		return type;
	}

	public String getTeamId() {
		return teamId;
	}

	public Stats getStats() {
		return stats;
	}

	public ConnectionMode getMode() {
		return mode;
	}

	public int getConnections() {
		return listeners.size();
	}

	protected String getFileExtension() {
		return handler.getFileExtension();
	}

	public String getMimeType() {
		return handler.getMimeType();
	}

	private void writeHeader(VideoStreamListener listener) throws IOException {
		handler.writeHeader(this, new IStreamListener() {
			@Override
			public void write(final byte[] b) {
				sendToListener(listener, l -> listener.write(b));
			}

			@Override
			public void write(byte[] b, int off, int len) {
				sendToListener(listener, l -> listener.write(b, off, len));
			}

			@Override
			public void flush() {
				sendToListener(listener, l -> listener.flush());
			}

			@Override
			public boolean isDone() {
				return false;
			}
		});
	}

	/**
	 * Execute an action on all listeners.
	 *
	 * @param num
	 * @param b
	 */
	private void sendToListener(VideoStreamListener l, StreamOperation a) {
		try {
			a.execute(l);
		} catch (Throwable t) {
			// could not send. kill this feed
			removeListener(l);
		}
	}

	/**
	 * Execute an action on all listeners.
	 *
	 * @param num
	 * @param b
	 */
	private void sendToListeners(StreamOperation a) {
		VideoStreamListener[] list = null;
		synchronized (listeners) {
			List<VideoStreamListener> listenerList = listeners;
			if (listenerList == null || listenerList.isEmpty())
				return;

			list = listenerList.toArray(new VideoStreamListener[0]);
		}

		for (VideoStreamListener l : list) {
			try {
				a.execute(l);
			} catch (Throwable t) {
				// could not send. kill this feed
				removeListener(l);
			}
		}
	}

	/**
	 * Add a new listener, and output header information (if there is any for the current format).
	 *
	 * @param listener
	 * @throws IOException
	 */
	public void addListener(VideoStreamListener listener) throws IOException {
		if (listener == null)
			return;

		writeHeader(listener);

		boolean isListening = false;
		synchronized (listeners) {
			isListening = !listeners.isEmpty();
			listeners.add(listener);
		}

		stats.newListener();

		if (isListening || mode == ConnectionMode.EAGER)
			return;

		// we're the first - time to connect!
		startReadThread();
	}

	public boolean removeListener(VideoStreamListener listener) {
		boolean worked;
		boolean wasLastListener = false;

		synchronized (listeners) {
			if (listeners == null || listeners.isEmpty())
				return false;

			worked = listeners.remove(listener);

			if (worked)
				wasLastListener = listeners.isEmpty();
		}

		listener.close();

		if (wasLastListener) {
			Trace.trace(Trace.INFO, "Last listener removed, closing: " + name);
			if (mode == ConnectionMode.LAZY) {
				synchronized (this) {
					if (thread != null) {
						thread.terminate();
						thread = null;
					}
				}
			}
			object = null;
		}

		if (worked)
			stats.dropListener(listener);

		return worked;
	}

	public void reset() {
		synchronized (listeners) {
			for (VideoStreamListener listener : listeners) {
				listener.close();
				stats.dropListener(listener);
			}

			listeners.clear();
		}

		synchronized (this) {
			if (thread != null)
				thread.terminate();
			thread = null;
			status = Status.UNKNOWN;
		}

		if (mode == ConnectionMode.EAGER)
			startReadThread();
	}

	private void removeListeners() {
		VideoStreamListener[] list = null;
		synchronized (listeners) {
			if (listeners == null || listeners.isEmpty())
				return;

			list = listeners.toArray(new VideoStreamListener[0]);
		}

		for (VideoStreamListener l : list) {
			try {
				removeListener(l);
			} catch (Throwable t) {
				// ignore
			}
		}
	}

	public void dropUntrustedListeners() {
		// TODO sync lock issue List<VideoStreamListener>
		synchronized (listeners) {
			for (VideoStreamListener vsl : listeners) {
				if (!vsl.isAnalyst()) {
					removeListener(vsl);
				}
			}
		}
	}

	public void setConnectionMode(ConnectionMode newMode) {
		if (newMode == mode)
			return;

		if (newMode == ConnectionMode.LAZY && listeners.isEmpty())
			stopThread(false);

		if (newMode == ConnectionMode.EAGER) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					startReadThread();
					mode = newMode;
				}
			});
		} else
			mode = newMode;
	}

	private void startReadThread() {
		object = null;

		URL url2 = null;
		try {
			url2 = new URL(url);
		} catch (MalformedURLException e) {
			try {
				File sample = new File(url);
				if (sample.exists())
					url2 = sample.toURI().toURL();
				else
					throw e;
			} catch (Exception ex) {
				// ignore
				Trace.trace(Trace.ERROR, "Malformed video URL", ex);
				return;
			}
		}

		final URL url3 = url2;

		Trace.trace(Trace.INFO, "Starting video stream: " + name + " " + url3);

		if (thread != null)
			thread.terminate();

		thread = new ReadThread() {
			protected InputStream in;
			protected boolean done = false;
			protected int failures = 0;

			@Override
			public void run() {
				while (!done) {
					long time = System.currentTimeMillis();
					try {
						URLConnection conn = HTTPSSecurity.createURLConnection(url3, null, null);
						conn.setConnectTimeout(15000);
						conn.setReadTimeout(10000);
						conn.setRequestProperty("Content-Type", handler.getMimeType());
						if (conn instanceof HttpURLConnection) {
							HttpURLConnection httpConn = (HttpURLConnection) conn;
							int httpStatus = httpConn.getResponseCode();
							if (httpStatus == HttpURLConnection.HTTP_NOT_FOUND)
								throw new IOException("404 Not found");
							else if (httpStatus == HttpURLConnection.HTTP_UNAUTHORIZED)
								throw new IOException("Not authorized (HTTP response code 401)");
						}

						/*if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
								|| status == HttpURLConnection.HTTP_SEE_OTHER) {
							conn = createConnection(new URL(conn.getHeaderField("Location")));
							conn.setReadTimeout(10000);
							if (localFile.exists())
								conn.setIfModifiedSince(localTime);
							status = conn.getResponseCode();
						}*/

						in = conn.getInputStream();

						if (in.markSupported()) {
							in.mark(50);
							if (!handler.validate(in)) {
								// stream not configured correctly
								Trace.trace(Trace.WARNING, "Stream didn't validate, likely mis-configuration");
							}
							in.reset();
						}

						handler.createReader(in, VideoStream.this, new IStreamListener() {
							@Override
							public void write(final byte[] b) {
								status = Status.ACTIVE;
								failures = 0;
								if (!done)
									sendToListeners(listener -> listener.write(b));
							}

							@Override
							public void write(byte[] b, int off, int len) {
								status = Status.ACTIVE;
								failures = 0;
								if (!done)
									sendToListeners(listener -> listener.write(b, off, len));
							}

							@Override
							public void flush() {
								if (!done)
									sendToListeners(listener -> listener.flush());
							}

							@Override
							public boolean isDone() {
								return done;
							}
						});

						// end of stream
					} catch (Exception e) {
						status = Status.FAILED;
						Trace.trace(Trace.ERROR, "Could not connect to video " + name + ": " + e.getMessage());
						failures++;
					} finally {
						if (in != null)
							try {
								in.close();
							} catch (Exception e) {
								// ignore
							}
					}

					if (done)
						return;

					// do a flush (which may clean up disconnected clients)
					sendToListeners(listener -> listener.flush());

					if (failures > 4) {
						// can't connect after many attempts, drop all listeners and give up
						Trace.trace(Trace.INFO, "Can't connect, removing all listeners: " + name);
						removeListeners();
						return;
					}

					if ("file".equals(url3.getProtocol())) {
						// not perfect, but assume that test samples are about a minute long, aim to
						// send
						// one every 59s
						long dt = System.currentTimeMillis() - time;
						if (dt < 59000)
							try {
								Thread.sleep(59000 - dt);
							} catch (Exception ex) {
								// ignore
							}
						time = System.currentTimeMillis();
					} else {
						// wait 3s before trying to reconnect
						try {
							Thread.sleep(3000);
						} catch (Exception ex) {
							// ignore
						}
					}
				}
			}

			@Override
			public void terminate() {
				done = true;
				try {
					if (in != null)
						in.close();
				} catch (Exception e) {
					// ignore
				}
			}
		};
		executor.execute(thread);
	}

	public void shutdownNow() {
		stopThread(true);
	}

	private void stopThread(boolean force) {
		synchronized (listeners) {
			if (!force && !listeners.isEmpty())
				return;
		}

		synchronized (this) {
			if (thread != null) {
				Trace.trace(Trace.INFO, "Stopping thread " + name);
				thread.terminate();
				thread = null;
			}
			status = Status.UNKNOWN;
		}
	}

	protected Object object;

	@Override
	public void setObject(Object obj) {
		this.object = obj;
	}

	@Override
	public Object getObject() {
		return object;
	}
}