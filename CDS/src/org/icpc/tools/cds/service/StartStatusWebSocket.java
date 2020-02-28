package org.icpc.tools.cds.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IStartStatus;
import org.icpc.tools.contest.model.IState;

@ServerEndpoint(value = "/contests/{id}/startstatus")
public class StartStatusWebSocket {
	protected IContestListener listener;
	protected Long lastStart;
	protected boolean lastRunning;
	protected Map<String, List<Session>> map = new HashMap<>();

	public StartStatusWebSocket() {
		listener = (contest, obj, d) -> {
			if (obj instanceof IState) {
				IState state = (IState) obj;
				boolean running = state.isRunning();
				if (running != lastRunning) {
					sendToAll(running ? "started" : "stopped", contest.getId());
					lastRunning = running;
				}
			}
			if (obj instanceof IStartStatus) {
				Long start = contest.getStartStatus();
				if ((start == null && lastStart != null) || (start != null && !start.equals(lastStart))) {
					sendToAll(start + "", contest.getId());
					lastStart = start;
				}
			}
		};
	}

	protected synchronized void sendToAll(String message, String id) {
		synchronized (map) {
			List<Session> sessions = map.get(id);
			if (sessions == null || sessions.isEmpty()) {
				IContest contest = CDSConfig.getContest(id).getContest();
				contest.removeListener(listener);
				return;
			}
			List<Session> remove = new ArrayList<>();
			for (Session s : sessions) {
				try {
					s.getBasicRemote().sendText(message);
				} catch (Exception e) {
					remove.add(s);
				}
			}
			sessions.removeAll(remove);
		}
	}

	@OnOpen
	public void onOpen(Session session, EndpointConfig config, @PathParam("id") String id) {
		Trace.trace(Trace.INFO, "New ws: " + id);
		synchronized (map) {
			IContest contest = CDSConfig.getContest(id).getContest();
			List<Session> sessions = map.get(id);
			if (sessions == null)
				sessions = new ArrayList<>();
			sessions.add(session);
			if (sessions.isEmpty())
				contest.addListener(listener);
			map.put(id, sessions);

			try {
				IState state = contest.getState();
				if (state != null)
					session.getBasicRemote().sendText(state.isRunning() ? "started" : "stopped");
				Long start = contest.getStartStatus();
				session.getBasicRemote().sendText(start + "");
			} catch (Exception e) {
				// ignore
			}

			sessions.add(session);
		}
	}

	@OnClose
	public void onClose(Session session, @PathParam("id") String id) {
		synchronized (map) {
			List<Session> sessions = map.get(id);
			sessions.remove(session);

			if (sessions.isEmpty()) {
				IContest contest = CDSConfig.getContest(id).getContest();
				contest.removeListener(listener);
			}
		}
	}
}