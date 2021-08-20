package org.icpc.tools.cds.video;

import java.io.IOException;
import java.io.OutputStream;

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
import org.icpc.tools.cds.video.VideoStream.StreamType;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IState;
import org.icpc.tools.contest.model.feed.JSONEncoder;
// OLD (will be removed once admin page is working again
// video/x?resetAll=true - reset all streams
// video/x?mode=y - set connection mode to y for all teams
// video/x/<teamId> - stream video for the given team id
// video/x/<teamId>?reset - reset video for the given team id
// video/x - list stream status
// video - list status of all streams
// where x = desktop or webcam

// NEW
// stream/x - stream x
// stream/x?reset - reset stream x
// stream/channel/x - stream channel x
// stream/status - list status of all streams
// stream?team=x - action on streams for the given team id
// stream?type=x - action on streams of the given type
// stream?action=reset/eager/lazy/lazy_close - actions on streams
@WebServlet(urlPatterns = "/stream/*", asyncSupported = true)
public class VideoServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final String RESET = "reset";
	private static final String ACTION = "action";
	private static final String TEAM = "team";
	private static final String TYPE = "type";

	private static VideoAggregator va = VideoAggregator.getInstance();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();
		if (path == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		boolean channel = false;
		if (path.startsWith("/status")) {
			response.setContentType("application/json");
			JSONEncoder je = new JSONEncoder(response.getWriter());
			writeStatus(je);
			return;
		} else if (path.startsWith("/channel")) {
			path = path.substring(8);
			channel = true;
		} else if (path.startsWith("/")) {
			path = path.substring(1);

			if (path.isEmpty()) {
				if (!Role.isBlue(request)) {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
					return;
				}
				String actionParam = request.getParameter(ACTION);
				if (actionParam != null) {
					if (!Role.isAdmin(request)) {
						response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
						return;
					}

					String typeParam = request.getParameter(TYPE);
					StreamType streamType = StreamType.valueOf(typeParam);
					if (typeParam != null) {
						streamType = StreamType.valueOf(typeParam);
						if (streamType == null) {
							response.sendError(HttpServletResponse.SC_NOT_FOUND);
							return;
						}
					}
					String teamParam = request.getParameter(TEAM);
					if ("reset".equals(actionParam)) {
						va.reset(teamParam, streamType);
					} else {
						ConnectionMode mode = VideoAggregator.getConnectionMode(actionParam);
						if (mode == null) {
							response.sendError(HttpServletResponse.SC_NOT_FOUND);
							return;
						}
						va.setConnectionMode(teamParam, streamType, mode);
					}

					return;
				}
			}
		}

		String videoId = null;
		String filename = "stream";
		int stream = -1;
		boolean trusted = Role.isTrusted(request);
		try {
			videoId = path.substring(1);
			stream = Integer.parseInt(videoId);

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
		if (videoId != null) {
			StreamType st = va.getStreamType(stream);
			for (ConfiguredContest cc : CDSConfig.getContests()) {
				// if (cc.getContest().getTeamById(videoId) != null) { // TODO 3 per team
				if (st == StreamType.DESKTOP)
					cc.incrementDesktop();
				else if (st == StreamType.WEBCAM)
					cc.incrementWebcam();
				else if (st == StreamType.AUDIO)
					cc.incrementAudio();
			}
		}

		Trace.trace(Trace.INFO, "Video request: " + ConfiguredContest.getUser(request) + " requesting video " + videoId
				+ " -> " + va.getStreamName(stream) + " (channel: " + channel + ")");

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

	private static void writeStatus(JSONEncoder je) {
		je.open();
		je.openChildArray("streams");
		int c = 0;
		for (VideoStream vi : va.getVideoInfo()) {
			je.open();
			je.encode("id", c++ + "");
			je.encode("name", vi.getName());
			je.encode("type", vi.getType().name());
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
}