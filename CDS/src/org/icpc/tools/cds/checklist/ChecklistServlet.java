package org.icpc.tools.cds.checklist;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.util.Role;
import org.icpc.tools.contest.Trace;

@WebServlet(urlPatterns = "/checklist/*")
@ServletSecurity(@HttpConstraint(transportGuarantee = ServletSecurity.TransportGuarantee.CONFIDENTIAL, rolesAllowed = {
		Role.ADMIN, Role.BLUE, Role.TRUSTED, Role.PUBLIC }))
public class ChecklistServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected Map<String, Item[]> items = new HashMap<>();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();
		if (path == null || !path.startsWith("/")) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String file = null;
		try {
			file = path.substring(1);
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (items.get(file) == null) {
			Item[] newItems = new Item[] { new Item("Configure the contest", "John", 0), new Item("Create CDP", "Doug", 1),
					new Item("Configure CDS", "Tim", 1), new Item("Start the CDS", "Tim", 0),
					new Item("Check config and run", "Tim", 1), new Item("Start contest", "Kattis", 0) };
			items.put(file, newItems);
		}

		request.setAttribute("file", file);
		request.setAttribute("name", "Contest Startup");
		request.setAttribute("checklist", items.get(file));
		getServletContext().getRequestDispatcher("/WEB-INF/jsps/checklist.jsp").forward(request, response);
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();
		if (path == null || !path.startsWith("/checklist/")) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		path = path.substring(11);

		if (!path.contains("/"))
			return;

		int ind = path.indexOf("/");
		String file = path.substring(0, ind);

		String command = path.substring(ind + 1);
		System.out.println("Checklist command: " + file + " : " + command);
		try {
			char tf = command.charAt(0);
			boolean complete = 't' == tf;
			int id = Integer.parseInt(command.substring(1));
			Item[] items2 = items.get(file);
			for (Item i : items2) {
				if (i.id == id)
					i.isComplete = complete;
			}
			response.getWriter().print(complete);
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			Trace.trace(Trace.ERROR, "Error processing checklist", e);
		}
	}
}