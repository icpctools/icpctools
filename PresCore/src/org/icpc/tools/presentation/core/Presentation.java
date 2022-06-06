package org.icpc.tools.presentation.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * The abstract presentation (renderer) class. All presentations displayed on-screen subclass this
 * class.
 */
public abstract class Presentation {
	protected int width;
	protected int height;

	private long timeMs;
	private long repeatTimeMs;

	private String previousPresentationId;
	private String nextPresentationId;

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

	protected void keyEvent(KeyEvent e, int type) {
		// do nothing by default
	}

	protected void mouseEvent(MouseEvent e, int type) {
		// do nothing by default
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
		g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
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