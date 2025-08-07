package org.icpc.tools.contest.model.feed;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

public class HTTPSSecurity {
	private static class AllHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String paramString, SSLSession paramSSLSession) {
			// ignore
			return true;
		}
	}

	public static class ContestTrustManager extends X509ExtendedTrustManager {
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			// ignore
			return null;
		}

		@Override
		public void checkServerTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString)
				throws CertificateException {
			// ignore
		}

		@Override
		public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString)
				throws CertificateException {
			// ignore
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
				throws CertificateException {
			// ignore
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
				throws CertificateException {
			// ignore
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
				throws CertificateException {
			// ignore
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
				throws CertificateException {
			// ignore
		}
	}

	public static HttpURLConnection createConnection(URL url, String user, String password) throws IOException {
		return (HttpURLConnection) createURLConnection(url, user, password);
	}

	public static URLConnection createURLConnection(URL url, String user, String password) throws IOException {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(null, new TrustManager[] { new ContestTrustManager() }, null);
			SSLContext.setDefault(ctx);

			URLConnection conn = url.openConnection();
			if (conn instanceof HttpsURLConnection) {
				((HttpsURLConnection) conn).setHostnameVerifier(new AllHostnameVerifier());
				((HttpsURLConnection) conn).setSSLSocketFactory(ctx.getSocketFactory());
			}
			if (user != null) {
				String auth = Base64.getEncoder().encodeToString((user + ":" + password).getBytes("UTF-8"));
				conn.setRequestProperty("Authorization", "Basic " + auth);
			}
			conn.setConnectTimeout(10000);

			return conn;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}
	}
}
