package org.icpc.tools.cds.presentations;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.CDSAuth;
import org.icpc.tools.cds.presentations.Client.ClientInfo;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.JSONEncoder;

@WebServlet(urlPatterns = "/presentation/admin/*")
public class PropertyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!CDSAuth.isPresAdmin(request) && !CDSAuth.isAdmin(request)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		String path = request.getPathInfo();

		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setContentType("application/json");
		response.setHeader("X-Frame-Options", "sameorigin");
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
			en.encode("uid", Integer.toHexString(c.getUID()));
			en.encode("name", c.getName());
			ClientInfo info = c.getClientInfo();
			if (info != null) {
				en.encode("presentation", info.presentation);
				en.encode("display", info.width + "x" + info.height + "@" + info.fps);
			}
			if (c.getVersion() != null)
				en.encode("version", c.getVersion());
			en.close();
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
		}
		en.closeArray();
	}

	private static String getPresentationKey(String className) {
		int ind = className.lastIndexOf(".");
		return "property[" + className.substring(ind + 1) + "|" + className.hashCode() + "]";
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!CDSAuth.isPresAdmin(request) && !CDSAuth.isAdmin(request)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		String path = request.getPathInfo();
		if (path == null || path.length() < 2) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		if (path.startsWith("/property")) {
			String val = path.substring(10);
			int ind = val.indexOf("=");
			String key = val;
			String value = null;
			if (ind > 0) {
				key = getPresentationKey(val.substring(0, ind));
				value = val.substring(ind + 1);
			}

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

			PresentationServer.getInstance().setProperty(uids, key, value);
		} else if (path.startsWith("/present/")) {
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
		} else if (path.startsWith("/stop/")) {
			int clientUID = Integer.parseUnsignedInt(path.substring(6), 16);
			PresentationServer ps = PresentationServer.getInstance();
			List<Client> clients = ps.getClients();

			List<Integer> uidList = new ArrayList<>();
			for (Client c : clients) {
				if (c.getUID() == clientUID)
					uidList.add(c.getUID());
			}

			int[] uids = new int[uidList.size()];
			for (int i = 0; i < uids.length; i++)
				uids[i] = uidList.get(i);

			Trace.trace(Trace.INFO, "Stopping client " + Integer.toHexString(clientUID));
			ps.stop(uids);
		} else if (path.startsWith("/restart/")) {
			int clientUID = Integer.parseUnsignedInt(path.substring(9), 16);
			PresentationServer ps = PresentationServer.getInstance();
			List<Client> clients = ps.getClients();

			List<Integer> uidList = new ArrayList<>();
			for (Client c : clients) {
				if (c.getUID() == clientUID)
					uidList.add(c.getUID());
			}

			int[] uids = new int[uidList.size()];
			for (int i = 0; i < uids.length; i++)
				uids[i] = uidList.get(i);

			Trace.trace(Trace.INFO, "Restarting client " + Integer.toHexString(clientUID));
			ps.restart(uids);
		} else if (path.equals("/restart-all")) {
			PresentationServer ps = PresentationServer.getInstance();
			List<Client> clients = ps.getClients();

			int size = clients.size();
			int[] uids = new int[size];
			for (int i = 0; i < size; i++)
				uids[i] = clients.get(i).getUID();

			Trace.trace(Trace.INFO, "Restarting all clients");
			ps.restart(uids);
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	}
}