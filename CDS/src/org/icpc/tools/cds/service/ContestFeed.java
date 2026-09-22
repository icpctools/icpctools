package org.icpc.tools.cds.service;

import java.io.PrintWriter;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.icpc.tools.cds.service.ContestObjectQueue.ContestObjectDelta;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObjectFilter;
import org.icpc.tools.contest.model.feed.NDJSONFeedWriter;
import org.icpc.tools.contest.model.internal.Contest;

/**
 * Handles a single contest event-feed, along with scheduling the executor to send new events or a
 * heartbeat.
 * <p>
 * Threading: {@link #add(IContestObject, IContestListener.Delta)} is called on the contest's
 * listener thread, while {@link #sendEvents()} and {@link #sendHeartbeat()} run on executor pool
 * threads. Access to the scheduled futures is guarded by {@link #scheduleLock}, which is never
 * held while doing stream I/O so that the listener thread is not blocked behind a slow client.
 * Stream I/O (and the {@link #num} counter) is guarded by synchronizing on the feed instance
 * itself, so only one writer thread touches the stream at a time.
 */
public abstract class ContestFeed {
	private static final ScheduledExecutorService executor = ExecutorListener.getExecutor();

	protected final ContestObjectQueue queue;
	protected final Contest contest;
	protected final String prefix;
	protected int num;
	protected final IContestObjectFilter filter;
	protected final IContestListener listener;
	protected final PrintWriter printWriter;
	protected final NDJSONFeedWriter writer;

	private final Object scheduleLock = new Object();
	private ScheduledFuture<?> futureEvents;
	private ScheduledFuture<?> futureHeartbeat;
	private boolean shutdown;

	public ContestFeed(Contest contest, int num, PrintWriter printWriter, IContestObjectFilter filter) {
		this.contest = contest;
		this.num = num;

		queue = new ContestObjectQueue();
		prefix = NDJSONFeedWriter.getContestPrefix(contest);

		this.printWriter = printWriter;
		writer = new NDJSONFeedWriter(printWriter);

		this.filter = filter;

		scheduleHeartbeat();

		listener = (contest2, obj, d) -> add(obj, d);
	}

	public void startListening() {
		contest.addListenerAfterEvent(listener, num);
	}

	public void add(IContestObject obj, IContestListener.Delta d) {
		IContestObject obj2 = filter.filter(obj);
		if (obj2 == null) {
			// object is filtered from this feed, no change
			return;
		}

		queue.add(obj2, d);

		synchronized (scheduleLock) {
			if (shutdown)
				return;

			// cancel any upcoming heartbeat and make sure events are scheduled
			if (futureHeartbeat != null && !futureHeartbeat.isDone()) {
				futureHeartbeat.cancel(false);
				futureHeartbeat = null;
			}

			long time = futureEvents != null ? futureEvents.getDelay(TimeUnit.MILLISECONDS) : 0;
			if (time > 0) {
				// events are already scheduled to be sent
				return;
			}

			try {
				futureEvents = executor.schedule(() -> sendEvents(), 200, TimeUnit.MILLISECONDS);
			} catch (RejectedExecutionException e) {
				// executor is shutting down, nothing more to do
			}
		}
	}

	protected void scheduleHeartbeat() {
		synchronized (scheduleLock) {
			if (shutdown)
				return;

			long time = futureHeartbeat != null ? futureHeartbeat.getDelay(TimeUnit.SECONDS) : 0;
			if (time > 90) {
				// don't bother rescheduling
				return;
			}
			if (futureHeartbeat != null && !futureHeartbeat.isDone()) {
				futureHeartbeat.cancel(false);
				futureHeartbeat = null;
			}

			try {
				futureHeartbeat = executor.schedule(() -> sendHeartbeat(), 100, TimeUnit.SECONDS);
			} catch (RejectedExecutionException e) {
				// executor is shutting down, nothing more to do
			}
		}
	}

	protected synchronized void sendEvents() {
		try {
			boolean isDone = contest.isDoneUpdating();
			setupThread();

			ContestObjectDelta co = queue.poll();
			while (co != null) {
				writer.writeEvent(co.obj, prefix + num++, co.d);
				co = queue.poll();
			}
			if (isDone || printWriter.checkError()) {
				remove();
				return;
			}
			scheduleHeartbeat();
		} catch (Throwable t) {
			// failed to write to stream
			Trace.trace(Trace.WARNING, "Could not write to feed", t);
			remove();
		}
	}

	protected synchronized void sendHeartbeat() {
		try {
			writer.writeHeartbeat();
			if (printWriter.checkError()) {
				remove();
				return;
			}
			scheduleHeartbeat();
		} catch (Throwable t) {
			// failed to write to stream
			Trace.trace(Trace.WARNING, "Could not send heartbeat to feed", t);
			remove();
		}
	}

	protected synchronized void remove() {
		synchronized (scheduleLock) {
			if (shutdown)
				return;
			shutdown = true;

			// cancel anything scheduled
			if (futureEvents != null) {
				futureEvents.cancel(false);
				futureEvents = null;
			}
			if (futureHeartbeat != null) {
				futureHeartbeat.cancel(false);
				futureHeartbeat = null;
			}
		}

		contest.removeListener(listener);

		try {
			printWriter.close();
		} catch (Exception e) {
			// ignore
		}

		try {
			cleanup();
		} catch (Exception e) {
			// ignore
		}
	}

	protected abstract void setupThread();

	protected abstract void cleanup();
}
