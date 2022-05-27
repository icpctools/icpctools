package org.icpc.tools.cds.service;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.util.PlaybackContest;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener.Delta;
import org.icpc.tools.contest.model.IInfo;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.feed.RelativeTime;
import org.icpc.tools.contest.model.feed.Timestamp;
import org.icpc.tools.contest.model.internal.Contest;

public class StartTimeService {
	private static boolean errorIfContestNotCountingDown(Long time, HttpServletResponse response) throws IOException {
		long now = System.currentTimeMillis();
		if (time == null || time < 0 || time < now) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Contest not in countdown");
			return true;
		}
		return false;
	}

	private static boolean errorIfContestNotPaused(Long time, HttpServletResponse response) throws IOException {
		if (time == null || time > 0) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Contest not paused");
			return true;
		}
		return false;
	}

	protected static void doPut(HttpServletResponse response, String command, ConfiguredContest cc) throws IOException {
		// valid commands:
		// pause - pause the countdown
		// resume - resume the countdown
		// clear - clear start time completely
		// set:0:00:00 - set the countdown to the given time
		// add:0:00:00 - add the given time to the countdown
		// remove:0:00:00 - remove the given time from the countdown

		IContest contest = cc.getContest();
		Long startStatus = contest.getStartStatus();
		long currentStart = 0;
		if (startStatus != null)
			currentStart = startStatus.longValue();

		long now = System.currentTimeMillis();

		Trace.trace(Trace.USER, "Start time command: " + command);
		try {
			if (command.startsWith("set:")) {
				setStartTime(cc, (long) -RelativeTime.parse(command.substring(4).trim()));
			} else if (command.startsWith("add:")) {
				if (errorIfContestNotPaused(currentStart, response))
					return;

				long time = RelativeTime.parse(command.substring(4).trim());
				if (time <= 0) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid add time");
					return;
				}
				setStartTime(cc, currentStart - time);
			} else if (command.startsWith("remove:")) {
				if (errorIfContestNotPaused(currentStart, response))
					return;

				long time = RelativeTime.parse(command.substring(7).trim());
				if (time <= 0 || time > -currentStart) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid remove time");
					return;
				}
				setStartTime(cc, currentStart + time);
			} else if (command.equals("pause")) {
				if (errorIfContestNotCountingDown(currentStart, response))
					return;
				setStartTimeInSeconds(cc, now - currentStart);
			} else if (command.equals("resume")) {
				if (errorIfContestNotPaused(currentStart, response))
					return;
				setStartTimeInSeconds(cc, now - currentStart);
			} else if (command.equals("clear")) {
				if (contest.getState().isRunning()) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Contest already running");
					return;
				}
				setStartTime(cc, null);
			}
		} catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return;
		} catch (IOException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "CCS error: " + e.getMessage());
			return;
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			Trace.trace(Trace.ERROR, "Error setting start time", e);
			return;
		}
	}

	/**
	 * Sets the start time to null or a new date given in seconds.
	 *
	 * @param newTime
	 * @throws Exception
	 */
	private static void setStartTimeInSeconds(ConfiguredContest cc, Long newTime) throws Exception {
		if (newTime == null) {
			setStartTime(cc, null);
			return;
		}

		Long time = newTime;
		if (time > 0) // resume at the closest second
			time = (time / 1000) * 1000;
		else // pause with the full current second
			time = (long) (Math.floor(time / 1000.0)) * 1000;

		setStartTime(cc, time);
	}

	/**
	 * Sets the start time to null or a new date given in milliseconds.
	 *
	 * The request should fail with a 401 if the user does not have sufficient access rights, or a
	 * 403 if the contest is started or within 30s of starting, or if the new start time is in the
	 * past or within 30s.
	 *
	 * @param newTime
	 * @throws Exception
	 */
	protected static void setStartTime(ConfiguredContest cc, Long time) throws Exception {
		if (time == null)
			Trace.trace(Trace.INFO, "Setting start time to: null");
		else if (time < 0)
			Trace.trace(Trace.INFO, "Setting start time to paused at: " + RelativeTime.format(-time.intValue()));
		else
			Trace.trace(Trace.INFO,
					"Setting start time to: " + Timestamp.format(time) + " (" + ContestUtil.formatStartTime(time) + ")");

		Contest contest = cc.getContest();
		long now = System.currentTimeMillis();
		Long currentStartTime = contest.getInfo().getStartTime();

		if (currentStartTime != null && currentStartTime > 0 && currentStartTime < now)
			throw new IllegalArgumentException("Contest has already started");

		if (currentStartTime != null && currentStartTime < 0 && time != null && time < 0)
			throw new IllegalArgumentException("Contest already paused");

		if (time != null && time > 0 && time < now)
			throw new IllegalArgumentException("Can't start contest in the past");

		if (contest instanceof PlaybackContest)
			((PlaybackContest) contest).setStartTime(time);

		IInfo newInfo = contest.setStartStatus(time);

		if (!cc.isTesting() && cc.getCCS() != null) {
			ContestSource source = cc.getContestSource();
			if (source instanceof RESTContestSource) {
				RESTContestSource restSource = (RESTContestSource) source;
				restSource.cacheClientSideEvent(newInfo, Delta.UPDATE);
			}
			source.setStartTime(time);
		}
	}
}