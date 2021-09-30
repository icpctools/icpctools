package org.icpc.tools.presentation.contest.internal;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.icpc.tools.client.core.BasicClient;
import org.icpc.tools.client.core.IPropertyListener;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.internal.NetworkUtil;
import org.icpc.tools.presentation.core.DisplayConfig;
import org.icpc.tools.presentation.core.IPresentationHandler;
import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.Transition;
import org.icpc.tools.presentation.core.internal.PresentationWindowImpl;

public class PresentationClient extends BasicClient {
	private static final String HIDDEN = "hidden";
	private static final String DISPLAY = "display";
	private static final String MULTI_DISPLAY = "multi_display";
	private static final String FPS = "fps";
	private static final String PRESENTATION = "presentation";

	protected ThreadPoolExecutor executor;

	protected IPresentationHandler window;

	protected long graphicsChecksum;
	protected int[] graphicsInfo;
	protected boolean sendingInfoUpdate;
	protected boolean sendingInfo;

	protected PresentationClient(RESTContestSource source, String clientId, int uid, String role) {
		this(source, clientId, uid, role, "presentation");
	}

	public PresentationClient(RESTContestSource source, String clientId, int uid, String role, String type) {
		super(source, clientId, uid, role, type);
		executor = new ThreadPoolExecutor(2, 4, 20L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(5),
				new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r, "Presentation executor");
						t.setPriority(Thread.NORM_PRIORITY);
						t.setDaemon(true);
						return t;
					}
				});
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				executor.shutdown();
			}
		});

		init();
	}

	public PresentationClient(String clientId, String role, RESTContestSource source) {
		this(clientId, role, source, "presentation");
	}

	public PresentationClient(String clientId, String role, RESTContestSource source, String type) {
		this(source, clientId, 0, role, type);
		String s = clientId + NetworkUtil.getLocalAddress();
		setUID(s.hashCode());
	}

	private void init() {
		addListener(new IPropertyListener() {
			@Override
			public void propertyUpdated(String key, String value) {
				handleProperty(key, value);
			}
		});
	}

	protected void handleProperty(String key, String value) {
		Trace.trace(Trace.USER, key + ": " + value);
		if ("displayConfig".equals(key)) {
			if (window == null || value == null)
				return;

			String display = value;
			String multiDisplay = null;
			int ind = value.indexOf(",");
			if (ind > 0) {
				display = value.substring(0, ind);
				multiDisplay = value.substring(ind + 1);
			}
			window.setDisplayConfig(new DisplayConfig(display, multiDisplay));
			writeInfo();
		}
		if ("hidden".equals(key)) {
			if (window == null || value == null)
				return;

			window.setHidden("true".equals(value));
			writeInfo();
		}
		if ("displayRect".equals(key)) {
			Rectangle r = null;
			if (value != null && !value.isEmpty()) {
				try {
					StringTokenizer st = new StringTokenizer(value, ",");
					r = new Rectangle(Integer.parseInt(st.nextToken().trim()), Integer.parseInt(st.nextToken().trim()),
							Integer.parseInt(st.nextToken().trim()), Integer.parseInt(st.nextToken().trim()));
				} catch (Exception e) {
					Trace.trace(Trace.WARNING, "Could not set display rect (" + value + ")");
				}
			}
			if (window instanceof PresentationWindowImpl)
				((PresentationWindowImpl) window).setDisplayRect(r);
			writeInfo();
		} else if ("presentation".equals(key)) {
			setPresentations(value);
		} else if (key != null && value != null && window != null) {
			String a = key;
			String b = value;
			int ind = value.indexOf("|*|");
			if (ind > 0) {
				a = value.substring(0, ind);
				b = value.substring(ind + 3);
			}
			window.setProperty(a, b);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends Object> T loadClass(String className, Class<T> type) {
		Trace.trace(Trace.INFO, "Attempting to load class: " + className);

		try {
			ClassLoader classLoader = getClass().getClassLoader();
			Class<?> c = classLoader.loadClass(className);
			if (c != null) {
				T newObject = (T) c.getDeclaredConstructor().newInstance();

				if (newObject != null) {
					Trace.trace(Trace.INFO, "Class loaded ok");
					return newObject;
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Class load failed: " + className + " " + e.getMessage());
			return null;
		}

		Trace.trace(Trace.ERROR, "Class load failed: " + className);
		return null;
	}

	private Presentation loadPresentation(String className) {
		return loadClass(className, Presentation.class);
	}

	private Transition loadTransition(String className) {
		return loadClass(className, Transition.class);
	}

	protected void setPresentations(String presentations) {
		StringTokenizer st = new StringTokenizer(presentations, "|");

		final List<Presentation> pres = new ArrayList<>();
		final List<Transition> trans = new ArrayList<>();

		String timeStr = st.nextToken();
		long timeVal = 0;
		try {
			timeVal = Long.parseLong(timeStr);
		} catch (Exception e) {
			// ignore
		}
		final long time = timeVal;

		while (st.hasMoreTokens()) {
			String s = st.nextToken();
			String value = null;
			int ind = s.indexOf(":");
			if (ind > 0) {
				value = s.substring(ind + 1);
				s = s.substring(0, ind);
			}

			if (s.startsWith("t/")) {
				Transition t = loadTransition(s.substring(2));
				if (t != null) {
					if (value != null)
						t.setProperty(value);
					trans.add(t);
				}
			} else {
				Presentation p = loadPresentation(s);
				if (p != null) {
					if (value != null)
						p.setProperty(value);
					pres.add(p);
				}
			}
		}

		try {
			if (window != null)
				window.setPresentations(time, pres.toArray(new Presentation[0]), trans.toArray(new Transition[0]));
		} catch (Throwable t) {
			Trace.trace(Trace.ERROR, "Error setting new presentation", t);
		}
	}

	protected int[] getGraphicsInfo() {
		GraphicsDevice[] gds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

		int count = 0;
		int checksum = 0;
		for (GraphicsDevice gd : gds) {
			if (gd.getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
				count++;
				DisplayMode dm = gd.getDisplayMode();
				checksum += dm.getWidth() + dm.getHeight() + dm.getBitDepth();
			}
		}

		if (checksum == graphicsChecksum)
			return graphicsInfo;

		int[] temp = new int[count * 3];

		int i = 0;
		for (GraphicsDevice gd : gds) {
			if (gd.getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
				DisplayMode dm = gd.getDisplayMode();
				temp[i * 3 + 0] = dm.getWidth();
				temp[i * 3 + 1] = dm.getHeight();
				temp[i * 3 + 2] = dm.getRefreshRate();
				i++;
			}
		}

		graphicsInfo = temp;
		graphicsChecksum = checksum;
		return graphicsInfo;
	}

	@Override
	protected void addBasicClientInfo(JSONEncoder je) {
		int[] temp = getGraphicsInfo();
		int num = temp.length / 3;
		je.openChildArray(DISPLAYS);
		for (int i = 0; i < num; i++) {
			je.open();
			je.encode(WIDTH, temp[i * 3]);
			je.encode(HEIGHT, temp[i * 3 + 1]);
			je.encode(REFRESH, temp[i * 3 + 2]);
			je.close();
		}
		je.closeArray();
	}

	protected void sendInfoPresentation() throws IOException {
		sendInfo(je -> {
			Dimension d = window.getPresentationSize();
			je.encode(WIDTH, d.width);
			je.encode(HEIGHT, d.height);
			je.encode(HIDDEN, window.isHidden());
			DisplayConfig displayConfig = window.getDisplayConfig();
			if (displayConfig != null) {
				je.encode(DISPLAY, displayConfig.getDisplay());
				String multi = displayConfig.getMultiDisplay();
				if (multi != null)
					je.encode(MULTI_DISPLAY, multi);
			}
		});
	}

	public void writeInfo() {
		// last thread still going, try next time
		if (sendingInfo)
			return;

		if (window == null)
			return;

		sendingInfo = true;

		execute(new Runnable() {
			@Override
			public void run() {
				try {
					sendInfoPresentation();
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error sending state", e);
				}
				sendingInfo = false;
			}

			@Override
			public String toString() {
				return "State task";
			}
		});
	}

	protected void execute(Runnable r) {
		if (executor.isShutdown())
			return;

		try {
			executor.execute(r);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not execute runnable: " + e.getMessage(), e);
		}
	}

	public void writeInfoUpdate(final BufferedImage image) {
		// last thread still going, try next time
		if (sendingInfoUpdate)
			return;

		sendingInfoUpdate = true;

		execute(new Runnable() {
			@Override
			public void run() {
				try {
					sendInfo(je -> {
						je.encode(PRESENTATION, window.getPresentationName());
						je.encode(FPS, window.getFPS());
						encodeImage(je, imageToBytes(image));
					});
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error sending status", e);
				}
				sendingInfoUpdate = false;
			}

			@Override
			public String toString() {
				return "Status task";
			}
		});
	}

	@Override
	protected void handleTime(long time) {
		super.handleTime(time);
		if (window != null && window instanceof PresentationWindowImpl)
			((PresentationWindowImpl) window).setZeroTimeMs(time);
	}

	protected void writeSnapshot(final BufferedImage image) {
		execute(new Runnable() {
			@Override
			public void run() {
				try {
					sendSnapshot(image);
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error sending image", e);
				}
			}

			@Override
			public String toString() {
				return "Snapshot task";
			}
		});
	}

	@Override
	protected void handleSnapshot(int clientUID) throws IOException {
		if (window != null) {
			BufferedImage img = window.createImage(1f);
			sendSnapshot(img, clientUID);
		}
	}

	@Override
	public String toString() {
		return "Presentation Client [" + getClientId() + "]";
	}
}