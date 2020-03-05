package org.icpc.tools.cds.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IClarification;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.ILanguage;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.JSONEncoder;

@WebServlet(urlPatterns = { "/search", "/search/*" })
public class SearchService extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("ICPC-Tools", "CDS");
		response.setHeader("X-Frame-Options", "sameorigin");

		String searchTerm = request.getParameter("value");
		if (searchTerm != null) {
			StringBuilder sb = new StringBuilder();
			for (char c : searchTerm.toCharArray()) {
				if (Character.isAlphabetic(c) || Character.isDigit(c) || Character.isWhitespace(c))
					sb.append(c);
				else
					sb.append("-");
			}
			request.setAttribute("value", sb.toString());
			request.getRequestDispatcher("/WEB-INF/jsps/search.jsp").forward(request, response);
			return;
		}

		String path = request.getPathInfo();
		if (path == null || !path.startsWith("/")) {
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setHeader("X-Frame-Options", "sameorigin");
			response.setHeader("Content-Security-Policy", "frame-ancestors");
			request.getRequestDispatcher("/WEB-INF/jsps/search.jsp").forward(request, response);
			return;
		}

		try {
			searchTerm = path.substring(1);
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (searchTerm.length() < 3) {
			response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Search string too short");
			return;
		}

		if (!searchTerm.matches("[\\w*\\s*]*")) {
			response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Please enter only letters, numbers, and spaces.");
			return;
		}

		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setContentType("application/json");
		JSONEncoder en = new JSONEncoder(response.getWriter());
		search(request, searchTerm, en);
	}

	protected static void write(JSONEncoder e, IContestObject obj, String text) {
		e.open();
		e.encode("type", IContestObject.ContestTypeNames[obj.getType().ordinal()]);
		e.encode("id", obj.getId());
		e.encode("text", text);
		e.close();
	}

	protected static void search(HttpServletRequest request, String searchTerm, JSONEncoder en) {
		Trace.trace(Trace.INFO, "Searching for: " + searchTerm);
		en.openArray();
		en.open();
		en.encode("search_term", searchTerm);
		en.openChildArray("results");

		String search = searchTerm.toLowerCase();

		for (ConfiguredContest cc : CDSConfig.getContests()) {
			IContest contest = cc.getContestByRole(request);
			if (contest == null)
				continue;
			en.open();
			en.encode("contest_id", cc.getId());
			en.openChildArray("results");
			try {
				ILanguage[] langs = contest.getLanguages();
				for (ILanguage lang : langs) {
					if (lang.getName().toLowerCase().contains(search)) {
						write(en, lang, lang.getName());
					}
				}

				IProblem[] probs = contest.getProblems();
				for (IProblem prob : probs) {
					if (prob.getName().toLowerCase().contains(search)) {
						write(en, prob, prob.getName());
					}
				}

				IOrganization[] orgs = contest.getOrganizations();
				for (IOrganization org : orgs) {
					if (org.getName().toLowerCase().contains(search)) {
						write(en, org, org.getName());
					} else if (org.getFormalName() != null && org.getFormalName().toLowerCase().contains(search)) {
						write(en, org, org.getFormalName());
					} else if (org.getCountry() != null && org.getCountry().toLowerCase().contains(search)) {
						write(en, org, org.getCountry());
					}
				}

				IGroup[] groups = contest.getGroups();
				for (IGroup group : groups) {
					if (group.getName().toLowerCase().contains(search)) {
						write(en, group, group.getName());
					}
				}

				ITeam[] teams = contest.getTeams();
				for (ITeam team : teams) {
					if (team.getName().toLowerCase().contains(search)) {
						write(en, team, team.getName());
					} else if (team.getDisplayName() != null && team.getDisplayName().toLowerCase().contains(search)) {
						write(en, team, team.getDisplayName());
					}
				}

				IClarification[] clars = contest.getClarifications();
				for (IClarification clar : clars) {
					if (clar.getText().toLowerCase().contains(search)) {
						write(en, clar, clar.getText());
					}
				}
			} catch (Exception e) {
				System.err.println("Could not search " + cc.getId() + ": " + e.getMessage());
			}
			en.closeArray();
			en.close();
		}
		en.closeArray();
		en.close();
		en.closeArray();
	}
}