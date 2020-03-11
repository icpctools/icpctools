package org.icpc.tools.cds.video;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.imageio.ImageIO;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.service.AppAsyncListener;
import org.icpc.tools.cds.util.Role;
import org.icpc.tools.cds.video.VideoAggregator.ConnectionMode;
import org.icpc.tools.cds.video.VideoAggregator.Stats;
import org.icpc.tools.cds.video.VideoAggregator.Status;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IState;
import org.icpc.tools.contest.model.feed.JSONEncoder;

// video/x?resetAll=true - reset all streams
// video/x?mode=y - set connection mode to y for all teams
// video/x/<teamId> - stream video for the given team id
// video/x/<teamId>?reset - reset video for the given team id
// video/x - list stream status
// video - list status of all streams
// where x = desktop or webcam
@WebServlet(urlPatterns = "/video/*", asyncSupported = true)
public class VideoServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final String RESET_ALL = "resetAll";
	private static final String RESET = "reset";
	private static final String MODE = "mode";

	private static final Dimension SIZE = new Dimension(800, 600);

	private static final Color[] STATUS_COLORS = new Color[] { Color.WHITE, new Color(230, 63, 63),
			new Color(95, 95, 230), new Color(63, 230, 63) };

	private static VideoAggregator va = VideoAggregator.getInstance();

	public static VideoMapper getMapper(String s) {
		if (s.equals("desktop"))
			return VideoMapper.DESKTOP;
		else if (s.equals("webcam"))
			return VideoMapper.WEBCAM;
		else if (s.equals("audio"))
			return VideoMapper.AUDIO;
		return null;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();
		if (path == null || !path.startsWith("/")) {
			response.setContentType("application/json");
			writeStatus(response.getWriter());
			return;
		}

		VideoMapper map = null;
		boolean channel = false;
		if (path.startsWith("/desktop")) {
			map = VideoMapper.DESKTOP;
			path = path.substring(8);
		} else if (path.startsWith("/webcam")) {
			map = VideoMapper.WEBCAM;
			path = path.substring(7);
		} else if (path.startsWith("/audio")) {
			map = VideoMapper.AUDIO;
			path = path.substring(6);
		} else if (path.startsWith("/stream")) {
			path = path.substring(7);
		} else if (path.startsWith("/channel")) {
			path = path.substring(8);
			channel = true;
		}

		if (path == null || !path.startsWith("/")) {
			if (!Role.isBlue(request)) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
			String resetAll = request.getParameter(RESET_ALL);
			String modeParam = request.getParameter(MODE);
			if ((resetAll != null || modeParam != null) && !Role.isAdmin(request)) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			if (resetAll != null) {
				map.resetAll();
				return;
			} else if (modeParam != null) {
				ConnectionMode mode = VideoAggregator.getConnectionMode(modeParam);
				if (mode == null) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND);
					return;
				}
				map.setConnectionMode(mode);
				return;
			}
			response.setContentType("application/json");
			JSONEncoder je = new JSONEncoder(response.getWriter());
			map.writeStatus(je);
			return;
		}

		String teamId = null;
		String filename = "video";
		int stream = -1;
		boolean trusted = Role.isTrusted(request);
		try {
			teamId = path.substring(1);
			if (map != null) {
				stream = map.getVideoStream(teamId);
				filename = teamId;
			} else {
				stream = Integer.parseInt(teamId);
				if (!channel) {
					if (stream < 0 || stream >= VideoAggregator.MAX_STREAMS) {
						response.sendError(HttpServletResponse.SC_NOT_FOUND);
						return;
					}
					filename = "stream-" + stream;
				} else {
					if (stream < 0 || stream >= VideoAggregator.MAX_CHANNELS) {
						response.sendError(HttpServletResponse.SC_NOT_FOUND);
						return;
					}
					filename = "channel-" + stream;
				}
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String reset = request.getParameter(RESET);
		if (reset != null) {
			if (!Role.isAdmin(request)) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
			VideoAggregator.getInstance().reset(stream);
			return;
		}

		// block https connections
		if (request.isSecure() && !Role.isAdmin(request)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Use the Contest API. Incorrect URL, should be http");
			return;
		}

		Trace.trace(Trace.INFO,
				"Video request: " + teamId + " -> " + stream + " " + ConfiguredContest.getUser(request) + " " + channel);

		// check if any contests are in freeze
		if (!trusted) {
			for (ConfiguredContest cc : CDSConfig.getContests()) {
				IState state = cc.getContest().getState();
				if (state.isFrozen() && state.isRunning()) {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Contest is frozen");
					return;
				}
			}
		}

		// increment stats
		if (teamId != null) {
			for (ConfiguredContest cc : CDSConfig.getContests()) {
				if (cc.getContest().getTeamById(teamId) != null) {
					if (map == VideoMapper.DESKTOP)
						cc.incrementDesktop();
					else
						cc.incrementWebcam();
				}
			}
		}

		doVideo(request, response, filename, stream, channel, trusted);
	}

	public static void doVideo(HttpServletRequest request, HttpServletResponse response, final String filename,
			final int stream, boolean channel, boolean trusted) throws IOException {
		response.setHeader("Cache-Control", "no-cache");
		response.setContentType("application/octet");

		OutputStream out = response.getOutputStream();
		va.handler.writeHeader(out, stream);
		final VideoStreamListener listener = new VideoStreamListener(out, trusted);

		String format = va.handler.getFormat();
		response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "." + format + "\"");
		if (!channel)
			va.addStreamListener(stream, listener);
		else
			va.addChannelListener(stream, listener);

		final AsyncContext asyncCtx = request.startAsync();
		asyncCtx.addListener(new AppAsyncListener() {
			@Override
			public void onComplete(AsyncEvent asyncEvent) throws IOException {
				if (!channel)
					va.removeStreamListener(stream, listener);
				else
					va.removeChannelListener(stream, listener);
			}
		});
		asyncCtx.setTimeout(0);
	}

	private static void writeStatus(PrintWriter pw) {
		JSONEncoder je = new JSONEncoder(pw);
		je.open();
		je.openChildArray("streams");
		int c = 0;
		for (VideoStream vi : va.getVideoInfo()) {
			je.open();
			je.encode("id", c++ + "");
			je.encode("name", vi.getName());
			je.encode("order", vi.getOrder());
			je.encode("mode", vi.getMode().name());
			je.encode("status", vi.getStatus().name());
			Stats s = vi.getStats();
			je.encode("current", s.concurrentListeners);
			je.encode("max_current", s.maxConcurrentListeners);
			je.encode("total_listeners", s.totalListeners);
			je.encode("total_time", ContestUtil.formatTime(s.totalTime));
			je.close();
		}
		je.closeArray();
		je.encode("current", va.getConcurrent());
		je.encode("max_current", va.getMaxConcurrent());
		je.encode("total_listeners", va.getTotal());
		je.encode("total_time", ContestUtil.formatTime(va.getTotalTime()));
		je.close();
	}

	public static void writeStatusImage(IContest contest, VideoMapper mapper, OutputStream out) throws IOException {
		FloorMap map = new FloorMap(contest);
		Rectangle r = new Rectangle(SIZE);
		BufferedImage image = new BufferedImage(SIZE.width, SIZE.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) image.getGraphics();
		map.drawFloor(g, r, new FloorMap.FloorColors() {
			@Override
			public Color getDeskFillColor(String teamId) {
				Status status = mapper.getStatus(teamId);
				if (status == null)
					return null;
				return STATUS_COLORS[status.ordinal()];
			}
		}, true);
		g.dispose();

		ImageIO.write(image, "png", out);
	}
}