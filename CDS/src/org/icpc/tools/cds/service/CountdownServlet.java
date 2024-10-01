package org.icpc.tools.cds.service;

import java.io.IOException;

import org.icpc.tools.cds.CDSAuth;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/countdown", asyncSupported = true)
public class CountdownServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!CDSAuth.isAdmin(request)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		}

		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("ICPC-Tools", "CDS");
		response.setHeader("X-Frame-Options", "sameorigin");
		request.getRequestDispatcher("/WEB-INF/jsps/countdown.jsp").forward(request, response);
	}
}
