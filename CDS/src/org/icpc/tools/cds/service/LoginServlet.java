package org.icpc.tools.cds.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/login", "/logout", "/loginError" })
public class LoginServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("ICPC-Tools", "CDS");
		response.setHeader("X-Frame-Options", "sameorigin");
		if ("/loginError".equals(request.getRequestURI()))
			request.getRequestDispatcher("/WEB-INF/jsps/loginError.jsp").forward(request, response);
		else if ("/logout".equals(request.getRequestURI())) {
			request.logout();
			request.getSession().invalidate();
			request.getRequestDispatcher("/WEB-INF/jsps/welcome.jsp").forward(request, response);
		} else
			request.getRequestDispatcher("/WEB-INF/jsps/login.jsp").forward(request, response);
	}
}