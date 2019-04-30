package org.icpc.tools.contest.util.cms;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HTTPSSecurity {
	private static class CDSHostVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String paramString, SSLSession paramSSLSession) {
			// System.err.println("verify: " + paramString + " " + paramSSLSession);
			return true;
		}
	}

	public static class CDSTrustManager implements X509TrustManager {
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
	}

	public static HttpURLConnection createConnection(URL url) throws IOException {
		try {
			System.setProperty("jsse.enableSNIExtension", "false");

			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(null, new TrustManager[] { new CDSTrustManager() }, null);
			SSLContext.setDefault(ctx);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			if (conn instanceof HttpsURLConnection) {
				((HttpsURLConnection) conn).setHostnameVerifier(new CDSHostVerifier());
			}

			return conn;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}
	}
}