package org.icpc.tools.cds.presentations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class PresentationServer {
	private static final boolean DEBUG_INPUT = true;
	protected static final boolean DEBUG_OUTPUT = true;

	private static final String PREF_ID = "org.icpc.tools.cds";
	private static final String PRES = "presentation";
	private static final String DEFAULT_PREFIX = "default:";

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

	protected void onMessage(Session s, String message) throws IOException {
		Client[] cl = clients.toArray(new Client[clients.size()]);

		Client c = null;
		for (Client cli : cl) {
			if (s.equals(cli.getSession())) {
				c = cli;
			}
		}

		if (c == null)
			throw new IOException("Client " + s.getId() + " does not exist");

		if (DEBUG_INPUT) {
			String st = message;
			if (st.length() > 140)
				st = st.substring(0, 140) + "...";
			Trace.trace(Trace.USER, "> " + st + " from " + c.getUID());
		}

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
					if (c.handlePing())
						scheduleClient(c);
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
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Client connection failure", e);
		}
		return c;
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
		public void run(Client c) throws IOException;
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

	private void forEachClient(List<Client> targetClients, final ClientRun r) {
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
		synchronized (pendingClients) {
			if (pendingClients.contains(cl))
				return;

			pendingClients.add(cl);
			executor.schedule(() -> sendData(cl), 500, TimeUnit.MILLISECONDS);
		}
	}

	protected void sendData(Client cl) {
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
		for (int i = 0; i < children.length; i++) {
			try {
				cl[i] = Integer.parseInt((String) children[i]);
			} catch (Exception e) {
				// ignore
			}
		}

		return cl;
	}

	public void setProperty(int[] clientUIDs, String key, String value) {
		Trace.trace(Trace.USER, "Setting property: " + key + "=" + value);
		Properties p = new Properties();
		p.put(key, value);
		sendProperties(clientUIDs, p);
	}

	protected void handleProperties(JsonObject obj) {
		int[] cl = readClients(obj);
		int size = obj.getInt("num");
		if (size == 0)
			return;

		if (size == -1) {
			// clear the properties
			for (int id : cl) {
				Trace.trace(Trace.INFO, "Clearing preferences for " + cl);

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

		Properties p = new Properties();
		for (int i = 0; i < size; i++) {
			String key = obj.getString("key" + i);
			String value = obj.getString("value" + i);
			p.put(key, value);
		}

		// remember some properties
		try {
			for (int id : cl) {
				boolean changed = false;
				Preferences prefs = getPreferences().node("uid" + id);

				for (Object s : p.keySet()) {
					String key = s.toString();
					String value = p.getProperty(key);
					if (!"control".equals(key) && !key.startsWith(DEFAULT_PREFIX)) {
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
		int requestUID = obj.getInt("target");
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
		int requestUID = obj.getInt("target");
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

		executor.scheduleWithFixedDelay(() -> forEachClient(clients, cl -> cl.writePing()), 15L, 15L, TimeUnit.SECONDS);
	}
}