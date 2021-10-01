package org.icpc.tools.presentation.contest.internal.presentations.clock;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.DigitalFont;

public class ClockPresentation extends AbstractICPCPresentation {
	private static final Color DARK_GRAY = new Color(12, 12, 12);
	private static final Color LIGHT_GRAY = new Color(250, 250, 250);

	private BufferedImage image;
	protected int verticalOffset;

	protected Long getClock() {
		IContest contest = getContest();
		if (contest == null)
			return null;

		Long startTime = contest.getStartStatus();
		if (startTime == null)
			return null;

		double timeMultiplier = contest.getTimeMultiplier();
		if (startTime < 0)
			return Math.round(startTime * timeMultiplier);

		return Math.round((getTimeMs() - startTime) * timeMultiplier);
	}

	public Color getTextBackgroundColor() {
		return isLightMode() ? LIGHT_GRAY : DARK_GRAY;
	}

	public Color getTextForegroundColor() {
		return isLightMode() ? Color.BLACK : Color.WHITE;
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		IContest contest = getContest();
		if (contest == null)
			return;

		image = contest.getBannerImage((int) (width * 0.7), (int) (height * 0.2), true, true);
	}

	protected void paintBanner(Graphics2D g) {
		if (image != null)
			g.drawImage(image, (width - image.getWidth()) / 2, height - image.getHeight() - 20, null);
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		paintBanner(g);

		Long ms = getClock();
		if (ms == null)
			return;

		String time = AbstractICPCPresentation.getTime(ms, true);
		if (!time.startsWith("-"))
			time = "+" + time;

		int yh = (int) Math.min(0.225f * width, height * 0.55);
		int w = DigitalFont.stringWidth(time, yh);
		if (w > width) {
			yh = yh * width / w;
			w = DigitalFont.stringWidth(time, yh);
		}
		DigitalFont.drawString(g, time, (width - w) / 2, (int) ((height + height / 2.5f) / 2f - verticalOffset), yh,
				getTextForegroundColor(), getTextBackgroundColor());
	}
}