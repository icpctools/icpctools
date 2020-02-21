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
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.Award;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.AwardUtil;

public class FinalizeService {
	protected static void doPut(HttpServletResponse response, String command, ConfiguredContest cc) throws IOException {
		IContest contest = cc.getContest();
		if (contest.getState().isRunning()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Contest still running");
			return;
		}

		// valid commands:
		// b# - set value of b and assign awards

		Trace.trace(Trace.USER, "Finalize command: " + command);
		try {
			Contest c = (Contest) contest;
			if (command.startsWith("b:")) {
				int b = Integer.parseInt(command.substring(2));
				AwardUtil.createWorldFinalsAwards(c, b);
			} else if ("template".equals(command)) {

				Award[] template = loadFromFile(
						cc.getLocation() + File.separator + "config" + File.separator + "award-template.json");
				Trace.trace(Trace.USER, "Assigning awards: " + template.length);
				AwardUtil.applyAwards(c, template);
			}
		} catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			Trace.trace(Trace.ERROR, "Error durng finalization", e);
		}
	}

	protected static Award[] loadFromFile(String name) throws IOException {
		JSONParser parser = new JSONParser(new FileInputStream(name));
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