package org.icpc.tools.cds.presentations;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.presentations.Client.ClientDisplay;
import org.icpc.tools.cds.util.Role;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.JSONEncoder;

@WebServlet(urlPatterns = "/presentation/admin/*")
@ServletSecurity(@HttpConstraint(transportGuarantee = ServletSecurity.TransportGuarantee.CONFIDENTIAL, rolesAllowed = {
		Role.ADMIN, Role.PRES_ADMIN }))
public class PropertyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();

		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setContentType("application/json");
		if ("/clients".equals(path)) {
			PrintWriter pw = response.getWriter();
			outputClients(pw);
			return;
		} else if ("/present".equals(path)) {
			PrintWriter pw = response.getWriter();
			outputPresentations(pw);
			return;
		} else if ("/image".equals(path)) {
			//
		} else if ("/web".equals(path)) {
			request.getRequestDispatcher("/WEB-INF/jsps/present.jsp").forward(request, response);
			return;
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	}

	protected void outputClients(PrintWriter pw) {
		PresentationServer ps = PresentationServer.getInstance();
		List<Client> clients = ps.getClients();

		JSONEncoder en = new JSONEncoder(pw);
		en.openArray();
		for (Client c : clients) {
			en.open();
			en.encode("uid", c.getUID());
			en.encode("id", c.getId());
			if (c.getPresentation() != null)
				en.encode("presentation", c.getPresentation());
			ClientDisplay[] displays = c.getDisplays();
			if (displays != null && displays.length > 0)
				en.encode("display", displays[0].width + "x" + displays[0].height + "@" + displays[0].refresh);
			en.close();
			en.unreset();
		}
		en.closeArray();
	}

	protected void outputPresentations(PrintWriter pw) throws IOException {
		PresentationCache.getPresentationCacheAdmin();
		File cache = PresentationCache.getPresentationCache();
		if (cache == null)
			return;
		File f = new File(cache, "META-INF/presentations.xml");
		PresentationsParser parser = new PresentationsParser();
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
		try {
			parser.load(in);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading presentation xml", e);
		}
		in.close();

		List<PresentationInfo> presentations = parser.getPresentations();

		JSONEncoder en = new JSONEncoder(pw);
		en.openArray();
		for (PresentationInfo p : presentations) {
			en.open();
			en.encode("id", p.getId());
			en.encode("name", p.getName());
			en.encode("description", p.getDescription());
			en.encode("category", p.getCategory());
			en.encode("classname", p.getClassName());
			en.close();
			en.unreset();
		}
		en.closeArray();
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();
		if (path == null || path.length() < 2) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		if ("/property".equals(path)) {
			BufferedReader br = request.getReader();
			String key = br.readLine();
			String value = br.readLine();

			// TODO
			PresentationServer.getInstance().setProperty(null, key, value);
		}
		if (path.startsWith("/present/")) {
			String presId = path.substring(9);

			PresentationServer ps = PresentationServer.getInstance();
			List<Client> clients = ps.getClients();

			List<Integer> uidList = new ArrayList<>();
			for (Client c : clients) {
				if (!c.isAdmin())
					uidList.add(c.getUID());
			}

			int[] uids = new int[uidList.size()];
			for (int i = 0; i < uids.length; i++)
				uids[i] = uidList.get(i);

			Trace.trace(Trace.INFO, "Setting presentation " + presId + " to " + uids.length + " clients");
			ps.setProperty(uids, "presentation", "1100|" + presId);
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	}
}