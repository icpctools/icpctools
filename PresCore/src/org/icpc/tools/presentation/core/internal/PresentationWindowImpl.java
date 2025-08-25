package org.icpc.tools.presentation.core.internal;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.LockSupport;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.presentation.core.DisplayConfig;
import org.icpc.tools.presentation.core.DisplayConfig.Mode;
import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.PresentationWindow;
import org.icpc.tools.presentation.core.RenderPerfTimer;
import org.icpc.tools.presentation.core.Transition;
import org.icpc.tools.presentation.core.Transition.TimeOverlap;

/**
 * Main presentation window.
 */
public class PresentationWindowImpl extends PresentationWindow {
	private static final long serialVersionUID = 1L;
	private static final long DEFAULT_REPEAT_TIME = 10000;
	private static final long PLAN_FADE_TIME = 800; // time to fade in/out when changing plan
	private static final long MAX_FPS = 1000_000_000L / (60L + 7); // limit to 60fps max with some
																						// margin

	private boolean lightMode;

	private static final NumberFormat nf = NumberFormat.getInstance(Locale.US);

	static {
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(1);
	}

	private static final int DEFAULT_THUMBNAIL_HEIGHT = 180;
	private static final long DEFAULT_THUMBNAIL_DELAY = 2_000_000_000L;

	static class PresentationSegment {
		protected long startTime;
		protected long endTime;
		protected Transition trans;
		protected Presentation p1;
		protected long p1time; // repeat time for p1
		protected Presentation p2;

		@Override
		public String toString() {
			return "Segment [" + startTime + " -> " + endTime + ", " + p1 + " > " + trans + " > " + p2 + ", " + p1time
					+ "]";
		}
	}

	private static String getPresentationKey(String className) {
		int ind = className.lastIndexOf(".");
		return "property[" + className.substring(ind + 1) + "|" + className.hashCode() + "]";
	}

	static class PresentationPlan {
		protected long startTime;
		protected long endTime = Long.MAX_VALUE;
		protected long totalRepeatTime;
		protected PresentationSegment[] segments;
		private Presentation[] presentations;
		private Transition[] transitions;
		private boolean immediate;

		public PresentationPlan(Presentation[] pres, Transition[] trans, boolean immediate2) {
			presentations = pres;
			transitions = trans;
			if (transitions == null)
				transitions = new Transition[0];

			this.immediate = immediate2;
			build();
		}

		private void updateEndTimes() {
			for (int i = 0; i < segments.length - 1; i++)
				segments[i].endTime = segments[i + 1].startTime;

			segments[segments.length - 1].endTime = totalRepeatTime;
		}

		public void setSize(Dimension d) {
			for (Presentation p : presentations)
				if (!p.getSize().equals(d))
					p.setSize(d);

			for (Transition t : transitions)
				t.setSize(d);
		}

		public void setProperty(String key, String value) {
			for (Presentation p : presentations) {
				if (key.equals(getPresentationKey(p.getClass().getName())))
					p.setProperty(value);
				else if ("BrandingPresentation".equals(p.getClass().getSimpleName()))
					p.setProperty(key + ":" + value);
			}

			if (isGlobalKey(key))
				for (Presentation p : presentations) {
					p.setProperty(key + ":" + value);
				}
		}

		public void dispose() {
			for (Presentation p : presentations)
				p.dispose();
		}

		protected void build() {
			if (immediate && segments != null)
				return;

			long time = 0;
			List<PresentationSegment> segs = new ArrayList<>();
			if (transitions == null || transitions.length == 0) {
				for (Presentation p : presentations) {
					PresentationSegment pp = new PresentationSegment();
					pp.startTime = time;
					pp.p1 = p;
					segs.add(pp);

					long repeat = p.getRepeat();
					if (repeat <= 0) {
						if (presentations.length == 1 || immediate)
							time += 1000L * 60 * 60 * 24 * 365 * 10; // 10 years
						else
							time += DEFAULT_REPEAT_TIME;
					} else
						time += repeat;
				}

				totalRepeatTime = time;
				segments = segs.toArray(new PresentationSegment[0]);
				updateEndTimes();
				return;
			}

			int curPres = 0;
			int curTrans = 0;
			Transition lastTrans = transitions[(presentations.length - 1) % transitions.length];
			TimeOverlap lastTo = lastTrans.getTimeOverlap();

			while (curPres < presentations.length) {
				Presentation pres = presentations[curPres];
				PresentationSegment pp = new PresentationSegment();
				pp.startTime = time;
				pp.p1 = pres;
				if (lastTo == TimeOverlap.MID)
					pp.p1time = -lastTrans.getLength() / 2;
				else if (lastTo == TimeOverlap.FULL)
					pp.p1time = -lastTrans.getLength();
				segs.add(pp);

				Transition trans = transitions[curTrans % transitions.length];
				TimeOverlap to = trans.getTimeOverlap();

				long duration = DEFAULT_REPEAT_TIME;
				long repeat = pres.getRepeat();
				if (repeat > 0)
					duration = repeat;
				time += duration + pp.p1time;

				if (to == TimeOverlap.MID)
					time -= trans.getLength() / 2;
				else if (to == TimeOverlap.FULL)
					time -= trans.getLength();

				PresentationSegment tp = new PresentationSegment();
				tp.startTime = time;
				tp.trans = trans;
				tp.p1 = pres;
				tp.p1time = duration;
				tp.p2 = presentations[(curPres + 1) % presentations.length];

				segs.add(tp);

				lastTo = to;
				time += lastTrans.getLength();
				curPres++;
				curTrans++;
			}

			totalRepeatTime = time;
			segments = segs.toArray(new PresentationSegment[0]);

			if (immediate) {
				long rTime = segments[1].startTime;
				PresentationSegment[] segments2 = new PresentationSegment[segments.length - 2];
				System.arraycopy(segments, 1, segments2, 0, segments.length - 2);
				segments = segments2;

				for (PresentationSegment s : segments) {
					s.startTime -= rTime;
				}
				totalRepeatTime = System.currentTimeMillis() - 100;
			}

			updateEndTimes();
		}

