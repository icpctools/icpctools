package org.icpc.tools.cds.service;

import java.io.IOException;
import java.util.List;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.feed.JSONWriter;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ResolveInfo;
import org.icpc.tools.contest.model.resolver.ResolutionControl;
import org.icpc.tools.contest.model.resolver.ResolutionUtil;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ResolutionStep;

import jakarta.servlet.http.HttpServletResponse;

public class ResolverService {
	protected static void doGet(HttpServletResponse response, ConfiguredContest cc) throws IOException {
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");

		JsonObject obj = new JsonObject();
		ResolutionControl control = cc.getResolutionControl();
		if (control != null) {
			obj.put("pause", control.getCurrentPause());
			List<ResolutionStep> steps = control.getSteps();
			obj.put("total_pauses", ResolutionUtil.getTotalPauses(steps));
			obj.put("total_time", ResolutionUtil.getTotalTime(steps));
			obj.put("stepping", control.isStepping());
		} else {
			obj.put("pause", -1);
			obj.put("total_pauses", -1);
			obj.put("total_time", -1);
			obj.put("stepping", false);
		}

		JSONWriter pw = new JSONWriter(response.getWriter());
		pw.writeObject(obj);
	}

	protected static void doPut(HttpServletResponse response, String command, ConfiguredContest cc) throws IOException {
		Contest contest = cc.getContest();
		if (contest.getState().isRunning()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Contest still running");
			return;
		}

		Trace.trace(Trace.USER, "Resolver command: " + command);
		try {
			final ResolutionControl control = cc.getResolutionControl();
			if ("reset".equals(command) && control == null) {
				ResolveInfo resolveInfo = new ResolveInfo();
				contest.add(resolveInfo);
			} else if ("init".equals(command)) {
				cc.initResolution(true);
				return;
			}

			if (control == null || control.isStepping())
				return;

			// TODO: output a ResolveInfo event instead?
			ExecutorListener.getExecutor().submit(new Runnable() {
				@Override
				public void run() {
					if ("fast-forward".equals(command))
						control.forward(false);
					else if ("forward".equals(command))
						control.forward(true);
					else if ("rewind".equals(command))
						control.rewind(true);
					else if ("fast-rewind".equals(command))
						control.rewind(false);
					else if ("reset".equals(command))
						control.reset();
				}
			});
		} catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
			Trace.trace(Trace.ERROR, "Error durng finalization", e);
		}
	}
}