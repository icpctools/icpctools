package org.icpc.tools.contest.model.internal;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NetworkUtil {
	private static String localAddress;

	public static String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			// ignore
			return null;
		}
	}

	public static String getLocalAddress() {
		if (localAddress != null)
			return localAddress;

		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("google.com", 80));
			localAddress = socket.getLocalAddress().getHostAddress();
			return localAddress;
		} catch (Exception e) {
			// ignore
		}
		try {
			localAddress = InetAddress.getLocalHost().getHostAddress();
			return localAddress;
		} catch (Exception e) {
			// ignore
		}
		return null;
	}
}