		public String summary() {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (PresentationSegment pp : segments) {
				if (!first)
					sb.append(", ");
				sb.append(pp.p1.getClass().getSimpleName());
				first = false;
			}
			return sb.toString();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Presentation plan ");
			sb.append(startTime + " -> " + endTime + ", " + totalRepeatTime);
			for (PresentationSegment pp : segments)
				sb.append("\n  " + pp.toString());
			return sb.toString();
		}
	}

	protected PresentationPlan currentPlan;

	private long nanoTimeDelta = System.currentTimeMillis() * 1_000_000L - System.nanoTime();

	protected Rectangle displayRect;

	protected Thread thread;
	protected static Object lock = new Object();
	protected boolean hidden = false;

	protected int fps;
	protected boolean showDebug;
	protected boolean showTimerStats;
	protected boolean showTimerSparklines;

	private long thumbnailDelay = DEFAULT_THUMBNAIL_DELAY;
	private int thumbnailHeight = DEFAULT_THUMBNAIL_HEIGHT;
	private long lastThumbnailTime;
	private long timeUntilPlanChange = -1;
	private PresentationPlan nextPlan = null;
	private long updateTime = 0;
	private DisplayConfig displayConfig;

	private IThumbnailListener thumbnailListener;

	protected GraphicsDevice device;
	protected String title;

	public interface IThumbnailListener {
		void handleThumbnail(BufferedImage image);
	}

	public PresentationWindowImpl(String title, Rectangle r, Image iconImage) {
		super(title);
		this.title = title;

		setIconImage(iconImage);
		if (Taskbar.isTaskbarSupported())
			Taskbar.getTaskbar().setIconImage(iconImage);

		setBounds(r);
		setBackground(Color.black);
		setUndecorated(true);
		setFocusableWindowState(true);
		setFocusable(true);

		setCursor(getCustomCursor());

		logDevices();

		Presentation dp;
		String defaultPresentation = System.getProperty("ICPC_DEFAULT_PRESENTATION");
		if (defaultPresentation == null)
			defaultPresentation = System.getenv("ICPC_DEFAULT_PRESENTATION");

		if (defaultPresentation != null) {
			try {
				Class<?> dc = getClass().getClassLoader().loadClass(defaultPresentation);
				dp = (Presentation) dc.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error loading branding", e);
				dp = new NoPresentation();
			}
		} else {
			dp = new NoPresentation();
		}
		setPresentation(dp);
	}

	protected static boolean isGlobalKey(String key) {
		return "lightMode".equals(key) || "name".equals(key);
	}

	private static void logDevices() {
		GraphicsDevice[] gds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

		for (int i = 0; i < gds.length; i++) {
			GraphicsDevice gd = gds[i];
			if (gd.getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
				Trace.trace(Trace.INFO, "Display " + (i + 1) + " found, modes:");

				DisplayMode[] modes = gd.getDisplayModes();
				Arrays.sort(modes, new Comparator<DisplayMode>() {
					@Override
					public int compare(DisplayMode m1, DisplayMode m2) {
						int d = m2.getWidth() - m1.getWidth();
						if (d != 0)
							return d;
						d = m2.getHeight() - m1.getHeight();
						if (d != 0)
							return d;
						d = m2.getRefreshRate() - m1.getRefreshRate();
						if (d != 0)
							return d;
						d = m2.getBitDepth() - m1.getBitDepth();
						if (d != 0)
							return d;
						return 0;
					}
				});

				String last = "";
				for (DisplayMode mode : modes) {
					String cur = mode.getWidth() + "x" + mode.getHeight() + " @ " + mode.getRefreshRate() + "hz in "
							+ mode.getBitDepth() + "bit";
					if (!last.equals(cur))
						Trace.trace(Trace.INFO, "   " + cur);
					last = cur;
				}
				for (GraphicsConfiguration cfg : gd.getConfigurations()) {
					Trace.trace(Trace.INFO,
							"   Config: " + cfg.getBufferCapabilities().isFullScreenRequired() + ", "
									+ cfg.getBufferCapabilities().isMultiBufferAvailable() + ", "
									+ cfg.getBufferCapabilities().isPageFlipping() + ", "
									+ cfg.getColorModel().getClass().getSimpleName());
				}
			}
		}
	}

