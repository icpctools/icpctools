package org.icpc.tools.cds.presentations;

import java.security.Principal;

import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.contest.model.IAccount;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

public class WebSocketConfig extends ServerEndpointConfig.Configurator {
	public static class UserInfo {
		public boolean isAdmin;
		public boolean isBlue;
		public boolean isBalloon;
	}

	@Override
	public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
		Principal principal = request.getUserPrincipal();
		if (principal == null)
			return;

		String user = principal.getName();
		if (config.getUserProperties().containsKey(user))
			return;

		WebSocketConfig.UserInfo info = new WebSocketConfig.UserInfo();
		IAccount account = CDSConfig.getInstance().getAccount(user);
		if (account != null) {
			String type = account.getAccountType();
			info.isAdmin = IAccount.ADMIN.equals(type) || IAccount.PRES_ADMIN.equals(type);
			info.isBlue = IAccount.STAFF.equals(type);
			info.isBalloon = IAccount.BALLOON.equals(type);
		}
		config.getUserProperties().put(user, info);
	}
}