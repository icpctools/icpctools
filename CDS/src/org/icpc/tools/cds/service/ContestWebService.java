package org.icpc.tools.cds.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.RSSWriter;
import org.icpc.tools.cds.util.HttpHelper;
import org.icpc.tools.cds.video.VideoAggregator;
import org.icpc.tools.cds.video.VideoAggregator.Stats;
import org.icpc.tools.cds.video.VideoStream;
import org.icpc.tools.cds.video.VideoStream.StreamType;
import org.icpc.tools.cds.web.BalloonPDFService;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.Scoreboard;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.ContestSource.Validation;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.AwardUtil;
import org.icpc.tools.contest.model.util.ContestComparator;
import org.icpc.tools.contest.model.util.ScoreboardData;
import org.icpc.tools.contest.model.util.ScoreboardUtil;

@WebServlet(urlPatterns = { "/contests", "/contests/*" }, asyncSupported = true)
public class ContestWebService extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static class VideoStats {
		int numStreams;
		int current;
		int total;
		int time;
		String mode;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpHelper.setThreadHost(request);
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("ICPC-Tools", "CDS");
		response.setHeader("X-Frame-Options", "sameorigin");

		String path = request.getPathInfo();
		if (path == null || path.equals("/")) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		String[] segments = path.substring(1).split("/");
		if (segments == null || segments.length == 0) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Content not found");
			return;
		}

		/*System.out.print("Web segments:");
		for (String s : segments)
			System.out.print(" " + s);*/

		ConfiguredContest cc = CDSConfig.getContest(segments[0]);
		if (cc == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Contest not found");
			return;
		}

		if (segments.length >= 1) {
			doWeb(request, response, segments, cc);
			return;
		}
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "Contest not found");
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");

		String path = request.getPathInfo();
		if (path == null || path.equals("/")) {
			// list contests

			return;
		}

		String[] segments = path.substring(1).split("/");
		if (segments == null || segments.length == 0) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Content not found");
			return;
		}

		/*System.out.print("Web PUT segments:");
		for (String s : segments)
			System.out.print(" " + s);*/

		ConfiguredContest cc = CDSConfig.getContest(segments[0]);
		if (cc == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Contest not found");
			return;
		}

		if (segments.length >= 2) {
			request.setAttribute("cc", cc);
			cc.incrementWeb();
			if (segments[1].equals("admin")) {
				if (!cc.isAdmin(request)) {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
					return;
				}

				if (segments.length == 3) {
					if (segments[2].equals("reset-feed"))
						ContestFeedService.reset(response, cc);
				} else if (segments.length == 4) {
					String command = segments[3];
					if (segments[2].equals("time"))
						StartTimeService.doPut(response, command, cc);
					else if (segments[2].equals("finalize"))
						FinalizeService.doPut(response, command, cc);
					else if (segments[2].equals("resolve"))
						ResolverService.doPut(response, command, cc);
				}
				return;
			} else if (segments.length == 3 && segments[1].equals("video") && segments[2].equals("status")) {
				// VideoStatusServlet.doPut(cc, request, response);
				request.getRequestDispatcher("/WEB-INF/jsps/video.jsp").forward(request, response);
				return;
			}
		}
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "Contest not found");
	}

	private static InputStream getHTTPInputStream(String url, String user, String password) throws Exception {
		try {
			HttpURLConnection conn = HTTPSSecurity.createConnection(new URL(url), user, password);
			conn.setReadTimeout(15 * 1000); // 15s timeout
			return new BufferedInputStream(conn.getInputStream());
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error downloading from " + url, e);
			throw e;
		}
	}

	private static void doWeb(HttpServletRequest request, HttpServletResponse response, String[] segments,
			ConfiguredContest cc) throws ServletException, IOException {
		request.setAttribute("cc", cc);
		cc.incrementWeb();
		if (segments.length >= 2) {
			boolean isAdmin = cc.isAdmin(request);
			if (segments[1].equals("details")) {
				if (isAdmin)
					request.getRequestDispatcher("/WEB-INF/jsps/details-admin.jsp").forward(request, response);
				else
					request.getRequestDispatcher("/WEB-INF/jsps/details.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("registration")) {
				if (isAdmin)
					request.getRequestDispatcher("/WEB-INF/jsps/registration-admin.jsp").forward(request, response);
				else
					request.getRequestDispatcher("/WEB-INF/jsps/registration.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("clarifications")) {
				if (isAdmin)
					request.getRequestDispatcher("/WEB-INF/jsps/clarifications-admin.jsp").forward(request, response);
				else
					request.getRequestDispatcher("/WEB-INF/jsps/clarifications.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("submissions")) {
				if (isAdmin)
					request.getRequestDispatcher("/WEB-INF/jsps/submissions-admin.jsp").forward(request, response);
				else
					request.getRequestDispatcher("/WEB-INF/jsps/submissions.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("teamSummary") && segments.length == 3) {
				request.setAttribute("teamId", segments[2]);
				request.getRequestDispatcher("/WEB-INF/jsps/teamSummary.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("scoreboard")) {
				request.getRequestDispatcher("/WEB-INF/jsps/scoreboard.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("commentary")) {
				request.getRequestDispatcher("/WEB-INF/jsps/commentary.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("contestCompare")) {
				try {
					Contest contestA = cc.getContestByRole(request);
					request.setAttribute("a", cc.getId());
					String cId = segments[2];

					Contest contestB = null;
					if ("compare2cds".equals(cId)) {
						contestB = contestA.clone(true);
						// remove any existing awards
						IAward[] awards = contestB.getAwards();
						if (awards != null)
							for (IAward a : awards)
								contestB.removeFromHistory(a);
						AwardUtil.createDefaultAwards(contestB);
					} else
						contestB = CDSConfig.getContest(cId).getContestByRole(request);
					request.setAttribute("b", segments[2]);

					request.setAttribute("info", ContestComparator.compareInfo(contestA, contestB).printHTMLSummary());
					request.setAttribute("problems",
							ContestComparator.compareProblems(contestA, contestB).printHTMLSummary());
					request.setAttribute("languages",
							ContestComparator.compareLanguages(contestA, contestB).printHTMLSummary());
					request.setAttribute("judgement-types",
							ContestComparator.compareJudgementTypes(contestA, contestB).printHTMLSummary());
					request.setAttribute("groups", ContestComparator.compareGroups(contestA, contestB).printHTMLSummary());
					request.setAttribute("organizations",
							ContestComparator.compareOrganizations(contestA, contestB).printHTMLSummary());
					request.setAttribute("teams", ContestComparator.compareTeams(contestA, contestB).printHTMLSummary());
					request.setAttribute("submissions",
							ContestComparator.compareSubmissions(contestA, contestB).printHTMLSummary());
					request.setAttribute("judgements",
							ContestComparator.compareJudgements(contestA, contestB).printHTMLSummary());
					request.setAttribute("awards", ContestComparator.compareAwards(contestA, contestB).printHTMLSummary());
					request.getRequestDispatcher("/WEB-INF/jsps/contestCompare.jsp").forward(request, response);
				} catch (Exception e) {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
				return;
			} else if (segments[1].equals("scoreboardCompare")) {
				if (!cc.isStaff(request)) {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
					return;
				}
				String src = segments[2];
				String st = request.getRequestURL().toString();
				String oldPath = "/contests/" + cc.getId() + "/scoreboardCompare/" + src;
				st = st.substring(0, st.length() - oldPath.length());
				String path = st + "/api/contests/" + cc.getId() + "/scoreboard";
				request.setAttribute("a", path);

				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				Scoreboard.writeScoreboard(new PrintWriter(bout), cc.getContest());
				ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
				ScoreboardData sd1 = ScoreboardUtil.read(bin);

				ScoreboardData sd2 = sd1;
				if ("compare2src".equals(src)) {
					if (cc.getContestSource() instanceof RESTContestSource) {
						RESTContestSource cs = (RESTContestSource) cc.getContestSource();
						path = cs.getURL().toExternalForm() + "/scoreboard";
						request.setAttribute("b", path);
						InputStream in = null;
						try {
							in = getHTTPInputStream(path, cs.getUser(), cs.getPassword());
						} catch (Exception e) {
							response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "CCS error: " + e.getMessage());
							return;
						}
						sd2 = ScoreboardUtil.read(in);
					} else {
						request.setAttribute("b", "same");
					}
				} else {
					ConfiguredContest cc2 = CDSConfig.getContest(src);
					path = st + "/api/contests/" + src + "/scoreboard";
					request.setAttribute("b", path);
					bout = new ByteArrayOutputStream();
					Scoreboard.writeScoreboard(new PrintWriter(bout), cc2.getContest());
					bin = new ByteArrayInputStream(bout.toByteArray());
					sd2 = ScoreboardUtil.read(bin);
				}

				request.setAttribute("compare", ScoreboardUtil.compareHTML(sd1, sd2));
				request.getRequestDispatcher("/WEB-INF/jsps/scoreboardCompare.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("validation")) {
				request.getRequestDispatcher("/WEB-INF/jsps/validation.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("validate")) {
				validate(segments[1].substring(8), response, cc);
				return;
			} else if (segments[1].equals("time")) {
				request.getRequestDispatcher("/WEB-INF/jsps/time.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("admin")) {
				if (segments.length == 3) {
					if (segments[2].equals("time")) {
						StartTimeService.doGet(response, cc);
						return;
					}
				}
				if (isAdmin)
					request.getRequestDispatcher("/WEB-INF/jsps/admin.jsp").forward(request, response);
				else
					request.getRequestDispatcher("/WEB-INF/jsps/non-admin.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("freeze")) {
				request.getRequestDispatcher("/WEB-INF/jsps/freeze.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("rss")) {
				response.setContentType("application/rss+xml");
				Contest contest = cc.getContestByRole(request);

				RSSWriter writer = new RSSWriter(response.getWriter(), contest);
				writer.writePrelude();

				ISubmission[] submissions = contest.getSubmissions();
				for (ISubmission s : submissions) {
					if (contest.isSolved(s))
						writer.write(s);
				}

				writer.writePostlude();
				return;
			} else if (segments[1].equals("video")) {
				if (!isAdmin) {
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access only");
					return;
				}
				if (segments.length == 3 && segments[2].equals("status")) {
					response.setContentType("application/json");
					JSONEncoder je = new JSONEncoder(response.getWriter());
					je.open();

					je.openChildArray("teams");
					VideoAggregator va = VideoAggregator.getInstance();
					Map<String, Map<StreamType, List<Integer>>> streamMap = cc.getStreams();
					Map<StreamType, VideoStats> videoStats = new HashMap<>();

					for (String teamId : streamMap.keySet()) {
						Map<StreamType, List<Integer>> teamStreams = streamMap.get(teamId);
						je.open();
						je.encode("team_id", teamId);
						long teamTime = 0;

						for (StreamType s : teamStreams.keySet()) {
							VideoStats vs = videoStats.get(s);
							if (vs == null) {
								vs = new VideoStats();
								videoStats.put(s, vs);
							}

							je.openChild(s.toString().toLowerCase());
							je.openChildArray("streams");
							int time = 0;
							int current = 0;
							int listeners = 0;
							List<Integer> streams = teamStreams.get(s);
							for (Integer in : streams) {
								je.open();
								je.encode("id", in);
								VideoStream stream = va.getStream(in);
								je.encode("mode", stream.getMode().name().toLowerCase());
								je.encode("status", stream.getStatus().name().toLowerCase());
								Stats stats = stream.getStats();
								je.encode("time", ContestUtil.formatTime(stats.getTotalTime()));
								je.encode("current", stats.getCurrentListeners());
								je.encode("total", stats.getTotalListeners());
								time += stats.getTotalTime();
								teamTime += stats.getTotalTime();
								vs.time += stats.getTotalTime();
								current += stats.getCurrentListeners();
								vs.current += stats.getCurrentListeners();
								listeners = stats.getTotalListeners();
								vs.total += stats.getTotalListeners();
								vs.mode = stream.getMode().name().toLowerCase();
								vs.numStreams++;

								je.close();
							}
							je.closeArray();
							je.encode("current", current);
							je.encode("total_listeners", listeners);
							je.encode("total_time", ContestUtil.formatTime(time));
							je.close();

						}
						je.encode("total_time", ContestUtil.formatTime(teamTime));
						je.close();
					}

					je.closeArray();

					int numStreams = 0;
					for (StreamType s : videoStats.keySet()) {
						VideoStats vs = videoStats.get(s);
						je.openChild(s.toString().toLowerCase());
						je.encode("num_streams", vs.numStreams);
						numStreams += vs.numStreams;
						je.encode("current", vs.current);
						je.encode("total_listeners", vs.total);
						je.encode("total_time", ContestUtil.formatTime(vs.time));
						je.encode("mode", vs.mode);
						je.close();
					}

					je.encode("num_streams", numStreams);
					je.encode("current", va.getConcurrent());
					je.encode("max_current", va.getMaxConcurrent());
					je.encode("total_listeners", va.getTotal());
					je.encode("total_time", ContestUtil.formatTime(va.getTotalTime()));
					je.close();
					return;
				}

				request.getRequestDispatcher("/WEB-INF/jsps/video.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("reports")) {
				request.getRequestDispatcher("/WEB-INF/jsps/reports.jsp").forward(request, response);
				return;
			} else if (segments[1].startsWith("balloon")) {
				BalloonPDFService.generate(request, response, cc);
				return;
			} else if (segments[1].equals("resolver")) {
				ResolverService.doGet(response, cc);
				return;
			} /* else if (segments.length == 4 && segments[1].equals("video") && segments[2].equals("map")) {
				VideoMapper va = null;
				if (segments[3].equals("webcam"))
					va = WebcamAggregator.getInstance();
				else if (segments[3].equals("desktop"))
					va = DesktopAggregator.getInstance();
				VideoStatusServlet.writeStatusImage(cc, va, response.getOutputStream());
				return;
				}*/
		}
		if (segments.length > 1) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		request.getRequestDispatcher("/WEB-INF/jsps/overview.jsp").forward(request, response);
	}

	private static void validate(String file, ServletResponse response, ConfiguredContest cc) throws IOException {
		PrintWriter writer = response.getWriter();
		ContestSource source = cc.getContestSource();
		Validation v = source.validate();
		if (v.messages.isEmpty())
			writer.print("No validation");
		else {
			for (String s : v.messages) {
				writer.println(s + "<br/>");
			}
		}
	}
}