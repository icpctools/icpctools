package org.icpc.tools.cds.presentations;

import java.util.List;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAccount;

@ServerEndpoint(value = "/presentation/ws", configurator = WebSocketConfig.class)
public class PresentationWebSocket {
	protected static String getParam(Session s, String key) {
		List<String> list = s.getRequestParameterMap().get(key);
		if (list == null || list.isEmpty())
			return null;

		return list.get(0);
	}

	@OnOpen
	public void onOpen(Session session, EndpointConfig config) {
		// set buffer to 500k. thumbnails are usually under 20k, but snapshots can be much bigger
		session.setMaxTextMessageBufferSize(500 * 1024);

		String name = getParam(session, "name");
		if (name == null) {
			try {
				Trace.trace(Trace.INFO, "Disconnecting client with no name");
				session.close(new CloseReason(CloseCodes.UNEXPECTED_CONDITION, "Client is missing required name"));
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error disconnecting websocket with no name");
			}
			return;
		}

		String uidStr = getParam(session, "uid");
		int uid = 0;
		try {
			uid = Integer.parseUnsignedInt(uidStr, 16);
		} catch (Exception e) {
			Trace.trace(Trace.INFO, "Could not find or parse uid: " + uidStr);
			// ignore
		}
		if (uid == 0) {
			try {
				Trace.trace(Trace.INFO, "Disconnecting client with no uid");
				session.close(new CloseReason(CloseCodes.UNEXPECTED_CONDITION, "Client is missing uid parameter"));
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error disconnecting websocket with invalid uid");
			}
			return;
		}

		if (PresentationServer.getInstance().doesClientExist(uid)) {
			try {
				Trace.trace(Trace.INFO,
						"Disconnecting, client with uid " + Integer.toHexString(uid) + " already logged in.");
				session.close(new CloseReason(CloseCodes.UNEXPECTED_CONDITION, "Client is already logged in"));
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error disconnecting websocket with existing uid");
			}
			return;
		}

		// check version
		String version = getParam(session, "version");
		if (version == null || !version.equals("1.0")) {
			try {
				Trace.trace(Trace.INFO,
						"Disconnecting client " + Integer.toHexString(uid) + " with invalid version: " + version);
				session.close(new CloseReason(CloseCodes.UNEXPECTED_CONDITION,
						"CDS: Client version " + version + " is incompatible with CDS"));
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error disconnecting websocket with invalid version");
			}
			return;
		}

		// check and verify role
		String user = session.getUserPrincipal().getName();
		WebSocketConfig.UserInfo info = null;
		try {
			info = (WebSocketConfig.UserInfo) config.getUserProperties().get(user);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error getting user info", e);
		}
		boolean isAdmin = info.isAdmin;
		String role = getParam(session, "role");
		if ("!admin".equals(role)) {
			if (isAdmin) {
				try {
					Trace.trace(Trace.INFO, "Disconnecting user " + user + " with incorrect admin role");
					session.close(new CloseReason(CloseCodes.UNEXPECTED_CONDITION,
							"CDS: User cannot be an admin - try staff, analyst, or public user"));
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error disconnecting websocket with invalid role");
				}
				return;
			}
		} else if (!"any".equals(role)) {
			boolean isBlue = info.isBlue;
			if (((IAccount.PRES_ADMIN.equals(role)) && !isAdmin)
					|| (IAccount.STAFF.equals(role) && !(isAdmin || isBlue))) {
				try {
					Trace.trace(Trace.INFO, "Disconnecting user " + user + " with insufficient role: " + role);
					session.close(new CloseReason(CloseCodes.UNEXPECTED_CONDITION,
							"CDS: User does not have required role: " + role));
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error disconnecting websocket with invalid role");
				}
				return;
			}
		}

		Client c = new Client(session, user, uid, name, isAdmin);
		PresentationServer.getInstance().addClient(c);
	}

	@OnMessage
	public void onMessage(String message, Session session) {
		try {
			PresentationServer.getInstance().onMessage(session, message);
		} catch (Throwable t) {
			Trace.trace(Trace.ERROR, "Request error", t);
		}
	}

	@OnClose
	public void onClose(Session session) {
		PresentationServer.getInstance().remove(session);
	}
}