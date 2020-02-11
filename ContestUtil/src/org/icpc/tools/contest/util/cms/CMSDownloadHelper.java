package org.icpc.tools.contest.util.cms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * CMS download helper. Downloads all relevant contest data from the CMS.
 *
 * Place a file called login.tsv in the same folder as this class. It should contain 3
 * tab-separated columns: folder-name, CMS-key, and CMS-access-token. If there is more than one
 * line this utility will download each contest in order.
 *
 * Arguments: output folder
 */
public class CMSDownloadHelper {
	private static final String ROOT_URL = "https://icpc.baylor.edu/cm5-contest-rest/rest/contest/myicpc/";
	private static final String ROOT_URL_2 = "https://icpc.baylor.edu/cm5-contest-rest/rest/contest/export/CLICS/CONTEST/";
	protected static File ROOT_FOLDER;
	protected static File TEAM_FOLDER;
	protected static File INSTITUTION_FOLDER;

	public static class ContestInfo {
		String shortName; // (typically used as a folder name)
		String key;
		String token;

		public ContestInfo(String name, String key, String token) {
			this.shortName = name;
			this.key = key;
			this.token = token;
		}
	}

	private static final String TOKEN = CMSLogin.getContests()[0].token;

	protected static void configure(String[] args) {
		if (args == null || args.length != 1) {
			System.err.println("Usage: command [folder] [option]");
			System.exit(1);
		}

		setRootFolder(args[0]);
	}

	protected static void setRootFolder(String f) {
		ROOT_FOLDER = new File(f);
		if (!ROOT_FOLDER.exists()) {
			System.err.println("Root folder does not exist");
			System.exit(1);
		}

		TEAM_FOLDER = new File(ROOT_FOLDER, "teams");
		INSTITUTION_FOLDER = new File(ROOT_FOLDER, "institutions");
	}

	public static void main(String[] args) {
		configure(args);

		for (int i = 0; i < CMSLogin.getContests().length; i++) {
			ContestInfo contestInfo = CMSLogin.getContests()[i];
			System.out.println(contestInfo.shortName);
			downloadYear(contestInfo);
		}

		if (!TEAM_FOLDER.exists())
			TEAM_FOLDER.mkdirs();

		if (!INSTITUTION_FOLDER.exists())
			INSTITUTION_FOLDER.mkdirs();
	}

	protected static File getFolder(String year) {
		return new File(ROOT_FOLDER, year);
	}

