package org.icpc.tools.cds.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.icpc.tools.contest.Trace;

public class ContestFeedExecutor {
	interface Feed {
		public boolean doOutput();
	}

	private List<Feed> feeds = new ArrayList<>();
	private static ContestFeedExecutor instance;

	protected ContestFeedExecutor() {
		instance = this;
	}

	protected void start(ScheduledExecutorService executor) {
		executor.scheduleAtFixedRate(() -> output(), 5000, 750, TimeUnit.MILLISECONDS);
	}

	public void output() {
		notifyListeners();
	}

	public static ContestFeedExecutor getInstance() {
		return instance;
	}

	public void addFeedSource(Feed feed) {
		synchronized (feeds) {
			feeds.add(feed);
		}
	}

	public void removeListener(Feed feed) {
		synchronized (feeds) {
			feeds.remove(feed);
		}
	}

	private void notifyListeners() {
		Feed[] list = null;
		synchronized (feeds) {
			list = feeds.toArray(new Feed[0]);
		}

		for (Feed feed : list) {
			try {
				if (!feed.doOutput())
					removeListener(feed);
			} catch (Throwable t) {
				Trace.trace(Trace.ERROR, "Error notifying feeds", t);
			}
		}
	}
}