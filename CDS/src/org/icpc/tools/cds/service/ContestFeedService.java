package org.icpc.tools.cds.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.service.ContestFeedExecutor.Feed;
import org.icpc.tools.cds.service.ContestObjectQueue.ContestObjectDelta;
import org.icpc.tools.cds.util.HttpHelper;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IContestObjectFilter;
import org.icpc.tools.contest.model.TypeFilter;
import org.icpc.tools.contest.model.feed.DiskContestSource;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.NDJSONFeedWriter;
import org.icpc.tools.contest.model.internal.Contest;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ContestFeedService {
	protected static void doStream(HttpServletRequest request, IContestObjectFilter filter, PrintWriter writer2,
			Contest contest, int ind, ConfiguredContest cc) {
		NDJSONFeedWriter writer = new NDJSONFeedWriter(writer2);

		final ContestObjectQueue queue = new ContestObjectQueue(ind);
		IContestListener listener = (contest2, obj, d) -> queue.add(obj, d);
		contest.addListenerFromStart(listener);
		final String prefix = NDJSONFeedWriter.getContestPrefix(contest);

		final AsyncContext asyncCtx = request.startAsync();
		asyncCtx.setTimeout(0); // no timeout
		cc.add(asyncCtx);

		final IAccount account = HttpHelper.getAccountFromRequest(request);
		final String accountToken = account != null ? HttpHelper.getAccountToken(account) : null;
		final String threadHost = "https://" + request.getServerName() + ":" + request.getServerPort();

		ContestFeedExecutor.getInstance().addFeedSource(new Feed() {
			protected long lastActivityTime = System.currentTimeMillis();
			protected int ind3 = ind;

			@Override
			public synchronized boolean doOutput() {
				try {
					long time = System.currentTimeMillis();
					boolean isDone = contest.isDoneUpdating();
					ContestObjectDelta co = queue.poll();
					boolean hasEvent = co != null;
					if (hasEvent) {
						JSONEncoder.setThreadHost(threadHost);
						JSONEncoder.setAccountToken(accountToken);
					}
					while (co != null) {
						IContestObject obj = filter.filter(co.obj);
						if (obj != null) {
							writer.writeEvent(obj, prefix + ind3++, co.d);
						}
						co = queue.poll();
					}
					if (hasEvent) {
						lastActivityTime = time;
						writer2.flush();
					}
					/*if (writer.checkError()) {
						remove();
						return false;
					}*/
					if (isDone) {
						remove();
						return false;
					}

					if (time > lastActivityTime + 100 * 1000) { // 100s
						writer.writeHeartbeat();
						writer2.flush();
						lastActivityTime = time;
					}
					return true;
				} catch (Throwable t) {
					// failed to write to stream
					t.printStackTrace();
					remove();
					return false;
				}
			}

			protected void remove() {
				contest.removeListener(listener);
				asyncCtx.complete();
				cc.remove(asyncCtx);
			}
		});
	}

	/**
	 * Parses HTTP parameters like "?types=teams,submissions" or "?types=testcases" into a type
	 * filter.
	 *
	 * @param request
	 * @return
	 */
	protected static void addFeedEventFilter(HttpServletRequest request, CompositeFilter filter) {
		List<ContestType> types = new ArrayList<>();
		Enumeration<String> en = request.getParameterNames();
		while (en.hasMoreElements()) {
			String name = en.nextElement();
			if ("types".equals(name)) {
				String[] values = request.getParameterValues(name);
				for (String val : values) {
					StringTokenizer st = new StringTokenizer(val, ",");
					while (st.hasMoreTokens()) {
						ContestType type = IContestObject.getTypeByName(st.nextToken());
						if (type != null)
							types.add(type);
					}
				}
			}
		}
		if (!types.isEmpty())
			filter.addFilter(new TypeFilter(types));
	}

	/**
	 * Parses HTTP parameters like "?output=id,label" into a filter list.
	 *
	 * @param request
	 * @return
	 */
	protected static List<String> getAttributeOutputFilter(HttpServletRequest request) {
		List<String> filter = new ArrayList<>();
		Enumeration<String> en = request.getParameterNames();
		while (en.hasMoreElements()) {
			String name = en.nextElement();
			if ("output".equals(name)) {
				String[] values = request.getParameterValues(name);
				for (String val : values) {
					StringTokenizer st = new StringTokenizer(val, ",");
					while (st.hasMoreTokens()) {
						filter.add(st.nextToken());
					}
				}
			}
		}
		return filter;
	}

	protected static void reset(HttpServletResponse response, ConfiguredContest cc) throws IOException {
		Trace.trace(Trace.USER, "Resetting contest event feed");
		try {
			DiskContestSource source = cc.getContestSource();
			File root = source.getRootFolder();
			int hash = -1;
			File hashFile = new File(root, "hash.txt");
			if (hashFile.exists()) {
				try (BufferedReader br = new BufferedReader(new FileReader(hashFile))) {
					hash = Integer.parseInt(br.readLine());
				} catch (Exception e) {
					Trace.trace(Trace.WARNING, "Couldn't read contest hash file", e);
				}
			}

			try (BufferedWriter bw = new BufferedWriter(new FileWriter(hashFile))) {
				bw.write((hash + 1) + "");
				Trace.trace(Trace.INFO, "Bumped up contest hash to " + hash + 1);
			} catch (Exception e) {
				Trace.trace(Trace.WARNING, "Couldn't write contest hash file", e);
			}
		} catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			Trace.trace(Trace.ERROR, "Error updating contest hash", e);
		}
	}
}