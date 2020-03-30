package org.icpc.tools.cds.video;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.icpc.tools.cds.video.containers.MPEGTSHandler;
import org.icpc.tools.contest.Trace;

public class VideoAggregator {
	public static enum Status {
		UNKNOWN, FAILED, ACTIVE
	}

	public static enum ConnectionMode {
		LAZY, EAGER, LAZY_CLOSE
	}

	public static final int MAX_STREAMS = 300;
	public static final int MAX_CHANNELS = 20;

	public static class Stats {
		protected int concurrentListeners;
		protected int maxConcurrentListeners;
		protected int totalListeners;
		protected long totalTime;

		public void newListener() {
			concurrentListeners++;
			if (concurrentListeners > maxConcurrentListeners)
				maxConcurrentListeners = concurrentListeners;
			totalListeners++;

			if (this != stats) {
				stats.newListener();
			} else if (totalListeners % 5 == 0) {
				Trace.trace(Trace.INFO, "Video info [current:" + concurrentListeners + ", maxConcurrent:"
						+ maxConcurrentListeners + ", total:" + totalListeners + "]");
			}
		}

		public void dropListener(VideoStreamListener listener) {
			long startTime = listener.getStartTime();
			totalTime += System.currentTimeMillis() - startTime;
			concurrentListeners--;

			if (this != stats) {
				stats.dropListener(listener);
			}
		}
	}

	protected static Stats stats = new Stats();

	protected VideoHandler handler = new MPEGTSHandler();

	protected static VideoAggregator instance = new VideoAggregator();

	class Channel {
		int stream = -1;
		protected List<VideoStreamListener> listeners = new ArrayList<>(3);
	}

	private Channel[] channels = new Channel[MAX_CHANNELS];

	private List<VideoStream> videoStream = new ArrayList<>();

	protected ThreadPoolExecutor executor = null;

