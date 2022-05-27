package org.icpc.tools.contest.model.internal;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

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

		// Try to use a socket to connect to google and find the local IP address for the socket
		// This is the best approach but only works when there is internet
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("google.com", 80));
			localAddress = socket.getLocalAddress().getHostAddress();
			return localAddress;
		} catch (Exception e) {
			// ignore
		}

		// Loop over all network interfaces and find the first non-IPv6, non loopback IP
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					String address = addresses.nextElement().getHostAddress();
					// Do not consider localhost or IPv6 addresses
					if (!address.startsWith("127.") && !address.contains(":")) {
						localAddress = address;
						return address;
					}
				}
			}
		} catch (SocketException e) {
			// ignore
		}

		// All else failed, use the local IP address. This looks up the hostname and tries
		// to resolve it. Most Linux machines map this to 127.0.0.1 or similar, so it's not
		// very useful
		try {
			localAddress = InetAddress.getLocalHost().getHostAddress();
			return localAddress;
		} catch (Exception e) {
			// ignore
		}
		return null;
	}
}