package org.icpc.tools.cds.presentations;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.icpc.tools.cds.util.Role;

public class WebSocketConfig extends ServerEndpointConfig.Configurator {
	public static class UserInfo {
		public boolean isAdmin;
		public boolean isBlue;
		public boolean isBalloon;
	}

	@Override
	public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
		String user = request.getUserPrincipal().getName();
		if (config.getUserProperties().containsKey(user))
			return;

		WebSocketConfig.UserInfo info = new WebSocketConfig.UserInfo();
		info.isAdmin = request.isUserInRole(Role.PRES_ADMIN) | request.isUserInRole(Role.ADMIN);
		info.isBlue = request.isUserInRole(Role.BLUE);
		info.isBalloon = request.isUserInRole(Role.BALLOON);
		config.getUserProperties().put(user, info);
	}
}