	public VideoAggregator() {
		super();

		for (int i = 0; i < MAX_CHANNELS; i++)
			channels[i] = new Channel();

		executor = new ThreadPoolExecutor(MAX_STREAMS + 2, MAX_STREAMS + 2, 15L, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(MAX_STREAMS + 2), new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r, "CDS Video Worker");
						t.setPriority(Thread.NORM_PRIORITY);
						t.setDaemon(true);
						return t;
					}
				});
	}

	public static boolean isRunning() {
		return instance != null;
	}

	public static VideoAggregator getInstance() {
		return instance;
	}

	public List<VideoStream> getVideoInfo() {
		return videoStream;
	}

	public int addReservation(String name, String url) {
		return addReservation(name, url, ConnectionMode.LAZY, 0);
	}

	public int addReservation(String name, String url, ConnectionMode mode, int order) {
		if (name == null)
			throw new IllegalArgumentException();

		VideoStream info = new VideoStream(this, name, url, order);
		info.setConnectionMode(mode);

		int numReserved = videoStream.size();
		videoStream.add(info);

		Trace.trace(Trace.INFO, "Video reservation for " + name + " at " + url + " on stream " + numReserved);

		return numReserved;
	}

	public int getChannel(int channel) {
		return channels[channel].stream;
	}

	public boolean setChannel(int channel, int stream) {
		if (channel < 0 || channel >= MAX_CHANNELS)
			return false;

		if (stream < 0 || stream >= videoStream.size())
			return false;

		Channel c = channels[channel];
		if (c.stream == stream)
			return false;

		int oldStream = c.stream;
		Trace.trace(Trace.INFO, "Channel " + channel + " changing from " + oldStream + " to " + stream);

		// remove old channel
		if (oldStream >= 0) {
			VideoStream oldInfo = videoStream.get(oldStream);
			for (VideoStreamListener vsl : c.listeners) {
				oldInfo.removeListener(vsl);
			}
		}

		VideoStream info = videoStream.get(stream);
		for (VideoStreamListener vsl : c.listeners) {
			info.addListener(vsl);
		}

		c.stream = stream;
		return true;
	}

	public void addChannelListener(int channel, VideoStreamListener listener) {
		if (channel < 0 || channel >= MAX_CHANNELS || listener == null)
			return;

		Channel c = channels[channel];
		c.listeners.add(listener);
		int stream = c.stream;
		Trace.trace(Trace.INFO, "Adding channel listener: " + channel + " Stream: " + stream);
		if (stream >= 0) {
			VideoStream info = videoStream.get(stream);
			info.addListener(listener);
		}
	}

	public String getStreamName(int stream) {
		if (stream < 0 || stream >= videoStream.size())
			return null;

		return videoStream.get(stream).getName();
	}

	public void addStreamListener(int stream, VideoStreamListener listener) {
		if (stream < 0 || stream >= videoStream.size() || listener == null)
			return;

		VideoStream info = videoStream.get(stream);
		info.addListener(listener);
	}

	public static ConnectionMode getConnectionMode(String s) {
		if ("eager".equalsIgnoreCase(s))
			return ConnectionMode.EAGER;
		else if ("lazy".equalsIgnoreCase(s))
			return ConnectionMode.LAZY;
		else if ("lazy_close".equalsIgnoreCase(s))
			return ConnectionMode.LAZY_CLOSE;
		return VideoAggregator.ConnectionMode.LAZY;
	}

	public ConnectionMode getConnectionMode(int stream) {
		if (stream < 0 || stream >= videoStream.size())
			return null;

		return videoStream.get(stream).getMode();
	}

	public void setConnectionMode(int stream, ConnectionMode mode) {
		if (stream < 0 || stream >= videoStream.size())
			return;

		VideoStream info = videoStream.get(stream);
		info.setConnectionMode(mode);
	}

	public boolean removeChannelListener(int channel, VideoStreamListener listener) {
		if (channel < 0 || channel >= MAX_CHANNELS || listener == null)
			return false;

		Channel c = channels[channel];
		int stream = c.stream;
		if (stream >= 0)
			videoStream.get(stream).removeListener(listener);
		return c.listeners.remove(listener);
	}

	public boolean removeStreamListener(int stream, VideoStreamListener listener) {
		if (stream < 0 || stream >= videoStream.size() || listener == null)
			return false;

		VideoStream info = videoStream.get(stream);
		return info.removeListener(listener);
	}

	/**
	 * Remove all listeners and close all streams.
	 */
	public void resetAll() {
		Trace.trace(Trace.INFO, "Disconnecting all stream listeners");

		for (int i = 0; i < videoStream.size(); i++) {
			reset(i);
		}
	}

	/**
	 * Remove all listeners and close a specific stream.
	 *
	 * @param stream
	 * @return
	 */
	public void reset(int stream) {
		if (stream < 0 || stream >= videoStream.size())
			return;

		for (int i = 0; i < MAX_CHANNELS; i++) {
			if (channels[i].stream == stream) {
				channels[i].listeners.clear();
			}
		}

		videoStream.get(stream).reset();
	}

	public int getConcurrent() {
		return stats.concurrentListeners;
	}

	public int getMaxConcurrent() {
		return stats.maxConcurrentListeners;
	}

	public int getTotal() {
		return stats.totalListeners;
	}

	public long getTotalTime() {
		return stats.totalTime;
	}

	public void dropUntrustedListeners() {
		Trace.trace(Trace.INFO, "Contest freeze! Cutting off unauthorized video feeds");
		if (!executor.isShutdown())
			executor.execute(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < videoStream.size(); i++) {
						videoStream.get(i).dropUntrustedListeners();
					}
				}
			});
	}

	public void shutdownNow() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < videoStream.size(); i++) {
					try {
						videoStream.get(i).shutdownNow();
					} catch (Exception e) {
						Trace.trace(Trace.ERROR, "Error stopping video threads", e);
					}
				}
			}
		});

		executor.shutdownNow();
	}
}