	@Override
	public void openIt() {
		setVisible(true);
		toFront();
		requestFocus();

		createBufferStrategy(2);

		createThread();
	}

	public void openInBackground() {
		setVisible(true);

		createBufferStrategy(2);

		setVisible(false);

		createThread();
	}

	protected void createThread() {
		thread = new Thread("Animation Thread") {
			@Override
			public void run() {
				try {
					int frameCount = 0;
					long ns = System.nanoTime();
					long startTime = ns;
					while (true) {
						// check if the window is visible
						if (!isVisible()) {
							// we're in the background, just delay by 500ms to slow things down
							LockSupport.parkNanos(500_000_000L);
						}

						long now = System.nanoTime();
						try {
							boolean b = false;
							while (!b)
								b = paintImmediately();
						} catch (Throwable t) {
							Trace.trace(Trace.ERROR, "Error painting", t);
						}

						// send thumbnails or info if it has been a while
						if (thumbnailListener != null) {
							if (lastThumbnailTime == 0 || (now - lastThumbnailTime) > thumbnailDelay) {
								sendThumbnail();
								lastThumbnailTime = now;
							}
						}

						long delayNs = 0;
						// ask the current presentation when to repaint
						// but don't extend past where the plan changes
						if (timeUntilPlanChange == 0)
							delayNs = 0;
						else if (currentPresentation != null)
							delayNs = Math.min(timeUntilPlanChange * 1_000_000L,
									currentPresentation.getDelayTimeMs() * 1_000_000L);

						// cap speed at our max frame rate
						delayNs = Math.max(delayNs, MAX_FPS - (System.nanoTime() - now));

						if (delayNs <= 0) {
							// no need to delay here - do nothing
						} else if (delayNs <= 250_000_000L) {
							// short delay of less than 1/4s, just delay the thread
							LockSupport.parkNanos(delayNs);
						} else {
							// if we aren't going to change for more than 5s,
							// send a thumbnail and info
							if (thumbnailListener != null && delayNs > 500_000_000L) {
								fps = 0;
								sendThumbnail();
							}

							// longer/indefinite delay, so lock a monitor
							synchronized (lock) {
								try {
									lock.wait(delayNs / 1_000_000L);
								} catch (Exception e) {
									// ignore
								}
							}
						}

						frameCount++;
						if (now - startTime > 1_000_000_000L) { // 1 second
							fps = frameCount;
							frameCount = 0;
							startTime = now;
						}
					}
				} catch (Throwable t) {
					Trace.trace(Trace.ERROR, "Error in animation thread", t);
				}
			}
		};

		thread.setPriority(Thread.MAX_PRIORITY - 1);
		thread.setDaemon(true);
		thread.start();
	}

	public void setThumbnailListener(IThumbnailListener listener) {
		thumbnailListener = listener;
	}

	@Override
	public void setPresentation(Presentation p) {
		setPresentations(0, new Presentation[] { p }, null);
	}

	@Override
	public void setPresentation(Transition transition, Presentation p) {
		setPresentations(0, new Presentation[] { currentPresentation, p }, new Transition[] { transition }, true);
	}

	@Override
	public void setPresentations(long timeMs2, Presentation[] newPresentations, Transition[] newTransitions) {
		setPresentations(timeMs2, newPresentations, newTransitions, false);
	}

	public void setPresentations(long timeMs2, Presentation[] newPresentations, Transition[] newTransitions,
			boolean immediate) {
		if (newPresentations == null)
			return;

		long timeMs = timeMs2;
		if (timeMs == 0)
			timeMs = getCurrentTimeMs();

		long dt = timeMs - getCurrentTimeMs();
		if (dt <= 0)
			Trace.trace(Trace.INFO, "New presentation plan now");
		else
			Trace.trace(Trace.INFO, "New presentation plan in " + dt + "ms");

		// set sizes
		Dimension d = getPresentationSize();
		for (Presentation p : newPresentations) {
			Trace.trace(Trace.INFO, "Initializing presentation " + p);
			if (lightMode)
				p.setProperty("lightMode:light");
			else
				p.setProperty("lightMode:dark");
			if (!p.getSize().equals(d))
				p.setSize(d);
			p.init();
		}
		if (newTransitions != null) {
			for (Transition t : newTransitions) {
				Trace.trace(Trace.INFO, "Initializing transition " + t);
				t.setSize(d);
				t.init();
			}
		}

		PresentationPlan newPlan = createPresentationPlan(newPresentations, newTransitions, immediate);
		if (newPlan != null)
			newPlan.startTime = timeMs;
		if (currentPlan != null)
			currentPlan.endTime = timeMs;
		nextPlan = newPlan;

		// trace plan
		Trace.trace(Trace.INFO, "Next presentation plan: " + nextPlan);

		notifyPaintThread();
	}

