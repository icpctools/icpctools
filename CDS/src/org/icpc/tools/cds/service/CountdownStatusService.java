package org.icpc.tools.cds.service;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ICountdown;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.Countdown;

public class CountdownStatusService {
	protected static void doGet(HttpServletResponse response, ConfiguredContest cc) throws IOException {
		Contest contest = cc.getContest();

		StringBuilder sb = new StringBuilder();
		ICountdown countdown = contest.getCountdown();
		if (countdown == null)
			countdown = new Countdown();
		boolean[] status = countdown.getStatus();
		for (boolean s : status) {
			if (s)
				sb.append("Y");
			else
				sb.append("N");
		}

		response.getWriter().println(sb.toString());
	}

	protected static void doPut(HttpServletResponse response, String path, ConfiguredContest cc) throws IOException {
		if (path == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (path.length() != 9) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		String command = path;

		Trace.trace(Trace.INFO, "Countdown status command: " + command);
		Contest contest = cc.getContest();

		Countdown countdown = (Countdown) contest.getCountdown();
		if (countdown == null)
			countdown = new Countdown();
		boolean[] status = countdown.getStatus();
		status = Arrays.copyOf(status, status.length);

		boolean changed = false;
		for (int i = 0; i < 9; i++) {
			if (command.charAt(i) == 'Y') {
				if (!status[i]) {
					status[i] = true;
					changed = true;
				}
			} else if (command.charAt(i) == 'N') {
				if (status[i]) {
					status[i] = false;
					changed = true;
				}
			}
		}

		if (!changed)
			return;

		countdown = (Countdown) countdown.clone();
		countdown.setStatus(status);
		contest.add(countdown);
	}
}