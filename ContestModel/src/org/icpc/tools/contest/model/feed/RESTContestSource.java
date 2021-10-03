package org.icpc.tools.contest.model.feed;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HttpsURLConnection;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContestListener.Delta;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.FileReference;
import org.icpc.tools.contest.model.internal.Info;

/**
 * A REST contest source for loading a contest over HTTP. The contest may be backed by data in a
 * local folder (and additional contest files will be stored here), otherwise data will be cached
 * in a temp folder.
 */
public class RESTContestSource extends DiskContestSource {
	protected static final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
	static {
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(1);
		nf.setGroupingUsed(true);
	}

	private URL url;
	private File feedFile;
	private String user = null;
	private String password = null;
	private String baseUrl;
	private String contestId;

	private NDJSONFeedParser parser = new NDJSONFeedParser();

	private File feedCacheFile;
	private FileOutputStream feedCacheOut;
	private boolean firstConnection = true;
	private int contestSizeBeforeFeed;
	private boolean isCDS;

	/**
	 * Creates a REST contest source with local (temp) caching.
	 *
	 * @param url
	 * @param user
	 * @param password
	 * @throws MalformedURLException
	 */
	public RESTContestSource(String url, String user, String password) throws MalformedURLException {
		this(null, null, url, user, password);
	}

	/**
	 * Creates a contest source from a local event feed file, with ability to load absolute
	 * references, with local temp caching.
	 *
	 * @param eventFeedFile
	 * @param user
	 * @param password
	 * @throws MalformedURLException
	 */
	public RESTContestSource(File eventFeedFile, String user, String password) throws MalformedURLException {
		this(eventFeedFile, null, user, password);
	}

	/**
	 * Creates a REST contest source backed by a local contest archive format folder.
	 *
	 * @param folder
	 * @param url
	 * @param user
	 * @param password
	 * @param folder
	 */
	public RESTContestSource(File folder, String url, String user, String password) throws MalformedURLException {
		this(null, folder, url, user, password);
	}

	/**
	 * General purpose constructor for a REST contest source. Usually only one of the eventFeedFile
	 * and folder are used. URL is only optional if you already have a local contest cached and only
	 * want the ability to load absolute file references.
	 */
	private RESTContestSource(File eventFeedFile, File folder, String url, String user, String password)
			throws MalformedURLException {
		super(eventFeedFile, folder, url);

		this.url = new URL(url);

		if (user != null && user.trim().length() > 0)
			this.user = user;

		if (password != null && password.trim().length() > 0)
			this.password = password;

		instance = this;

		validateURL();
		setup();

		if (eventFeedFile == null) {
			String name = System.getProperty("CDS-name");
			if (name != null)
				feedCacheFile = new File(cacheFolder, "events-" + name + ".log");
			else
				feedCacheFile = new File(cacheFolder, "events.log");

			// delete if older than 8h
			if (feedCacheFile.exists() && feedCacheFile.lastModified() < System.currentTimeMillis() - 8 * 60 * 60 * 1000) {
				feedCacheFile.delete();
			}
		}
	}

	private void setup() {
		String path = url.getPath();
		contestId = path.substring(path.lastIndexOf("/") + 1);

		Pattern pattern = Pattern.compile("^(.*/)contests/[a-zA-Z0-9_-]+");
		Matcher matcher = pattern.matcher(url.toExternalForm());
		if (matcher.find())
			baseUrl = matcher.group(1);
		else
			baseUrl = url.toExternalForm();
	}

	public static RESTContestSource ensureContestAPI(ContestSource source) {
		if (source == null || !(source instanceof RESTContestSource)) {
			Trace.trace(Trace.ERROR, "Source argument must be a Contest API");
			System.exit(1);
		}
		return (RESTContestSource) source;
	}

