package org.icpc.tools.cds.presentations;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Session;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class Client {
	private static final String VERSION = "version";
	private static final String CLIENT_TYPE = "client.type";
	private static final String CONTEST_IDS = "contest.ids";
	private static final String DISPLAYS = "displays";
	private static final String REFRESH = "refresh";
	private static final String WIDTH = "width";
	private static final String HEIGHT = "height";
	private static final String FPS = "fps";
	private static final String PRESENTATION = "presentation";

	protected enum Type {
		PING, // ping, used to guage client response time
		TIME, // time sync sent to clients after several successful pings
		INFO, // information about the client sent back to admins
		PROPERTIES, // properties to set on the client
		LOG, // request for client to send log + response message
		SNAPSHOT, // request for client to send snapshot + response message
		COMMAND, // client command to stop, restart, request a log, or request a snapshot
		CLIENTS, // client list, sent to admins
		PRES_LIST // list of available presentations, sent to admin clients
	}

	interface AddAttrs {
		void add(JSONEncoder je) throws IOException;
	}

	public class ClientDisplay {
		public int width;
		public int height;
		public int refresh;
	}

	public class ClientInfo {
		public int width;
		public int height;
		public String presentation;
		public int fps;
	}

	protected class TimeSync {
		public long sentAt;
		public long delta = -1;

		public TimeSync() {
			sentAt = System.currentTimeMillis();
		}
	}

	class Message {
		String message;
		int source;
		Type type;
		// flag is currently overloaded for two purposes: 1) to indicate info messages that only
		// contain thumbnails and can be thrown away if high traffic causes duplication
		// 2) to indicate disconnect messages (stop or restart) so that the server disconnects
		// cleanly
		boolean flag;
	}

	private Session session;
	private int uid;
	private String name;
	private String user;
	private boolean isAdmin;
	private List<TimeSync> timeSync = new ArrayList<>();
	private Queue<Message> queue = new ConcurrentLinkedQueue<>();
	private ClientDisplay[] displays;
	private ClientInfo clientInfo;
	private String[] contestIds;
	private String clientType;
	private String version;
	private long lastTimeSync = -1;

	public Client(Session session, String user, int uid, String name, boolean isAdmin) {
		this.session = session;
		this.user = user;
		this.uid = uid;
		this.name = name;
		this.isAdmin = isAdmin;
	}

	public String getUser() {
		return user;
	}

	public int getUID() {
		return uid;
	}

	public String getName() {
		return name;
	}

	public String getContestId() {
		return contestIds[0];
	}

	public String[] getContestIds() {
		return contestIds;
	}

	public String getVersion() {
		return version;
	}

	public String getType() {
		return clientType;
	}

	public Session getSession() {
		return session;
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public ClientInfo getClientInfo() {
		return clientInfo;
	}

	public ClientDisplay[] getDisplays() {
		return displays;
	}

	private void queueIt(Type type, String message) {
		queueIt(type, -1, message, false);
	}

	private void queueIt(Type type, String message, boolean flag) {
		queueIt(type, -1, message, flag);
	}

	private void queueIt(Type type, int source, String message, boolean flag) {
		synchronized (queue) {
			if (type == Type.INFO && flag) {
				Message remove = null;
				for (Message m : queue) {
					if (m.type == type && m.source == source) {
						Trace.trace(Trace.INFO,
								"Throwing out duplicate packet type " + type + " bound for " + Integer.toHexString(uid));
						remove = m;
						break;
					}
				}
				if (remove != null)
					queue.remove(remove);
			}
			Message m = new Message();
			m.type = type;
			m.source = source;
			m.message = message;
			m.flag = flag;
			queue.add(m);
		}
	}

	protected boolean sendData() throws IOException {
		if (!session.isOpen())
			throw new IOException("Session is not open");

		Message message = null;
		synchronized (queue) {
			message = queue.poll();
		}

		while (message != null) {
			if (message.type == Type.PING || message.type == Type.TIME) {
				// log when a ping went out
				synchronized (timeSync) {
					timeSync.add(new TimeSync());
				}
			}
			if ((message.type != Type.PING && message.type != Type.INFO) || PresentationServer.TRACE_ALL)
				PresentationServer.trace("> " + message.message, uid);

			session.getBasicRemote().sendText(message.message);
			if (message.type == Type.COMMAND && message.flag)
				return false;

			synchronized (queue) {
				message = queue.poll();
			}
		}
		return true;
	}

	private void writeTime(long delta) throws IOException {
		// if last guess time is close enough, return w/o syncing time again
		if (lastTimeSync >= 0 && Math.abs(delta) < 20)
			return;

		Trace.trace(Trace.INFO, Integer.toHexString(uid) + " - Syncing time to " + delta + "ms.");

		lastTimeSync = delta;
		createJSON(Type.TIME, je -> {
			je.encode("time", System.currentTimeMillis() + delta);
		});
	}

	protected long handlePing() throws IOException {
		int count = 0;
		int total = 0;
		long min = Long.MAX_VALUE;
		long max = 0;
		StringBuilder sb = new StringBuilder();
		synchronized (timeSync) {
			while (timeSync.size() > 5)
				timeSync.remove(0);

			for (TimeSync ts : timeSync) {
				count++;
				boolean last = false;
				if (ts.delta == -1) {
					ts.delta = (System.currentTimeMillis() - ts.sentAt) / 2;
					last = true;
				}

				if (count > 1)
					sb.append(", ");
				sb.append(ts.delta + "ms");
				total += ts.delta;
				min = Math.min(min, ts.delta);
				max = Math.max(max, ts.delta);

				if (last)
					break;
			}
		}

		Trace.trace(Trace.INFO, Integer.toHexString(uid) + " - Recent pings: [" + sb.toString() + "] " + lastTimeSync);

		// if delta difference is < 20ms, we're close enough
		if (count > 1 && max < 20) {
			writeTime(total / count);
			return 0;
		}

		if (count < 3) {
			// not enough response time data yet, ping again in 500ms
			writePing();
			return 500;
		}

		if (count == 3 && max - min > 50) {
			// very inconsistent response times, ping once more in 750ms
			writePing();
			return 750;
		}

		// get rid of 'worst' time outlier and determine new average time sync
		writeTime((total - max) / (count - 1));
		return 0;
	}

	protected void writePing() {
		StringWriter sw = new StringWriter();
		JSONEncoder je = new JSONEncoder(new PrintWriter(sw));
		je.open();
		je.encode("type", Type.PING.name().toLowerCase());
		je.close();
		queueIt(Type.PING, sw.toString());
	}

	protected void writeCommand(String source, String action) {
		StringWriter sw = new StringWriter();
		JSONEncoder je = new JSONEncoder(new PrintWriter(sw));
		je.open();
		je.encode("type", Type.COMMAND.name().toLowerCase());
		je.encode("source", source);
		je.encode("action", action);
		je.close();
		queueIt(Type.COMMAND, sw.toString(), "stop".equals(action) || "restart".equals(action));
	}

	/**
	 * Send the list of clients to an admin.
	 *
	 * @param clients the full list of clients for this admin
	 * @param newClients the list of clients that are new to this admin, and hence should include
	 *           full/all information
	 * @throws IOException
	 */
	protected void writeClients(List<Client> clients, List<Client> newClients) throws IOException {
		createJSON(Type.CLIENTS, je -> {
			je.openChildArray("clients");
			for (Client c : clients) {
				je.open();
				je.encode("name", c.name);
				je.encode("uid", Integer.toHexString(c.uid));

				if (newClients == null || !newClients.contains(c)) {
					je.close();
					continue;
				}

				if (c.clientInfo != null) {
					je.encode("width", c.clientInfo.width);
					je.encode("height", c.clientInfo.height);
				}

				if (c.contestIds != null) {
					je.openChildArray("contest.ids");
					for (String cId : c.contestIds)
						je.encodeValue(cId);
					je.closeArray();
				}
				if (c.version != null)
					je.encode("version", c.version);
				if (c.clientType != null)
					je.encode("client.type", c.clientType);

				if (c.displays != null) {
					je.openChildArray("displays");
					for (int i = 0; i < c.displays.length; i++) {
						ClientDisplay cd = c.displays[i];
						je.open();
						je.encode("width", cd.width);
						je.encode("height", cd.height);
						je.encode("refresh", cd.refresh);
						je.close();
					}
					je.closeArray();
				}

				je.close();
			}
			je.closeArray();
		});
	}

	/*protected void writeClientInfo(List<Client> clients) throws IOException {
		for (Client c : clients) {
			createJSON(Type.INFO, je -> {
				if (c.clientInfo != null) {
					je.encode("uid", Integer.toHexString(c.uid));
					je.encode("width", c.clientInfo.width);
					je.encode("height", c.clientInfo.height);
				}
			});
		}
	}*/

	protected void writeLog(String message) {
		queueIt(Type.LOG, message);
	}

	protected void writeSnapshot(String message) {
		queueIt(Type.SNAPSHOT, message);
	}

	protected boolean storeClientInfo(JsonObject obj) {
		boolean baseInfo = false;
		if (obj.containsKey(VERSION)) {
			version = obj.getString(VERSION);
			baseInfo = true;
		}
		if (obj.containsKey(CLIENT_TYPE)) {
			clientType = obj.getString(CLIENT_TYPE);
			baseInfo = true;
		}
		Object[] children = obj.getArray(CONTEST_IDS);
		if (children != null) {
			contestIds = new String[children.length];
			for (int i = 0; i < children.length; i++)
				contestIds[i] = (String) children[i];
			baseInfo = true;
		}
		children = obj.getArray(DISPLAYS);
		if (children != null) {
			int size = children.length;
			displays = new ClientDisplay[size];
			for (int i = 0; i < children.length; i++) {
				ClientDisplay d = new ClientDisplay();
				JsonObject dobj = (JsonObject) children[i];
				d.height = dobj.getInt(HEIGHT);
				d.width = dobj.getInt(WIDTH);
				d.refresh = dobj.getInt(REFRESH);
				displays[i] = d;
			}
			baseInfo = true;
		}

		if (clientInfo == null)
			clientInfo = new ClientInfo();
		if (obj.containsKey(WIDTH))
			clientInfo.width = obj.getInt(WIDTH);
		if (obj.containsKey(HEIGHT))
			clientInfo.height = obj.getInt(HEIGHT);
		if (obj.containsKey(PRESENTATION))
			clientInfo.presentation = obj.getString(PRESENTATION);
		if (obj.containsKey(FPS))
			clientInfo.fps = obj.getInt(FPS);

		return baseInfo;
	}

	protected void writeInfo(int source, String message, boolean flag) {
		queueIt(Type.INFO, source, message, flag);
	}

	protected void writeProperties(Properties p) throws IOException {
		createJSON(Type.PROPERTIES, je -> {
			je.openChild("props");
			Object[] keys = p.keySet().toArray();
			for (int i = 0; i < keys.length; i++) {
				String key = keys[i].toString();
				// je.openChild("property");
				je.encode(key, p.getProperty(key));
				// je.close();
			}
			je.close();
		});
	}

	protected void createJSON(Type type, AddAttrs attr) throws IOException {
		StringWriter sw = new StringWriter();
		JSONEncoder je = new JSONEncoder(new PrintWriter(sw));
		je.open();
		je.encode("type", type.name().toLowerCase());

		attr.add(je);
		je.close();
		queueIt(type, sw.toString());
	}

	protected void writePresentationList(File file) throws IOException {
		createJSON(Type.PRES_LIST, je -> {
			byte[] b = writeFile(file);
			String s = Base64.getEncoder().encodeToString(b);
			je.encode("file", s);
		});
	}

	/**
	 * Writes the given file to the output stream.
	 *
	 * @param out
	 * @param file
	 * @throws IOException
	 */
	private static byte[] writeFile(File file) throws IOException {
		BufferedInputStream fin = null;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		try {
			fin = new BufferedInputStream(new FileInputStream(file));

			byte[] buf = new byte[8096];
			int n = fin.read(buf);
			while (n >= 0) {
				if (n > 0)
					bout.write(buf, 0, n);
				n = fin.read(buf);
			}
			return bout.toByteArray();
		} catch (FileNotFoundException e) {
			Trace.trace(Trace.ERROR, "Could not find file", e);
			throw e;
		} catch (IOException e) {
			Trace.trace(Trace.ERROR, "Error writing file", e);
			throw e;
		} finally {
			try {
				if (fin != null)
					fin.close();
			} catch (Exception e) {
				// don't log - it'll get finalized anyway
			}
		}
	}

	protected void disconnect() {
		try {
			if (session.isOpen())
				// TODO - this appears to be hanging, and not absolutely necessary
				session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Good-bye!"));
		} catch (Exception e) {
			// ignore
		}
	}

	@Override
	public String toString() {
		return name + " [uid " + Integer.toHexString(uid) + "]";
	}
}