package org.icpc.tools.cds.video;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.icpc.tools.cds.video.VideoStream.StreamType;
import org.icpc.tools.cds.video.containers.FLVHandler;
import org.icpc.tools.cds.video.containers.HLSHandler;
import org.icpc.tools.cds.video.containers.MPEGTSHandler;
import org.icpc.tools.cds.video.containers.OggHandler;
import org.icpc.tools.contest.Trace;

public class VideoAggregator {
	public static enum Status {
		UNKNOWN, FAILED, ACTIVE
	}

	public static enum ConnectionMode {
		DIRECT, LAZY, EAGER, LAZY_CLOSE
	}

	private static final int MAX_STREAMS = 850; // 140 * 3 * 2 + 10
	public static final int MAX_CHANNELS = 20;

	public static class Stats {
		protected int currentListeners;
		protected int maxConcurrentListeners;
		protected int totalListeners;
		protected long totalTime;

		public void newListener() {
			currentListeners++;
			if (currentListeners > maxConcurrentListeners)
				maxConcurrentListeners = currentListeners;
			totalListeners++;

			if (this != stats) {
				stats.newListener();
			} else if (totalListeners % 5 == 0) {
				Trace.trace(Trace.INFO, this.toString());
			}
		}

		public void dropListener(VideoStreamListener listener) {
			long startTime = listener.getStartTime();
			totalTime += System.currentTimeMillis() - startTime;
			currentListeners--;

			if (this != stats) {
				stats.dropListener(listener);
			}
		}

		public int getCurrentListeners() {
			return currentListeners;
		}

		public int getTotalListeners() {
			return totalListeners;
		}

		public long getTotalTime() {
			return totalTime;
		}

		@Override
		public String toString() {
			return "Video stats [current:" + currentListeners + ", maxConcurrent:" + maxConcurrentListeners + ", total:"
					+ totalListeners + "]";
		}
	}

	protected static Stats stats = new Stats();

	protected static VideoHandler[] HANDLERS = { new HLSHandler(), new MPEGTSHandler(), new OggHandler(),
			new FLVHandler() };

	protected static VideoHandler handler;

	static {
		if ("hls".equalsIgnoreCase(System.getProperty("ICPC_VIDEO")))
			handler = HANDLERS[0];
		else if ("ogg".equalsIgnoreCase(System.getProperty("ICPC_VIDEO")))
			handler = HANDLERS[2];
		else if ("mpeg".equalsIgnoreCase(System.getProperty("ICPC_VIDEO")))
			handler = HANDLERS[1];
		else
			handler = HANDLERS[0];
	}

	protected static VideoAggregator instance = new VideoAggregator();

	class Channel {
		int stream = -1;
		protected List<VideoStreamListener> listeners = new ArrayList<>(3);
	}

	private Channel[] channels = new Channel[MAX_CHANNELS];

	private List<VideoStream> videoStreams = new ArrayList<>();

	private ThreadPoolExecutor executor = null;

	public VideoAggregator() {
		super();

		for (int i = 0; i < MAX_CHANNELS; i++)
			channels[i] = new Channel();
	}

	public static boolean isRunning() {
		return instance != null;
	}

	public static VideoAggregator getInstance() {
		return instance;
	}

	private ThreadPoolExecutor getExecutor() {
		if (executor != null)
			return executor;

		synchronized (this) {
			if (executor != null)
				return executor;

			// we only need a big thread pool when streaming video
			int size = 10;
			if (handler instanceof VideoStreamHandler)
				size = MAX_STREAMS + 2;
			System.out.println("Creating thread pool: " + size);
			executor = new ThreadPoolExecutor(size, size, 15L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(size),
					new ThreadFactory() {
						@Override
						public Thread newThread(Runnable r) {
							Thread t = new Thread(r, "CDS Video Worker");
							t.setPriority(Thread.NORM_PRIORITY);
							t.setDaemon(true);
							return t;
						}
					});
		}
		return executor;
	}

	public void execute(Runnable r) {
		getExecutor().execute(r);
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

		VideoStream stream = new VideoStream(name, url, type, teamId);
		stream.setConnectionMode(mode);

		int numReserved = videoStreams.size();
		videoStreams.add(stream);

		Trace.trace(Trace.INFO, "Video reservation for " + name + " at " + url + " on stream " + numReserved);

		return numReserved;
	}

	public int getNumStreams() {
		return videoStreams.size();
	}

	public int getChannel(int channel) {
		return channels[channel].stream;
	}

	public boolean setChannel(int channel, int stream) throws IOException {
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
		for (VideoStreamListener vsl : c.listeners)
			info.addListener(vsl);

		c.stream = stream;
		return true;
	}

	public void addChannelListener(int channel, VideoStreamListener listener) throws IOException {
		if (channel < 0 || channel >= MAX_CHANNELS || listener == null)
			return;

		Channel c = channels[channel];
		c.listeners.add(listener);
		int stream = c.stream;
		Trace.trace(Trace.INFO, "Adding channel listener: " + channel + " Stream: " + stream);
		if (stream >= 0)
			videoStreams.get(stream).addListener(listener);
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

	public static ConnectionMode getConnectionMode(String s) {
		if ("eager".equalsIgnoreCase(s))
			return ConnectionMode.EAGER;
		else if ("lazy".equalsIgnoreCase(s))
			return ConnectionMode.LAZY;
		else if ("lazy_close".equalsIgnoreCase(s))
			return ConnectionMode.LAZY_CLOSE;
		else if ("direct".equalsIgnoreCase(s))
			return ConnectionMode.DIRECT;
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

		VideoStream vs = videoStreams.get(stream);
		Trace.trace(Trace.INFO, "Connection mode: " + vs.getMode() + " -> " + mode);
		vs.setConnectionMode(mode);
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

		return videoStreams.get(stream).removeListener(listener);
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

		VideoStream vs = videoStreams.get(stream);
		Trace.trace(Trace.INFO, "Resetting stream: " + vs);
		vs.reset();
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
		return stats.currentListeners;
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
		if (executor != null && !executor.isShutdown())
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
		if (executor == null)
			return;

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