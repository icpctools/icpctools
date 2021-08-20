package org.icpc.tools.cds;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.icpc.tools.cds.service.ExecutorListener;
import org.icpc.tools.cds.video.VideoAggregator;
import org.icpc.tools.cds.video.VideoAggregator.ConnectionMode;
import org.icpc.tools.cds.video.VideoStream.StreamType;
import org.icpc.tools.contest.Trace;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class CDSConfig {
	private static final Object INIT_LOCK = new Object();
	private static CDSConfig instance;

	public static class UserVideo {
		private Element video;

		protected UserVideo(Element e) {
			video = e;
		}

		public String getURL() {
			return getString(video, "url");
		}

		public String getName() {
			return getString(video, "name");
		}

		public String getMode() {
			return getString(video, "mode");
		}

		@Override
		public String toString() {
			return "Video [" + getName() + ":" + getURL() + "]";
		}
	}

	public static class Domain {
		private String[] users = null;

		protected Domain(Element e) {
			Element[] children = CDSConfig.getChildren(e, "user");
			int num = children.length;
			users = new String[num];

			for (int i = 0; i < num; i++)
				users[i] = children[i].getAttribute("name");
		}

		public String[] getUsers() {
			return users;
		}

		@Override
		public String toString() {
			return "Domain [" + getUsers().length + " users]";
		}
	}

	public static class TeamUser {
		private String name = null;
		private String teamId = null;
		private String password = null;

		protected TeamUser(Element e) {
			name = getString(e, "name");
			teamId = getString(e, "teamId");
			password = getString(e, "password");
		}

		@Override
		public String toString() {
			return "Team user [" + name + "]";
		}
	}

	public static class Host {
		private String teamId = null;
		private String host = null;

		protected Host(Element e) {
			teamId = getString(e, "teamId");
			host = getString(e, "host");
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Host [");
			if (teamId != null)
				sb.append(teamId + " / ");
			sb.append(host + "]");
			return sb.toString();
		}
	}

	private ConfiguredContest[] contests;
	private long[] contestHashes;
	private Domain[] domains;
	private TeamUser[] teamUsers;
	private Host[] hosts;
	private File file;
	private long lastModified;

	private CDSConfig(File file) {
		this.file = file;

		lastModified = file.lastModified();
		try {
			Element e = readElement(file);
			loadContests(e);
			loadOtherConfig(e);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not read from CDS config! No contests loaded", e);
		}

		ExecutorListener.getExecutor().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (lastModified == file.lastModified())
					return;

				Trace.trace(Trace.USER, "----- CDS config change detected -----");
				lastModified = file.lastModified();

				try {
					Element el = readElement(file);
					loadContests(el);
					loadOtherConfig(el);
				} catch (Exception e) {
					Trace.trace(Trace.USER, "Error reading CDS config, changes will be ignored until the file is fixed");
				}
			}
		}, 15, 5, TimeUnit.SECONDS);
	}

	private static void appendString(StringBuffer sb, Element el) {
		NamedNodeMap nnm = el.getAttributes();
		boolean first = true;
		for (int i = 0; i < nnm.getLength(); i++) {
			if (!first)
				sb.append(",");
			else
				first = false;
			Node n = nnm.item(i);
			sb.append(n.getNodeName() + "=" + n.getNodeValue());
		}

		Element[] children = getChildren(el, null);
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				if (!first)
					sb.append(",");
				else
					first = false;
				sb.append(children[i].getNodeName());
				sb.append("[");
				appendString(sb, children[i]);
				sb.append("]");
			}
		}
	}

	private static long getHash(Element e) {
		StringBuffer sb = new StringBuffer();
		appendString(sb, e);

		long hash = 0;
		for (int i = 0; i < sb.length(); i++)
			hash = hash * 31 + sb.charAt(i);

		return hash;
	}

	private ConfiguredContest getContestByHash(long hash) {
		if (contestHashes == null)
			return null;

		for (int i = 0; i < contestHashes.length; i++)
			if (contestHashes[i] == hash)
				return contests[i];

		return null;
	}

	private void loadContests(Element e) {
		Element[] children = getChildren(e, "contest");

		ConfiguredContest[] oldContests = contests;
		long[] oldHashes = contestHashes;
		if (children != null) {
			ConfiguredContest[] temp = new ConfiguredContest[children.length];
			long[] tempHash = new long[children.length];
			for (int i = 0; i < children.length; i++) {
				tempHash[i] = getHash(children[i]);

				ConfiguredContest cc = getContestByHash(tempHash[i]);
				if (cc == null)
					cc = new ConfiguredContest(children[i]);
				temp[i] = cc;
			}

			contests = temp;
			contestHashes = tempHash;
		} else {
			contests = new ConfiguredContest[0];
			contestHashes = new long[0];
		}

		// clean up any removed or changed projects
		if (oldContests != null) {
			for (int i = 0; i < oldContests.length; i++) {
				boolean stillInUse = false;
				for (int j = 0; j < contestHashes.length; j++) {
					if (oldHashes[i] == contestHashes[j]) {
						stillInUse = true;
						break;
					}
				}
				if (!stillInUse)
					oldContests[i].close();
			}
		}

		Trace.trace(Trace.USER, "Configured contests:");
		for (ConfiguredContest cc : contests)
			Trace.trace(Trace.USER, "   " + cc);
	}

	private void loadOtherConfig(Element e) {
		Element[] children = getChildren(e, "video");
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				UserVideo video = new UserVideo(children[i]);
				ConnectionMode mode = VideoAggregator.getConnectionMode(video.getMode());
				VideoAggregator.getInstance().addReservation(video.getName(), video.getURL(), StreamType.OTHER, mode);
			}
		}

		children = getChildren(e, "domain");
		if (children != null) {
			Domain[] temp = new Domain[children.length];
			for (int i = 0; i < children.length; i++) {
				temp[i] = new Domain(children[i]);
			}
			domains = temp;
		}

		Element teamElement = getChild(e, "teamUsers");
		if (teamElement != null) {
			children = CDSConfig.getChildren(teamElement, "user");
			int num = children.length;

			TeamUser[] temp = new TeamUser[num];
			for (int i = 0; i < num; i++) {
				temp[i] = new TeamUser(children[i]);
			}
			teamUsers = temp;

			String users = teamElement.getAttribute("users");
			if (users == null || users.isEmpty())
				users = "../users.xml";
			FileInputStream in = null;
			try {
				in = new FileInputStream(new File(file.getParentFile(), users));
				parseBasicRegistry(in);
			} catch (Exception ex) {
				Trace.trace(Trace.ERROR, "Could not load team password info", ex);
			} finally {
				if (in != null)
					try {
						in.close();
					} catch (Exception ex) {
						// ignore
					}
			}
		}

		Element hostElement = getChild(e, "hosts");
		if (hostElement != null) {
			children = CDSConfig.getChildren(hostElement, "host");
			int num = children.length;

			Host[] temp = new Host[num];
			for (int i = 0; i < num; i++) {
				temp[i] = new Host(children[i]);
			}
			hosts = temp;
		}
	}

	private void parseBasicRegistry(InputStream in) throws Exception {
		if (in == null)
			return;

		SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
		try {
			sp.parse(in, new DefaultHandler() {
				protected int level;

				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) {
					if (level == 0) {
						if (!qName.equals("server"))
							Trace.trace(Trace.ERROR, "Invalid format for basic registry");
					} else if (level == 2) {
						if ("user".equals(qName)) {
							String name = attributes.getValue("name");
							for (TeamUser user : teamUsers) {
								if (name.equals(user.name))
									user.password = attributes.getValue("password");
							}
						}
					}
					level++;
				}

				@Override
				public void endElement(String uri, String localName, String qName) {
					level--;
				}
			});
		} catch (SAXParseException e) {
			Trace.trace(Trace.ERROR, "Could not read basic registry:" + e.getLineNumber(), e);
			throw new IOException("Could not read basic registry");
		}
	}

	public Domain[] getDomains() {
		return domains;
	}

	public TeamUser[] getTeamLogins() {
		return teamUsers;
	}

	/**
	 * Returns true if there are any user logins for the given team id, and false otherwise.
	 *
	 * @param team id
	 * @return true if there is a login, and false otherwise
	 */
	public boolean hasLoginForId(String teamId) {
		if (teamUsers == null || teamId == null)
			return false;

		for (TeamUser user : teamUsers) {
			if (teamId.equals(user.teamId))
				return true;
		}
		return false;
	}

	/**
	 * Converts from team user name to team id, e.g. "team57" to "57".
	 *
	 * @param userName
	 * @return the team's id, or null if not found
	 */
	public String getTeamIdFromUser(String userName) {
		if (teamUsers == null || userName == null)
			return null;

		for (TeamUser user : teamUsers) {
			if (userName.equals(user.name))
				return user.teamId;
		}
		return null;
	}

	/**
	 * Converts from team id to host name or IP, e.g. "43" to "10.0.0.43".
	 *
	 * @param team id
	 * @return a list of hosts, or null if not found
	 */
	public List<String> getHostsForTeamId(String teamId) {
		if (hosts == null || teamId == null)
			return null;

		List<String> list = new ArrayList<String>();
		for (Host host : hosts) {
			if (host.teamId != null && !host.teamId.equals(teamId))
				continue;
			list.add(host.host.replace("{0}", teamId));
		}
		return list;
	}

	/**
	 * Converts from team host name or IP to team id, e.g. "10.0.0.43" to "43".
	 *
	 * @param host
	 * @return the team's id, or null if not found
	 */
	public String getTeamIdFromHost(String teamHost) {
		if (hosts == null || teamHost == null)
			return null;

		for (Host host : hosts) {
			// simple case is direct mapping with no substitution
			if (host.teamId != null && host.host.equals(teamHost))
				return host.teamId;

			// harder case, match to find the substitution
			Pattern p = Pattern.compile(host.host.replace(".", "\\.").replace("{0}", ".*"));
			Matcher m = p.matcher(teamHost);
			if (!m.matches())
				continue;

			return m.group(1);
		}
		return null;
	}

	/**
	 * Converts from team id to team user name, e.g. "57" to "team57".
	 *
	 * @param team id
	 * @return the team's user name, or null if not found
	 */
	public String getTeamUserName(String id) {
		if (teamUsers == null || id == null)
			return null;

		for (TeamUser user : teamUsers) {
			if (id.equals(user.teamId))
				return user.name;
		}
		return null;
	}

	/**
	 * Converts from team id to team password, e.g. "57" to "vej78e!".
	 *
	 * @param team id
	 * @return the team's password, or null if not found
	 */
	public String getTeamPassword(String id) {
		if (teamUsers == null || id == null)
			return null;

		for (TeamUser user : teamUsers) {
			if (id.equals(user.teamId))
				return user.password;
		}
		return null;
	}

	private static Element readElement(File file) throws Exception {
		Document document = null;
		try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder parser = factory.newDocumentBuilder();
			// clear default error handler - exception will be caught below
			parser.setErrorHandler(null);
			document = parser.parse(new InputSource(in));
			Node node = document.getFirstChild();
			if (node instanceof Element)
				return (Element) node;
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Error reading cdsConfig.xml: " + e.getMessage());
			throw e;
		}
		return null;
	}

	private static CDSConfig createInstance() {
		Exception ce = null;
		try {
			Context context = new InitialContext();
			String folderStr = (String) context.lookup("java:/comp/env/icpc.cds.config");
			File file = new File(folderStr, "cdsConfig.xml");
			Trace.trace(Trace.USER, "Loading CDS configuration from: " + file.getAbsolutePath());
			return new CDSConfig(file);
		} catch (Exception e) {
			ce = e;
		}

		try {
			Context context = new InitialContext();
			String folderStr = (String) context.lookup("icpc.cds.config");
			File file = new File(folderStr, "cdsConfig.xml");
			Trace.trace(Trace.USER, "Loading CDS configuration from: " + file.getAbsolutePath());
			return new CDSConfig(file);
		} catch (Exception e) {
			System.err.println("Could not load CDS configuration: " + ce.getMessage() + " / " + e.getMessage());
		}
		return null;
	}

	public static CDSConfig getInstance() {
		if (instance != null)
			return instance;

		synchronized (INIT_LOCK) {
			if (instance != null)
				return instance;

			instance = createInstance();
		}

		return instance;
	}

	public static File getFolder() {
		return getInstance().file.getParentFile();
	}

	public static ConfiguredContest[] getContests() {
		return getInstance().contests;
	}

	public static ConfiguredContest getContest(String id) {
		if (id == null)
			return null;

		for (ConfiguredContest cc : getInstance().contests) {
			if (id.equals(cc.getId()))
				return cc;
		}
		return null;
	}

	protected static Element getChild(Element element, String childName) {
		if (element == null)
			return null;

		NodeList nodes = element.getChildNodes();
		int size = nodes.getLength();
		for (int i = 0; i < size; i++) {
			Node n = nodes.item(i);
			if (n instanceof Element) {
				if (n.getNodeName().equals(childName))
					return (Element) n;
			}
		}

		return null;
	}

	protected static Element[] getChildren(Element element, String childName) {
		if (element == null)
			return null;

		List<Element> list = new ArrayList<>();
		NodeList nodes = element.getChildNodes();
		int size = nodes.getLength();
		for (int i = 0; i < size; i++) {
			Node n = nodes.item(i);
			if (n instanceof Element) {
				if (childName == null || n.getNodeName().equals(childName))
					list.add((Element) n);
			}
		}

		return list.toArray(new Element[0]);
	}

	protected static Boolean getBoolean(Element element, String key) {
		try {
			return Boolean.parseBoolean(getString(element, key));
		} catch (Exception e) {
			return null;
		}
	}

	protected static Integer getInteger(Element element, String key) {
		try {
			return Integer.parseInt(getString(element, key));
		} catch (Exception e) {
			return null;
		}
	}

	protected static Double getDouble(Element element, String key) {
		try {
			return Double.parseDouble(getString(element, key));
		} catch (Exception e) {
			return null;
		}
	}

	protected static String getString(Element element, String key) {
		Attr attr = element.getAttributeNode(key);
		if (attr == null)
			return null;
		return attr.getValue();
	}
}