package org.icpc.tools.client.core;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.internal.NetworkUtil;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ClientEndpointConfig.Configurator;
import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

public class BasicClient {
	// debug full json payloads
	private static final boolean DEBUG_JSON_PAYLOADS = false;
	private static final int TRACE_CHARS = 120;

	// debug all output (defaults to omit pings and info)
	private static final boolean DEBUG_BASIC = false;

	protected static final String TYPE = "type";
	protected static final String VERSION = "version";
	protected static final String CLIENT_TYPE = "client.type";
	protected static final String CONTEST_IDS = "contest.ids";
	protected static final String DISPLAYS = "displays";
	protected static final String REFRESH = "refresh";
	protected static final String WIDTH = "width";
	protected static final String HEIGHT = "height";
	protected static final String FPS = "fps";
	protected static final String PRESENTATION = "presentation";

	protected enum Type {
		PING, // ping, used to guage client response time and sync client time
		INFO, // information about the client sent back to admins
		PROPERTIES, // properties to set on the client
		LOG, // request for client to send log + response message
		SNAPSHOT, // request for client to send snapshot + response message
		COMMAND, // client command to stop, restart, request a log, or request a snapshot
		CLIENTS, // client list, sent to admins
		PRES_LIST // list of available presentations, sent to admin clients
	}

	protected interface AddAttrs {
		void add(JSONEncoder je) throws IOException;
	}

	class WSClientEndpoint extends Endpoint {
		private Session session;

		@Override
		public void onError(Session newSession, Throwable t) {
			Trace.trace(Trace.ERROR, "Error in websocket", t);
		}

		@Override
		public void onOpen(Session newSession, EndpointConfig config) {
			session = newSession;
			session.addMessageHandler(new MessageHandler.Whole<String>() {
				@Override
				public void onMessage(String message) {
					try {
						BasicClient.this.onMessage(message);
					} catch (IOException e) {
						Trace.trace(Trace.ERROR, "Error in websocket", e);
					}
				}
			});
		}

		protected void send(String message) throws IOException {
			if (session == null)
				return;

			session.getBasicRemote().sendText(message);
		}

		@Override
		public void onClose(Session session2, CloseReason closeReason) {
			Trace.trace(Trace.USER, name + " disconnected");
			fireConnectionStateEvent(false);
			session = null;
			if (closeReason != null && closeReason.getCloseCode() != CloseCodes.NORMAL_CLOSURE) {
				if (closeReason.getCloseCode() == CloseCodes.UNEXPECTED_CONDITION
						&& closeReason.getReasonPhrase().startsWith("CDS: ")) {
					Trace.trace(Trace.ERROR, closeReason.getReasonPhrase().substring(4));
					return;
				}
				Trace.trace(Trace.ERROR, "Unexpected websocket disconnect: " + closeReason.getReasonPhrase() + " ("
						+ closeReason.getCloseCode().toString() + ")");
			}

			// reconnect
			connect();
		}
	}

	class AuthorizationConfigurator extends Configurator {
		@Override
		public void beforeRequest(Map<String, List<String>> headers) {
			headers.put("Authorization", Arrays.asList("Basic " + auth));
		}
	}

	public static class Display {
		public int width;
		public int height;
		public int refresh;
	}

	public static class Client {
		public int uid;
		public String name;
		public String[] contestIds;
		public String type;
		public String version;
		public Display[] displays;
	}

	private String clientType = "Unknown";
	private String name = "Unknown";
	private int uid;
	private Client[] clients;
	private String url;
	private String auth;
	private String[] contestIds;
	private String role;
	protected long nanoTimeDelta = System.currentTimeMillis() * 1_000_000L - System.nanoTime();

	private List<IPropertyListener> listeners;
	private List<IConnectionListener> listeners2;

	private WSClientEndpoint clientEndpoint;

	public BasicClient(String url, String user, String password, String contestIds, String name, String role,
			String type) {
		this.url = url.replace("https", "wss").replace("http", "wss");
		if (this.url.endsWith("/"))
			this.url = this.url.substring(0, this.url.length() - 1);
		try {
			this.auth = getAuth(user, password);
		} catch (Exception e) {
			// ignore
		}
		if (contestIds != null)
			this.contestIds = new String[] { contestIds };
		this.name = name;
		this.role = role;
		this.clientType = type;
		if (this.name == null)
			this.name = NetworkUtil.getLocalAddress();
		uid = (user + type + NetworkUtil.getHostName() + NetworkUtil.getLocalAddress()).hashCode();
	}

