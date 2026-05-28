package org.icpc.tools.cds.service;

import java.io.IOException;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.util.ReplayContest;
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

import jakarta.servlet.http.HttpServletResponse;

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
		if (time == null || time < 0) {
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
		Long startTime = contest.getStartTime();
		Long pauseTime = contest.getCountdownPauseTime();

		long now = System.currentTimeMillis();

		Trace.trace(Trace.USER, "Start time command: " + command);
		try {
			if (command.startsWith("set:")) {
				if (startTime != null) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Contest currently in countdown");
					return;
				}

				long countdownPauseTime = RelativeTime.parse(command.substring(4).trim());
				if (countdownPauseTime < 30000) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Can't set contest to less than 30s countdown");
					return;
				}
				setContestStart(cc, null, countdownPauseTime);
			} else if (command.startsWith("add:")) {
				if (errorIfContestNotPaused(pauseTime, response))
					return;

				long time = RelativeTime.parse(command.substring(4).trim());
				if (time <= 0) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid add time");
					return;
				}
				setContestStart(cc, null, pauseTime + time);
			} else if (command.startsWith("remove:")) {
				if (errorIfContestNotPaused(pauseTime, response))
					return;

				long time = RelativeTime.parse(command.substring(7).trim());
				if (pauseTime - time < 300000) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid remove time");
					return;
				}
				setContestStart(cc, null, pauseTime - time);
			} else if (command.equals("pause")) {
				if (errorIfContestNotCountingDown(startTime, response))
					return;

				if (startTime - now < 30000) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Can't pause in the last 30s");
					return;
				}

				// pause at the next full second
				long newPauseTime = (long) (Math.floor((startTime - now) / 1000.0)) * 1000;
				setContestStart(cc, null, newPauseTime);
			} else if (command.equals("resume")) {
				if (errorIfContestNotPaused(pauseTime, response))
					return;

				// resume at nearest full second
				long newStartTime = ((now + pauseTime) / 1000) * 1000;
				setContestStart(cc, newStartTime, null);
			} else if (command.equals("clear")) {
				if (contest.getState().isRunning()) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Contest already running");
					return;
				}
				setContestStart(cc, null, null);
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
	 * Sets the start time to null or a new date given in milliseconds.
	 *
	 * The request should fail with a 401 if the user does not have sufficient access rights, or a
	 * 403 if the contest is started or within 30s of starting, or if the new start time is in the
	 * past or within 30s.
	 *
	 * @param newTime
	 * @throws Exception
	 */
	protected static void setContestStart(ConfiguredContest cc, Long startTime, Long countdownPauseTime)
			throws Exception {
		if (startTime == null && countdownPauseTime == null)
			Trace.trace(Trace.INFO, "Setting start time to: null");
		else if (countdownPauseTime != null)
			Trace.trace(Trace.INFO,
					"Setting start time to paused at: " + RelativeTime.format(countdownPauseTime.longValue()));
		else
			Trace.trace(Trace.INFO, "Setting start time to: " + Timestamp.format(startTime) + " ("
					+ ContestUtil.formatTime(startTime) + ")");

		Contest contest = cc.getContest();
		long now = System.currentTimeMillis();
		Long currentStartTime = contest.getInfo().getStartTime();

		if (currentStartTime != null && currentStartTime > 0 && currentStartTime < now)
			throw new IllegalArgumentException("Contest has already started");

		if (startTime != null && startTime < now)
			throw new IllegalArgumentException("Can't start contest in the past");

		if (cc.isTesting()) {
			if (contest instanceof ReplayContest)
				((ReplayContest) contest).setContestStart(startTime, countdownPauseTime);
		} else {
			if (cc.getCCS() != null) {
				ContestSource source = cc.getContestSource();
				if (source instanceof RESTContestSource) {
					RESTContestSource restSource = (RESTContestSource) source;
					IInfo newInfo = contest.setContestStart(startTime, countdownPauseTime);
					restSource.cacheClientSideEvent(newInfo, Delta.UPDATE);
				}
				source.setContestStart(startTime, countdownPauseTime);
			}
		}
	}
}