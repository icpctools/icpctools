package org.icpc.tools.contest.model.feed;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.net.ssl.HttpsURLConnection;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContestListener.Delta;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.feed.LinkParser.ILinkListener;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.FileReference;
import org.icpc.tools.contest.model.internal.Info;
import org.icpc.tools.contest.model.internal.Organization;
import org.icpc.tools.contest.model.internal.Team;

public class RESTContestSource extends DiskContestSource {
	protected static final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
	static {
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(1);
		nf.setGroupingUsed(true);
	}

	private URL url;
	private final String user;
	private final String password;

	private NDJSONFeedParser parser = new NDJSONFeedParser();

	private File feedCacheFile;
	private FileOutputStream feedCacheOut;
	private boolean firstConnection = true;
	private int contestSizeBeforeFeed;

	/**
	 * Creates a REST contest source with a local caching policy.
	 *
	 * @param url
	 * @param user
	 * @param password
	 * @throws MalformedURLException
	 */
	public RESTContestSource(String url, String user, String password) throws MalformedURLException {
		this(new URL(url), user, password);
	}

	/**
	 * Creates a REST contest source with a local caching policy.
	 *
	 * @param url
	 * @param user
	 * @param password
	 * @throws MalformedURLException
	 */
	public RESTContestSource(URL url, String user, String password) {
		super(url.getHost() + url.getPath());

		// make sure URL doesn't end with /
		this.url = removeTrailingSlash(url);

		if (user == null || user.trim().length() == 0)
			this.user = null;
		else
			this.user = user;

		if (password == null || password.trim().length() == 0)
			this.password = null;
		else
			this.password = password;

		instance = this;

		feedCacheFile = new File("events-" + getRemoteContestId() + ".log");
		if (feedCacheFile.exists()) {
			// delete if older than 6h
			if (feedCacheFile.lastModified() < System.currentTimeMillis() - 6 * 60 * 60 * 1000) {
				feedCacheFile.delete();
			}
		}
	}

	/**
	 * Creates a REST contest source backed by a local contest archive.
	 *
	 * @param url
	 * @param user
	 * @param password
	 * @param folder
	 */
	public RESTContestSource(String url, String user, String password, File folder) throws MalformedURLException {
		super(folder, false);

		// make sure URL doesn't end with /
		this.url = removeTrailingSlash(new URL(url));

		if (user == null || user.trim().length() == 0)
			this.user = null;
		else
			this.user = user;

		if (password == null || password.trim().length() == 0)
			this.password = null;
		else
			this.password = password;

		instance = this;

		feedCacheFile = new File("events-" + getRemoteContestId() + ".log");
		if (feedCacheFile.exists()) {
			// delete if older than 6h
			if (feedCacheFile.lastModified() < System.currentTimeMillis() - 6 * 60 * 60 * 1000)
				feedCacheFile.delete();
		}
	}

	public URL getURL() {
		return url;
	}

	protected static URL removeTrailingSlash(URL urls) {
		String extForm = urls.toExternalForm();
		if (!extForm.endsWith("/"))
			return urls;

		try {
			return new URL(extForm.substring(0, extForm.length() - 1));
		} catch (Exception e) {
			// ignore
		}
		return urls;
	}

	protected static URL ensureTrailingSlash(URL urls) {
		String extForm = urls.toExternalForm();
		if (extForm.endsWith("/"))
			return urls;

		try {
			return new URL(extForm + "/");
		} catch (Exception e) {
			// ignore
		}
		return urls;
	}