	private static void notifyPaintThread() {
		synchronized (lock) {
			try {
				lock.notify();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	protected static PresentationPlan createPresentationPlan(Presentation[] presentations, Transition[] transitions,
			boolean immediate) {
		if (presentations == null || presentations.length == 0)
			return null;

		Trace.trace(Trace.INFO, "Building presentation plan");
		return new PresentationPlan(presentations, transitions, immediate);
	}

	@Override
	public void setProperty(String key, String value) {
		if (currentPlan != null)
			currentPlan.setProperty(key, value);

		if (nextPlan != null)
			nextPlan.setProperty(key, value);
	}

	protected long getCurrentTimeMs() {
		return (System.nanoTime() + nanoTimeDelta) / 1_000_000L;
	}

	public void setDisplayRect(Rectangle r) {
		displayRect = r;
		setPresentationSize();
	}

	/**
	 * Sets the zero time in ms since the epoch.
	 *
	 * @param time
	 */
	@Override
	public void setNanoTimeDelta(long time) {
		nanoTimeDelta = time;
	}

	public void setClientName(String name) {
		setProperty("name", name);
	}

	@Override
	public void setLightMode(boolean light) {
		this.lightMode = light;
		if (lightMode)
			setProperty("lightMode", "light");
		else
			setProperty("lightMode", "dark");
	}

	@Override
	public boolean isHidden() {
		return hidden;
	}

	@Override
	public void setHidden(boolean b) {
		if (!b && currentPresentation != null)
			currentPresentation.aboutToShow();

		hidden = b;
		notifyPaintThread();
	}

	protected void setPresentationSize() {
		Dimension d = getPresentationSize();

		if (currentPlan != null)
			currentPlan.setSize(d);

		if (nextPlan != null)
			nextPlan.setSize(d);

		notifyPaintThread();
	}

	public Dimension getDisplaySize() {
		if (displayRect != null)
			return displayRect.getSize();
		return getSize();
	}

	@Override
	public Dimension getPresentationSize() {
		Dimension d = getDisplaySize();
		if (displayConfig != null && displayConfig.id != -1)
			d.setSize(d.width * displayConfig.ww, d.height * displayConfig.hh);
		return d;
	}

	@Override
	public String getPresentationName() {
		if (currentPresentation == null)
			return null;

		return currentPresentation.getClass().getSimpleName();
	}

	@Override
	public void setDisplayConfig(DisplayConfig displayConfig) {
		this.displayConfig = displayConfig;
		apply(displayConfig);
		setPresentationSize();
	}

	private void apply(DisplayConfig dc) {
		// On Mac, full-screen exclusive windows can coexist on different displays. When not in
		// full-screen mode, insets include the top menu title bar on every display, and the dock
		// on only the primary display( typically at the bottom). Both are always on top of the
		// window.

		// On Windows, full-screen exclusive windows are automatically minimized if you select any
		// other program (e.g. click on another display). When not in full-screen mode, insets
		// include the taskbar on every display (typically at the bottom). Windows can display over
		// the taskbar.

		// To mitigate the difference on Windows when there are multiple displays, automatically
		// switch full screen exclusive mode to full screen window with no insets.
		GraphicsDevice[] gds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		if (dc.device >= gds.length) {
			StringBuilder message = new StringBuilder(
					"Invalid device: " + device + ". Found a total of " + gds.length + " devices:\n");
			for (int i = 0; i < gds.length; i++) {
				Rectangle r = gds[i].getDefaultConfiguration().getBounds();
				message.append(i).append(": ").append(gds[i].getIDstring()).append(" - ").append(r.width).append("x")
						.append(r.height).append("\n");
			}
			throw new IllegalArgumentException(message.toString());
		}

		// exit full screen window if necessary
		if (dc.mode != Mode.FULL_SCREEN && dc.mode != Mode.FULL_SCREEN_MAX) {
			for (int i = 0; i < gds.length; i++) {
				if (this.equals(gds[i].getFullScreenWindow()))
					gds[i].setFullScreenWindow(null);
			}
		}

		GraphicsDevice gDevice = gds[dc.device];
		GraphicsConfiguration gConfig = gDevice.getDefaultConfiguration();
		Rectangle r = gConfig.getBounds();
		Insets in = Toolkit.getDefaultToolkit().getScreenInsets(gConfig);
		Trace.trace(Trace.INFO, "Applying display config " + dc + " (r=" + r + ",in=" + in + ")");
		boolean isWindowsMultiDisplay = System.getProperty("os.name").toLowerCase().contains("win") && gds.length > 1;
		if (isWindowsMultiDisplay && dc.mode == Mode.FULL_SCREEN) {
			dc.mode = Mode.FULL_WINDOW;
		} else {
			DisplayMode mode = gDevice.getDisplayMode();
			Trace.trace(Trace.INFO, "Device: " + mode.getWidth() + "x" + mode.getHeight());
			double scaling = mode.getWidth() / (double) r.width;
			if (scaling > 1 && dc.mode == Mode.FULL_SCREEN_MAX) {
				Trace.trace(Trace.INFO, "Scaling by " + scaling);
				r.width *= scaling;
				r.height *= scaling;
			}

			r.x += in.left;
			r.y += in.top;
			r.height -= in.top + in.bottom;
			r.width -= in.left + in.right;
		}

		if (dc.mode == Mode.TOP_LEFT)
			setBounds(r.x, r.y, r.width / 2, r.height / 2);
		else if (dc.mode == Mode.TOP_RIGHT)
			setBounds(r.x + r.width / 2, r.y, r.width / 2, r.height / 2);
		else if (dc.mode == Mode.BOTTOM_LEFT)
			setBounds(r.x, r.y + r.height / 2, r.width / 2, r.height / 2);
		else if (dc.mode == Mode.BOTTOM_RIGHT)
			setBounds(r.x + r.width / 2, r.y + r.height / 2, r.width / 2, r.height / 2);
		else if (dc.mode == Mode.MEDIUM)
			setBounds(r.x, r.y, r.width * 2 / 3, r.height * 2 / 3);
		else if (dc.mode == Mode.MEDIUM_BL)
			setBounds(r.x, r.y + r.height * 5 / 12, r.width * 7 / 12, r.height * 7 / 12);
		else if (dc.mode == Mode.MIDDLE)
			setBounds(r.x + r.width / 6, r.y + r.height / 6, r.width * 2 / 3, r.height * 2 / 3);
		else if (dc.mode == Mode.ALMOST)
			setBounds(r.x + r.width / 12, r.y + r.height / 12, r.width * 5 / 6, r.height * 5 / 6);
		else if (dc.mode == Mode.FULL_WINDOW)
			setBounds(r.x, r.y, r.width, r.height);

		if (dc.mode != Mode.FULL_SCREEN && dc.mode != Mode.FULL_SCREEN_MAX && dc.mode != Mode.FULL_SCREEN_T) {
			requestFocus();
			return;
		}
		gDevice.setFullScreenWindow(this);

		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			// See
			// https://stackoverflow.com/questions/13064607/fullscreen-swing-components-fail-to-receive-keyboard-input-on-java-7-on-mac-os-x
			// why we do this
			this.setVisible(false);
			this.setVisible(true);
		}

		if (dc.mode == Mode.FULL_SCREEN_MAX) {
			DisplayMode bestMode = null;
			int area = 0;
			for (DisplayMode dm : gDevice.getDisplayModes()) {
				if (dm.getRefreshRate() >= 40 && dm.getWidth() * dm.getHeight() > area) {
					area = dm.getWidth() * dm.getHeight();
					bestMode = dm;
				}
			}

			if (bestMode != null) {
				Trace.trace(Trace.INFO, "Best display mode: " + bestMode.getWidth() + " x " + bestMode.getHeight());
				gDevice.setDisplayMode(bestMode);
			}
		} else if (dc.mode == Mode.FULL_SCREEN_T) {
			DisplayMode[] modes = gDevice.getDisplayModes();
			int num = modes.length;
			if (dc.conf < 0 || dc.conf >= num)
				throw new IllegalArgumentException("Invalid T mode");

			Arrays.sort(modes, new Comparator<DisplayMode>() {
				@Override
				public int compare(DisplayMode m1, DisplayMode m2) {
					int d = m2.getWidth() - m1.getWidth();
					if (d != 0)
						return d;
					d = m2.getHeight() - m1.getHeight();
					if (d != 0)
						return d;
					d = m2.getRefreshRate() - m1.getRefreshRate();
					if (d != 0)
						return d;
					d = m2.getBitDepth() - m1.getBitDepth();
					if (d != 0)
						return d;
					return 0;
				}
			});
			DisplayMode tMode = modes[dc.conf];
			Trace.trace(Trace.INFO,
					"T display mode: " + tMode.getWidth() + " x " + tMode.getHeight() + " @ " + tMode.getRefreshRate());
			gDevice.setDisplayMode(tMode);
		}

		requestFocus();
	}

	@Override
	public DisplayConfig getDisplayConfig() {
		return displayConfig;
	}

	@Override
	public int getFullScreenWindow() {
		GraphicsDevice[] gds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		if (gds == null)
			return -1;

		for (int i = 0; i < gds.length; i++) {
			if (gds[i].getFullScreenWindow() == this)
				return i;
		}
		return -1;
	}

	/**
	 * Sends a thumbnail.
	 */
	protected void sendThumbnail() {
		if (thumbnailListener == null)
			return;

		try {
			float scale = thumbnailHeight / (float) getDisplaySize().height;
			BufferedImage image = createImage(scale);
			thumbnailListener.handleThumbnail(image);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating thumbnail", e);
		}
	}

	/**
	 * Set the delay between triggering thumbnails, in ms.
	 */
	public void setThumbnailDelay(long delayMs) {
		thumbnailDelay = delayMs * 1_000_000L;
	}

	@Override
	public void paint(Graphics g) {
		// ignore, next frame update will get it
	}

	private Presentation doFinalSetup(Presentation p) {
		Dimension d = getSize();
		if (!p.getSize().equals(d))
			p.setSize(d);
		p.aboutToShow();
		return p;
	}

	private void paintPresentations(Graphics2D g, PresentationPlan plan, long time, boolean hidden2) {
		if (plan == null)
			return;

		if (updateTime < time - 2000) {
			updateTime = time;
			plan.build();
		}

		long repeatTime = time % plan.totalRepeatTime;
		PresentationSegment[] segments = plan.segments;
		PresentationSegment segment = segments[0];
		for (PresentationSegment ps : segments) {
			if (ps.startTime < repeatTime) {
				segment = ps;
			}
		}
		repeatTime -= segment.startTime;

		if (displayConfig != null && displayConfig.id != -1) {
			Dimension d = getDisplaySize();
			g.translate(-d.width * (displayConfig.pos % displayConfig.ww),
					-d.height * (displayConfig.pos / displayConfig.ww));
		}

		setTitle(title + " - " + segment.p1.getClass().getSimpleName());

		if (segment.trans == null) {
			segment.p1.setTimeMs(time);
			long lastRepeatTime = segment.p1.getRepeatTimeMs();
			segment.p1.setRepeatTimeMs(repeatTime - segment.p1time);

			if (currentPresentation != segment.p1 || lastRepeatTime > repeatTime - segment.p1time)
				currentPresentation = doFinalSetup(segment.p1);

			if (!hidden2)
				segment.p1.paint(g);

			if (time < plan.startTime + PLAN_FADE_TIME)
				timeUntilPlanChange = 0;
			else if (time > plan.endTime - PLAN_FADE_TIME)
				timeUntilPlanChange = 0;
			else
				timeUntilPlanChange = segment.endTime - repeatTime - segment.startTime;
			return;
		}

		// update presentation time
		segment.p1.setTimeMs(time);
		segment.p2.setTimeMs(time);

		Transition trans = segment.trans;
		double x = repeatTime / (double) trans.getLength();
		timeUntilPlanChange = 0;

		TimeOverlap to = trans.getTimeOverlap();
		if (to == TimeOverlap.NONE) {
			segment.p1.setRepeatTimeMs(segment.p1time);
			segment.p2.setRepeatTimeMs(0);
		} else if (to == TimeOverlap.MID) {
			if (x < 0.5) {
				segment.p1.setRepeatTimeMs(segment.p1time - trans.getLength() / 2 + repeatTime);
				segment.p2.setRepeatTimeMs(0);
			} else {
				segment.p1.setRepeatTimeMs(segment.p1time);
				segment.p2.setRepeatTimeMs(repeatTime - trans.getLength() / 2);
			}
		} else {
			segment.p1.setRepeatTimeMs(segment.p1time - trans.getLength() + repeatTime);
			segment.p2.setRepeatTimeMs(repeatTime);
		}

		if (currentPresentation != segment.p2)
			currentPresentation = doFinalSetup(segment.p2);

		if (!hidden2)
			segment.trans.paint(g, x, segment.p1, segment.p2);
	}

	protected void paintImpl(Graphics2D g, boolean hidden2) {
		RenderPerfTimer.Counter frameMeasure = RenderPerfTimer.measure(RenderPerfTimer.Category.FRAME);
		frameMeasure.startMeasure();
		long time = getCurrentTimeMs();
		Font defaultFont = g.getFont();
		if (currentPlan != null && time > currentPlan.endTime - PLAN_FADE_TIME) {
			if (time < currentPlan.endTime)
				g.setComposite(AlphaComposite.SrcOver.derive((currentPlan.endTime - time) / (float) PLAN_FADE_TIME));
			else {
				currentPlan = null;
				Trace.trace(Trace.INFO, "Ending presentation plan: " + currentPlan);
			}
		}

		if (nextPlan != null && time >= nextPlan.startTime) {
			Trace.trace(Trace.INFO, "Switching presentation plans: " + currentPlan + " -> " + nextPlan);
			PresentationPlan oldPlan = currentPlan;
			currentPlan = nextPlan;
			nextPlan = null;
			if (oldPlan != null)
				oldPlan.dispose();
		}

		frameMeasure.stopMeasure();
		PresentationPlan plan = currentPlan;
		if (plan != null) {
			long start = plan.startTime;
			if (time > start && time < start + PLAN_FADE_TIME && !plan.immediate)
				g.setComposite(AlphaComposite.SrcOver.derive((time - start) / (float) PLAN_FADE_TIME));
			paintPresentations(g, plan, time, hidden2);
		}
		frameMeasure.startMeasure();
		final Color TRANSPARENT_WHITE = new Color(255, 255, 255, 196);
		final Color TRANSPARENT_BLACK = new Color(0, 0, 0, 196);
		if (showDebug) {
			g.setFont(defaultFont);
			FontMetrics fm = g.getFontMetrics();
			Dimension d = getDisplaySize();
			String[] ss = new String[] { fps + " fps", d.width + " x " + d.height, "No presentation" };
			if (plan != null)
				ss[2] = plan.summary();

			g.setColor(lightMode ? TRANSPARENT_WHITE : TRANSPARENT_BLACK);
			g.fillRect(d.width - fm.stringWidth(ss[2]) - 15, d.height - fm.getHeight() * 3 - 15,
					fm.stringWidth(ss[2]) + 10, fm.getHeight() * 3 + 10);
			g.setColor(lightMode ? Color.BLACK : Color.WHITE);
			for (int i = 0; i < 3; i++)
				g.drawString(ss[i], d.width - fm.stringWidth(ss[i]) - 10,
						d.height - fm.getDescent() - fm.getHeight() * 2 + fm.getHeight() * i - 10);
		}

		colorCacheMisses = showDebug && (showTimerStats || showTimerSparklines);
		if (showDebug && (showTimerStats || showTimerSparklines)) {
			FontMetrics fm = g.getFontMetrics();
			Dimension d = getDisplaySize();
			final Color TRANSPARENT_RED = new Color(255, 0, 0, 196);
			final Color MORE_TRANSPARENT_RED = new Color(255, 0, 0, 95);
			RenderPerfTimer.DEFAULT_INSTANCE.frame();
			int i = RenderPerfTimer.Category.values().length;
			int categoryTextWidth = 15;
			for (RenderPerfTimer.Category category : RenderPerfTimer.Category.values()) {
				String s = String.format(Locale.ENGLISH, "%s %.2f%%", category, 100.0);
				categoryTextWidth = Math.max(categoryTextWidth, fm.stringWidth(s));
			}
			int frameX = d.width - categoryTextWidth - 10 - RenderPerfTimer.N_FRAMES * 5;
			for (RenderPerfTimer.Category category : RenderPerfTimer.Category.values()) {
				int y = d.height - fm.getDescent() - fm.getHeight() * 3 - (fm.getDescent() + 10) * i;

				g.setColor(lightMode ? TRANSPARENT_WHITE : TRANSPARENT_BLACK);
				g.fillRect(d.width - categoryTextWidth - 5, y - (fm.getDescent() + 10), categoryTextWidth + 10,
						fm.getDescent() + 10);
				if (showTimerSparklines) {
					g.fillRect(frameX - 5, y - (fm.getDescent() + 10), RenderPerfTimer.N_FRAMES * 5 + 10,
							(fm.getDescent() + 10));
				}

				RenderPerfTimer.Counter measure = RenderPerfTimer.measure(category);
				double nps = measure.nanosPerSecond();
				double percent = 100 * nps / 1e9;
				if (percent < 0.01) {
					if (showTimerSparklines) {
						i--; // keep vertical positioning stable for sparklines
					}
					continue;
				}

				String s = String.format(Locale.ENGLISH, "%s %.2f%%", category, percent);
				g.setColor(lightMode ? Color.WHITE : Color.BLACK);
				int x = d.width - fm.stringWidth(s) - 10;
				g.drawString(s, x + 1, y + 1);
				g.setColor(lightMode ? Color.BLACK : Color.WHITE);
				g.drawString(s, x, y);

				if (showTimerSparklines) {
					int j = 0;
					g.setColor(category == RenderPerfTimer.Category.FRAME ? MORE_TRANSPARENT_RED : TRANSPARENT_RED);
					for (long frameNanos : measure.getFrameNanos()) {
						int h = (int) (frameNanos / 1e6);
						g.fillRect(frameX + 5 * j, y - h, 4, h + 1);
						j++;
					}
				}

				i--;
			}
			long nanos = System.nanoTime();
			long oldestFrameNanos = RenderPerfTimer.DEFAULT_INSTANCE.getFrameStartNanos()[0];
			int oldestX = d.width - categoryTextWidth - 15 - (int) (60 * 5 * (nanos - oldestFrameNanos) / 1e9);
			oldestX = Math.max(0, oldestX);
			int y = d.height - (fm.getDescent() + 10) * 1 - 5;
			g.setColor(lightMode ? TRANSPARENT_WHITE : TRANSPARENT_BLACK);
			g.fillRoundRect(oldestX - 7, y - 7, d.width - categoryTextWidth - 15 - oldestX + 14, 14, 7, 7);
			for (long frameStartNanos : RenderPerfTimer.DEFAULT_INSTANCE.getFrameStartNanos()) {
				int x = d.width - categoryTextWidth - 15 - (int) (60 * 5 * (nanos - frameStartNanos) / 1e9);
				g.setColor(TRANSPARENT_RED);
				g.fillOval(x - 2, y - 2, 5, 5);
			}
			collectGcStats();
		}
		frameMeasure.stopMeasure();
	}

	private void collectGcStats() {
		long totalGarbageCollections = 0;
		long garbageCollectionTime = 0;
		for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
			long count = gc.getCollectionCount();
			if (count >= 0) {
				totalGarbageCollections += count;
			}
			long gcTime = gc.getCollectionTime();
			if (gcTime >= 0) {
				garbageCollectionTime += gcTime;
			}
		}
		RenderPerfTimer.Counter gcMeasure = RenderPerfTimer.measure(RenderPerfTimer.Category.GC);
		if (lastGarbageCollectionTime > 0)
			gcMeasure.addMeasurement((garbageCollectionTime - lastGarbageCollectionTime) * 1_000_000);
		RenderPerfTimer.Counter gcCountMeasure = RenderPerfTimer.measure(RenderPerfTimer.Category.GC_COUNT);
		if (totalGarbageCollections > 0)
			gcCountMeasure.addMeasurement((totalGarbageCollections - lastTotalGarbageCollections) * 1_000_000);

		lastGarbageCollectionTime = garbageCollectionTime;
		lastTotalGarbageCollections = totalGarbageCollections;
	}

	long lastGarbageCollectionTime;
	long lastTotalGarbageCollections;

	@Override
	public int getFPS() {
		return fps;
	}

	@Override
	public void toggleDebug() {
		showDebug = !showDebug;
		RenderPerfTimer.DEFAULT_INSTANCE.frameReset();
	}

	@Override
	public void setShowTimerStats(boolean show) {
		showTimerStats = show;
	}

	@Override
	public void setShowTimerSparklines(boolean show) {
		showTimerSparklines = show;
	}

	@Override
	public void clearCaches() {
		currentPresentation.setProperty("clearCaches:true"); // TODO: clear all presentations'
																				// caches?
	}

	@Override
	public void resetTime() {
		nanoTimeDelta = -System.nanoTime();
	}

	public boolean paintImmediately() {
		BufferStrategy bs = getBufferStrategy();
		if (bs == null)
			return true;

		Graphics2D bg = (Graphics2D) bs.getDrawGraphics();
		Dimension d = getSize();
		if (lightMode)
			bg.setColor(Color.WHITE);
		else
			bg.setColor(Color.BLACK);
		bg.fillRect(0, 0, d.width, d.height);

		if (displayRect != null)
			bg.translate(displayRect.x, displayRect.y);

		paintImpl(bg, hidden);

		bg.dispose();
		if (!bs.contentsRestored()) {
			bs.show();
			RenderPerfTimer.Counter syncMeasure = RenderPerfTimer.measure(RenderPerfTimer.Category.SYNC);
			syncMeasure.startMeasure();
			Toolkit.getDefaultToolkit().sync();
			syncMeasure.stopMeasure();
			return true;
		}

		return false;
	}

	public void reduceThumbnails() {
		thumbnailDelay = DEFAULT_THUMBNAIL_DELAY * 2;
		thumbnailHeight = DEFAULT_THUMBNAIL_HEIGHT / 2;
	}

	@Override
	public BufferedImage createImage(float scale) {
		Dimension d = getDisplaySize();

		BufferedImage image = new BufferedImage((int) (d.width * scale), (int) (d.height * scale), Transparency.OPAQUE);
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		if (lightMode)
			g.setColor(Color.WHITE);
		else
			g.setColor(Color.BLACK);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());

		g.scale(scale, scale);
		paintImpl(g, false);
		g.dispose();

		return image;
	}

	private static Cursor getCustomCursor() {
		Toolkit t = Toolkit.getDefaultToolkit();
		Dimension d = t.getBestCursorSize(1, 1);
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gd.getDefaultConfiguration();
		BufferedImage image = gc.createCompatibleImage(
				// Never pass in a cursor size smaller than 1x1.
				Math.max(1, d.width), Math.max(1, d.height), Transparency.TRANSLUCENT);
		return t.createCustomCursor(image, new Point(0, 0), "no cursor");
	}

	@Override
	public void update(Graphics g) {
		paint(g);
	}

	private static volatile boolean colorCacheMisses;

	public static boolean shouldColorCacheMisses() {
		return colorCacheMisses;
	}
}