	// helper method to create based on a REST contest source
	public BasicClient(RESTContestSource contestSource, String name, String role, String type) {
		try {
			URL url2 = contestSource.getURL();
			this.url = "wss://" + url2.getHost();
			if (url2.getPort() > 0)
				this.url += ":" + url2.getPort();
			this.auth = getAuth(contestSource.getUser(), contestSource.getPassword());
		} catch (Exception e) {
			// ignore
		}
		this.contestIds = new String[] { contestSource.getContestId() };
		this.name = name;
		this.role = role;
		this.clientType = type;
		if (this.name == null)
			this.name = NetworkUtil.getLocalAddress();
		uid = (contestSource.getUser() + type + NetworkUtil.getHostName() + NetworkUtil.getLocalAddress()).hashCode();
	}

	/**
	 * Create a generic client for a client type.
	 *
	 * @param contestSource
	 * @param clientType
	 */
	public BasicClient(RESTContestSource contestSource, String clientType) {
		try {
			URL url2 = contestSource.getURL();
			this.url = "wss://" + url2.getHost();
			if (url2.getPort() > 0)
				this.url += ":" + url2.getPort();
			this.auth = getAuth(contestSource.getUser(), contestSource.getPassword());
		} catch (Exception e) {
			// ignore
		}
		this.contestIds = new String[] { contestSource.getContestId() };

		this.name = clientType;
		this.clientType = clientType.toLowerCase();

		name += " " + NetworkUtil.getLocalAddress();
		uid = (contestSource.getUser() + clientType + NetworkUtil.getHostName() + NetworkUtil.getLocalAddress())
				.hashCode();
	}

	private static String getAuth(String user, String password) throws UnsupportedEncodingException {
		return Base64.getEncoder().encodeToString((user + ":" + password).getBytes("UTF-8"));
	}

	public void setUID(int uid) {
		this.uid = uid;
	}

	public void setRole(String role) {
		this.role = role;
	}

