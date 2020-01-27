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

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.service.ContestFeedExecutor.Feed;
import org.icpc.tools.cds.service.ContestObjectQueue.ContestObjectDelta;
import org.icpc.tools.cds.util.HttpHelper;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IContestObjectFilter;
import org.icpc.tools.contest.model.TypeFilter;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.DiskContestSource;
import org.icpc.tools.contest.model.feed.NDJSONFeedWriter;
import org.icpc.tools.contest.model.internal.Contest;

public class ContestFeedService {
	protected static void doStream(HttpServletRequest request, IContestObjectFilter filter, PrintWriter writer2,
			Contest contest, int ind, ConfiguredContest cc) {
		NDJSONFeedWriter writer = new NDJSONFeedWriter(writer2, contest);

		final ContestObjectQueue queue = new ContestObjectQueue(ind);
		IContestListener listener = (contest2, obj, d) -> queue.add(obj, d);
		contest.addListenerFromStart(listener);

		final AsyncContext asyncCtx = request.startAsync();
		asyncCtx.setTimeout(0); // no timeout
		cc.add(asyncCtx);
		ContestFeedExecutor.getInstance().addFeedSource(new Feed() {
			protected int count = 120;
			protected int ind3 = ind;

			@Override
			public synchronized boolean doOutput() {
				try {
					HttpHelper.setThreadHost(request);
					count++;

					boolean isDone = contest.isDoneUpdating();
					ContestObjectDelta co = queue.poll();
					while (co != null) {
						IContestObject obj = filter.filter(co.obj);
						if (obj != null) {
							writer.writeEvent(obj, ind3++, co.d);
							count = 0;
						}
						co = queue.poll();
					}
					writer2.flush();
					/*if (writer.checkError()) {
						remove();
						return false;
					}*/
					if (isDone) {
						remove();
						return false;
					}
					if (count > 120) {
						writer.writeHeartbeat();
						count = 0;
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
			ContestSource source = cc.getContestSource();
			if (!(source instanceof DiskContestSource)) {
				response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, "Contest not stored on disk");
				return;
			}
			DiskContestSource diskSource = (DiskContestSource) source;
			File root = diskSource.getRootFolder();
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
			response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
			return;
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			Trace.trace(Trace.ERROR, "Error updating contest hash", e);
			return;
		}
	}
}