	protected static void downloadYear(ContestInfo contestInfo) {
		System.out.println("---- " + contestInfo.shortName + " ----");
		File folder = getFolder(contestInfo.shortName);
		if (!folder.exists())
			folder.mkdirs();

		try {
			CMSDownloadHelper.downloadFile(ROOT_URL_2 + contestInfo.key, contestInfo.token, new File(folder, "wf.json"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			CMSDownloadHelper.downloadFile(ROOT_URL + contestInfo.key + "/details", contestInfo.token,
					new File(folder, "details.json"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			CMSDownloadHelper.downloadFile(ROOT_URL + contestInfo.key + "/teams", contestInfo.token,
					new File(folder, "teams.json"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			CMSDownloadHelper.downloadFile(ROOT_URL + contestInfo.key + "/institutions", contestInfo.token,
					new File(folder, "institutions.json"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			CMSDownloadHelper.downloadFile(ROOT_URL + contestInfo.key + "/sites", contestInfo.token,
					new File(folder, "sites.json"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			CMSDownloadHelper.downloadFile(ROOT_URL + contestInfo.key + "/staff-members", contestInfo.token,
					new File(folder, "staff-members.json"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void downloadTeam(String teamId) {
		try {
			CMSDownloadHelper.downloadFile(ROOT_URL + "team/" + teamId + "/", TOKEN,
					new File(TEAM_FOLDER, teamId + ".json"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			CMSDownloadHelper.downloadFile(ROOT_URL + "team/" + teamId + "/past-results", TOKEN,
					new File(TEAM_FOLDER, teamId + "-past-results.json"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void downloadInstitution(String institutionId) {
		try {
			CMSDownloadHelper.downloadFile(ROOT_URL + "institution/" + institutionId + "/", TOKEN,
					new File(INSTITUTION_FOLDER, institutionId + ".json"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			CMSDownloadHelper.downloadFile(ROOT_URL + "institution/" + institutionId + "/attend-wf", TOKEN,
					new File(INSTITUTION_FOLDER, institutionId + "-attend-wf.json"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void downloadFile(String url, String token, File f) throws Exception {
		if (f == null || f.exists())
			return;

		System.out.print("   " + f.getName() + " (" + url + ") [");
		HttpURLConnection conn = HTTPSSecurity.createConnection(new URL(url));
		if ("wf.json".equals(f.getName()))
			conn.setRequestProperty("Accept", "application/json");
		// String auth = Base64.getEncoder().encodeToString((token + ":").getBytes("UTF-8"));
		// conn.setRequestProperty("Authorization", "Basic " + auth);
		conn.setRequestProperty("Authorization", "bearer " + getOAuthToken(token));
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(90000);
		conn.setRequestMethod("GET");

		if (conn.getResponseCode() != 200)
			throw new Exception(conn.getResponseCode() + ": " + conn.getResponseMessage());

		BufferedInputStream bin = new BufferedInputStream(conn.getInputStream());
		BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(f));
		byte[] b = new byte[8096];
		int n = bin.read(b);
		while (n >= 0) {
			System.out.print(".");
			bout.write(b, 0, n);
			n = bin.read(b);
		}
		bout.close();
		System.out.println("]");
	}

	/**
	 * curl -d "client_id=cm5-token" -d "username=token:<webservice access token>" -d "password=" -d
	 * "grant_type=password" "https://icpc.baylor.edu/auth/realms/cm5/protocol/openid-connect/token"
	 * | sed 's/.*access_token":"//g' | sed ...
	 */
	protected static String getOAuthToken(String token) throws Exception {
		String url = "https://icpc.baylor.edu/auth/realms/cm5/protocol/openid-connect/token";
		StringBuilder token2 = new StringBuilder();

		// url += "?";
		String body = "client_id=cm5-token";
		body += "&username=token:" + token;
		body += "&password=";
		body += "&grant_type=password";

		System.out.print("token");
		// System.out.println(url);
		// System.out.println(body);
		HttpURLConnection conn = HTTPSSecurity.createConnection(new URL(url));
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		/*conn.setRequestProperty("client_id", "cm5-token");
		String auth = Base64.getEncoder().encodeToString((token + ":").getBytes("UTF-8"));
		conn.setRequestProperty("username", "token:" + auth);
		conn.setRequestProperty("password", "");
		conn.setRequestProperty("grant_type", "password");*/

		// conn.setRequestProperty("Authorization", "Basic " + auth);
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(90000);
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");

		OutputStream out = conn.getOutputStream();
		out.write(body.getBytes());
		out.flush();

		if (conn.getResponseCode() != 200)
			throw new Exception(conn.getResponseCode() + ": " + conn.getResponseMessage());

		BufferedInputStream bin = new BufferedInputStream(conn.getInputStream());
		// BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(f));
		byte[] b = new byte[8096];
		int n = bin.read(b);
		while (n >= 0) {
			System.out.print(".");
			token2.append(new String(b));
			n = bin.read(b);
		}
		// System.out.println("]");
		int ind = token2.indexOf("access_token");
		String accessToken = token2.substring(ind + 15);
		ind = accessToken.indexOf("\"");
		accessToken = accessToken.substring(0, ind);
		// System.out.println("[" + token2 + "]");
		// System.out.println("[" + accessToken + "]");
		return accessToken;
	}
}