	protected URL getChildURL(String path) {
		if (path == null || path.isEmpty())
			return url;

		// check for root url
		if (path.startsWith("http"))
			try {
				return new URL(path);
			} catch (Exception e) {
				return null;
			}

		if (path.startsWith("/"))
			try {
				return new URL(url, path);
			} catch (Exception e) {
				return null;
			}

		String extForm = url.toExternalForm();
		try {
			return new URL(extForm + "/" + path);
		} catch (Exception e) {
			return url;
		}
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public String getAuth() throws UnsupportedEncodingException {
		return Base64.getEncoder().encodeToString((user + ":" + password).getBytes("UTF-8"));
	}

	protected InputStream getResource(String path) throws IOException {
		InputStream in = null;

		HttpURLConnection conn = createConnection(path);
		conn.setReadTimeout(10000);

		File localFile = null;
		long localTime = -1;
		localFile = super.getFile(path);
		if (localFile.exists()) {
			localTime = localFile.lastModified();
			conn.setIfModifiedSince(localTime);
		}

		int status = conn.getResponseCode();
		if (status == HttpURLConnection.HTTP_NOT_FOUND)
			return null;

		if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
				|| status == HttpURLConnection.HTTP_SEE_OTHER) {
			conn = createConnection(new URL(conn.getHeaderField("Location")));
			conn.setReadTimeout(10000);
			if (localFile.exists())
				conn.setIfModifiedSince(localTime);
			status = conn.getResponseCode();
		}

		if (status == HttpURLConnection.HTTP_NOT_MODIFIED)
			return new FileInputStream(localFile);

		if (status == HttpURLConnection.HTTP_UNAUTHORIZED)
			throw new IOException("Not authorized (HTTP response code 401)");

		if (conn.getLastModified() <= 0)
			return conn.getInputStream();

		if (localFile.exists())
			localFile.delete();

		in = conn.getInputStream();

		if (!localFile.getParentFile().exists())
			localFile.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(localFile);

		byte[] buf = new byte[8096];
		int n = in.read(buf);
		while (n >= 0) {
			out.write(buf, 0, n);
			n = in.read(buf);
		}
		in.close();
		out.close();
		localFile.setLastModified(conn.getLastModified());

		return new FileInputStream(localFile);
	}

	public HttpURLConnection createConnection(String path) throws IOException {
		return createConnection(getChildURL(path));
	}

	public HttpURLConnection createConnection(URL url2) throws IOException {
		try {
			return HTTPSSecurity.createConnection(url2, user, password);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}
	}

	@Override
	public File getFile(String path) throws IOException {
		File localFile = super.getFile(path);
		if (localFile.exists() && !super.isCache())
			return localFile;

		downloadIfNecessary(getRealURL(path), localFile);
		return localFile;
	}

	@Override
	public File getFile(IContestObject obj, FileReference ref, String property) throws IOException {
		File file = super.getFile(obj, ref, property);

		return downloadIfNecessary(ref, file);
	}

	public File downloadFile(IContestObject obj, FileReference ref, String property) throws IOException {
		File file = super.getNewFile(obj, ref, property);
		if (file == null)
			return null;

		return downloadIfNecessary(ref, file);
	}

	protected String getRealURL(String href) {
		if (href.startsWith("http"))
			return href;

		// if href starts with / it means from the root
		String path = url.toExternalForm();
		int ind = path.indexOf("/api/");
		if (href.startsWith("/")) {
			if (ind > 0)
				return path.substring(0, ind) + href;
		}

		// otherwise, it's relative to /api, so remove the last two segments
		if (ind > 0)
			return path.substring(0, ind + 5) + href;
		return href;
	}

	public File downloadIfNecessary(FileReference ref, File localFile) throws IOException {
		if (localFile == null)
			return null;

		downloadIfNecessary(getRealURL(ref.href), localFile);
		return localFile;
	}

	public void downloadIfNecessary(String href, File localFile) throws IOException {
		try {
			downloadIfNecessaryImpl(href, localFile);
		} catch (Exception e) {
			Trace.trace(Trace.INFO, "Connection failed to " + href + ", trying again", e);
			try {
				downloadIfNecessaryImpl(href, localFile);
			} catch (Exception ex) {
				try {
					Thread.sleep(500);
				} catch (Exception exc) {
					// ignore
				}
				Trace.trace(Trace.INFO, "Connection failed to " + href + " again, trying again after 500ms", e);
				downloadIfNecessaryImpl(href, localFile);
			}
		}
	}

