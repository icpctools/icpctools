package org.icpc.tools.cds;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.icpc.tools.cds.service.ExecutorListener;
import org.icpc.tools.cds.video.VideoAggregator;
import org.icpc.tools.cds.video.VideoAggregator.ConnectionMode;
import org.icpc.tools.contest.Trace;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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

	private ConfiguredContest[] contests;
	private long[] contestHashes;
	private Domain[] domains;
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
			Trace.trace(Trace.ERROR, "Could not read from CDS config! No contests loaded");
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

		// initialize
		for (int i = 0; i < contests.length; i++)
			contests[i].init();
	}

	private void loadOtherConfig(Element e) {
		Element[] children = getChildren(e, "video");
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				UserVideo video = new UserVideo(children[i]);
				ConnectionMode mode = VideoAggregator.getConnectionMode(video.getMode());
				VideoAggregator.getInstance().addReservation(video.getName(), video.getURL(), mode, i);
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
	}

	public Domain[] getDomains() {
		return domains;
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

		return list.toArray(new Element[list.size()]);
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