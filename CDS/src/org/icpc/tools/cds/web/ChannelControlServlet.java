package org.icpc.tools.cds.web;

import java.io.IOException;

import org.icpc.tools.cds.CDSAuth;
import org.icpc.tools.cds.video.VideoAggregator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/video/control", "/video/control/*" })
public class ChannelControlServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();
		if (path == null || !path.startsWith("/")) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		int channel = 0;
		try {
			channel = Integer.parseInt(path.substring(1));
		} catch (NumberFormatException nfe) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (channel < 0 || channel >= VideoAggregator.MAX_CHANNELS) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		request.setAttribute("channel", channel + "");
		request.setAttribute("stream", VideoAggregator.getInstance().getChannel(channel));
		request.getRequestDispatcher("/WEB-INF/jsps/channels.jsp").forward(request, response);
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();
		if (!CDSAuth.isAnalyst(request) || path == null || !path.startsWith("/")) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		int index = path.indexOf("/", 1);
		if (index < 1) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		int channel = 0;
		int stream = 0;
		try {
			channel = Integer.parseInt(path.substring(1, index));
			stream = Integer.parseInt(path.substring(index + 1));
		} catch (NumberFormatException nfe) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (channel < 0 || channel >= VideoAggregator.MAX_CHANNELS) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		VideoAggregator va = VideoAggregator.getInstance();
		if (stream < 0 || stream >= va.getNumStreams()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		va.setChannel(channel, stream);
	}
}