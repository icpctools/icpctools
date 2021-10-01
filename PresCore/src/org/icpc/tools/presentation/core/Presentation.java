package org.icpc.tools.presentation.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * The abstract presentation (renderer) class. All presentations displayed on-screen subclass this
 * class.
 */
public abstract class Presentation {
	protected static final byte MOUSE_ENTERED = 1;
	protected static final byte MOUSE_EXITED = 2;
	protected static final byte MOUSE_CLICKED = 3;
	protected static final byte MOUSE_PRESSED = 4;
	protected static final byte MOUSE_RELEASED = 5;

	protected static final byte MOUSE_MOTION_MOVED = 1;
	protected static final byte MOUSE_MOTION_DRAGGED = 2;

	protected static final byte KEY_PRESSED = 1;
	protected static final byte KEY_RELEASED = 2;
	protected static final byte KEY_TYPED = 3;

	protected int width;
	protected int height;

	private long timeMs;
	private long repeatTimeMs;

	private String previousPresentationId;
	private String nextPresentationId;

	private List<KeyListener> keyListeners;
	private List<MouseListener> mouseListeners;
	private List<MouseMotionListener> mouseMotionListeners;

	private boolean lightMode;

	/**
	 * Sets the size of this presentation.
	 *
	 * @param the size
	 */
	public void setSize(Dimension d) {
		width = d.width;
		height = d.height;
	}

	/**
	 * Returns the size of this presentation.
	 *
	 * @return the size
	 */
	public Dimension getSize() {
		return new Dimension(width, height);
	}

	/**
	 * Set a property using the given value.
	 *
	 * @param value
	 */
	public void setProperty(String value) {
		if (value != null && value.startsWith("lightMode:"))
			lightMode = value.substring(10).equals("light");
	}

	/**
	 * True if light mode is enabled, false otherwise.
	 */
	public boolean isLightMode() {
		return lightMode;
	}

	/**
	 * Initialize this presentation. This method should be as short-lived as possible (the
	 * presentation won't be shown the first time until it returns) but it should do enough work
	 * that the other presentation methods (e.g. getRepeat() have enough data to return correctly.
	 */
	public void init() {
		// do nothing
	}

	/**
	 * Dispose of this presentation.
	 */
	public void dispose() {
		// do nothing
	}

	/**
	 * Notification that this presentation is about to be shown or repeated. The size and current
	 * time will be correct.
	 */
	public void aboutToShow() {
		// do nothing
	}

	/**
	 * Returns the id of the previous presentation, if known.
	 *
	 * @return
	 */
	public String getPreviousPresentation() {
		return previousPresentationId;
	}

	/**
	 * Returns the id of the next presentation, if known.
	 *
	 * @return
	 */
	public String getNextPresentation() {
		return nextPresentationId;
	}

	/**
	 * Returns the current time (in ms) of the presentation.
	 *
	 * @return the current time, in ms
	 */
	public long getTimeMs() {
		return timeMs;
	}

	/**
	 * Returns the repeat (i.e. cycle or loop) time of the presentation, or 0 for no repeat. By
	 * default presentations do not repeat; subclasses can override this method to return a desired
	 * repeat-time.
	 *
	 * @return the repeat time, in ms
	 */
	public long getRepeat() {
		return 0;
	}

	/**
	 * Returns the current time (in ms) of the presentation, taking repeats into account.
	 *
	 * @return the current repeat time, in ms
	 */
	public long getRepeatTimeMs() {
		return repeatTimeMs;
	}

	/**
	 * Helper method called to help with animations. The given time is the number of elapsed
	 * milliseconds since the last time the paint() method was called.
	 *
	 * @param dt the amount of time to increment, in ms
	 */
	protected void incrementTimeMs(long dt) {
		// do nothing
	}

	/**
	 * Sets the current time.
	 *
	 * @param time - the current time, in ms
	 */
	public void setTimeMs(long time) {
		long dt = time - timeMs;
		if (dt > 0 && dt < 2000)
			incrementTimeMs(dt);

		timeMs = time;
	}

	/**
	 * Return the amount of time that could be skipped before repainting. Return <= 0 for immediate
	 * refresh (which is limited by display frame rate and CPU performance), Return >0 for # of ms
	 * that can be skipped, e.g. 2000 to be repainted next in 2 seconds.
	 *
	 * @return - <= 0, or the time that can be skipped until painting again, in ms
	 */
	public long getDelayTimeMs() {
		return 0;
	}

	/**
	 * Sets the current repeat time.
	 *
	 * @param time - the current repeat time, in ms
	 */
	public void setRepeatTimeMs(long time) {
		repeatTimeMs = time;
	}

