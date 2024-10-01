package org.icpc.tools.cds.web;

import java.io.IOException;

import org.icpc.tools.cds.presentations.PresentationServer;
import org.icpc.tools.contest.Trace;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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