	public void downloadIfNecessaryImpl(String href, File localFile) throws IOException {
		StringBuilder sb = new StringBuilder("Download " + href + " to " + localFile);
		long time = System.currentTimeMillis();
		HttpURLConnection conn = createConnection(href);
		conn.setReadTimeout(10000);

		long localTime = -1;
		if (localFile.exists()) {
			localTime = localFile.lastModified();
			conn.setIfModifiedSince(localTime);

			FileReference ref = getFileRef(localFile);
			if (ref != null && ref.etag != null)
				conn.setRequestProperty("If-None-Match", ref.etag);
		}

		int status = conn.getResponseCode();
		sb.append(" (" + status + ")");

		if (status == HttpURLConnection.HTTP_NOT_FOUND) {
			Trace.trace(Trace.INFO, sb.toString());
			return;
		}

		if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
				|| status == HttpURLConnection.HTTP_SEE_OTHER) {
			conn = createConnection(new URL(conn.getHeaderField("Location")));
			conn.setReadTimeout(10000);
			if (localFile.exists())
				conn.setIfModifiedSince(localTime);
			status = conn.getResponseCode();
		}

		if (status == HttpURLConnection.HTTP_NOT_MODIFIED) {
			Trace.trace(Trace.INFO, sb.toString());
			return;
		}

		if (status == HttpURLConnection.HTTP_UNAUTHORIZED) {
			Trace.trace(Trace.INFO, sb.toString() + " not authorized!");
			return;
		}

		if (localFile.exists())
			localFile.delete();

		InputStream in = conn.getInputStream();

		if (!localFile.getParentFile().exists())
			localFile.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(localFile);

		byte[] buf = new byte[8096];
		int n = in.read(buf);
		while (n >= 0) {
			out.write(buf, 0, n);
			n = in.read(buf);
		}
		in.close();
		out.close();
		long mod = conn.getLastModified();
		if (mod != 0)
			localFile.setLastModified(mod);
		time = System.currentTimeMillis() - time;
		String size = nf.format(localFile.length() / 1024.0);
		sb.append(" (" + size + "kb in " + time + "ms)");
		Trace.trace(Trace.INFO, sb.toString());

