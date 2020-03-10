package org.icpc.tools.cds.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.util.HttpHelper;
import org.icpc.tools.cds.util.Role;
import org.icpc.tools.cds.video.ReactionVideoRecorder;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IContestObjectFilter;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.IInfo;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Scoreboard;
import org.icpc.tools.contest.model.feed.JSONArrayWriter;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.feed.NDJSONFeedWriter;
import org.icpc.tools.contest.model.feed.RelativeTime;
import org.icpc.tools.contest.model.feed.Timestamp;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.Deletion;

@WebServlet(urlPatterns = { "/api/", "/api/*" }, asyncSupported = true)
@ServletSecurity(@HttpConstraint(transportGuarantee = ServletSecurity.TransportGuarantee.CONFIDENTIAL, rolesAllowed = {
		Role.ADMIN, Role.PRES_ADMIN, Role.BLUE, Role.TRUSTED, Role.BALLOON, Role.PUBLIC }))
public class ContestRESTService extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpHelper.setThreadHost(request);
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("ICPC-Tools", "CDS");
		response.setHeader("X-Frame-Options", "sameorigin");

		String path = request.getPathInfo();
		if (path == null || !path.startsWith("/contests")) {
			request.getRequestDispatcher("/WEB-INF/jsps/contestAPI.jsp").forward(request, response);
			return;
		}
		path = path.substring(9);

		if (path == null || path.equals("") || path.equals("/")) {
			// list contests
			PrintWriter pw = response.getWriter();
			JSONArrayWriter writer = new JSONArrayWriter(pw);
			response.setContentType(writer.getContentType());
			response.setHeader("Access-Control-Allow-Origin", "*");
			writer.writePrelude();
			ConfiguredContest[] ccs = CDSConfig.getContests();
			boolean first = true;
			for (ConfiguredContest cc2 : ccs) {
				if (!cc2.isHidden() || Role.isAdmin(request) || Role.isBlue(request)) {
					try {
						IInfo info = cc2.getContestByRole(request).getInfo();
						if (!first)
							writer.writeSeparator();
						else
							first = false;
						writer.write(info);
					} catch (Exception e) {
						// ignore
					}
				}
			}
			writer.writePostlude();
			return;
		}

		String[] segments = path.substring(1).split("/");
		if (segments == null || segments.length == 0) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Content not found");
			return;
		}

		ConfiguredContest cc = CDSConfig.getContest(segments[0]);
		if (cc == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Contest " + segments[0] + " not found");
			return;
		}

		Contest contest = cc.getContestByRole(request);
		try {
			contest.getInfo();
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Contest not configured");
		}

		if (segments.length == 2) {
			if ("event-feed".equals(segments[1])) {
				CompositeFilter filter = new CompositeFilter();
				ContestFeedService.addFeedEventFilter(request, filter);

				PrintWriter pw = response.getWriter();
				response.setContentType("application/json");
				cc.incrementFeed();
				int ind = getSinceIdIndex(request, contest);
				if (ind == -2) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid event id");
					return;
				}
				if (ind == -1)
					ind = 0;
				ContestFeedService.doStream(request, filter, pw, contest, ind, cc);
				return;
			} else if ("scoreboard".equals(segments[1])) {
				int ind = getAfterEventIndex(request, contest);
				if (ind == -2) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid event id");
					return;
				}
				contest = Scoreboard.getScoreboard(contest, ind);
				cc.incrementScoreboard();
				response.setContentType("application/json");
				Scoreboard.writeScoreboard(response.getWriter(), contest);
				return;
			} else if ("projectedScoreboard".equals(segments[1])) {
				int ind = getAfterEventIndex(request, contest);
				if (ind == -2) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid event id");
					return;
				}
				contest = Scoreboard.getScoreboard(contest, ind);
				cc.incrementScoreboard();
				response.setContentType("application/json");
				ProjectionScoreboardService.writeScoreboard(response.getWriter(), contest);
				return;
			} else if ("optimisticScoreboard".equals(segments[1])) {
				int ind = getAfterEventIndex(request, contest);
				if (ind == -2) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid event id");
					return;
				}
				contest = Scoreboard.getScoreboard(contest, ind);
				cc.incrementScoreboard();
				response.setContentType("application/json");
				OptimisticScoreboardService.beOptimistic(contest);
				Scoreboard.writeScoreboard(response.getWriter(), contest);
				return;
			}
		}

		if (segments.length == 3 && "report".equals(segments[1])) {
			response.setContentType("application/json");
			ReportGenerator.report(response.getWriter(), contest, segments[2]);
			return;
		}

		String typeName = "contests";
		String id = cc.getId();
		int ind = 1;
		if (segments.length > 1 && IContestObject.getTypeByName(segments[1]) != null) {
			typeName = segments[1];
			if (segments.length > 2)
				id = segments[2];
			ind = 3;
		}

		ContestType type2 = IContestObject.getTypeByName(typeName);
		if (type2 == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if ((ind == 1 && segments.length >= 2) || (ind == 3 && segments.length >= 4)) {
			// download
			try {
				String url = segments[ind];
				for (int i = ind + 1; i < segments.length; i++)
					url += "/" + segments[i];
				response.setHeader("Access-Control-Allow-Origin", "*");
				if (doDownload(request, response, cc, type2, id, url))
					return;
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error in download", e);
			}
		}

		if (segments.length > ind) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		CompositeFilter filter = new CompositeFilter();

		boolean isArray = true;
		if (segments.length == 1 || type2 == ContestType.STATE || type2 == ContestType.MAP_INFO)
			isArray = false;
		if (ind == 3 && segments.length == 3) {
			// id filtering
			isArray = false;
			final String id2 = id;
			filter.addFilter(new IContestObjectFilter() {
				@Override
				public IContestObject filter(IContestObject obj) {
					if (id2.equals(obj.getId()))
						return obj;
					return null;
				}
			});
		}

		addRESTAttributeFilter(request, filter);

		PrintWriter pw = response.getWriter();
		JSONArrayWriter writer = new JSONArrayWriter(pw);
		response.setContentType(writer.getContentType());
		response.setHeader("Access-Control-Allow-Origin", "*");
		cc.incrementRest();

		doREST(isArray, type2, filter, writer, contest);
	}

	protected static int getSinceIdIndex(HttpServletRequest request, IContest contest) {
		return getEventIndexFromParameter(request, contest, "since_id");
	}

	protected static int getAfterEventIndex(HttpServletRequest request, IContest contest) {
		return getEventIndexFromParameter(request, contest, "after_event_id");
	}

	/**
	 * Returns the indexed event id from a parameter. If the id is clearly invalid (e.g. doesn't
	 * start with 'cds') then -2 is returned. If there is no param, -1 is returned.
	 *
	 * @param request
	 * @param param
	 * @return
	 */
	private static int getEventIndexFromParameter(HttpServletRequest request, IContest contest, String param) {
		Enumeration<String> en = request.getParameterNames();
		String prefix = NDJSONFeedWriter.getContestPrefix(contest);
		while (en.hasMoreElements()) {
			String name = en.nextElement();
			if (param.equals(name)) {
				String[] values = request.getParameterValues(name);
				if (values.length != 1)
					return -2;
				String idVal = values[0];

				if (idVal == null || !idVal.startsWith(prefix))
					return -2;

				try {
					return Integer.parseInt(idVal.substring(3)) + 1;
				} catch (Exception e) {
					// ignore
					return -2;
				}
			}
		}
		return -1;
	}

	/**
	 * Parses HTTP parameters like "?filter=language:C,team-id:46" into a property filter.
	 * filter=<attribute>:<value>[,<attribute>:<value>...]
	 *
	 * @param request
	 * @return
	 */
	protected static void addRESTAttributeFilter(HttpServletRequest request, CompositeFilter filter) {
		PropertyFilter propFilter = new PropertyFilter();
		Enumeration<String> en = request.getParameterNames();
		while (en.hasMoreElements()) {
			String name = en.nextElement();
			if ("filter".equals(name)) {
				String[] values = request.getParameterValues(name);
				for (String val2 : values) {
					StringTokenizer st = new StringTokenizer(val2, ",");
					while (st.hasMoreTokens()) {
						String val = st.nextToken();
						int ind = val.indexOf(":");
						if (ind > 0) {
							String n = val.substring(0, ind);
							String v = val.substring(ind + 1, val.length());
							propFilter.addFilter(n, v);
						}
					}
				}
			}
		}
		if (propFilter.hasProperties())
			filter.addFilter(propFilter);
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

	protected static void doREST(boolean isArray, ContestType type, IContestObjectFilter filter, JSONArrayWriter writer,
			Contest contest) {
		if (isArray)
			writer.writePrelude();

		boolean first = true;
		IContestObject[] objects = contest.getObjects(type);
		for (IContestObject obj : objects) {
			obj = filter.filter(obj);
			if (obj != null && !(obj instanceof IDelete)) {
				if (!first)
					writer.writeSeparator();
				else
					first = false;
				writer.write(obj);
			}
		}
		if (isArray)
			writer.writePostlude();
	}

	protected static boolean doDownload(HttpServletRequest request, HttpServletResponse response, ConfiguredContest cc,
			ContestType type, String id, String url) throws IOException {
		Contest contest = cc.getContestByRole(request);
		IContestObject obj = contest.getObjectByTypeAndId(type, id);
		if (obj == null)
			return false;

		Object ext = obj.resolveFileReference(url);
		if (ext == null)
			return false;

		if (ext instanceof File) {
			if (obj instanceof ISubmission) {
				ISubmission s = (ISubmission) obj;
				if (!Role.isTrusted(request) && "files".equals(url)) {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
					return true;
				} else if (!Role.isTrusted(request) && "reactions".equals(url) && !contest.isBeforeFreeze(s)) {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
					return true;
				}
			}

			if (obj instanceof ITeam) {
				if (!Role.isTrusted(request) && ("key_log".equals(url) || "tool_data".equals(url))) {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
					return true;
				}
			}
			cc.incrementDownload();
			HttpHelper.sendFile(request, response, (File) ext);
			return true;
		} else if (ext instanceof String) {
			String s = (String) ext;
			if ("reaction".equals(s) && obj instanceof ISubmission) {
				ISubmission sub = (ISubmission) obj;
				cc.incrementDownload();
				ReactionVideoRecorder.streamReaction(cc, sub, request, response);
				return true;
			}
			/*VideoAggregator aggregator = null;
			if ("desktop".equals(s))
				aggregator = DesktopAggregator.getInstance();
			else if ("webcam".equals(s))
				aggregator = WebcamAggregator.getInstance();
			else
				return false;

			int num = -1;
			try {
				num = Integer.parseInt(id);
			} catch (Exception e) {
				e.printStackTrace();
			}
			VideoServlet.doVideo(request, response, aggregator, num, false);
			return true;*/
			return false;
		}

		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		return true;
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setAttribute("error-type", "plain");
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");

		String method = request.getMethod();
		if (method.equals("PATCH"))
			doPatch(request, response);
		else
			super.service(request, response);
	}

	// {"id":"finals","start_time":null}
	// {"id":"systest","start_time":"2014-06-25T10:00:00+01"}
	// {"id":"dress","start_time":"2017-04-08T14:41:43.000-04"}
	// {"id":"finals","start_time":null,"countdown_pause_time":"0:12:15.000"}
	protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (!Role.isAdmin(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		String path = request.getPathInfo();
		if (path == null || path.equals("/") || path.length() < 10) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		String[] segments = path.substring(10).split("/");
		if (segments == null || segments.length == 0 || segments.length > 3) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		ConfiguredContest cc = CDSConfig.getContest(segments[0]);
		if (cc == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Contest not found");
			return;
		}

		if (segments.length == 1) {
			// attempting to change the start time
			InputStream is = request.getInputStream();
			JSONParser parser = new JSONParser(is);
			JsonObject obj = parser.readObject();

			// confirm the contest id is correct
			String id = obj.getString("id");
			if (id == null || !id.equals(cc.getId())) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid contest id");
				return;
			}

			// check for start or countdown time
			String time = obj.getString("start_time");
			String countdownTime = obj.getString("countdown_pause_time");

			Long newTime = null;
			if (time != null && !"null".equals(time))
				try {
					newTime = Timestamp.parse(time.toString());
					Trace.trace(Trace.INFO, "Patch time: " + Timestamp.format(newTime));
				} catch (Exception e) {
					Trace.trace(Trace.WARNING, "Invalid patch time: " + e.getMessage());
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
					return;
				}

			if (countdownTime != null && !"null".equals(countdownTime))
				try {
					newTime = new Long(-RelativeTime.parse(countdownTime.toString()));
					Trace.trace(Trace.INFO, "Patch countdown time: " + RelativeTime.format(newTime.intValue()));
				} catch (Exception e) {
					Trace.trace(Trace.WARNING, "Invalid patch countdown time: " + e.getMessage());
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
					return;
				}

			try {
				Trace.trace(Trace.INFO, "Patching start time");
				StartTimeService.setStartTime(cc, newTime);
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error setting start time", e);
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
			return;
		}

		String type = segments[1];
		IContestObject.ContestType cType = IContestObject.getTypeByName(type);
		if (cType == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid type");
			return;
		}

		String id = null;
		if (segments.length == 3)
			id = segments[2];
		else if (!IContestObject.isSingleton(cType)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		Contest contest = cc.getContest();
		ContestObject co = (ContestObject) contest.getObjectByTypeAndId(cType, id);
		if (co == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Object does not exist");
			return;
		}

		JsonObject obj = null;
		try {
			InputStream is = request.getInputStream();
			JSONParser parser = new JSONParser(is);
			obj = parser.readObject();

			// confirm the id is correct
			if (id != null) {
				String jsonId = obj.getString("id");
				if (jsonId == null || !jsonId.equals(id)) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid id");
					return;
				}
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not parse object");
			return;
		}

		try {
			Trace.trace(Trace.USER, "Updating contest object: " + type + "/" + id);
			co = (ContestObject) co.clone();
			for (String key : obj.props.keySet())
				co.add(key, obj.props.get(key));

			contest.add(co);
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not update object");
			Trace.trace(Trace.ERROR, "Could not update contest " + id, e);
		}
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (!Role.isAdmin(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		String path = request.getPathInfo();
		if (path == null || path.equals("/") || path.length() < 10) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		String[] segments = path.substring(10).split("/");
		if (segments == null || segments.length < 2 || segments.length > 3) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		ConfiguredContest cc = CDSConfig.getContest(segments[0]);
		if (cc == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Contest not found");
			return;
		}

		String type = segments[1];
		ContestType cType = IContestObject.getTypeByName(type);
		if (cType == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid type");
			return;
		}

		String id = null;
		if (segments.length == 3)
			id = segments[2];
		else if (!IContestObject.isSingleton(cType)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		Contest contest = cc.getContest();
		if (contest.getObjectByTypeAndId(cType, id) != null) {
			Trace.trace(Trace.USER, "Replacing contest object: " + type + "/" + id);
		}

		JsonObject obj = null;
		try {
			InputStream is = request.getInputStream();
			JSONParser parser = new JSONParser(is);
			obj = parser.readObject();

			// confirm the id is correct
			if (id != null) {
				String jsonId = obj.getString("id");
				if (jsonId == null || !jsonId.equals(id)) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid id");
					return;
				}
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not parse object");
			return;
		}

		try {
			Trace.trace(Trace.USER, "Adding contest object: " + type + "/" + id);
			ContestObject co = (ContestObject) IContestObject.createByType(cType);
			for (String key : obj.props.keySet())
				co.add(key, obj.props.get(key));

			contest.add(co);
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not add object");
			Trace.trace(Trace.ERROR, "Could not add to contest " + id, e);
		}
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (!Role.isAdmin(request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		String path = request.getPathInfo();
		if (path == null || path.equals("/") || path.length() < 10) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		String[] segments = path.substring(10).split("/");
		if (segments == null || segments.length != 3) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		ConfiguredContest cc = CDSConfig.getContest(segments[0]);
		if (cc == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Contest not found");
			return;
		}

		String type = segments[1];
		IContestObject.ContestType cType = IContestObject.getTypeByName(type);
		if (cType == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid type");
			return;
		}

		String id = segments[2];
		try {
			Trace.trace(Trace.USER, "Deleting contest object: " + type + "/" + id);
			Deletion d = new Deletion(id, cType);
			Contest contest = cc.getContest();
			if (contest.getObjectByTypeAndId(cType, id) == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Object does not exist");
				return;
			}
			contest.add(d);
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not remove object");
			Trace.trace(Trace.ERROR, "Could not remove from contest! " + id, e);
		}
	}
}