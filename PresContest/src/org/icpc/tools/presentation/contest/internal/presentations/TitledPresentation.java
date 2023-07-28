package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

/**
 * Abstract presentation with a title area and contest time.
 */
public abstract class TitledPresentation extends AbstractICPCPresentation {
	protected static final int TRANSITION_TIME = 2000;
	protected static final int BORDER = 8;
	protected static final int CLOCK_MARGIN = 2;

	private Font titleFont;
	private Font clockFont;

	protected int titleHeight = 20;
	protected int headerHeight = 20;
	private BufferedImage headerImg;
	private boolean showClock = true;

	protected void setup() {
		final float dpi = 96;
		float size = (int) (height * 72.0 * 0.028 / dpi);
		titleFont = ICPCFont.deriveFont(Font.BOLD, size * 2.2f);
		clockFont = ICPCFont.deriveFont(Font.BOLD, size * 1.25f);

		headerImg = createHeaderImage();
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		headerImg = null;

		if (d.width == 0 || d.height == 0)
			return;

		setup();
	}

	protected boolean isInStartTransition() {
		if (getPreviousPresentation() != null && getRepeatTimeMs() < TRANSITION_TIME)
			return true;
		return false;
	}

	protected boolean isInEndTransition() {
		if (getNextPresentation() != null && getRepeatTimeMs() > getRepeat() - TRANSITION_TIME)
			return true;
		return false;
	}

	protected boolean isInTransition() {
		return isInStartTransition() || isInEndTransition();
	}

	/**
	 * Helper method. Returns a number between 0 and 1 if the current time is between 0 and the
	 * transition time respectively, and 1 and 0 if the time is between the transition and the
	 * end/repeat time respectively.
	 *
	 * @return a number between 0 and 1
	 */
	protected double getTransitionTime() {
		long repeat = getRepeat();
		long repeatTimeMs = getRepeatTimeMs();
		if (repeatTimeMs < TRANSITION_TIME)
			return repeatTimeMs / (double) TRANSITION_TIME;
		else if (repeatTimeMs > repeat - TRANSITION_TIME)
			return (repeat - repeatTimeMs) / (double) TRANSITION_TIME;

		return 1.0;
	}

	protected void paintHeader(Graphics2D g) {
		int h = 0;
		if (isInTransition())
			h = (int) (getTransitionTime() * headerHeight) - headerHeight;
		g.drawImage(headerImg, 0, h + titleHeight, null);

		if (showClock) {
			if (getContest().getState().isFrozen())
				g.setColor(isLightMode() ? Color.ORANGE.darker() : ICPCColors.YELLOW);
			else
				g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
			g.setFont(clockFont);
			FontMetrics fm = g.getFontMetrics();
			String s = getContestTime();
			if (s != null)
				g.drawString(s, width / 12 - fm.stringWidth(s) / 2, fm.getAscent() + CLOCK_MARGIN);

			s = getRemainingTime();
			if (s != null)
				g.drawString(s, width * 11 / 12 - fm.stringWidth(s) / 2, fm.getAscent() + CLOCK_MARGIN);
		}
	}

	protected BufferedImage createHeaderImage() {
		BufferedImage img = new BufferedImage(width, headerHeight, Transparency.OPAQUE);
		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		drawHeader(g);

		g.dispose();
		return img;
	}

	protected void drawHeader(Graphics2D g) {
		g.setColor(isLightMode() ? Color.WHITE : Color.BLACK);
		g.fillRect(0, 0, width, headerHeight + 2);

		g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
		g.drawLine(0, headerHeight - 1, width, headerHeight - 1);
	}

	protected String getTitle() {
		return null;
	}

	@Override
	public void paintImpl(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		paintHeader(g);

		String s = getTitle();
		if (s != null) {
			g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
			g.setFont(titleFont);
			FontMetrics fm = g.getFontMetrics();
			g.drawString(s, (width - fm.stringWidth(s)) / 2, fm.getAscent());
		}

		IContest c = getContest();
		if (c == null || c.getNumTeams() == 0)
			return;

		Graphics2D g2 = (Graphics2D) g.create();
		g2.translate(0, headerHeight + titleHeight);
		g2.setClip(0, 0, width, height - headerHeight - titleHeight);
		paintImplTitled(g2);
		g2.dispose();
	}

	protected abstract void paintImplTitled(Graphics2D g);

	@Override
	public void setProperty(String value) {
		super.setProperty(value);
		if (value.startsWith("clockOn"))
			showClock = true;
		else if (value.startsWith("clockOff"))
			showClock = false;
	}
}