package org.icpc.tools.cds.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.internal.Award;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.State;
import org.icpc.tools.contest.model.util.AwardUtil;

public class FinalizeService {
	protected static void doPut(HttpServletResponse response, String command, ConfiguredContest cc) throws IOException {
		IContest contest = cc.getContest();
		if (contest.getState().isRunning()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Contest still running");
			return;
		}

		// valid commands:
		// template - assign awards from template
		// eou - set end of updates
		// thaw - thaw contest

		Trace.trace(Trace.USER, "Finalize command: " + command);
		try {
			Contest c = (Contest) contest;
			if ("template".equals(command)) {
				File f = new File(cc.getPath() + File.separator + "config" + File.separator + "award-template.json");
				if (!f.exists()) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No award template found");
					return;
				}
				Award[] template = loadFromFile(f);
				Trace.trace(Trace.USER, "Assigning awards: " + template.length);
				AwardUtil.applyAwards(c, template);
			} else if ("eou".equals(command)) {
				// send end of updates
				State state = (State) contest.getState();
				if (state.getEndOfUpdates() != null)
					return;
				state = (State) state.clone();
				state.setEndOfUpdates(System.currentTimeMillis());
				((Contest) contest).add(state);
			} else if ("thaw".equals(command)) {
				if (cc.getCCS() != null) {
					ContestSource source = cc.getContestSource();
					if (source instanceof RESTContestSource) {
						long now = System.currentTimeMillis();
						source.setContestThawTime(now);
					}
				}
			}
		} catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
			Trace.trace(Trace.ERROR, "Error durng finalization", e);
		}
	}

	protected static Award[] loadFromFile(File f) throws IOException {
		JSONParser parser = new JSONParser(new FileInputStream(f));
		Object[] arr = parser.readArray();
		List<Award> list = new ArrayList<>();
		for (Object obj : arr) {
			JsonObject data = (JsonObject) obj;
			Award award = new Award();
			for (String key : data.props.keySet())
				award.add(key, data.props.get(key));

			list.add(award);
		}
		return list.toArray(new Award[0]);
	}
}