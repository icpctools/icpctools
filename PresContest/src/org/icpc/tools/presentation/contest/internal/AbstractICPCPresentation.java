package org.icpc.tools.presentation.contest.internal;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IState;
import org.icpc.tools.presentation.contest.internal.nls.Messages;
import org.icpc.tools.presentation.core.Presentation;

public abstract class AbstractICPCPresentation extends Presentation {
	private IContest contest = ContestData.getContest();

	private Font contestTitleFont;
	private final int contestTitleHeight = 20;
	private final int contestTitleMargin = 2;
	private BufferedImage contestTitleImage;
	private static String contestTitleTemplate;
	protected static Color contestTitleColor;

	protected void setupContestTitle() {
		final float dpi = 96;
		float size = (int) (height * 72.0 * 0.028 / dpi);
		contestTitleFont = ICPCFont.deriveFont(Font.BOLD, size * 2.2f);

		contestTitleImage = createContestTitleImage();
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		contestTitleImage = null;

		if (d.width == 0 || d.height == 0)
			return;

		setupContestTitle();
	}

	protected void paintContestTitle(Graphics2D g) {
		g.drawImage(contestTitleImage, 0, 0, null);
	}

	protected BufferedImage createContestTitleImage() {
		// The + 2 is here (and in paint(g)) to make sure there is some room between the header and the rest of the presentation
		BufferedImage img = new BufferedImage(width, contestTitleHeight + 2 * contestTitleMargin + 2, Transparency.OPAQUE);
		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		if (getContestTitle() != null) {
			drawContestTitle(g);
		}

		g.dispose();
		return img;
	}

	protected void drawContestTitle(Graphics2D g) {
		String s = getContestTitle();

		Color color = isLightMode() ? Color.WHITE : Color.BLACK;
		if (contestTitleColor != null) {
			color = contestTitleColor;
		}
		g.setColor(color);
		g.fillRect(0, 0, width, contestTitleHeight + contestTitleMargin * 2);

		g.setColor(ICPCColors.foregroundColor(color));
		g.setFont(contestTitleFont);
		FontMetrics fm = g.getFontMetrics();
		g.drawString(s, (width - fm.stringWidth(s)) / 2, contestTitleMargin + fm.getAscent());
	}

	protected boolean shouldDrawContestTitle() {
		return getContestTitle() != null;
	}

	public static void setContestTitleTemplate(String titleTemplate) {
		AbstractICPCPresentation.contestTitleTemplate = titleTemplate;
	}

	public static void setContestTitleColor(Color titleColor) {
		AbstractICPCPresentation.contestTitleColor = titleColor;
	}

	protected String getContestTitle() {
		if (contestTitleTemplate == null) {
			return null;
		}

		return contestTitleTemplate
				.replace("{contest.name}", contest.getName())
				.replace("{contest.formal_name}", contest.getActualFormalName());
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		Graphics2D g2 = g;

		if (shouldDrawContestTitle()) {
			paintContestTitle(g);

			IContest c = getContest();
			if (c == null || c.getNumTeams() == 0)
				return;

			g2 = (Graphics2D) g.create();
			g2.translate(0, contestTitleMargin * 2 + contestTitleHeight + 2);
			g2.setClip(0, 0, width, height - contestTitleMargin * 2 - contestTitleHeight - 2);
		}

		paintImpl(g2);

		if (shouldDrawContestTitle()) {
			g2.dispose();
		}
	}

	protected abstract void paintImpl(Graphics2D g);

	protected IContest getContest() {
		return contest;
	}

	public void setContest(IContest newContest) {
		contest = newContest;
		if (width > 0 && height > 0) {
			setupContestTitle();
		}
	}

	/**
	 * Format contest time as a string.
	 *
	 * @param time contest time, in seconds
	 * @return
	 */
	public String getContestTime() {
		if (contest == null)
			return null;

		IState state = contest.getState();
		if (state.getEnded() != null)
			return Messages.contestOver;

		double timeMultiplier = contest.getTimeMultiplier();
		if (state.getStarted() != null)
			return getTime((getTimeMs() - state.getStarted()) * timeMultiplier, true);

		Long pauseTime = contest.getCountdownPauseTime();
		if (pauseTime != null)
			return NLS.bind(Messages.pausedAt, getTime(-pauseTime * timeMultiplier, false));

		if (contest.getStartTime() != null)
			return getTime((getTimeMs() - contest.getStartTime()) * timeMultiplier, true);

		return "";
	}

	/**
	 * Format remaining contest time as a string.
	 *
	 * @param time remaining contest time, in seconds
	 * @return
	 */
	public String getRemainingTime() {
		if (contest == null)
			return null;

		IState state = contest.getState();
		if (state.getEnded() != null)
			return Messages.contestOver;

		double timeMultiplier = contest.getTimeMultiplier();
		if (state.getStarted() != null)
			return getTime((state.getStarted() - getTimeMs()) * timeMultiplier + contest.getDuration(), false);

		Long pauseTime = contest.getCountdownPauseTime();
		if (pauseTime != null)
			return NLS.bind(Messages.pausedAt, getTime(-pauseTime * timeMultiplier, false));

		if (contest.getStartTime() != null)
			return getTime((contest.getStartTime() - getTimeMs()) * timeMultiplier + contest.getDuration(), false);

		return "";
	}

	/**
	 * Formats a time in seconds.
	 *
	 * @param time
	 * @param floor
	 * @return
	 */
	public static String getTime(double ms, boolean floor) {
		int ss = 0;
		if (floor)
			ss = (int) Math.floor(ms / 1000.0);
		else
			ss = (int) Math.ceil(ms / 1000.0);
		int h = (ss / 3600) % 48;
		int m = (Math.abs(ss) / 60) % 60;
		int s = (Math.abs(ss) % 60);

		StringBuilder sb = new StringBuilder();
		if (ms < 0 && h == 0)
			sb.append("-");
		sb.append(h + ":");
		if (m < 10)
			sb.append("0");
		sb.append(m + ":");
		if (s < 10)
			sb.append("0");
		sb.append(Math.abs(s));

		return sb.toString();
	}

	protected static String[] splitString(Graphics2D g, String str, int width) {
		if (str == null)
			return new String[0];

		String s = str;
		FontMetrics fm = g.getFontMetrics();
		List<String> list = new ArrayList<>();

		while (fm.stringWidth(s) > width) {
			// find spot
			int x = s.length() - 1;
			while (x > 0 && fm.stringWidth(s.substring(0, x)) > width)
				x--;

			if (x == 0) // too narrow, can't even crop a char!
				return new String[] { s };

			// try to find space a few chars back
			int y = x;
			while (y > x * 0.6f && s.charAt(y) != ' ')
				y--;

			// otherwise crop anyway
			if (s.charAt(y) != ' ') {
				list.add(s.substring(0, x));
				s = "-" + s.substring(x);
			} else {
				list.add(s.substring(0, y));
				s = s.substring(y + 1);
			}
		}
		list.add(s);
		return list.toArray(new String[0]);
	}
}