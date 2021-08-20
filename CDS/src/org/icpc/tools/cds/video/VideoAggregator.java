package org.icpc.tools.cds.video;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.icpc.tools.cds.video.VideoStream.StreamType;
import org.icpc.tools.cds.video.containers.MPEGTSHandler;
import org.icpc.tools.contest.Trace;

public class VideoAggregator {
	public static enum Status {
		UNKNOWN, FAILED, ACTIVE
	}

	public static enum ConnectionMode {
		LAZY, EAGER, LAZY_CLOSE
	}

	public static final int MAX_STREAMS = 850; // 140 * 3 * 2 + 10
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

	private List<VideoStream> videoStreams = new ArrayList<>();

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
		return videoStreams;
	}

	public int addReservation(String name, String url, StreamType type, ConnectionMode mode) {
		return addReservation(name, url, mode, type, null);
	}

	public int addReservation(String name, String url, ConnectionMode mode, StreamType type, String teamId) {
		if (name == null)
			throw new IllegalArgumentException();

		VideoStream stream = new VideoStream(this, name, url, type, teamId);
		stream.setConnectionMode(mode);

		int numReserved = videoStreams.size();
		videoStreams.add(stream);

		Trace.trace(Trace.INFO, "Video reservation for " + name + " at " + url + " on stream " + numReserved);

		return numReserved;
	}

	public int getChannel(int channel) {
		return channels[channel].stream;
	}

	public boolean setChannel(int channel, int stream) {
		if (channel < 0 || channel >= MAX_CHANNELS)
			return false;

		if (stream < 0 || stream >= videoStreams.size())
			return false;

		Channel c = channels[channel];
		if (c.stream == stream)
			return false;

		int oldStream = c.stream;
		Trace.trace(Trace.INFO, "Channel " + channel + " changing from " + oldStream + " to " + stream);

		// remove old channel
		if (oldStream >= 0) {
			VideoStream oldInfo = videoStreams.get(oldStream);
			for (VideoStreamListener vsl : c.listeners) {
				oldInfo.removeListener(vsl);
			}
		}

		VideoStream info = videoStreams.get(stream);
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
			VideoStream info = videoStreams.get(stream);
			info.addListener(listener);
		}
	}

	public VideoStream getStream(int stream) {
		if (stream < 0 || stream >= videoStreams.size())
			return null;

		return videoStreams.get(stream);
	}

	public String getStreamName(int stream) {
		if (stream < 0 || stream >= videoStreams.size())
			return null;

		return videoStreams.get(stream).getName();
	}

	public StreamType getStreamType(int stream) {
		if (stream < 0 || stream >= videoStreams.size())
			return null;

		return videoStreams.get(stream).getType();
	}

	public void addStreamListener(int stream, VideoStreamListener listener) {
		if (stream < 0 || stream >= videoStreams.size() || listener == null)
			return;

		VideoStream info = videoStreams.get(stream);
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
		if (stream < 0 || stream >= videoStreams.size())
			return null;

		return videoStreams.get(stream).getMode();
	}

	public void setConnectionMode(int stream, ConnectionMode mode) {
		if (stream < 0 || stream >= videoStreams.size())
			return;

		VideoStream info = videoStreams.get(stream);
		info.setConnectionMode(mode);
	}

	public boolean removeChannelListener(int channel, VideoStreamListener listener) {
		if (channel < 0 || channel >= MAX_CHANNELS || listener == null)
			return false;

		Channel c = channels[channel];
		int stream = c.stream;
		if (stream >= 0)
			videoStreams.get(stream).removeListener(listener);
		return c.listeners.remove(listener);
	}

	public boolean removeStreamListener(int stream, VideoStreamListener listener) {
		if (stream < 0 || stream >= videoStreams.size() || listener == null)
			return false;

		VideoStream info = videoStreams.get(stream);
		return info.removeListener(listener);
	}

	/**
	 * Remove all listeners and close all streams.
	 */
	public void resetAll() {
		Trace.trace(Trace.INFO, "Disconnecting all stream listeners");

		for (int i = 0; i < videoStreams.size(); i++) {
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
		if (stream < 0 || stream >= videoStreams.size())
			return;

		for (int i = 0; i < MAX_CHANNELS; i++) {
			if (channels[i].stream == stream) {
				channels[i].listeners.clear();
			}
		}

		videoStreams.get(stream).reset();
	}

	public void reset(String teamId, StreamType type) {
		Trace.trace(Trace.INFO, "Resetting streams " + teamId + ", " + type);

		for (VideoStream stream : videoStreams) {
			if (teamId != null && !teamId.equals(stream.getTeamId()))
				continue;
			if (type != null && type != stream.getType())
				continue;
			stream.reset();
		}
	}

	public void setConnectionMode(String teamId, StreamType type, ConnectionMode mode) {
		Trace.trace(Trace.INFO, "Setting stream connection mode: " + type + ", " + mode);

		for (VideoStream stream : videoStreams) {
			if (teamId != null && !teamId.equals(stream.getTeamId()))
				continue;
			if (type != null && type != stream.getType())
				continue;
			stream.setConnectionMode(mode);
		}
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
					for (int i = 0; i < videoStreams.size(); i++) {
						videoStreams.get(i).dropUntrustedListeners();
					}
				}
			});
	}

	public void shutdownNow() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < videoStreams.size(); i++) {
					try {
						videoStreams.get(i).shutdownNow();
					} catch (Exception e) {
						Trace.trace(Trace.ERROR, "Error stopping video threads", e);
					}
				}
			}
		});

		executor.shutdownNow();
	}
}