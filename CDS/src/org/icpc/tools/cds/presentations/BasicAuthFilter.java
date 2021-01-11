package org.icpc.tools.cds.presentations;

import java.io.IOException;
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Authorization filter for the REST API and presentations. Requests that are already authorized
 * (typically through a browser and form-based login on the webpages) go through unchanged.
 * Unauthorized requests are checked for a basic auth header. Requests without a valid access token
 * are refused with a <code>401</code>.
 */
@WebFilter(urlPatterns = { "/presentation/*", "/api/*" }, asyncSupported = true)
public class BasicAuthFilter implements Filter {
	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BASIC_PREFIX = "Basic ";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// ignore
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		if (request.getRemoteUser() != null) {
			filterChain.doFilter(request, response);
			return;
		}

		String authHeader = request.getHeader(AUTHORIZATION_HEADER);
		if (authHeader == null || !authHeader.startsWith(BASIC_PREFIX)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "BASIC authentication is required");
			return;
		}

		String authValue = authHeader.substring(BASIC_PREFIX.length());
		String userAndPassword = new String(Base64.getDecoder().decode(authValue));
		if (!userAndPassword.contains(":")) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "BASIC authentication is required");
			return;
		}

		String user = userAndPassword.substring(0, userAndPassword.indexOf(':'));
		String password = userAndPassword.substring(userAndPassword.indexOf(':') + 1);

		try {
			request.login(user, password);
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid user or password");
			return;
		}
		filterChain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		// ignore
	}
}