package org.icpc.tools.cds;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.icpc.tools.cds.video.VideoAggregator;
import org.icpc.tools.cds.video.VideoAggregator.ConnectionMode;
import org.icpc.tools.contest.Trace;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class CDSConfig {
	private static final Object INIT_LOCK = new Object();
	private static CDSConfig instance;
	private static File folder;

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
	private UserVideo[] video;
	private Domain[] domains;

	private CDSConfig(Element e) {
		Element[] contests3 = getChildren(e, "contest");
		if (contests3 != null) {
			contests = new ConfiguredContest[contests3.length];
			for (int i = 0; i < contests3.length; i++)
				contests[i] = new ConfiguredContest(contests3[i]);
		} else
			contests = new ConfiguredContest[0];

		listConfiguration();

		// initialize
		for (int i = 0; i < contests.length; i++)
			contests[i].init();

		Element[] children = getChildren(e, "video");
		if (children != null) {
			video = new UserVideo[children.length];
			for (int i = 0; i < children.length; i++) {
				video[i] = new UserVideo(children[i]);
				String name = video[i].getName();
				String url = video[i].getURL();
				ConnectionMode mode = VideoAggregator.getConnectionMode(video[i].getMode());
				VideoAggregator.getInstance().addReservation(name, url, mode, i);
			}
		}

		children = getChildren(e, "domain");
		if (children != null) {
			domains = new Domain[children.length];
			for (int i = 0; i < children.length; i++) {
				domains[i] = new Domain(children[i]);
			}
		}
	}

	public Domain[] getDomains() {
		return domains;
	}

	protected void listConfiguration() {
		Trace.trace(Trace.USER, "Configured contests:");
		for (ConfiguredContest cc : contests)
			Trace.trace(Trace.USER, "   " + cc);
	}

	private static CDSConfig createReadRoot(InputStream in) {
		Document document = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder parser = factory.newDocumentBuilder();
			document = parser.parse(new InputSource(in));
			Node node = document.getFirstChild();
			if (node instanceof Element)
				return new CDSConfig((Element) node);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error reading CDS config", e);
		} finally {
			try {
				in.close();
			} catch (Exception e) {
				// ignore
			}
		}
		return new CDSConfig(null);
	}

	public static CDSConfig getInstance() {
		if (instance != null)
			return instance;

		synchronized (INIT_LOCK) {
			if (instance != null)
				return instance;

			InputStream in = null;
			Exception ce = null;
			try {
				Context context = new InitialContext();
				String folderStr = (String) context.lookup("java:/comp/env/icpc.cds.config");
				folder = new File(folderStr);
				Trace.trace(Trace.USER, "Loading CDS configuration from: " + folder.getAbsolutePath());
				in = new BufferedInputStream(new FileInputStream(new File(folder, "cdsConfig.xml")));
				instance = CDSConfig.createReadRoot(in);
			} catch (Exception e) {
				ce = e;
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (Exception e) {
					// ignore
				}
			}

			if (in == null) {
				try {
					Context context = new InitialContext();
					String folderStr = (String) context.lookup("icpc.cds.config");
					folder = new File(folderStr);
					Trace.trace(Trace.USER, "Loading CDS configuration from: " + folder.getAbsolutePath());
					in = new BufferedInputStream(new FileInputStream(new File(folder, "cdsConfig.xml")));
					instance = CDSConfig.createReadRoot(in);
				} catch (Exception e) {
					System.err.println("Could not load CDS configuration: " + ce.getMessage() + " / " + e.getMessage());
				} finally {
					try {
						if (in != null)
							in.close();
					} catch (Exception e) {
						// ignore
					}
				}
			}
		}

		return instance;
	}

	public static File getFolder() {
		return folder;
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
				if (n.getNodeName().equals(childName))
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

	static String getString(Element element, String key) {
		Attr attr = element.getAttributeNode(key);
		if (attr == null)
			return null;
		return attr.getValue();
	}
}