	public static RESTContestSource ensureCDS(ContestSource source) {
		if (source == null || !(source instanceof RESTContestSource)) {
			Trace.trace(Trace.ERROR, "Source argument must be a CDS");
			System.exit(1);
		}
		RESTContestSource restSource = (RESTContestSource) source;

		try {
			HttpURLConnection conn = restSource.createConnection("", false);
			int response = conn.getResponseCode();
			if ("CDS".equals(conn.getHeaderField("ICPC-Tools")))
				restSource.isCDS = true;

			if (response == HttpURLConnection.HTTP_UNAUTHORIZED) {
				// v.err("Invalid user or password");
			} else if (response != HttpURLConnection.HTTP_OK) {
				// v.err("Invalid response code: " + response);
			}
		} catch (SocketException se) {
			Trace.trace(Trace.INFO, "Socket error", se);
			// v.err("Socket error, may be due to invalid URL, user, or password");
		} catch (Exception e) {
			// v.err("Unexpected error during validation: " + e.getMessage());
			Trace.trace(Trace.INFO, "Validation error", e);
		}

		if (!restSource.isCDS()) {
			Trace.trace(Trace.ERROR, "Source argument must be a CDS");
			System.exit(1);
		}
		return restSource;
	}

	public URL getURL() {
		return url;
	}

