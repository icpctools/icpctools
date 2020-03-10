package org.icpc.tools.cds.service;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/error")
public class ErrorService extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("ICPC-Tools", "CDS");
		response.setHeader("X-Frame-Options", "sameorigin");

		if ("plain".equals(request.getAttribute("error-type"))) {
			PrintWriter pw = response.getWriter();
			pw.write(request.getAttribute("javax.servlet.error.message") + " (" + response.getStatus() + ")");
			pw.flush();
			return;
		}

		request.getRequestDispatcher("/WEB-INF/jsps/errorPage.jsp").forward(request, response);
	}
}