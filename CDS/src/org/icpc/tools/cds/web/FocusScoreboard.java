package org.icpc.tools.cds.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.presentations.PresentationServer;
import org.icpc.tools.contest.Trace;

@WebServlet(urlPatterns = { "/focus", "/focus/*" })
public class FocusScoreboard extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.getRequestDispatcher("/focusScoreboard.jsp").forward(request, response);
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();
		if (path == null || !path.startsWith("/")) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String teamId = null;
		try {
			teamId = path.substring(1);
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		Trace.trace(Trace.USER, "Changing focus scoreboard to: " + teamId);
		PresentationServer ps = PresentationServer.getInstance();
		ps.setProperty(null, "org.icpc.tools.cds.presentation.contest.internal.presentations.ScoreboardPresentation",
				"focusTeam" + teamId);
	}
}