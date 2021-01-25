package org.icpc.tools.presentation.core;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

public interface IPresentationHandler {
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

	public int getFPS();

	/**
	 * Set the window display configuration.
	 */
	public void setDisplayConfig(DisplayConfig p);

	public DisplayConfig getDisplayConfig();

	public int getFullScreenWindow();

	public void setZeroTimeMs(long time);

	public BufferedImage createImage(float scale);

	public boolean isHidden();

	public void setHidden(boolean b);

	public void setProperty(String key, String value);
}