package org.icpc.tools.cds.presentations;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/presentation/*")
public class PresentationFilesServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();
		if (path == null || !path.startsWith("/")) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		File f = PresentationFilesHelper.getFile(path);
		if (f == null || !f.exists()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (f.isDirectory()) {
			File[] files = f.listFiles();

			request.setAttribute("folder", f.getName());
			request.setAttribute("files", files);
			request.getRequestDispatcher("/WEB-INF/jsps/directory.jsp").forward(request, response);
			return;
		}

		sendFile(f, request, response);
	}

	public static void sendFile(File f, HttpServletRequest request, HttpServletResponse response) throws IOException {
		long lastModified = f.lastModified() / 1000 * 1000;
		try {
			long ifModifiedSince = request.getDateHeader("If-Modified-Since");
			if (ifModifiedSince != -1 && ifModifiedSince >= lastModified) {
				response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			}
		} catch (Exception e) {
			// ignore, send anyway
		}

		response.setContentType("text/plain");
		response.setContentLength((int) f.length());
		response.setDateHeader("Last-Modified", lastModified);

		ServletOutputStream out = response.getOutputStream();
		BufferedInputStream bin = new BufferedInputStream(new FileInputStream(f));
		byte[] b = new byte[1024 * 8];
		int n = bin.read(b);
		while (n != -1) {
			out.write(b, 0, n);
			n = bin.read(b);
		}

		bin.close();
	}
}