	/**
	 * Paints this presentation.
	 *
	 * @param g - the graphics context
	 */
	public void paint(Graphics2D g) {
		// do nothing, subclasses will override
	}

	public void addKeyListener(KeyListener listener) {
		if (keyListeners == null)
			keyListeners = new ArrayList<>();
		if (!keyListeners.contains(listener))
			keyListeners.add(listener);
	}

	public void addMouseListener(MouseListener listener) {
		if (mouseListeners == null)
			mouseListeners = new ArrayList<>();
		if (!mouseListeners.contains(listener))
			mouseListeners.add(listener);
	}

	public void addMouseMotionListener(MouseMotionListener listener) {
		if (mouseMotionListeners == null)
			mouseMotionListeners = new ArrayList<>();
		if (!mouseMotionListeners.contains(listener))
			mouseMotionListeners.add(listener);
	}

	public void removeKeyListener(KeyListener listener) {
		if (keyListeners == null)
			return;
		keyListeners.remove(listener);
	}

	public void removeMouseListener(MouseListener listener) {
		if (mouseListeners == null)
			return;
		mouseListeners.remove(listener);
	}

	public void removeMouseMotionListener(MouseMotionListener listener) {
		if (mouseMotionListeners == null)
			return;
		mouseMotionListeners.remove(listener);
	}

	protected final void fireMouseEvent(MouseEvent e, byte type) {
		if (mouseListeners == null)
			return;

		MouseListener[] list;
		synchronized (mouseListeners) {
			list = new MouseListener[mouseListeners.size()];
			mouseListeners.toArray(list);
		}

		int size = list.length;
		for (int i = 0; i < size; i++) {
			if (type == MOUSE_CLICKED)
				list[i].mouseClicked(e);
			else if (type == MOUSE_ENTERED)
				list[i].mouseEntered(e);
			else if (type == MOUSE_EXITED)
				list[i].mouseExited(e);
		}
	}

	protected final void fireMouseMotionEvent(MouseEvent e, byte type) {
		if (mouseMotionListeners == null)
			return;

		MouseMotionListener[] list;
		synchronized (mouseMotionListeners) {
			list = new MouseMotionListener[mouseMotionListeners.size()];
			mouseMotionListeners.toArray(list);
		}

		int size = list.length;
		for (int i = 0; i < size; i++) {
			if (type == MOUSE_MOTION_MOVED)
				list[i].mouseMoved(e);
			else if (type == MOUSE_MOTION_DRAGGED)
				list[i].mouseDragged(e);
		}
	}

	protected final void fireKeyEvent(KeyEvent e, byte type) {
		if (keyListeners == null)
			return;

		KeyListener[] list;
		synchronized (keyListeners) {
			list = new KeyListener[keyListeners.size()];
			keyListeners.toArray(list);
		}

		int size = list.length;
		for (int i = 0; i < size; i++) {
			if (type == KEY_PRESSED)
				list[i].keyPressed(e);
			else if (type == KEY_RELEASED)
				list[i].keyReleased(e);
			else if (type == KEY_TYPED)
				list[i].keyTyped(e);
		}
	}

	public interface Job {
		public boolean isComplete();
	}

	protected Job execute(final Runnable r) {
		final boolean[] done = new boolean[1];

		Job job = new Job() {
			@Override
			public boolean isComplete() {
				return done[0];
			}
		};

		Thread t = new Thread("Presentation Worker") {
			@Override
			public void run() {
				try {
					r.run();
				} finally {
					done[0] = true;
				}
			}
		};
		t.setPriority(Thread.MIN_PRIORITY + 1);
		t.setDaemon(true);
		t.start();

		return job;
	}

	/**
	 * Display a message to the user, typically because the presentation is missing some
	 * configuration.
	 *
	 * @param g
	 * @param message
	 */
	protected void paintHelp(Graphics2D g, String[] message) {
		paintHelp(g, message, "");
	}

	/**
	 * Display a message to the user, typically because the presentation is missing some
	 * configuration.
	 *
	 * @param g
	 * @param message
	 * @param subs a substitution variable to replace any instances of {0} in the original message
	 */
	protected void paintHelp(Graphics2D g, String[] message, String subs) {
		Dimension d = getSize();
		g.setColor(Color.WHITE);
		Font f = g.getFont().deriveFont(16f);
		g.setFont(f);
		FontMetrics fm = g.getFontMetrics();
		for (int i = 0; i < message.length; i++) {
			String s = message[i].replace("{0}", subs);
			g.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 2 + (i - message.length / 2) * fm.getHeight());
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}