	private URL getChildURL(String path) throws MalformedURLException {
		String u = url.toExternalForm();
		if (u.endsWith("/"))
			return new URL(u + path);

		return new URL(u + "/" + path);
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	/**
	 *
	 * @param partialURL
	 * @param baseURL true for base url relative, false for contest url relative
	 * @return
	 * @throws IOException
	 */
	private HttpURLConnection createConnection(String partialURL, boolean baseURL) throws IOException {
		if (baseURL)
			return createConnection(new URL(getResolvedURL(partialURL)));

		return createConnection(getChildURL(partialURL));
	}

	private HttpURLConnection createConnection(URL url2) throws IOException {
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

		downloadIfNecessary(path, localFile);
		return localFile;
	}

	@Override
	public File getFile(IContestObject obj, FileReference ref, String property) throws IOException {
		File file = super.getFile(obj, ref, property);

		return downloadIfNecessary(ref, file);
	}

	public File downloadFile(IContestObject obj, FileReference ref, String property) throws IOException {
		if (obj == null)
			return null;

		File file = super.getNewFile(obj.getType(), obj.getId(), property, ref.mime);
		if (file == null)
			return null;

		return downloadIfNecessary(ref, file);
	}

	private String getResolvedURL(String href) {
		if (href.startsWith("http"))
			return href;

		if (feedFile != null)
			return null;

		// if href starts with / it means from the root
		if (href.startsWith("/"))
			return url.getProtocol() + "://" + url.getAuthority() + href;

		// otherwise, it's relative to the API, so determine that
		return baseUrl + href;
	}

	private File downloadIfNecessary(FileReference ref, File localFile) throws IOException {
		if (localFile == null)
			return null;

		downloadIfNecessary(ref.href, localFile);
		return localFile;
	}

	private void downloadIfNecessary(String href, File localFile) throws IOException {
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

	private static boolean hasMoved(int status) {
		return (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
				|| status == HttpURLConnection.HTTP_SEE_OTHER || status == 307 || status == 308);
	}

	private void downloadIfNecessaryImpl(String href, File localFile) throws IOException {
		StringBuilder sb = new StringBuilder("Download " + href + " to " + localFile);
		long time = System.currentTimeMillis();
		HttpURLConnection conn = createConnection(href, true);
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
			localFile.delete();
			Trace.trace(Trace.INFO, sb.toString());
			return;
		}

		if (hasMoved(status)) {
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
			HttpURLConnection conn = createConnection(path, false);
			conn.setReadTimeout(130000);

			int status = conn.getResponseCode();
			if (hasMoved(status)) {
				conn = createConnection(new URL(conn.getHeaderField("Location")));
			} else if (status == HttpURLConnection.HTTP_UNAUTHORIZED)
				throw new IOException("Not authorized (HTTP response code 401)");
			else if (status == HttpURLConnection.HTTP_BAD_REQUEST)
				throw new IOException("Bad request (HTTP response code 400)");

			if ("CDS".equals(conn.getHeaderField("ICPC-Tools")))
				isCDS = true;

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

			return LinkParser.parse(file);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error reading filenames", e);
		}
		return null;
	}

	private String getLastCachedEventId() throws Exception {
		if (!feedCacheFile.exists())
			return null;

		InputStream in = null;
		NDJSONFeedLogParser tempParser = new NDJSONFeedLogParser();
		try {
			Trace.trace(Trace.INFO, "Checking feed cache: " + feedCacheFile.getAbsolutePath());
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
	protected void initializeContestImpl() throws Exception {
		super.initializeContestImpl();

		if (feedFile == null || !feedFile.exists())
			return;

		InputStream in;
		try {
			in = new FileInputStream(feedFile);
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Could not read event feed", e);
			throw e;
		}
		try {
			if (feedFile.getName().endsWith("xml")) {
				XMLFeedParser parser2 = new XMLFeedParser();
				parser2.parse(contest, in);
			} else {
				NDJSONFeedParser parser2 = new NDJSONFeedParser();
				parser2.parse(contest, in);
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error reading event feed", e);
			throw e;
		} finally {
			try {
				in.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	@Override
	protected void loadContestImpl() throws Exception {
		if (feedFile != null)
			return;

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

				if (feedCacheFile.exists()) {
					if (!feedCacheFile.delete())
						Trace.trace(Trace.WARNING, "Could not delete cache file");
				}

				feedCacheOut = new FileOutputStream(feedCacheFile, true);

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
	public void close() throws Exception {
		super.close();
		if (parser != null)
			parser.close();
	}

	@Override
	public String getContestId() {
		return contestId;
	}

	/**
	 * Helper method to grab any HTTP response body error text or build a generic error message.
	 *
	 * @param conn an HTTP connection
	 * @return an error message suitable for returning to clients
	 * @throws IOException
	 */
	private static String getResponseError(HttpURLConnection conn) throws IOException {
		try {
			InputStream in = conn.getErrorStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			String s = br.readLine();
			StringBuilder sb = new StringBuilder();
			while (s != null) {
				sb.append(s);
				s = br.readLine();
			}

			if (sb.length() > 0) {
				// try to parse as json error object first
				try {
					JSONParser rdr = new JSONParser(sb.toString());
					JsonObject obj = rdr.readObject();
					String message = obj.getString("message");
					if (message != null && message.length() > 0)
						return message;
				} catch (Exception x) {
					// ignore
				}

				// otherwise, just return the text
				return sb.toString();
			}
		} catch (Exception e) {
			// ignore
		}

		return conn.getResponseCode() + ": " + conn.getResponseMessage();
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

			HttpURLConnection conn = createConnection(url);
			// conn.setRequestMethod("PATCH") not allowed
			setRequestMethod(conn, "PATCH");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);

			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			bw.write("{ \"id\":\"" + contestId + "\", \"start_time\":");

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
				throw new IOException("Error setting contest start time (" + getResponseError(conn) + ")");
		} catch (IOException e) {
			Trace.trace(Trace.INFO, "Error setting contest start time", e);
			throw e;
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}
	}

	/**
	 * Encode the given files into a base-64 encoded string of a zip archive containing the files.
	 *
	 * @param files the files to encode
	 * @return a base-64 encoded string
	 * @throws IOException if anything goes wrong
	 */
	private static String zipAndEncode(File... files) throws IOException {
		ZipOutputStream zipOut = null;

		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			zipOut = new ZipOutputStream(bout);

			for (File file : files) {
				zipOut.putNextEntry(new ZipEntry(file.getName()));

				BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
				byte[] buf = new byte[8096];
				int n = in.read(buf);
				while (n > 0) {
					zipOut.write(buf, 0, n);
					n = in.read(buf);
				}
				zipOut.closeEntry();
			}
			zipOut.close();

			return Base64.getEncoder().encodeToString(bout.toByteArray());
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error encoding files", e);
			throw new IOException(e);
		} finally {
			try {
				if (zipOut != null)
					zipOut.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	/**
	 * Utility method to send JSON object to a specific URL with the given HTTP method.
	 *
	 * @param method
	 * @param partialURL
	 * @param obj
	 * @param expectReturn
	 * @return an object (if expectReturn is true)
	 * @throws IOException
	 */
	private JsonObject httpUtil(String method, String partialURL, JsonObject obj, boolean expectReturn)
			throws IOException {

		try {
			Trace.trace(Trace.INFO, method + "ing to " + partialURL + " at " + url);

			HttpURLConnection conn = createConnection(partialURL, false);
			conn.setRequestMethod(method);
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Accept", "application/json");
			conn.setDoOutput(true);

			if (obj != null) {
				JSONWriter jw = new JSONWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
				jw.writeObject(obj);
				jw.close();
			}

			if (conn.getResponseCode() != 200)
				throw new IOException("Error " + method + "ing (" + getResponseError(conn) + ")");

			if (!expectReturn)
				return null;

			JSONParser parser2 = new JSONParser(conn.getInputStream());
			JsonObject rObj = parser2.readObject();
			if (rObj == null)
				throw new IOException(method + " successful but invalid object returned");

			return rObj;
		} catch (IOException e) {
			Trace.trace(Trace.INFO, "Error " + method + "ing", e);
			throw e;
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}
	}

	private IContestObject httpUtil(String method, ContestType type, String partialURL2, IContestObject obj,
			boolean expectReturn) throws IOException {

		try {
			String partialURL = IContestObject.getTypeName(type);
			if (partialURL2 != null)
				partialURL += partialURL2;
			Trace.trace(Trace.INFO, method + "ing to " + partialURL + " at " + url);

			HttpURLConnection conn = createConnection(partialURL, false);
			conn.setRequestMethod(method);
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Accept", "application/json");
			conn.setDoOutput(true);

			if (obj != null) {
				PrintWriter pw = new PrintWriter(conn.getOutputStream());
				JSONEncoder je = new JSONEncoder(pw);
				je.open();
				((ContestObject) obj).writeBody(je);
				je.close();
				pw.flush();
			}

			if (conn.getResponseCode() != 200)
				throw new IOException("Error " + method + "ing (" + getResponseError(conn) + ")");

			if (!expectReturn)
				return null;

			JSONParser parser2 = new JSONParser(conn.getInputStream());
			JsonObject rObj = parser2.readObject();
			if (rObj == null)
				throw new IOException(method + " successful but invalid object returned");

			ContestObject co = (ContestObject) IContestObject.createByType(type);
			for (String key : rObj.props.keySet())
				co.add(key, rObj.props.get(key));

			return co;
		} catch (IOException e) {
			Trace.trace(Trace.INFO, "Error " + method + "ing", e);
			throw e;
		} catch (Exception e) {
			throw new IOException("Connection error", e);
		}
	}

	/**
	 * Enter a submission to the contest, as either a team or an admin.
	 *
	 * JSON attributes: 'language_id', 'problem_id', and 'files' are required. Files can either be
	 * included in the attributes or specified via the files parameter. 'entry_point' may be
	 * required based on language. When submitting as an admin 'team_id' is required, 'id' and/or
	 * 'time' are optional.
	 *
	 * @param obj a json object with the submission attributes
	 * @param files an optional list of files to zip and submit as the "files" attribute
	 * @return the accepted submission
	 * @throws IOException if there is any problem connecting to the server or with the submission
	 */
	public JsonObject postSubmission(JsonObject obj, File... files) throws IOException {
		if (obj == null)
			throw new IllegalArgumentException();

		try {
			if (files != null && files.length > 0) {
				JsonObject filesObj = new JsonObject();
				filesObj.put("data", zipAndEncode(files));
				obj.props.put("files", new Object[] { filesObj });
			}
		} catch (Exception e) {
			Trace.trace(Trace.INFO, "Error attaching files to submission", e);
			throw e;
		}
		return httpUtil("POST", "submissions", obj, true);
	}

	/**
	 * POST an object to the contest.
	 *
	 * @param type the contest type of the object
	 * @param obj a json object containing the attributes
	 * @return the accepted object
	 * @throws IOException if there is any problem connecting to the server or posting the object
	 */
	public JsonObject post(ContestType type, JsonObject obj) throws IOException {
		return httpUtil("POST", IContestObject.getTypeName(type), obj, true);
	}

	/**
	 * POST an object to the contest.
	 *
	 * @param obj a contest object
	 * @return the accepted object
	 * @throws IOException if there is any problem connecting to the server or posting the object
	 */
	public IContestObject post(IContestObject obj) throws IOException {
		return httpUtil("POST", obj.getType(), null, obj, true);
	}

	/**
	 * PUT an object to the contest. JSON attribute 'id' is required.
	 *
	 * @param type the contest type of the object
	 * @param obj a json object containing the attributes
	 * @return the updated object
	 * @throws IOException if there is any problem connecting to the server or putting the object
	 */
	public JsonObject put(ContestType type, JsonObject obj) throws IOException {
		return httpUtil("PUT", IContestObject.getTypeName(type) + "/" + obj.getString("id"), obj, true);
	}

	/**
	 * PUT an object to the contest.
	 *
	 * @param obj a contest object
	 * @return the updated object
	 * @throws IOException if there is any problem connecting to the server or putting the object
	 */
	public IContestObject put(IContestObject obj) throws IOException {
		return httpUtil("PUT", obj.getType(), "/" + obj.getId(), obj, true);
	}

	/**
	 * PATCH an object to the contest. JSON attribute 'id' is required.
	 *
	 * @param type the contest type of the object
	 * @param obj a json object containing the attributes
	 * @return the updated object
	 * @throws IOException if there is any problem connecting to the server or patching the object
	 */
	public JsonObject patch(ContestType type, JsonObject obj) throws IOException {
		return httpUtil("PATCH", IContestObject.getTypeName(type) + "/" + obj.getString("id"), obj, true);
	}

	/**
	 * PATCH an object to the contest.
	 *
	 * @param obj a json object containing the attributes
	 * @return the updated object
	 * @throws IOException if there is any problem connecting to the server or patching the object
	 */
	public IContestObject patch(IContestObject obj) throws IOException {
		return httpUtil("PATCH", obj.getType(), "/" + obj.getId(), obj, true);
	}

	/**
	 * DELETE an object from the contest.
	 *
	 * @param type the contest type of the object
	 * @param id the id of the object
	 * @throws IOException if there is any problem connecting to the server or deleting the object
	 */
	public void delete(ContestType type, String id) throws IOException {
		httpUtil("DELETE", type, "/" + id, null, false);
	}

	/**
	 * DELETE an object from the contest.
	 *
	 * @param obj a contest object
	 * @throws IOException if there is any problem connecting to the server or deleting the object
	 */
	public void delete(IContestObject obj) throws IOException {
		httpUtil("DELETE", obj.getType(), "/" + obj.getId(), null, false);
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
		return (ITeam) parseContestObject("teams/" + teamId, ContestType.TEAM);
	}

	public IOrganization getOrganization(String orgId) throws IOException {
		return (IOrganization) parseContestObject("organizations/" + orgId, ContestType.ORGANIZATION);
	}

	private IContestObject parseContestObject(String partialURL, ContestType type) throws IOException {
		try {
			Trace.trace(Trace.INFO, "Getting contest object: " + type.name());
			HttpURLConnection conn = createConnection(partialURL, false);
			conn.setRequestProperty("Content-Type", "application/json");

			if (conn.getResponseCode() != 200)
				throw new IOException(getResponseError(conn));

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

	private void validateURL() {
		if (feedFile != null)
			return;

		try {
			this.url = validateURL(url);
			return;
		} catch (Exception e) {
			Trace.trace(Trace.INFO, "Invalid contest URL: " + e.getMessage());
		}

		try {
			this.url = tryAlternateURLs();
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not find valid Contest API at " + url);
		}
	}

	private URL tryAlternateURLs() {
		String[] paths = new String[] { "api/contests", "contests", "/api/contests", "/contests",
				"/domjudge/api/contests", "/clics-api/contests" };
		for (String path : paths) {
			try {
				URL testURL = null;
				if (path.startsWith("/"))
					testURL = new URL(url.getProtocol() + "://" + url.getAuthority() + path);
				else
					testURL = getChildURL(path);
				return validateURL(testURL);
			} catch (Exception e) {
				Trace.trace(Trace.INFO, "Check for " + path + " URL failed: " + e.getMessage());
			}
		}

		throw new IllegalArgumentException("Could not detect valid contest API");
	}

	private URL validateURL(URL aURL) throws Exception {
		HttpURLConnection conn = createConnection(aURL);
		int response = conn.getResponseCode();

		if (response == HttpURLConnection.HTTP_UNAUTHORIZED)
			throw new IllegalArgumentException("Invalid user or password (401)");
		else if (hasMoved(response)) {
			URL newURL = new URL(conn.getHeaderField("Location"));
			return validateURL(newURL);
		} else if (response == HttpURLConnection.HTTP_NOT_FOUND) {
			throw new IllegalArgumentException("Invalid, URL not found (404)");
		} else if (response == HttpURLConnection.HTTP_OK) {
			if ("CDS".equals(conn.getHeaderField("ICPC-Tools")))
				isCDS = true;
			return validateContent(conn, aURL);
		}

		throw new IllegalArgumentException("Invalid response code (" + response + ")");
	}

	private static URL validateContent(HttpURLConnection conn, URL theURL) throws Exception {
		InputStream in = conn.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String s = br.readLine();
		while (s != null) {
			sb.append(s);
			s = br.readLine();
		}

		s = sb.toString();

		try {
			// try as array
			JSONParser parser2 = new JSONParser(s);
			Object[] arr = parser2.readArray();

			if (arr.length == 0)
				throw new IllegalArgumentException("Possible Contest API at " + theURL + ", but no contests found");

			Info[] infos = new Info[arr.length];
			for (int i = 0; i < arr.length; i++) {
				JsonObject obj = (JsonObject) arr[i];
				Info info = new Info();
				infos[i] = info;
				for (String key : obj.props.keySet())
					info.add(key, obj.props.get(key));
			}
			String u = theURL.toExternalForm();
			if (!u.endsWith("/"))
				u += "/";
			Info bestContest = pickBestContest(infos);
			if (bestContest != null) {
				URL newURL = new URL(u + bestContest.getId());
				if (infos.length == 1)
					Trace.trace(Trace.USER, "The URL did not point to a specific contest, but one contest was found.");
				else
					Trace.trace(Trace.USER,
							"The URL did not point to a specific contest, but " + infos.length + " contests were found.");
				Trace.trace(Trace.USER, "Auto-connecting to: " + newURL);
				return newURL;
			}

			StringBuilder sb2 = new StringBuilder(
					"Contest API found, but couldn't pick a contest. Try one of the following URLs:\n");
			for (int i = 0; i < arr.length; i++) {
				Info info = infos[i];
				if (info.getId() == null || info.getName() == null || info.getDuration() <= 0)
					throw new IllegalArgumentException("Unrecognized REST endpoint");

				sb2.append(u + info.getId() + " (" + info.getName() + " starting at "
						+ ContestUtil.formatStartTime(info.getStartTime()) + ")");
			}

			throw new IllegalArgumentException(sb2.toString());
		} catch (Exception e) {
			// ignore, not an array
		}

		try {
			JSONParser parser2 = new JSONParser(s);
			JsonObject obj = parser2.readObject();
			if (obj.getString("id") != null && obj.getString("name") != null && obj.getString("duration") != null)
				return theURL; // confirmed good contest endpoint

			throw new IllegalArgumentException("Unrecognized REST endpoint");
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

	public boolean isCDS() {
		return isCDS;
	}

	public void checkForUpdates(String prefix) {
		CDSUtil util = new CDSUtil(url.getProtocol() + "://" + url.getAuthority(), user, password);
		util.checkForUpdates(prefix);
	}

	@Override
	public String toString() {
		return "RESTContestSource[" + user + "|" + password + "@" + url + "]";
	}
}