	private void handleProperties(JsonObject obj) {
		// fire control properties first
		Properties p = new Properties();
		Object children = obj.get("props");
		if (children != null) {
			JsonObject jo = (JsonObject) children;
			for (String key : jo.props.keySet()) {
				String value = jo.getString(key);
				if (key.contains("."))
					p.setProperty(key, value);
				else
					firePropertyChangedEvent(key, value);
			}
		}

		// then fire properties to presentations
		Iterator<Object> iter = p.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			String value = p.getProperty(key);
			firePropertyChangedEvent(key, value);
		}
	}

	protected void handleInfo(int sourceUID, JsonObject obj) {
		Client cl = null;
		for (Client c : clients) {
			if (c.uid == sourceUID)
				cl = c;
		}
		if (cl == null)
			return;

		Object[] contestIds2 = obj.getArray(CONTEST_IDS);
		if (contestIds2 != null) {
			cl.contestIds = new String[contestIds2.length];
			for (int j = 0; j < contestIds2.length; j++)
				cl.contestIds[j] = (String) contestIds2[j];
		}
		if (obj.containsKey(CLIENT_TYPE))
			cl.type = obj.getString(CLIENT_TYPE);
		if (obj.containsKey(VERSION))
			cl.version = obj.getString(VERSION);

		Object[] displays = obj.getArray(DISPLAYS);
		if (displays != null) {
			int size = displays.length;
			cl.displays = new Display[size];
			for (int j = 0; j < size; j++) {
				Display d = new Display();
				JsonObject dobj = (JsonObject) displays[j];
				d.height = dobj.getInt(HEIGHT);
				d.width = dobj.getInt(WIDTH);
				d.refresh = dobj.getInt(REFRESH);
				cl.displays[j] = d;
			}
		}
	}

	private void handleCommand(JsonObject obj) throws IOException {
		String action = obj.getString("action");
		if ("stop".equals(action))
			System.exit(0);
		else if ("restart".equals(action))
			System.exit(255);

		if (!obj.containsKey("source"))
			return;

		int source = getUID(obj, "source");
		if ("log".equals(action))
			sendLog(source);
		else if ("snapshot".equals(action))
			handleSnapshot(source);
	}

	/**
	 * @throws IOException
	 */
	protected void handleLogResponse(JsonObject obj) throws IOException {
		// ignore
	}

	/**
	 * @throws IOException
	 */
	protected void handleSnapshotResponse(JsonObject obj) throws IOException {
		// ignore
	}

	/**
	 * @throws IOException
	 */
	protected void handleSnapshot(int clientUID) throws IOException {
		// do nothing
	}

	private void handleClientList(JsonObject obj) {
		Object[] children = obj.getArray("clients");

		Map<Integer, JsonObject> clientInfo = new HashMap<>();

		Client[] tempClients = new Client[children.length];
		for (int i = 0; i < children.length; i++) {
			JsonObject jo = (JsonObject) children[i];
			int uid2 = getUID(jo, "uid");

			Client cl = null;
			if (clients != null) {
				for (Client c : clients) {
					if (c.uid == uid2)
						cl = c;
				}
			}
			if (cl == null) {
				cl = new Client();
				cl.uid = uid2;
			}

			tempClients[i] = cl;
			tempClients[i].name = jo.getString("name");

			clientInfo.put(tempClients[i].uid, jo);
		}

		clients = tempClients;
		clientsChanged(clients);

		for (Integer key : clientInfo.keySet()) {
			JsonObject jo = clientInfo.get(key);
			handleInfo(key.intValue(), jo);
		}
	}

	/**
	 * Called whenever the list of logged in clients changes. Only called on admin clients.
	 *
	 * @param clients a list of clients
	 */
	protected void clientsChanged(Client[] clients2) {
		// do nothing
	}

	/**
	 * @throws IOException
	 */
	protected void handlePresentationList(JsonObject obj) throws IOException {
		// do nothing
	}

	/**
	 * @throws IOException
	 */
	public void sendProperty(int[] clientUIDs, String key, String value) throws IOException {
		createJSON(Type.PROPERTIES, je -> {
			writeClients(je, clientUIDs);
			if (key != null) {
				je.openChild("props");
				je.encode(key, value);
				je.close();
			}
		});
	}

	public void requestPresentationList() throws IOException {
		createJSON(Type.PRES_LIST, null);
	}

	private void sendCommand(int[] clientUIDs, String command) throws IOException {
		createJSON(Type.COMMAND, je -> {
			writeClients(je, clientUIDs);
			je.encode("source", Integer.toHexString(uid));
			je.encode("action", command);
		});
	}

	public void sendStop(int[] clientUIDs) throws IOException {
		sendCommand(clientUIDs, "stop");
	}

	public void sendRestart(int[] clientUIDs) throws IOException {
		sendCommand(clientUIDs, "restart");
	}

	public void sendLogRequest(int[] clientUIDs) throws IOException {
		sendCommand(clientUIDs, "log");
	}

	public void sendSnapshotRequest(int[] clientUIDs) throws IOException {
		sendCommand(clientUIDs, "snapshot");
	}

	public void sendSnapshot(BufferedImage image) throws IOException {
		createJSON(Type.SNAPSHOT, je -> {
			je.encode("source", Integer.toHexString(uid));
			encodeImage(je, imageToBytes(image));
		});
	}

	protected void sendInfo(AddAttrs attr) throws IOException {
		createJSON(Type.INFO, je -> {
			je.encode("source", Integer.toHexString(uid));
			attr.add(je);
		});
	}

	protected void createJSON(Type type, AddAttrs attr) throws IOException {
		StringWriter sw = new StringWriter();
		JSONEncoder je = new JSONEncoder(new PrintWriter(sw));
		je.open();
		je.encode(TYPE, type.name().toLowerCase());
		if (attr != null)
			attr.add(je);
		je.close();
		sendIt(type, sw.toString());
	}

	protected void sendLog(int toUID) throws IOException {
		createJSON(Type.LOG, je -> {
			je.encode("source", Integer.toHexString(uid));
			je.encode("target", Integer.toHexString(toUID));
			je.encode("data", Base64.getEncoder().encodeToString(Trace.getLogContents2().getBytes()));
		});
	}

	protected void sendInfo() throws IOException {
		createJSON(Type.INFO, je -> {
			je.encode("source", Integer.toHexString(getUID()));
			je.encode(CLIENT_TYPE, clientType);
			je.encode(VERSION, Trace.getVersion());
			je.encode("os.name", System.getProperty("os.name"));
			je.encode("os.version", System.getProperty("os.version"));
			je.encode("java.vendor", System.getProperty("java.vendor"));
			je.encode("java.version", System.getProperty("java.version"));
			je.encode("host.address", NetworkUtil.getLocalAddress());
			je.encode("host.name", NetworkUtil.getHostName());
			je.encode("locale", Locale.getDefault().toString());
			je.encode("timezone", Calendar.getInstance().getTimeZone().getDisplayName());

			if (contestIds != null) {
				je.openChildArray(CONTEST_IDS);
				for (String id : contestIds)
					je.encodeValue(id);
				je.closeArray();
			}

			addBasicClientInfo(je);
		});
	}

	protected void sendInfoUpdate(String presentationName, int fps, byte[] image) throws IOException {
		sendInfo(je -> {
			je.encode(PRESENTATION, presentationName);
			je.encode(FPS, fps);
			encodeImage(je, image);
		});
	}

	public void sendInfoUpdate() {
		// to override
	}

	protected void addBasicClientInfo(JSONEncoder je) {
		// do nothing
	}

	protected byte[] imageToBytes(BufferedImage image) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ImageIO.write(image, "jpg", bout);
		return bout.toByteArray();
	}

	protected void encodeImage(JSONEncoder je, byte[] b) {
		je.encode("image", Base64.getEncoder().encodeToString(b));
	}

	protected static byte[] decodeImage(JsonObject obj) {
		String imgStr = obj.getString("image");
		return Base64.getDecoder().decode(imgStr);
	}

	protected static byte[] decodeFile(JsonObject obj) {
		return Base64.getDecoder().decode(obj.getString("file"));
	}

	protected static int getUID(JsonObject obj, String key) {
		return Integer.parseUnsignedInt(obj.getString(key), 16);
	}

	protected void sendSnapshot(BufferedImage image, int toUID) throws IOException {
		createJSON(Type.SNAPSHOT, je -> {
			je.encode("source", Integer.toHexString(uid));
			je.encode("target", Integer.toHexString(toUID));
			encodeImage(je, imageToBytes(image));
		});
	}

	protected boolean sendIt(Type type, String message) throws IOException {
		if (clientEndpoint == null)
			return false;

		if (DEBUG_BASIC || (type != Type.PING && type != Type.INFO))
			trace("> " + message, DEBUG_JSON_PAYLOADS);
		clientEndpoint.send(message);
		return true;
	}

	public void addListener(IConnectionListener listener) {
		if (listeners2 == null)
			listeners2 = new ArrayList<>();
		if (!listeners2.contains(listener))
			listeners2.add(listener);
	}

	public void removeListener(IConnectionListener listener) {
		if (listeners2 == null)
			return;
		listeners2.remove(listener);
	}

	private void fireConnectionStateEvent(boolean state) {
		if (listeners2 == null)
			return;
		IConnectionListener[] list;
		synchronized (listeners2) {
			list = new IConnectionListener[listeners2.size()];
			listeners2.toArray(list);
		}

		int size = list.length;
		for (int i = 0; i < size; i++) {
			list[i].connectionStateChanged(state);
		}
	}

	public void addListener(IPropertyListener listener) {
		if (listeners == null)
			listeners = new ArrayList<>();
		if (!listeners.contains(listener))
			listeners.add(listener);
	}

	public void removeListener(IPropertyListener listener) {
		if (listeners == null)
			return;
		listeners.remove(listener);
	}

	private void firePropertyChangedEvent(String key, String value) {
		if (listeners == null)
			return;
		IPropertyListener[] list;
		synchronized (listeners) {
			list = new IPropertyListener[listeners.size()];
			listeners.toArray(list);
		}

		int size = list.length;
		for (int i = 0; i < size; i++) {
			try {
				list[i].propertyUpdated(key, value);
			} catch (Throwable t) {
				Trace.trace(Trace.ERROR, "Error firing property change event", t);
			}
		}
	}

	public void connect() {
		connect(true);
	}

	public void connect(boolean daemon) {
		Thread connectionThread = new Thread("Client connection thread") {
			// try to connect right away, then add some incremental back-off
			private int[] DELAY = new int[] { 2, 5, 20 };

			@Override
			public void run() {
				Trace.trace(Trace.USER, name + " connecting...");
				Session s = null;
				int i = 0;
				while (s == null) {
					s = connectImpl();
					if (s == null) {
						Trace.trace(Trace.INFO, "Trying again in " + DELAY[i] + " seconds");
						try {
							sleep(1000 * DELAY[i]);
						} catch (InterruptedException e) {
							// ignore
						}
						if (i < DELAY.length - 1)
							i++;
					}
				}
			}
		};

		connectionThread.setPriority(Thread.NORM_PRIORITY + 1);
		connectionThread.setDaemon(daemon);
		connectionThread.start();
	}

	/**
	 * Check to see if we can successfully connect to the CDS as the current role.
	 */
	public boolean ping() {
		Trace.trace(Trace.USER, name + " pinging...");
		boolean connected = false;
		Session s = connectImpl();
		if (s != null && s.isOpen()) {
			connected = true;
			try {
				s.close();
			} catch (Exception e) {
				// ignore
			}
		}
		return connected;
	}

	protected Session connectImpl() {
		Session s = null;
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			ctx.init(null, new TrustManager[] { new HTTPSSecurity.ContestTrustManager() }, null);
			SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(ctx, true, false, false);
			sslEngineConfigurator.setHostVerificationEnabled(false);

			ClientManager client = ClientManager.createClient();
			client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);

			clientEndpoint = new WSClientEndpoint();
			ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create()
					.configurator(new AuthorizationConfigurator()).build();
			StringBuilder sb = new StringBuilder(url);
			sb.append("/presentation/ws");
			sb.append("?name=" + URLEncoder.encode(name, "UTF-8"));
			sb.append("&uid=" + Integer.toHexString(uid));
			if (role == null)
				sb.append("&role=any");
			else
				sb.append("&role=" + role);
			sb.append("&version=1.0");

			URI uri = new URI(sb.toString());

			client.setDefaultMaxSessionIdleTimeout(60000L);
			s = client.connectToServer(clientEndpoint, endpointConfig, uri);

			Trace.trace(Trace.USER, name + " connected");
			fireConnectionStateEvent(true);
			sendInfo();
		} catch (IOException | DeploymentException e) {
			String msg = e.getMessage();
			if (e.getCause() != null)
				msg += " (" + e.getCause().getMessage() + ")";
			Trace.trace(Trace.ERROR,
					"Error connecting to " + url + ": " + msg + ". Confirm URL and user/password or view log for details");
			Trace.trace(Trace.INFO, "Failure details", e);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Unexected error connecting to " + url + ": " + e.getMessage());
			Trace.trace(Trace.INFO, "Failure details", e);
		} catch (Throwable t) {
			Trace.trace(Trace.ERROR, "Unexected error connecting to " + url + ": " + t.getMessage());
			Trace.trace(Trace.INFO, "Failure details", t);
		}
		return s;
	}

	private void handlePing() throws IOException {
		StringWriter sw = new StringWriter();
		JSONEncoder je = new JSONEncoder(new PrintWriter(sw));
		je.open();
		je.encode(TYPE, Type.PING.name().toLowerCase());
		je.encode("time", System.currentTimeMillis());
		je.close();
		sendIt(Type.PING, sw.toString());
	}

	protected void handleTime() {
		// react to time change
	}

	private static void trace(String message, boolean user) {
		String s = message;
		if (s.length() > TRACE_CHARS)
			s = s.substring(0, TRACE_CHARS) + "...";

		if (user)
			Trace.trace(Trace.USER, s);
		else
			Trace.trace(Trace.INFO, s);
	}

	private void onMessage(String message) throws IOException {
		trace("< " + message, DEBUG_JSON_PAYLOADS);

		JSONParser rdr = new JSONParser(message);
		JsonObject obj = rdr.readObject();
		String type = obj.getString(TYPE);

		Type action = null;
		for (Type t : Type.values()) {
			if (t.name().equalsIgnoreCase(type))
				action = t;
		}

		switch (action) {
			case PING: {
				if (obj.containsKey("time_delta")) {
					long timeDeltaMs = obj.getLong("time_delta");
					nanoTimeDelta = (System.currentTimeMillis() + timeDeltaMs) * 1_000_000L - System.nanoTime();
					handleTime();
				}
				handlePing();
				break;
			}
			case CLIENTS: {
				handleClientList(obj);
				break;
			}
			case INFO: {
				int sourceUID = getUID(obj, "source");
				handleInfo(sourceUID, obj);
				break;
			}
			case COMMAND: {
				handleCommand(obj);
				break;
			}
			case LOG: {
				handleLogResponse(obj);
				break;
			}
			case SNAPSHOT: {
				handleSnapshotResponse(obj);
				break;
			}
			case PROPERTIES: {
				handleProperties(obj);
				break;
			}
			case PRES_LIST: {
				handlePresentationList(obj);
				break;
			}
			default: {
				Trace.trace(Trace.WARNING, "Unknown action: " + message);
				return;
			}
		}
	}

	private static void writeClients(JSONEncoder je, int[] uids) {
		je.openChildArray("clients");
		if (uids != null) {
			for (int i = 0; i < uids.length; i++)
				je.encodeValue(Integer.toHexString(uids[i]));
		}
		je.closeArray();
	}

	public int getUID() {
		return uid;
	}

	public String getClientId() {
		return name;
	}

	@Override
	public String toString() {
		return "Basic Client [" + name + "/uid " + Integer.toHexString(uid) + "]";
	}
}