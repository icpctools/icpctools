package org.icpc.tools.cds.presentations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import javax.websocket.Session;

import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.cds.CDSConfig.Domain;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class PresentationServer {
	private static final String PREF_ID = "org.icpc.tools.cds";
	private static final String PRES = "presentation";
	private static final String DEFAULT_PREFIX = "default:";
	private static final String TIME_PREFIX = "time:";

	public class TimedEvent {
		public String contestId;
		public long contestTimeMs;
		public int[] ids;
		public String key;
		public String value;
	}

	private List<TimedEvent> events = new ArrayList<>();

	private ScheduledExecutorService executor;

	private PresentationServer() {
		super();
	}

	private static PresentationServer instance = new PresentationServer();
	private static Preferences cdsPrefs = new PropertiesPreferences(PREF_ID);

	protected static Preferences getPreferences() {
		return cdsPrefs;
	}

	public static PresentationServer getInstance() {
		return instance;
	}

	protected static int getUID(JsonObject obj, String key) {
		return Integer.parseUnsignedInt(obj.getString(key), 16);
	}

	protected List<Client> clients = new ArrayList<>();
	protected List<Client> pendingClients = new ArrayList<>();

	// maps both clients to their list of admins, and admins to the list of clients
	protected Map<Client, List<Client>> adminMap = new HashMap<>();

	protected List<Client> getClients() {
		return clients;
	}

	protected boolean doesClientExist(int uid) {
		for (Client cl : clients) {
			if (uid == cl.getUID())
				return true;
		}

		return false;
	}

	protected static void trace(String message, int uid) {
		String s = message;
		if (s.length() > 150)
			s = s.substring(0, 150) + "...";

		Trace.trace(Trace.INFO, Integer.toHexString(uid) + " " + s);
	}

	protected void onMessage(Session s, String message) throws IOException {
		Client[] cl = clients.toArray(new Client[0]);

		Client c = null;
		for (Client cli : cl) {
			if (s.equals(cli.getSession())) {
				c = cli;
			}
		}

		if (c == null)
			throw new IOException("Client " + s.getId() + " does not exist");

		trace("< " + message, c.getUID());

		int sourceUID = c.getUID();

		try {
			JSONParser rdr = new JSONParser(message);
			JsonObject obj = rdr.readObject();
			String type = obj.getString("type");

			Client.Type action = null;
			for (Client.Type t : Client.Type.values()) {
				if (t.name().equalsIgnoreCase(type))
					action = t;
			}

			switch (action) {
				case PROPERTIES: {
					handleProperties(obj);
					break;
				}
				case THUMBNAIL: {
					handleThumbnail(c, obj, message);
					break;
				}
				case INFO: {
					handleInfo(c, obj, message);
					break;
				}
				case CLIENT_INFO: {
					handleClientInfo(c, obj, message);
					break;
				}
				case PRES_LIST: {
					handlePresentationList(sourceUID);
					break;
				}
				case STOP: {
					handleStop(obj);
					return;
				}
				case RESTART: {
					handleRestart(obj);
					return;
				}
				case REQUEST_LOG: {
					handleRequestLog(message, obj);
					return;
				}
				case LOG: {
					handleLog(message, obj);
					return;
				}
				case REQUEST_SNAPSHOT: {
					handleRequestSnapshot(message, obj);
					return;
				}
				case SNAPSHOT: {
					handleSnapshot(message, obj);
					return;
				}
				case PING: {
					long delay = c.handlePing();
					if (delay > 0)
						scheduleClient(c, delay);
					return;
				}
				default: {
					Trace.trace(Trace.WARNING, "Unknown action: " + action);
					return;
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Client connection failure", e);
		}
	}

	protected boolean isAdminOf(String admin, String client) {
		Domain[] domains = CDSConfig.getInstance().getDomains();
		if (domains == null || domains.length == 0)
			return true;

		for (Domain d : domains) {
			String[] users = d.getUsers();
			boolean hasAdmin = false;
			boolean hasClient = false;
			if (users != null) {
				for (String user : users) {
					if (admin.equals(user))
						hasAdmin = true;
					if (client.equals(user))
						hasClient = true;
				}
			}
			if (hasAdmin && hasClient)
				return true;
		}
		return false;
	}

	protected synchronized Client addClient(Client c) {
		try {
			Trace.trace(Trace.USER, "Client connected: " + c);

			synchronized (clients) {
				clients = safeAdd(clients, c);
			}

			synchronized (adminMap) {
				if (c.isAdmin()) {
					// determine which clients this admin should manage
					List<Client> admClients = new ArrayList<>(5);
					for (Client cl : clients) {
						if (!cl.isAdmin() && isAdminOf(c.getUser(), cl.getUser()))
							admClients.add(cl);
					}

					forClient(c, cl -> cl.writeClients(admClients));
					adminMap.put(c, admClients);

					// set each client's admin
					for (Client cl : admClients) {
						List<Client> admins = adminMap.get(cl);
						adminMap.put(cl, safeAdd(admins, c));
					}
				} else {
					// determine which admins should manage this client
					List<Client> adm = new ArrayList<>();
					for (Client admin : clients) {
						if (admin.isAdmin() && isAdminOf(admin.getUser(), c.getUser())) {
							adm.add(admin);

							List<Client> adminClients = adminMap.get(admin);
							List<Client> adminClients2 = safeAdd(adminClients, c);
							adminMap.put(admin, adminClients2);
							forClient(admin, cli -> cli.writeClients(adminClients2));
						}
					}
					adminMap.put(c, adm);

					sendInitialProperties(c);
				}
			}

			c.writePing();
			scheduleClient(c);
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Client connection failure", e);
		}
		return c;
	}

	protected void updateClients(Client c) {
		synchronized (adminMap) {
			List<Client> admins = adminMap.get(c);
			if (admins == null)
				return;

			for (Client admin : clients) {
				if (admin.isAdmin() && isAdminOf(admin.getUser(), c.getUser())) {
					List<Client> adminClients = adminMap.get(admin);
					forClient(admin, cli -> cli.writeClients(adminClients));
				}
			}
		}
	}

	protected void sendInitialProperties(Client c) {
		try {
			Preferences prefs = getPreferences().node("uid" + c.getUID());
			String[] keys = prefs.keys();
			Properties p = new Properties();
			if (keys != null && keys.length > 0) {
				for (String key : keys) {
					String value = prefs.get(key, null);
					if (value != null) {
						p.put(key, value);
					}
				}
			}
			Preferences defPrefs = getPreferences().node("default");
			if (defPrefs != null) {
				String[] defKeys = defPrefs.keys();
				for (String s : defKeys) {
					String defVal = defPrefs.get(s, null);
					if (defVal != null && !p.containsKey(s)) {
						p.put(s, defVal);
					}
				}
			}

			// always sort presentations first
			if (!p.isEmpty()) {
				if (p.size() > 1 && p.containsKey(PRES)) {
					Properties p2 = new Properties();
					p2.put(PRES, p.get(PRES));
					c.writeProperties(p2);
					p.remove(PRES);
				}
				if (!p.isEmpty())
					c.writeProperties(p);
			}
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Client connection failure", e);
		}
	}

	protected void remove(Session s) {
		Client remove = null;
		for (Client c : clients)
			if (c.getSession().equals(s))
				remove = c;

		if (remove != null)
			remove(remove);
	}

	protected synchronized void remove(Client cl) {
		synchronized (adminMap) {
			if (cl.isAdmin()) {
				// remove from clients
				List<Client> clients2 = adminMap.get(cl);
				for (Client cli : clients2) {
					List<Client> cliAdms = adminMap.get(cli);
					List<Client> cliAdms2 = safeRemove(cliAdms, cl);
					adminMap.put(cli, cliAdms2);
				}
			} else {
				// remove from current admins
				List<Client> admins = adminMap.get(cl);
				for (Client adm : admins) {
					List<Client> clientList = adminMap.get(adm);
					List<Client> clientList2 = safeRemove(clientList, cl);
					adminMap.put(adm, clientList2);
					forClient(adm, cli -> cli.writeClients(clientList2));
				}
			}
			adminMap.remove(cl);
		}

		synchronized (clients) {
			clients = safeRemove(clients, cl);
		}

		if (!executor.isShutdown())
			executor.execute(() -> cl.disconnect());

		Trace.trace(Trace.USER, "Client disconnected: " + cl);
	}

	private static List<Client> safeAdd(List<Client> list, Client cl) {
		List<Client> tempList = new ArrayList<>(list.size() + 1);
		tempList.addAll(list);
		tempList.add(cl);
		return tempList;
	}

	private static List<Client> safeRemove(List<Client> list, Client cl) {
		List<Client> tempList = new ArrayList<>(list.size());
		tempList.addAll(list);
		tempList.remove(cl);
		return tempList;
	}

	interface ClientRun {
		void run(Client c) throws IOException;
	}

	protected List<Client> getClients(int[] clientUIDs) {
		List<Client> list = new ArrayList<>(5);
		for (Client c : clients) {
			for (int uid : clientUIDs)
				if (c.getUID() == uid)
					list.add(c);
		}
		return list;
	}

	protected Client getClient(int clientUID) {
		for (Client c : clients) {
			if (c.getUID() == clientUID)
				return c;
		}
		return null;
	}

	private void forEachClient(List<Client> targetClients, ClientRun r) {
		if (targetClients == null || targetClients.isEmpty())
			return;

		for (Client client : targetClients)
			forClient(client, r);
	}

	private void forClient(Client client, final ClientRun r) {
		if (client == null)
			return;

		try {
			r.run(client);
			scheduleClient(client);
		} catch (IOException e) {
			Trace.trace(Trace.WARNING, "Error in forEachClient", e);
		}
	}

	protected void scheduleClient(Client cl) {
		scheduleClient(cl, cl.isAdmin() ? 500 : 25);
	}

	protected void scheduleClient(Client cl, long delay) {
		synchronized (pendingClients) {
			if (pendingClients.contains(cl))
				return;

			pendingClients.add(cl);
			executor.schedule(() -> sendData(cl), delay, TimeUnit.MILLISECONDS);
		}
	}

	private void sendData(Client cl) {
		synchronized (pendingClients) {
			pendingClients.remove(cl);
		}

		try {
			if (!cl.sendData())
				remove(cl);
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Error connecting to client - removing: " + e.getMessage());
			remove(cl);
		}
	}

	protected void sendProperties(int[] clientUIDs, final Properties p) {
		forEachClient(getClients(clientUIDs), cl -> cl.writeProperties(p));
	}

	protected int[] readClients(JsonObject obj) {
		Object[] children = obj.getArray("clients");

		int[] cl = new int[children.length];
		for (int i = 0; i < children.length; i++)
			cl[i] = Integer.parseUnsignedInt((String) children[i], 16);

		return cl;
	}

	public void setProperty(int[] clientUIDs, String key, String value) {
		Trace.trace(Trace.USER, "Setting property: " + key + "=" + value);
		Properties p = new Properties();
		p.put(key, value);
		sendProperties(clientUIDs, p);
	}

	public void restart(int[] clientUIDs) {
		Trace.trace(Trace.USER, "Restarting clients");
		forEachClient(getClients(clientUIDs), new ClientRun() {
			@Override
			public void run(Client cl) throws IOException {
				cl.writeRestart();
			}
		});
	}

	public void stop(int[] clientUIDs) {
		Trace.trace(Trace.USER, "Stopping clients");
		forEachClient(getClients(clientUIDs), new ClientRun() {
			@Override
			public void run(Client cl) throws IOException {
				cl.writeStop();
			}
		});
	}

	protected void handleProperties(JsonObject obj) {
		int[] cl = readClients(obj);
		Properties p = new Properties();
		Object children = obj.get("props");
		if (children != null) {
			JsonObject jo = (JsonObject) children;
			for (String key : jo.props.keySet()) {
				p.put(key, jo.getString(key));
			}
		}

		if (p.size() == 0) {
			// clear the properties
			for (int id : cl) {
				Trace.trace(Trace.INFO, "Clearing preferences for " + Arrays.toString(cl));

				Preferences prefs = getPreferences().node("uid" + id);

				try {
					prefs.clear();
					prefs.flush();
				} catch (Exception e) {
					// ignore
				}
			}
			return;
		}

		// remember some properties
		try {
			for (int id : cl) {
				boolean changed = false;
				Preferences prefs = getPreferences().node("uid" + id);

				for (Object s : p.keySet()) {
					String key = s.toString();
					String value = p.getProperty(key);
					if (!"control".equals(key) && !key.startsWith(DEFAULT_PREFIX) && !key.startsWith(TIME_PREFIX)) {
						changed = true;
						if (value == null || value.isEmpty())
							prefs.remove(key);
						else
							prefs.put(key, value);
					}
				}
				if (changed) {
					try {
						prefs.flush();
					} catch (Exception e) {
						Trace.trace(Trace.ERROR, "Error storing client properties", e);
					}
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error storing properties", e);
		}

		// and default properties
		try {
			boolean defChanged = false;
			Preferences defPrefs = getPreferences().node("default");
			List<String> remove = new ArrayList<>();

			for (Object s : p.keySet()) {
				String key = s.toString();
				String value = p.getProperty(key);
				if (key.startsWith(DEFAULT_PREFIX)) {
					remove.add(key);
					key = key.substring(DEFAULT_PREFIX.length());
					defChanged = true;
					if (value == null || value.isEmpty())
						defPrefs.remove(key);
					else
						defPrefs.put(key, value);
				}
			}
			if (defChanged) {
				try {
					defPrefs.flush();
				} catch (Exception e) {
					// ignore
				}
				for (String key : remove) {
					p.remove(key);
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error storing default properties", e);
		}

		// and timed properties
		try {
			for (int id : cl) {
				boolean timeChanged = false;
				Preferences timePrefs = getPreferences().node("time" + id);
				List<String> remove = new ArrayList<>();

				for (Object s : p.keySet()) {
					String key = s.toString();
					String value = p.getProperty(key);
					if (key.startsWith(TIME_PREFIX)) {
						remove.add(key);

						TimedEvent event = new TimedEvent();
						event.contestId = getClient(cl[0]).getContestId();
						key = key.substring(TIME_PREFIX.length());
						event.key = key;
						event.value = value;
						event.ids = cl;

						IContest c = CDSConfig.getContest(event.contestId).getContest();

						// parse time
						int index = key.indexOf(":");
						int time = 0;
						if (index > 0) {
							String timeStr = key.substring(0, index);
							if (timeStr.startsWith("+")) {
								time = Integer.parseInt(timeStr.substring(1));

								Long start = c.getStartTime();
								if (start != null)
									time = time + (int) (start - System.currentTimeMillis());
								else
									Trace.trace(Trace.WARNING, "Attempt to set relative time on unscheduled contest");
							} else if (timeStr.endsWith("e")) {
								time = Integer.parseInt(timeStr.substring(0, timeStr.length() - 1));
								time = c.getDuration() - time;
							} else
								time = Integer.parseInt(timeStr);
							key = key.substring(index + 1);
							event.key = key;
						}
						timeChanged = true;
						if (value == null || value.isEmpty())
							timePrefs.remove(key);
						else
							timePrefs.put(key, value);
						Trace.trace(Trace.INFO, "Timed presentation: @" + time + " do " + key + " = " + value);

						event.contestTimeMs = time;

						events.add(event);

						try {
							scheduleCallback(c, time);
						} catch (Exception e) {
							Trace.trace(Trace.WARNING, "Could not schedule callback for timed presentation");
						}
					}
				}
				if (timeChanged) {
					try {
						timePrefs.flush();
					} catch (Exception e) {
						// ignore
					}
					for (String key : remove) {
						p.remove(key);
					}
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error storing default properties", e);
		}

		if (!p.isEmpty())
			sendProperties(cl, p);
	}

	protected void handleThumbnail(Client c, JsonObject obj, String message) {
		List<Client> cadmins = adminMap.get(c);
		forEachClient(cadmins, cl -> cl.writeThumbnail(c.getUID(), message));
	}

	protected void handleInfo(Client c, JsonObject obj, String message) {
		c.storeInfo(obj);

		List<Client> cadmins = adminMap.get(c);
		forEachClient(cadmins, cl -> cl.writeInfo(c.getUID(), message));
	}

	protected void handleClientInfo(Client c, JsonObject obj, String message) {
		c.storeClientInfo(obj);
		updateClients(c);
	}

	protected void handlePresentationList(int sourceUID) {
		Trace.trace(Trace.USER, "Sending presentation lists");

		File f = PresentationCache.getPresentationCacheAdmin();
		if (f != null) {
			forEachClient(getClients(new int[] { sourceUID }), new ClientRun() {
				@Override
				public void run(Client cl) throws IOException {
					cl.writePresentationList(f);
				}
			});
		}
	}

	protected void handleRequestLog(String message, JsonObject obj) {
		int[] clientUIDs = readClients(obj);
		forEachClient(getClients(clientUIDs), new ClientRun() {
			@Override
			public void run(Client cl) throws IOException {
				cl.writeLogRequest(message);
			}
		});
	}

	protected void handleLog(String message, JsonObject obj) {
		int requestUID = getUID(obj, "target");
		forClient(getClient(requestUID), new ClientRun() {
			@Override
			public void run(Client cl) throws IOException {
				cl.writeLog(message);
			}
		});
	}

	protected void handleRequestSnapshot(String message, JsonObject obj) {
		int[] clientUIDs = readClients(obj);
		forEachClient(getClients(clientUIDs), new ClientRun() {
			@Override
			public void run(Client cl) throws IOException {
				cl.writeSnapshotRequest(message);
			}
		});
	}

	protected void handleSnapshot(String message, JsonObject obj) {
		int requestUID = getUID(obj, "target");
		forClient(getClient(requestUID), new ClientRun() {
			@Override
			public void run(Client cl) throws IOException {
				cl.writeSnapshot(message);
			}
		});
	}

	protected void handleStop(JsonObject obj) {
		int[] clientUIDs = readClients(obj);

		forEachClient(getClients(clientUIDs), new ClientRun() {
			@Override
			public void run(Client cl) throws IOException {
				cl.writeStop();
			}
		});
	}

	protected void handleRestart(JsonObject obj) {
		int[] clientUIDs = readClients(obj);

		forEachClient(getClients(clientUIDs), new ClientRun() {
			@Override
			public void run(Client cl) throws IOException {
				cl.writeRestart();
			}
		});
	}

	public void setExecutor(ScheduledExecutorService executor) {
		this.executor = executor;

		executor.scheduleWithFixedDelay(() -> forEachClient(clients, cl -> cl.writePing()), 30L, 30L, TimeUnit.SECONDS);
	}

	public void scheduleCallback(IContest c, int contestTimeMs) {
		// Future: schedule based on next callback
		/*// find next event
		int nextEventTime = Integer.MAX_VALUE;
		for (TimedEvent event : events)
			nextEventTime = (int) Math.min(event.contestTimeMs, nextEventTime);
		
		// leave if there are no scheduled events
		if (nextEventTime == Integer.MAX_VALUE)
			return;*/

		// determine how long that is from now
		Long startTime = c.getStartTime();
		if (startTime == null || startTime < 0)
			return;

		double contestTime = System.currentTimeMillis() - startTime;
		long dt = (long) (contestTimeMs / c.getTimeMultiplier() - contestTime);

		final int nextEventTimeMs = contestTimeMs;
		Trace.trace(Trace.INFO, "Callback scheduled in " + (dt / 1000) + " seconds");
		executor.schedule(() -> onTime(nextEventTimeMs), dt / 1000, TimeUnit.SECONDS);
	}

	protected void onTime(int contestTimeMs) {
		List<TimedEvent> remove = new ArrayList<>();
		for (TimedEvent event : events) {
			if (event.contestTimeMs <= contestTimeMs) {
				Trace.trace(Trace.INFO, "Timed event! " + contestTimeMs + " > " + event.contestTimeMs);
				remove.add(event);
				Properties p = new Properties();
				p.put(event.key, event.value);
				forEachClient(getClients(event.ids), new ClientRun() {
					@Override
					public void run(Client cl) throws IOException {
						cl.writeProperties(p);
					}
				});
			}
		}
		if (!remove.isEmpty())
			events.removeAll(remove);
	}
}