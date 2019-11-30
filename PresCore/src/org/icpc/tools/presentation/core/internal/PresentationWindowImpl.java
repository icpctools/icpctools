package org.icpc.tools.presentation.core.internal;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.LockSupport;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.PresentationWindow;
import org.icpc.tools.presentation.core.Transition;
import org.icpc.tools.presentation.core.Transition.TimeOverlap;

/**
 * Main presentation window.
 */
public class PresentationWindowImpl extends PresentationWindow {
	private static final long serialVersionUID = 1L;
	private static final long TRANSITION_TIME = 2000;
	private static final long DEFAULT_REPEAT_TIME = 10000;
	private static final long PLAN_FADE_TIME = 800; // time to fade in/out when changing plan
	private static final long MAX_FPS = 1000000000L / 60L; // limit to 60fps max

	private static final NumberFormat nf = NumberFormat.getInstance(Locale.US);

	static {
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(1);
	}

	private static final int DEFAULT_THUMBNAIL_HEIGHT = 180;
	private static final long DEFAULT_THUMBNAIL_DELAY = 2000000000L;
	private static final long INFO_DELAY = 4000000000L;

	private int pw, ph, pp = -1;

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

		public PresentationPlan(Presentation[] pres, Transition[] trans) {
			presentations = pres;
			transitions = trans;
			if (transitions == null)
				transitions = new Transition[0];

			Trace.trace(Trace.INFO, "Building presentation plan");
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
			}
		}

		public void dispose() {
			for (Presentation p : presentations)
				p.dispose();
		}

		protected void build() {
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
						if (presentations.length == 1)
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
					pp.p1time = -TRANSITION_TIME / 2;
				else if (lastTo == TimeOverlap.FULL)
					pp.p1time = -TRANSITION_TIME;
				segs.add(pp);

				Transition trans = transitions[curTrans % transitions.length];
				TimeOverlap to = trans.getTimeOverlap();

				long duration = DEFAULT_REPEAT_TIME;
				long repeat = pres.getRepeat();
				if (repeat > 0)
					duration = repeat;
				time += duration + pp.p1time;

				if (to == TimeOverlap.MID)
					time -= TRANSITION_TIME / 2;
				else if (to == TimeOverlap.FULL)
					time -= TRANSITION_TIME;

				PresentationSegment tp = new PresentationSegment();
				tp.startTime = time;
				tp.trans = trans;
				tp.p1 = pres;
				tp.p1time = duration;
				tp.p2 = presentations[(curPres + 1) % presentations.length];

				segs.add(tp);

				lastTo = to;
				time += TRANSITION_TIME;
				curPres++;
				curTrans++;
			}

			totalRepeatTime = time;
			segments = segs.toArray(new PresentationSegment[0]);
			updateEndTimes();
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

	protected long nanoTimeDelta;

	protected Rectangle displayRect;

	protected Thread thread;
	protected static Object lock = new Object();
	protected boolean hidden = false;

	protected int frameRate;

	private long thumbnailDelay = DEFAULT_THUMBNAIL_DELAY;
	private int thumbnailHeight = DEFAULT_THUMBNAIL_HEIGHT;
	private long lastThumbnailTime;
	private long lastInfoTime;
	private long timeUntilPlanChange = -1;
	private PresentationPlan nextPlan = null;
	private long updateTime = 0;

	private IThumbnailListener thumbnailListener;

	protected GraphicsDevice device;

	public interface IThumbnailListener {
		void handleThumbnail(BufferedImage image);

		void handleInfo();
	}

	public PresentationWindowImpl(String title, Rectangle r, Image iconImage) {
		super(title);

		setIconImage(iconImage);
		setMacIconImage(iconImage);

		nanoTimeDelta = System.currentTimeMillis() * 1000000L - System.nanoTime();

		setBounds(r);
		setBackground(Color.black);
		setUndecorated(true);
		setFocusableWindowState(true);
		setFocusable(true);

		setCursor(getCustomCursor());

		DeviceMode.logDevices();
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
							if (lastInfoTime == 0 || (now - lastInfoTime) > INFO_DELAY) {
								thumbnailListener.handleInfo();
								lastInfoTime = now;
							}
						}

						long delayNs = 0;
						if (currentPlan == null || currentPlan.segments.length == 0)
							// if there are no presentations, delay for 10s
							delayNs = 10000000000L;
						else {
							// if there are presentations, ask the current presentation when to repaint
							// but don't extend past where the plan changes
							if (timeUntilPlanChange == 0)
								delayNs = 0;
							else if (currentPresentation != null)
								delayNs = Math.min(timeUntilPlanChange * 1000000L,
										currentPresentation.getDelayTimeMs() * 1000000L);

							// cap speed at our max frame rate
							delayNs = Math.max(delayNs, MAX_FPS - (System.nanoTime() - now));
						}

						if (delayNs <= 0) {
							// no need to delay here - do nothing
						} else if (delayNs <= 250000000L) {
							// short delay of less than 1/4s, just delay the thread
							LockSupport.parkNanos(delayNs);
						} else {
							// if we aren't going to change for more than 5s,
							// send a thumbnail and info
							if (thumbnailListener != null && delayNs > 500000000L) {
								sendThumbnail();
								frameRate = 0;
								thumbnailListener.handleInfo();
							}

							// longer/indefinite delay, so lock a monitor
							synchronized (lock) {
								try {
									lock.wait(delayNs / 1000000L);
								} catch (Exception e) {
									// ignore
								}
							}
						}

						frameCount++;
						if (now - startTime > 1000000000L) { // 1 second
							frameRate = frameCount;
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
	public void setPresentations(long timeMs2, Presentation[] newPresentations, Transition[] newTransitions) {
		if (newPresentations == null)
			return;

		long timeMs = timeMs2;
		if (timeMs == 0)
			timeMs = getCurrentTimeMs();

		long dt = timeMs - getCurrentTimeMs();
		if (dt < 0)
			Trace.trace(Trace.INFO, "New presentation plan now");
		else
			Trace.trace(Trace.INFO, "New presentation plan in " + dt + "ms");

		// set sizes
		Dimension d = getPresentationSize();
		for (Presentation p : newPresentations) {
			Trace.trace(Trace.INFO, "Initializing presentation " + p);
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

		PresentationPlan newPlan = createPresentationPlan(newPresentations, newTransitions);
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

	protected static PresentationPlan createPresentationPlan(Presentation[] presentations, Transition[] transitions) {
		if (presentations == null || presentations.length == 0)
			return null;

		Trace.trace(Trace.INFO, "Building presentation plan");
		return new PresentationPlan(presentations, transitions);
	}

	@Override
	public void setProperty(String key, String value) {
		if (value != null && value.startsWith("pos:")) {
			if (value.length() == 4)
				pp = -1;
			else {
				try {
					pw = Integer.parseInt(value.charAt(4) + "");
					ph = Integer.parseInt(value.charAt(5) + "");
					pp = Integer.parseInt(value.charAt(6) + "");
				} catch (Exception e) {
					Trace.trace(Trace.WARNING, " " + value);
				}
			}
			setPresentationSize();
			return;
		}

		if (currentPlan != null)
			currentPlan.setProperty(key, value);

		if (nextPlan != null)
			nextPlan.setProperty(key, value);
	}

	protected long getCurrentTimeMs() {
		return (System.nanoTime() + nanoTimeDelta) / 1000000L;
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
	public void setZeroTimeMs(long time) {
		long newNanoTimeDelta = time * 1000000L - System.nanoTime();
		long diff = Math.abs(newNanoTimeDelta - nanoTimeDelta) / 1000000;
		Trace.trace(Trace.INFO, "Nano time diff: " + diff);
		nanoTimeDelta = newNanoTimeDelta;
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
		if (pp != -1)
			d.setSize(d.width * pw, d.height * ph);
		return d;
	}

	@Override
	public String getPresentationName() {
		if (currentPresentation == null)
			return null;

		return currentPresentation.getClass().getSimpleName();
	}

	@Override
	public DeviceMode getWindow() {
		return new DeviceMode();
	}

	@Override
	public void setWindow(DeviceMode p) {
		p.apply(this);
		setPresentationSize();
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
		thumbnailDelay = delayMs * 1000000L;
	}

	@Override
	public void paint(Graphics g) {
		// ignore, next frame update will get it
	}

	protected void paintPresentations(Graphics2D g, long time, boolean hidden2) {
		if (currentPlan == null)
			return;

		if (updateTime < time - 2000) {
			updateTime = time;
			currentPlan.build();
		}

		long repeatTime = time % currentPlan.totalRepeatTime;
		PresentationSegment[] segments = currentPlan.segments;
		PresentationSegment segment = segments[0];
		for (PresentationSegment ps : segments) {
			if (ps.startTime < repeatTime) {
				segment = ps;
			}
		}
		repeatTime -= segment.startTime;

		if (pp != -1) {
			Dimension d = getDisplaySize();
			g.translate(-d.width * (pp % pw), -d.height * (pp / pw));
		}

		if (segment.trans == null) {
			segment.p1.setTimeMs(time);
			long lastRepeatTime = segment.p1.getRepeatTimeMs();
			segment.p1.setRepeatTimeMs(repeatTime - segment.p1time);

			if (currentPresentation != segment.p1 || lastRepeatTime > repeatTime - segment.p1time) {
				segment.p1.aboutToShow();
				currentPresentation = segment.p1;
			}

			if (!hidden2)
				segment.p1.paint(g);

			if (time < currentPlan.startTime + PLAN_FADE_TIME)
				timeUntilPlanChange = 0;
			else if (time > currentPlan.endTime - PLAN_FADE_TIME)
				timeUntilPlanChange = 0;
			else
				timeUntilPlanChange = segment.endTime - repeatTime - segment.startTime;
			return;
		}

		// update presentation time
		segment.p1.setTimeMs(time);
		segment.p2.setTimeMs(time);

		double x = repeatTime / (double) TRANSITION_TIME;
		timeUntilPlanChange = 0;

		TimeOverlap to = segment.trans.getTimeOverlap();
		if (to == TimeOverlap.NONE) {
			segment.p1.setRepeatTimeMs(segment.p1time);
			segment.p2.setRepeatTimeMs(0);
		} else if (to == TimeOverlap.MID) {
			if (x < 0.5) {
				segment.p1.setRepeatTimeMs(segment.p1time - TRANSITION_TIME / 2 + repeatTime);
				segment.p2.setRepeatTimeMs(0);
			} else {
				segment.p1.setRepeatTimeMs(segment.p1time);
				segment.p2.setRepeatTimeMs(repeatTime - TRANSITION_TIME / 2);
			}
		} else {
			segment.p1.setRepeatTimeMs(segment.p1time - TRANSITION_TIME + repeatTime);
			segment.p2.setRepeatTimeMs(repeatTime);
		}

		if (currentPresentation != segment.p2) {
			segment.p2.aboutToShow();
			currentPresentation = segment.p2;
		}

		if (!hidden2)
			segment.trans.paint(g, x, segment.p1, segment.p2);
	}

	protected void paintImpl(Graphics2D g, boolean hidden2) {
		long time = getCurrentTimeMs();
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

		if (currentPlan != null) {
			long start = currentPlan.startTime;
			if (time > start && time < start + PLAN_FADE_TIME)
				g.setComposite(AlphaComposite.SrcOver.derive((time - start) / (float) PLAN_FADE_TIME));
			paintPresentations(g, time, hidden2);
		} else {
			g.setColor(Color.WHITE);
			FontMetrics fm = g.getFontMetrics();
			String s = "No presentation available";
			Dimension d = getSize();
			g.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 2 - fm.getAscent());
		}
	}

	@Override
	public int getFrameRate() {
		return frameRate;
	}

	public boolean paintImmediately() {
		BufferStrategy bs = getBufferStrategy();
		if (bs == null)
			return true;

		Graphics2D bg = (Graphics2D) bs.getDrawGraphics();
		Dimension d = getSize();
		bg.setColor(Color.BLACK);
		bg.fillRect(0, 0, d.width, d.height);

		if (displayRect != null)
			bg.translate(displayRect.x, displayRect.y);

		paintImpl(bg, hidden);

		bg.dispose();
		if (!bs.contentsRestored()) {
			bs.show();
			Toolkit.getDefaultToolkit().sync();
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
		BufferedImage image = gc.createCompatibleImage(d.width, d.height, Transparency.TRANSLUCENT);
		// im = alphaMultiply(im, 0.0);*/
		return t.createCustomCursor(image, new Point(0, 0), "no cursor");
	}

	@Override
	public void update(Graphics g) {
		paint(g);
	}

	private static void setMacIconImage(Image iconImage) {
		// call com.apple.eawt.Application.getApplication().setDockIconImage(img) without a direct
		// dependency
		try {
			Class<?> c = Class.forName("com.apple.eawt.Application");
			Method m = c.getDeclaredMethod("getApplication");
			Object o = m.invoke(null);
			m = c.getDeclaredMethod("setDockIconImage", Image.class);
			m.invoke(o, iconImage);
		} catch (Exception e) {
			// ignore, we're not on Mac
		}
	}
}
