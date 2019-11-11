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
	protected static enum Type {
		PING, PRES_LIST, PROPERTIES, CLIENTS, TIME, STOP, RESTART, REQUEST_LOG, LOG, REQUEST_SNAPSHOT, SNAPSHOT, INFO, CLIENT_INFO, THUMBNAIL
	}

	interface AddAttrs {
		public void add(JSONEncoder je) throws IOException;
	}

	public class ClientDisplay {
		public int width;
		public int height;
		public int refresh;
	}

	public class PresentationClientInfo {
		public int width;
		public int height;
		public int fps;
		public String presentation;
	}

	protected class TimeSync {
		public long sentAt;
		public long deltaGuess = -1;
		public long deltaReal = -1;

		public TimeSync() {
			sentAt = System.currentTimeMillis();
		}
	}

	class Message {
		String message;
		int source;
		Type type;
	}

	private Session session;
	private int uid;
	private String name;
	private String user;
	private boolean isAdmin;
	private List<TimeSync> timeSync = new ArrayList<>();
	private Queue<Message> queue = new ConcurrentLinkedQueue<>();
	private ClientDisplay[] displays;
	private PresentationClientInfo presInfo;
	private String[] contestIds;
	private String clientType;
	private String version;

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

	public PresentationClientInfo getPresentationClientInfo() {
		return presInfo;
	}

	public ClientDisplay[] getDisplays() {
		return displays;
	}

	private void queueIt(Type type, String message) {
		queueIt(type, -1, message);
	}

	private void queueIt(Type type, int source, String message) {
		synchronized (queue) {
			if (type == Type.INFO || type == Type.THUMBNAIL) {
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
			if (message.type == Type.PING) {
				// log when a ping went out
				synchronized (timeSync) {
					timeSync.add(new TimeSync());
				}
			}
			PresentationServer.trace("< " + message.message, uid);

			session.getBasicRemote().sendText(message.message);
			if (message.type == Type.STOP || message.type == Type.RESTART)
				return false;

			synchronized (queue) {
				message = queue.poll();
			}
		}
		return true;
	}

	private void writeTime(long delta) throws IOException {
		TimeSync ts = new TimeSync();
		ts.deltaGuess = delta;
		synchronized (timeSync) {
			timeSync.add(ts);
		}

		createJSON(Type.TIME, je -> {
			je.encode("time", ts.sentAt + ts.deltaGuess);
		});
	}

	protected boolean handlePing() throws IOException {
		int count = 0;
		int total = 0;
		long lastGuess = -1;
		long min = Long.MAX_VALUE;
		long max = 0;
		synchronized (timeSync) {
			while (timeSync.size() > 5)
				timeSync.remove(0);

			for (TimeSync ts : timeSync) {
				boolean last = false;
				if (ts.deltaReal == -1) {
					ts.deltaReal = (System.currentTimeMillis() - ts.sentAt) / 2;
					last = true;
				}
				count++;
				total += ts.deltaReal;
				min = Math.min(min, ts.deltaReal);
				max = Math.max(max, ts.deltaReal);
				if (ts.deltaGuess >= 0)
					lastGuess = ts.deltaGuess;
				else
					ts.deltaGuess = lastGuess;

				if (last)
					break;
			}
		}

		if (count < 2) {
			// not enough response time data yet
			writePing();
			return true;
		}

		if (count == 2 && max - min > 50) {
			// very inconsistent response times, ping again
			writePing();
			return true;
		}

		// get rid of 'worst' time outlier
		total -= max;
		count--;

		// if last guess time is still good enough, return
		final long bestGuess = total / count / 2;
		long networkTolerance = max - min + 10;
		if (lastGuess >= 0 && Math.abs(bestGuess - lastGuess) < networkTolerance)
			return false;

		Trace.trace(Trace.INFO, "Syncing time: " + bestGuess + " with " + toString());
		writeTime(bestGuess);
		return true;
	}

	protected void queueSimpleMessage(Type type) {
		StringWriter sw = new StringWriter();
		JSONEncoder je = new JSONEncoder(new PrintWriter(sw));
		je.open();
		je.encode("type", type.name().toLowerCase());
		je.close();
		queueIt(type, sw.toString());
	}

	protected void writePing() {
		queueSimpleMessage(Type.PING);
	}

	protected void writeStop() {
		queueSimpleMessage(Type.STOP);
	}

	protected void writeRestart() {
		queueSimpleMessage(Type.RESTART);
	}

	protected void writeClients(List<Client> clients) throws IOException {
		createJSON(Type.CLIENTS, je -> {
			je.openChildArray("clients");
			for (Client c : clients) {
				je.open();
				je.encode("name", c.name);
				je.encode("uid", Integer.toHexString(c.uid));
				if (c.contestIds != null) {
					je.openChildArray("contest.ids");
					for (String cId : c.contestIds)
						je.encodeValue(cId);
					je.closeArray();
				}
				je.encode("version", c.version);
				je.encode("client.type", c.clientType);
				je.close();
			}
			je.closeArray();
		});
	}

	protected void writeLogRequest(String message) {
		queueIt(Type.REQUEST_LOG, message);
	}

	protected void writeLog(String message) {
		queueIt(Type.LOG, message);
	}

	protected void writeSnapshotRequest(String message) {
		queueIt(Type.REQUEST_SNAPSHOT, message);
	}

	protected void writeSnapshot(String message) {
		queueIt(Type.SNAPSHOT, message);
	}

	protected void storeClientInfo(JsonObject obj) {
		version = obj.getString("version");
		clientType = obj.getString("client.type");
		Object[] children = obj.getArray("contest.ids");
		if (children != null) {
			contestIds = new String[children.length];
			for (int i = 0; i < children.length; i++)
				contestIds[i] = (String) children[i];
		}
		children = obj.getArray("displays");
		if (children != null) {
			int size = children.length;
			displays = new ClientDisplay[size];
			for (int i = 0; i < children.length; i++) {
				ClientDisplay d = new ClientDisplay();
				JsonObject dobj = (JsonObject) children[i];
				d.height = dobj.getInt("height");
				d.width = dobj.getInt("width");
				d.refresh = dobj.getInt("refresh");
				displays[i] = d;
			}
		}
	}

	protected void storeInfo(JsonObject obj) {
		if (presInfo == null)
			presInfo = new PresentationClientInfo();
		presInfo.presentation = obj.getString("presentation");
		presInfo.width = obj.getInt("width");
		presInfo.height = obj.getInt("height");
		presInfo.fps = obj.getInt("fps");
	}

	protected void writeThumbnail(int source, String message) {
		queueIt(Type.THUMBNAIL, source, message);
	}

	protected void writeInfo(int source, String message) {
		queueIt(Type.INFO, source, message);
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