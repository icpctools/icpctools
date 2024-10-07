package org.icpc.tools.cds.service;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.feed.JSONWriter;

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

		// get error message
		Object errMsg = request.getAttribute("javax.servlet.error.message");
		String errorMessage = (errMsg != null) ? errMsg.toString() : "Unknown error";

		// if client accepts json or it's an API call, return json
		if ((accept != null && accept.contains("json")) || request.getRequestURI().startsWith("/api")) {
			response.setContentType("application/json");

			JsonObject obj = new JsonObject();
			obj.put("code", response.getStatus());
			obj.put("message", errorMessage);

			JSONWriter jw = new JSONWriter(response.getWriter());
			jw.writeObject(obj);
			jw.flush();
			return;
		}

		// otherwise, send plain text
		PrintWriter pw = response.getWriter();
		pw.write(errorMessage + " (" + response.getStatus() + ")");
		pw.flush();
	}
}