		String etag = conn.getHeaderField("ETag");
		updateFileInfo(localFile, href, etag);
	}

	private InputStream connect(String path) throws IOException {
		try {
			HttpURLConnection conn = createConnection(path);
			conn.setReadTimeout(130000);

			int status = conn.getResponseCode();
			if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
					|| status == HttpURLConnection.HTTP_SEE_OTHER) {
				conn = createConnection(new URL(conn.getHeaderField("Location")));
			} else if (status == HttpURLConnection.HTTP_UNAUTHORIZED)
				throw new IOException("Not authorized (HTTP response code 401)");
			else if (status == HttpURLConnection.HTTP_BAD_REQUEST)
				throw new IOException("Bad request (HTTP response code 400)");

			return conn.getInputStream();
		} catch (ConnectException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}
	}

	@Override
	public String[] getDirectory(String path) throws IOException {
		try {
			String path2 = path;
			if (!path2.endsWith("/"))
				path2 += "/";
			File file = getFile(path2);
			if (file == null || !file.exists())
				return null;

			final List<String> list = new ArrayList<>();

			// We need to remove the doctype from the file, if it has any
			InputStream fs = new FileInputStream(file);
			BufferedReader reader = new BufferedReader(new InputStreamReader(fs));
			StringBuilder sb = new StringBuilder();
			String line;

			do {
				line = reader.readLine();
				if (line != null && !line.startsWith("<!doctype")) {
					sb.append(line).append("\n");
				}
			} while(line != null);

			String contents = sb.toString();

			LinkParser.parse(new ILinkListener() {
				@Override
				public void linkFound(String s) {
					list.add(s);
				}
			}, new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)));

			return list.toArray(new String[list.size()]);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error reading filenames", e);
		}
		return null;
	}

	public URI getURI(String protocol, String path) throws URISyntaxException {
		String path2 = path;
		if (path2 != null && path2.startsWith("/"))
			path2 = path2.substring(1);
		return new URI(protocol, null, url.getHost(), url.getPort(), url.getPath() + "/" + path2, null, null);
	}

	public URI getRootURI(String protocol, String path) throws URISyntaxException {
		return new URI(protocol, null, url.getHost(), url.getPort(), path, null, null);
	}

	private String getLastCachedEventId() throws Exception {
		if (!feedCacheFile.exists())
			return null;

		InputStream in = null;
		NDJSONFeedLogParser tempParser = new NDJSONFeedLogParser();
		try {
			Trace.trace(Trace.INFO, "Checking feed cache");
			in = new FileInputStream(feedCacheFile);
			tempParser.parse(in);
			String comment = tempParser.getFirstComment();
			Trace.trace(Trace.INFO, "First comment: " + comment);
			if (comment != null && comment.length() > 2) {
				int hash = Integer.parseInt(comment.substring(2));
				if (hash != getContest().hashCode()) {
					Trace.trace(Trace.INFO, "Contest change, ignoring cache");
					in.close();
					in = null;
					feedCacheFile.delete();
					return null;
				}
			}
			Trace.trace(Trace.INFO, "Found feed cache up to event id: " + tempParser.getLastEventId());
			return tempParser.getLastEventId();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error checking feed cache", e);
			feedCacheFile.delete();
			throw e;
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (Exception ex) {
				// ignore
			}
		}
	}

	private void readFromFeedCache() throws Exception {
		if (!feedCacheFile.exists())
			return;

		InputStream in = null;
		try {
			Trace.trace(Trace.INFO, "Reading feed cache");
			in = new FileInputStream(feedCacheFile);
			parser.parse(contest, in);
			Trace.trace(Trace.INFO, "Done reading feed cache");
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error initializing feed", e);
			throw e;
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (Exception ex) {
				// ignore
			}
		}
	}

	@Override
	protected void loadContestImpl() throws Exception {
		InputStream in = null;

		try {
			String lastId = parser.getLastEventId();
			if (firstConnection) {
				contestSizeBeforeFeed = contest.getNumObjects();
				lastId = getLastCachedEventId();
				try {
					feedCacheOut = new FileOutputStream(feedCacheFile, true);
					if (lastId == null) {
						String msg = "! " + contest.hashCode() + "\n";
						feedCacheOut.write(msg.getBytes());
					}
				} catch (Exception ex) {
					Trace.trace(Trace.WARNING, "Could not write to feed cache", ex);
				}
			}

			String path = "event-feed";
			if (lastId != null) {
				path += "?since_id=" + lastId;
				try {
					String msg = "\n!Connecting at event " + lastId + "\n";
					feedCacheOut.write(msg.getBytes());
				} catch (Exception ex) {
					Trace.trace(Trace.WARNING, "Could not write message to feed cache", ex);
				}
			}

			in = new BackupInputStream(connect(path), feedCacheOut);

			if (firstConnection) { // no 400 error! we're good to fill from the cache
				try {
					readFromFeedCache();
				} catch (Exception ex) {
					Trace.trace(Trace.WARNING, "Could not read from feed cache", ex);
				}
			}

			firstConnection = false;

			if (super.isCache())
				notifyListeners(ConnectionState.CONNECTED);

			parser.parse(contest, in);
			String msg = "\n!Connection closed normally after event " + parser.getLastEventId() + "\n";
			feedCacheOut.write(msg.getBytes());
		} catch (Exception e) {
			if (e instanceof IOException && e.getMessage().contains("400")) {
				Trace.trace(Trace.WARNING, "Contest has been reset! Throwing out cache and reconnecting");
				try {
					if (feedCacheOut != null)
						feedCacheOut.close();
				} catch (Exception ex) {
					// ignore
				}

				feedCacheOut = new FileOutputStream(feedCacheFile, true);

				if (feedCacheFile.exists())
					feedCacheFile.delete();

				if (contestSizeBeforeFeed != contest.getNumObjects()) {
					try {
						// we already connected and have some history that needs to be cleaned up!!
						Trace.trace(Trace.WARNING, "Removing invalid contest history!");
						parser = new NDJSONFeedParser();
						contest.removeSince(contestSizeBeforeFeed);
					} catch (Exception ex) {
						Trace.trace(Trace.ERROR, "Could not clean up contest history!", ex);
					}
				}
				throw new IOException("Contest has been reset");
			}
			Trace.trace(Trace.ERROR, "Error reading feed", e);
			String msg = "\n!Connection lost. " + e.getMessage() + " after event " + parser.getLastEventId() + "\n";
			feedCacheOut.write(msg.getBytes());
			throw e;
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (Exception ex) {
				// ignore
			}
		}
	}

	@Override
	public String getContestId() {
		return getRemoteContestId();
	}

	private String getRemoteContestId() {
		String path = url.getPath();
		return path.substring(path.lastIndexOf("/") + 1);
	}

	/**
	 * Set or clear the start time of a contest. Time is in s since Jan 1, 1970 (the Unix epoch), or
	 * null to clear.
	 *
	 * @param time
	 * @throws IOException
	 */
	@Override
	public void setStartTime(Long time) throws IOException {
		try {
			Trace.trace(Trace.INFO, "Setting contest time at " + url);

			HttpURLConnection conn = HTTPSSecurity.createConnection(url, user, password);
			// conn.setRequestMethod("PATCH") not allowed
			setRequestMethod(conn, "PATCH");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);

			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			bw.write("{ \"id\":\"" + getRemoteContestId() + "\", \"start_time\":");

			if (time == null || time <= 0)
				bw.write("null");
			else
				bw.write("\"" + Timestamp.format(time.longValue()) + "\"");

			if (time != null && time < 0) {
				bw.write(", \"countdown_pause_time\":");
				bw.write("\"" + RelativeTime.format(-time.intValue()) + "\"");
			}

			bw.write(" }");
			bw.close();

			if (conn.getResponseCode() != 200)
				throw new IOException(conn.getResponseCode() + ": " + conn.getResponseMessage());
		} catch (IOException e) {
			Trace.trace(Trace.ERROR, "Error setting contest start time", e);
			throw new IOException("500: Could not connect: " + e.getMessage());
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}
	}

	public void cacheClientSideEvent(IContestObject obj, Delta d) {
		try {
			if (feedCacheOut == null) {
				Trace.trace(Trace.WARNING, "Client-side event failed, haven't connected to source yet");
				return;
			}

			Trace.trace(Trace.INFO, "Adding client-side event to feed cache");
			PrintWriter pw = new PrintWriter(feedCacheOut);
			pw.println("\n!Client-side event");
			NDJSONFeedWriter writer = new NDJSONFeedWriter(pw, contest);
			// use same id as last event so if it crashes here we'll pick up at the same spot
			writer.writeEvent(obj, parser.getLastEventId(), d);
			pw.flush();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error logging client-side event", e);
		}
	}

	public ITeam getTeam(String teamId) throws IOException {
		return (Team) parseContestObject("teams/" + teamId, ContestType.TEAM);
	}

	public IOrganization getOrganization(String orgId) throws IOException {
		return (Organization) parseContestObject("organizations/" + orgId, ContestType.ORGANIZATION);
	}

	private IContestObject parseContestObject(String partialURL, ContestType type) throws IOException {
		try {
			URL url2 = new URL(url.toExternalForm() + "/" + partialURL);
			Trace.trace(Trace.INFO, "Getting contest object from " + url2 + " - " + type.name());
			HttpURLConnection conn = HTTPSSecurity.createConnection(url2, user, password);
			conn.setRequestProperty("Content-Type", "application/json");

			if (conn.getResponseCode() != 200)
				throw new IOException(conn.getResponseCode() + ": " + conn.getResponseMessage());

			InputStream in = conn.getInputStream();
			JSONParser rdr = new JSONParser(in);
			JsonObject obj = rdr.readObject();
			ContestObject co = (ContestObject) IContestObject.createByType(type);
			for (String key : obj.props.keySet())
				co.add(key, obj.props.get(key));

			return co;
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error getting object", e);
			throw e;
		}
	}

	private static void setRequestMethod(HttpURLConnection c, String value) {
		try {
			Object target = c;
			try {
				if (c instanceof HttpsURLConnection) {
					final Field delegate = c.getClass().getDeclaredField("delegate");
					delegate.setAccessible(true);
					target = delegate.get(c);
				}
			} catch (Exception e) {
				// ignore
			}
			Field f = HttpURLConnection.class.getDeclaredField("method");
			f.setAccessible(true);
			f.set(target, value);
		} catch (Exception ex) {
			throw new AssertionError(ex);
		}
	}

	@Override
	public Validation validate() {
		Validation v = new Validation();

		try {
			HttpURLConnection conn = createConnection("");
			int response = conn.getResponseCode();
			if (response == HttpURLConnection.HTTP_UNAUTHORIZED) {
				v.err("Invalid user or password");
				return v;
			} else if (response != HttpURLConnection.HTTP_OK) {
				if (tryAlternateURLs(v))
					return v;
				v.err("Invalid response code: " + response);
				return v;
			}
			try {
				validateContent(conn, v, null);
			} catch (Exception ex) {
				if (tryAlternateURLs(v))
					return v;
				v.err("Could not parse content: " + ex.getMessage());
				return v;
			}
		} catch (SocketException se) {
			Trace.trace(Trace.INFO, "Socket error", se);
			v.err("Socket error, may be due to invalid URL, user, or password");
		} catch (Exception e) {
			v.err("Unexpected error during validation: " + e.getMessage());
			Trace.trace(Trace.INFO, "Validation error", e);
		}

		return v;
	}

	protected boolean tryAlternateURLs(Validation v) {
		String[] paths = new String[] { "/api/contests/", "contests/", "api/contests/", "/domjudge/api/contests/" };
		// boolean redirected = false;
		for (String path : paths) {
			try {
				HttpURLConnection conn = createConnection(path);
				int response2 = conn.getResponseCode();
				if (response2 == HttpURLConnection.HTTP_OK) {
					validateContent(conn, v, path);
					return true;
				} else if (response2 == HttpURLConnection.HTTP_MOVED_TEMP || response2 == HttpURLConnection.HTTP_MOVED_PERM
						|| response2 == HttpURLConnection.HTTP_SEE_OTHER) {
					String newPath = conn.getHeaderField("Location");
					conn = createConnection(new URL(newPath));
					int response3 = conn.getResponseCode();
					if (response3 == HttpURLConnection.HTTP_OK) {
						validateContent(conn, v, newPath);
						return true;
					}
					// redirected = true;
				}
			} catch (Exception e) {
				Trace.trace(Trace.INFO, "Check for " + path + " URL failed");
			}
		}

		return false;
	}

	protected void validateContent(HttpURLConnection conn, Validation v, String postURL) throws Exception {
		InputStream in = conn.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String s = br.readLine();
		while (s != null) {
			sb.append(s);
			s = br.readLine();
		}

		validateJSON(v, postURL, sb.toString());
	}

	protected void validateJSON(Validation v, String path, String s) throws Exception {
		try {
			// try as array
			JSONParser parser2 = new JSONParser(s);
			Object[] arr = parser2.readArray();
			URL url2 = getChildURL(path);

			if (arr.length == 0) {
				v.err("Possible Contest API at " + url2 + ", but no contests found");
				return;
			}

			List<String> poss = new ArrayList<>();
			Info[] infos = new Info[arr.length];
			for (int i = 0; i < arr.length; i++) {
				JsonObject obj = (JsonObject) arr[i];
				Info info = new Info();
				infos[i] = info;
				for (String key : obj.props.keySet())
					info.add(key, obj.props.get(key));
			}
			Info bestContest = pickBestContest(infos);
			if (bestContest != null) {
				url2 = new URL(ensureTrailingSlash(url2), bestContest.getId());
				this.url = url2;
				Trace.trace(Trace.USER,
						"The URL did not point to a specific contest, but " + infos.length + " contest(s) were found.");
				Trace.trace(Trace.USER, "Auto-connecting to: " + url);
				return;
			}

			for (int i = 0; i < arr.length; i++) {
				Info info = infos[i];
				if (info.getId() == null || info.getName() == null || info.getDuration() <= 0) {
					v.err("Unrecognized REST endpoint");
					return;
				}
				poss.add(ensureTrailingSlash(url2) + info.getId() + " (" + info.getName() + " starting at "
						+ ContestUtil.formatStartTime(info.getStartTime()) + ")");
			}

			v.err("Contest API found, but couldn't pick a contest. Try one of the following URLs:");
			for (String p : poss)
				v.ok("  " + p);
			return;
		} catch (Exception e) {
			// ignore, not an array
		}

		try {
			JSONParser parser2 = new JSONParser(s);
			JsonObject obj = parser2.readObject();
			if (obj.getString("id") != null && obj.getString("name") != null && obj.getString("duration") != null)
				v.ok("Confirmed Contest API connection");
			else
				v.err("Unrecognized REST endpoint");
			return;
		} catch (Exception e) {
			// ignore, not an object
		}

		throw new Exception("Unrecognized endpoint");
	}

	private static Info pickBestContest(Info[] infos) {
		if (infos == null || infos.length == 0)
			return null;

		// if there's only one contest, pick it
		int numContests = infos.length;
		if (numContests == 1)
			return infos[0];

		// TODO: pick any paused contest. since we can't tell yet which contests
		// are paused, in the meantime pick any contest with no start time, as that's likely paused
		/*
		for (int i = 0; i < infos.length; i++) {
			// if paused, pick this one
		}*/
		Info paused = null;
		for (int i = 0; i < numContests; i++) {
			if (infos[i].getStartTime() == null) {
				if (paused != null) {
					Trace.trace(Trace.INFO, "Multiple contests don't have a start time, could not pick one");
					return null;
				}
				paused = infos[i];
			}
		}
		if (paused != null)
			return paused;

		// ok, so there are multiple contests, and none of them are paused.
		// let's start by figuring out the best contest(s) that are before, during, or
		// after the current time
		Info next = null;
		long timeUntilNext = Long.MAX_VALUE;
		boolean nextIsDup = false;
		Info during = null;
		Info previous = null;
		long timeSincePrevious = Long.MAX_VALUE;
		boolean previousIsDup = false;

		long now = System.currentTimeMillis();
		for (int i = 0; i < numContests; i++) {
			Info info = infos[i];

			if (now < info.getStartTime()) {
				// before the contest
				long timeUntilStart = info.getStartTime() - now;
				Trace.trace(Trace.INFO, "Next contest: " + timeUntilStart + " " + info.getId());
				if (timeUntilStart == timeUntilNext)
					nextIsDup = true;
				else if (timeUntilStart < timeUntilNext) {
					next = info;
					timeUntilNext = timeUntilStart;
					nextIsDup = false;
				}
			} else if (now < info.getStartTime() + info.getDuration()) {
				// during
				Trace.trace(Trace.INFO, "During contest: " + info.getId());
				if (during != null) {
					Trace.trace(Trace.ERROR, "Multiple contests are running, can't pick between them");
					return null;
				}
				during = info;
			} else {
				// after the contest
				long timeSince = now - (info.getStartTime() + info.getDuration());
				Trace.trace(Trace.INFO, "Previous contest: " + timeSince + " " + info.getId());
				if (timeSince == timeSincePrevious)
					previousIsDup = true;
				else if (timeSince < timeSincePrevious) {
					previous = info;
					timeSincePrevious = timeSince;
					previousIsDup = false;
				}
			}
		}

		// if we're during the one and only running contest, pick it
		if (during != null)
			return during;

		// if we're before all contests, pick the first one
		if (next != null && previous == null) {
			if (nextIsDup) { // unless the first two start at the same time
				Trace.trace(Trace.INFO, "The next two contests start at the same time, can't pick between them");
				return null;
			}
			return next;
		}

		// if we're after all contests, pick the last one
		if (previous != null && next == null) {
			if (previousIsDup) { // unless the previous two ended at the same time
				Trace.trace(Trace.INFO, "The previous two contests ended at the same time, can't pick between them");
				return null;
			}
			return previous;
		}

		// ok, so we're between two (or more) contests. if the previous contest ended more than 2h
		// ago or we're closer (weighted x 2) to the next one, switch to the next contest.
		// if the direction we chose to go has more than one contest, give up
		if (timeSincePrevious > 120 * 1000 || timeUntilNext < timeSincePrevious * 2) {
			// pick the next contest
			if (nextIsDup) { // unless the next two start at the same time
				Trace.trace(Trace.INFO, "The next two contests start at the same time, can't pick between them");
				return null;
			}
			return next;
		}

		// not close to the next contest, stick with the previous one
		if (previousIsDup) { // unless the previous two ended at the same time
			Trace.trace(Trace.INFO, "The previous two contests ended at the same time, can't pick between them");
			return null;
		}
		return previous;
	}

	private static Class<?> getCallerClass() {
		try {
			StackTraceElement[] stes = Thread.currentThread().getStackTrace();
			for (int i = 1; i < stes.length; i++) {
				String className = stes[i].getClassName();
				if (className.indexOf("RESTContestSource") < 0) {
					return Class.forName(className);
				}
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	private static String getVersion(String ver) {
		if (ver == null)
			return "dev";
		return ver;
	}

	private static String getVersion() {
		Class<?> c = getCallerClass();
		Package pack = c.getPackage();
		String spec = pack.getSpecificationVersion();
		String impl = pack.getImplementationVersion();
		if (spec == null && impl == null)
			try {
				java.util.Properties prop = new java.util.Properties();
				prop.load(c.getResourceAsStream("/META-INF/MANIFEST.MF"));
				spec = prop.getProperty("Specification-Version");
				impl = prop.getProperty("Implementation-Version");
			} catch (Exception e) {
				// ignore
			}

		return getVersion(spec) + "." + getVersion(impl);
	}

	/**
	 * This method will only work when the remote server is a CDS!
	 *
	 * Checks for existence of a specific zip file pattern on the server. If one or more exist, the
	 * newest copy's version will be compared with the local version. If the remote version is
	 * newer, it will be downloaded to the /update folder, and the process will exit with code 254.
	 * The calling script must support replacing the folders contents with the update and
	 * restarting.
	 *
	 * @param prefix the file pattern, e.g. "presentations-".
	 */
	public void checkForUpdates(String prefix) {
		try {
			// remove any previous updates
			File updateDir = new File("update");
			if (updateDir.exists()) {
				Files.walkFileTree(updateDir.toPath(), new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}
				});
			}

			// check on CDS for which versions are available
			String[] files = getDirectory("/presentation");
			List<String> presJars = new ArrayList<>();
			if (files != null && files.length > 0)
				for (String f : files) {
					if (f.startsWith(prefix) && f.endsWith(".zip"))
						presJars.add(f);
				}

			Trace.trace(Trace.INFO, "Updates found on CDS: " + presJars.size());
			if (presJars.size() > 0) {
				// pick latest version and compare with local
				presJars.sort(new Comparator<String>() {
					@Override
					public int compare(String s1, String s2) {
						return -s1.compareTo(s2);
					}
				});
				String localVersion = getVersion();
				String remote = presJars.get(0);
				String remoteVersion = remote.substring(prefix.length());
				remoteVersion = remoteVersion.substring(0, remoteVersion.length() - 4);
				Trace.trace(Trace.INFO, "Version check: " + localVersion + " (local) vs " + remoteVersion + " (remote)");

				if (!localVersion.contains("dev") && localVersion.compareTo(remoteVersion) < 0) {
					// download and unzip new version, restart
					Trace.trace(Trace.USER, "Newer version found on CDS (" + remote + "). Downloading and restarting...");
					File f = getFile("/presentation/" + remote);

					// unzip to /update
					unzip(f, updateDir);

					System.exit(254);
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Failure while checking for updates", e);
		}
	}

	private static void unzip(File zipFile2, File folder) throws IOException {
		ZipFile zipFile = new ZipFile(zipFile2);
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		boolean commonRootFolder = true;
		String rootFolder = null;
		while (zipEntries.hasMoreElements()) {
			ZipEntry zipEntry = zipEntries.nextElement();
			String name = zipEntry.getName();
			int ind = name.indexOf("/");
			if (ind < 0) {
				commonRootFolder = false;
				break;
			}
			name = name.substring(ind + 1);
			if (rootFolder == null)
				rootFolder = name;
			else if (!name.equals(rootFolder)) {
				commonRootFolder = false;
				break;
			}
		}

		zipFile = new ZipFile(zipFile2);
		zipEntries = zipFile.entries();
		while (zipEntries.hasMoreElements()) {
			ZipEntry zipEntry = zipEntries.nextElement();

			if (!zipEntry.isDirectory()) {
				String name = zipEntry.getName();
				if (commonRootFolder)
					name = name.substring(rootFolder.length() + 1);
				File f = new File(folder, name);
				if (!f.getParentFile().exists())
					f.getParentFile().mkdirs();
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
				BufferedInputStream bin = new BufferedInputStream(zipFile.getInputStream(zipEntry));
				byte[] b = new byte[1024 * 8];
				int n = bin.read(b);
				while (n != -1) {
					out.write(b, 0, n);
					n = bin.read(b);
				}

				out.close();
				bin.close();
				f.setLastModified(zipEntry.getTime());
				if (f.getName().endsWith(".sh") || f.getName().endsWith(".bat"))
					f.setExecutable(true, false);
			}
		}
	}

	public boolean isCDS() {
		// TODO use some CDS behaviour to confirm it's a CDS
		return true;
	}

	@Override
	public String toString() {
		return "RESTContestSource[" + user + "|" + password + "@" + url + "]";
	}
}