package org.icpc.tools.presentation.core;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;

import org.icpc.tools.contest.Trace;

public interface IPresentationHandler {
	public enum Mode {
		TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MEDIUM, MEDIUM_BL, MIDDLE, ALMOST, FULL_WINDOW, FULL_SCREEN, FULL_SCREEN_MAX
	}

	public static class DeviceMode {
		public int device;
		public Mode p = Mode.FULL_SCREEN;

		private char[] PosStrs = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'm', 'l', 'w', ' ', 'x' };

		public DeviceMode() {
			// default full screen on primary device
		}

		public DeviceMode(String displayStr) {
			if (displayStr == null)
				return;

			device = Integer.parseInt(displayStr.substring(0, 1)) - 1;
			GraphicsDevice[] gds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
			if (device >= gds.length)
				throw new IllegalArgumentException("Invalid device: " + device);

			p = Mode.FULL_SCREEN;
			if (displayStr.length() == 2) {
				char c = displayStr.charAt(1);
				for (int i = 0; i < PosStrs.length; i++)
					if (PosStrs[i] == c)
						p = Mode.values()[i];
			}
		}

		public String toDisplayString() {
			return (device + 1) + "" + PosStrs[p.ordinal()];
		}

		public void apply(Window w) {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gds = ge.getScreenDevices();

			// exit full screen window if necessary
			if (p != Mode.FULL_SCREEN && p != Mode.FULL_SCREEN_MAX) {
				for (int i = 0; i < gds.length; i++) {
					if (w.equals(gds[i].getFullScreenWindow()))
						gds[i].setFullScreenWindow(null);
				}
			}

			GraphicsDevice gDevice = gds[device];
			Rectangle r = gDevice.getDefaultConfiguration().getBounds();

			if (p == Mode.TOP_LEFT)
				w.setBounds(r.x, r.y, r.width / 2, r.height / 2);
			else if (p == Mode.TOP_RIGHT)
				w.setBounds(r.x + r.width / 2, r.y, r.width / 2, r.height / 2);
			else if (p == Mode.BOTTOM_LEFT)
				w.setBounds(r.x, r.y + r.height / 2, r.width / 2, r.height / 2);
			else if (p == Mode.BOTTOM_RIGHT)
				w.setBounds(r.x + r.width / 2, r.y + r.height / 2, r.width / 2, r.height / 2);
			else if (p == Mode.MEDIUM)
				w.setBounds(r.x, r.y, r.width * 2 / 3, r.height * 2 / 3);
			else if (p == Mode.MEDIUM_BL)
				w.setBounds(r.x, r.y + r.height * 5 / 12, r.width * 7 / 12, r.height * 7 / 12);
			else if (p == Mode.MIDDLE)
				w.setBounds(r.x + r.width / 6, r.y + r.height / 6, r.width * 2 / 3, r.height * 2 / 3);
			else if (p == Mode.ALMOST)
				w.setBounds(r.x + r.width / 12, r.y + r.height / 12, r.width * 5 / 6, r.height * 5 / 6);
			else if (p == Mode.FULL_WINDOW)
				w.setBounds(r.x, r.y, r.width, r.height);

			if (p != Mode.FULL_SCREEN && p != Mode.FULL_SCREEN_MAX) {
				w.requestFocus();
				return;
			}

			if (p == Mode.FULL_SCREEN_MAX) {
				DisplayMode bestMode = null;
				int area = 0;
				for (DisplayMode mode : gDevice.getDisplayModes()) {
					if (mode.getRefreshRate() >= 40 && mode.getWidth() * mode.getHeight() > area) {
						area = mode.getWidth() * mode.getHeight();
						bestMode = mode;
					}
				}

				if (bestMode != null) {
					Trace.trace(Trace.INFO, "Best display mode: " + bestMode.getWidth() + " x " + bestMode.getHeight());
					gDevice.setDisplayMode(bestMode);
				}
			}

			gDevice.setFullScreenWindow(w);
			w.requestFocus();
		}

		public static void logDevices() {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gds = ge.getScreenDevices();

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
		public String toString() {
			return "Mode " + toDisplayString();
		}
	}

	/**
	 * Show one presentation with no transitions.
	 *
	 * @param presentation a presentation, may not be null
	 */
	public void setPresentation(Presentation presentation);

	/**
	 * Show a set of presentations in the given order. The list of transitions may be null (no
	 * transitions), a single entry (always use the same transition), or the same length as the
	 * number of presentations (to specify a unique transition between each presentation).
	 *
	 * @param time the time to take effect, or 0 for immediately
	 * @param newPresentations a list of presentations, may not be null or have null entries
	 * @param newTransitions null, a single transition, or a list of transitions the same length as
	 *           the presentation list with no null entries
	 */
	public void setPresentations(long time, Presentation[] newPresentations, Transition[] newTransitions);

	public Dimension getPresentationSize();

	public String getPresentationName();

	public int getFrameRate();

	public DeviceMode getWindow();

	/**
	 * Set the window position.
	 */
	public void setWindow(DeviceMode p);

	public void setZeroTimeMs(long time);

	public BufferedImage createImage(float scale);

	public boolean isHidden();

	public void setHidden(boolean b);

	public void setProperty(String key, String value);
}