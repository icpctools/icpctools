package org.icpc.tools.cds.service;

import org.icpc.tools.cds.CDSAuth;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/countdown", asyncSupported = true)
public class CountdownServlet extends HttpServlet {
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
