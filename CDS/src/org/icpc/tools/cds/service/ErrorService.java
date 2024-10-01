package org.icpc.tools.cds.service;

import java.io.IOException;
import java.io.PrintWriter;

import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.feed.JSONWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

		// if the client accepts html, return the error via html
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("html")) {
			request.getRequestDispatcher("/WEB-INF/jsps/errorPage.jsp").forward(request, response);
			return;
		}

		// if client accepts json or it's an API call, return json
		if ((accept != null && accept.contains("json")) || request.getRequestURI().startsWith("/api")) {
			response.setContentType("application/json");

			JsonObject obj = new JsonObject();
			obj.put("code", response.getStatus());
			obj.put("message", request.getAttribute("javax.servlet.error.message"));

			JSONWriter jw = new JSONWriter(response.getWriter());
			jw.writeObject(obj);
			jw.flush();
			return;
		}

		// otherwise, send plain text
		PrintWriter pw = response.getWriter();
		pw.write(request.getAttribute("javax.servlet.error.message") + " (" + response.getStatus() + ")");
		pw.flush();
	}
}