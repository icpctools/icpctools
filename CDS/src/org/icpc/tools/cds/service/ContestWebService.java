package org.icpc.tools.cds.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.RSSWriter;
import org.icpc.tools.cds.presentations.PresentationFilesServlet;
import org.icpc.tools.cds.util.HttpHelper;
import org.icpc.tools.cds.util.Role;
import org.icpc.tools.cds.web.VideoStatusServlet;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.Scoreboard;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.ContestSource.Validation;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.AwardUtil;
import org.icpc.tools.contest.model.util.EventFeedUtil;
import org.icpc.tools.contest.model.util.ScoreboardData;
import org.icpc.tools.contest.model.util.ScoreboardUtil;

@WebServlet(urlPatterns = { "/contests", "/contests/*" }, asyncSupported = true)
@ServletSecurity(@HttpConstraint(transportGuarantee = ServletSecurity.TransportGuarantee.CONFIDENTIAL, rolesAllowed = {
		Role.ADMIN, Role.BLUE, Role.BALLOON, Role.TRUSTED, Role.PUBLIC }))
public class ContestWebService extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpHelper.setThreadHost(request);
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");

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
				if (!Role.isAdmin(request)) {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
					return;
				}

				if (segments.length == 4) {
					String command = segments[3];
					if (segments[2].equals("time"))
						StartTimeService.doPut(response, command, cc);
					else if (segments[2].equals("status"))
						CountdownStatusService.doPut(response, command, cc);
					else if (segments[2].equals("finalize"))
						FinalizeService.doPut(response, command, cc);
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

	private static InputStream getHTTPInputStream(String url, String user, String password) {
		try {
			HttpURLConnection conn = HTTPSSecurity.createConnection(new URL(url), user, password);
			conn.setReadTimeout(15 * 1000); // 15s timeout
			return new BufferedInputStream(conn.getInputStream());
		} catch (IOException e) {
			Trace.trace(Trace.ERROR, "I/O error downloading", e);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error downloading", e);
		}
		return null;
	}

	private static void doWeb(HttpServletRequest request, HttpServletResponse response, String[] segments,
			ConfiguredContest cc) throws ServletException, IOException {
		request.setAttribute("cc", cc);
		cc.incrementWeb();
		if (segments.length >= 2) {
			if (segments[1].equals("details")) {
				request.setAttribute("version", Trace.getVersion());
				request.getRequestDispatcher("/WEB-INF/jsps/details.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("orgs")) {
				request.getRequestDispatcher("/WEB-INF/jsps/orgs.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("teams")) {
				request.getRequestDispatcher("/WEB-INF/jsps/teams.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("submissions")) {
				request.getRequestDispatcher("/WEB-INF/jsps/submissions.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("teamSummary") && segments.length == 3) {
				request.setAttribute("teamId", segments[2]);
				request.getRequestDispatcher("/WEB-INF/jsps/teamSummary.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("clarifications")) {
				request.getRequestDispatcher("/WEB-INF/jsps/clarifications.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("scoreboard")) {
				request.getRequestDispatcher("/WEB-INF/jsps/scoreboard.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("awards")) {
				request.getRequestDispatcher("/WEB-INF/jsps/awards.jsp").forward(request, response);
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

					/*String st = request.getRequestURL().toString();
					String oldPath = "/contests/" + cc.getId() + "/feedCompare";
					st = st.substring(0, st.length() - oldPath.length());
					String path = st + "/api/contests/" + cc.getId() + "/event-feed";
					request.setAttribute("a", path);
					InputStream in = getHTTPInputStream(path, "admin", "adm1n");
					NDJSONFeedParser parserA = new NDJSONFeedParser();
					Contest contestA = new Contest(false);
					parserA.parse(contestA, in);
					
					Contest contestB = new Contest(false);
					if (cc.getContestSource() instanceof RESTContestSource) {
						RESTContestSource cs = (RESTContestSource) cc.getContestSource();
						path = cs.getURL().toExternalForm() + "event-feed";
						request.setAttribute("b", path);
						in = getHTTPInputStream(path, cs.getUser(), cs.getPassword());
						NDJSONFeedParser parserB = new NDJSONFeedParser();
					
						parserB.parse(contestB, in);
					} else {
						request.setAttribute("b", "same");
					}
					
					Thread.sleep(5000);*/

					request.setAttribute("info", EventFeedUtil.compareInfo(contestA, contestB).printSingletonSummaryHTML());
					request.setAttribute("problems", EventFeedUtil.compareProblems(contestA, contestB).printSummaryHTML());
					request.setAttribute("languages", EventFeedUtil.compareLanguages(contestA, contestB).printSummaryHTML());
					request.setAttribute("judgement-types",
							EventFeedUtil.compareJudgementTypes(contestA, contestB).printSummaryHTML());
					request.setAttribute("groups", EventFeedUtil.compareGroups(contestA, contestB).printSummaryHTML());
					request.setAttribute("organizations",
							EventFeedUtil.compareOrganizations(contestA, contestB).printSummaryHTML());
					request.setAttribute("teams", EventFeedUtil.compareTeams(contestA, contestB).printSummaryHTML());
					request.setAttribute("submissions",
							EventFeedUtil.compareSubmissions(contestA, contestB).printSummaryHTML());
					request.setAttribute("judgements",
							EventFeedUtil.compareJudgements(contestA, contestB).printSummaryHTML());
					request.setAttribute("awards", EventFeedUtil.compareAwards(contestA, contestB).printSummaryHTML());
					request.getRequestDispatcher("/WEB-INF/jsps/contestCompare.jsp").forward(request, response);
				} catch (Exception e) {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
				return;
			} else if (segments[1].equals("scoreboardCompare")) {
				if (!Role.isBlue(request))
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
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
						InputStream in = getHTTPInputStream(path, cs.getUser(), cs.getPassword());
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
					} else if (segments[2].equals("status")) {
						CountdownStatusService.doGet(response, cc);
						return;
					}
				}
				request.getRequestDispatcher("/WEB-INF/jsps/admin.jsp").forward(request, response);
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
			} else if (segments.length == 3 && segments[1].equals("video") && segments[2].equals("status")) {
				VideoStatusServlet.doGet(cc, request, response);
				return;
			} else if (segments[1].equals("reports")) {
				request.getRequestDispatcher("/WEB-INF/jsps/reports.jsp").forward(request, response);
				return;
			} else if (segments[1].equals("floor-map.tsv")) {
				File f = new File(cc.getLocation() + File.separator + "config" + File.separator + "floor-map.tsv");
				PresentationFilesServlet.sendFile(f, request, response);
				return;
			} else if (segments[1].equals("staff-members.tsv")) {
				File f = new File(cc.getLocation() + File.separator + "config" + File.separator + "staff-members.tsv");
				PresentationFilesServlet.sendFile(f, request, response);
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