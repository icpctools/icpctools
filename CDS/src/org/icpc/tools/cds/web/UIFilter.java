package org.icpc.tools.cds.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Serves the single-page web UI (built from CDS/ui) under /ui. Static files are served as-is, and
 * any other path is an app route (e.g. /ui/contests/x/teams) that is handled by index.html.
 */
@WebFilter(urlPatterns = "/ui/*")
public class UIFilter extends HttpFilter {
	private static final long serialVersionUID = 1L;

	private static final String INDEX = "/ui/index.html";
	private static final String IMMUTABLE = "/ui/_app/immutable/";

	@Override
	protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		response.setHeader("ICPC-Tools", "CDS");
		response.setHeader("X-Frame-Options", "sameorigin");

		String path = request.getRequestURI().substring(request.getContextPath().length());

		// build output with content hashes in the name never changes
		if (path.startsWith(IMMUTABLE)) {
			response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
			chain.doFilter(request, response);
			return;
		}

		if (!path.endsWith("/") && getServletContext().getResource(path) != null) {
			chain.doFilter(request, response);
			return;
		}

		response.setHeader("Cache-Control", "no-cache");
		request.getRequestDispatcher(INDEX).forward(request, response);
	}
}
