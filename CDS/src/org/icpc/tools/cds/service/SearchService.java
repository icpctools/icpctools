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
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.ILanguage;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.JSONEncoder;

@WebServlet(urlPatterns = { "/search", "/search/*" })
public class SearchService extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		String path = request.getPathInfo();
		if (path == null || !path.startsWith("/")) {
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setHeader("X-Frame-Options", "sameorigin");
			response.setHeader("Content-Security-Policy", "frame-ancestors");
			request.getRequestDispatcher("/WEB-INF/jsps/search.jsp").forward(request, response);
			return;
		}

		String searchTerm = null;
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

		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setContentType("application/json");
		JSONEncoder en = new JSONEncoder(response.getWriter());
		search(request, searchTerm, en);
	}

	protected static void write(JSONEncoder e, IContestObject obj) {
		e.open();
		e.encode("type", IContestObject.ContestTypeNames[obj.getType().ordinal()]);
		e.encode("id", obj.getId());
		e.close();
		e.unreset();
	}

	protected static void search(HttpServletRequest request, String searchTerm, JSONEncoder en) {
		Trace.trace(Trace.INFO, "Searching for: " + searchTerm);
		en.openArray();
		en.open();
		en.encode("search_term", searchTerm);
		en.encode3("results");
		en.openArray();

		String search = searchTerm.toLowerCase();

		for (ConfiguredContest cc : CDSConfig.getContests()) {
			IContest contest = cc.getContestByRole(request);
			if (contest == null)
				continue;
			en.open();
			en.encode("contest_id", cc.getId());
			en.encode3("results");
			en.openArray();
			try {
				ILanguage[] langs = contest.getLanguages();
				for (ILanguage lang : langs) {
					if (lang.getName().toLowerCase().contains(search)) {
						write(en, lang);
					}
				}

				IOrganization[] orgs = contest.getOrganizations();
				for (IOrganization org : orgs) {
					if (org.getName().toLowerCase().contains(search) || org.getFormalName().toLowerCase().contains(search)
							|| org.getCountry().toLowerCase().contains(search)) {
						write(en, org);
					}
				}

				IGroup[] groups = contest.getGroups();
				for (IGroup group : groups) {
					if (group.getName().toLowerCase().contains(search)) {
						write(en, group);
					}
				}

				ITeam[] teams = contest.getTeams();
				for (ITeam team : teams) {
					if (team.getName().toLowerCase().contains(search)) {
						write(en, team);
					}
				}
			} catch (Exception e) {
				System.err.println("Could not search " + cc.getId() + ": " + e.getMessage());
			}
			en.closeArray();
			en.close();
			en.unreset();
		}
		en.closeArray();
		// System.out.println("Results: " + list.size());
		en.close();
		en.closeArray();
	}
}