package org.icpc.tools.presentation.contest.internal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IState;
import org.icpc.tools.presentation.core.Presentation;

public abstract class AbstractICPCPresentation extends Presentation {
	private IContest contest = ContestData.getContest();

	protected IContest getContest() {
		return contest;
	}

	public void setContest(IContest newContest) {
		contest = newContest;
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

		double timeMultiplier = contest.getTimeMultiplier();
		Long startTime = contest.getStartStatus();
		if (startTime == null)
			return "";

		if (startTime < 0)
			return "Paused @ " + getTime(startTime * timeMultiplier, true);

		IState state = contest.getState();
		if (state.getEnded() != null)
			return "Contest is over";
		else if (state.getStarted() == null)
			return getTime((getTimeMs() - contest.getStartTime()) * timeMultiplier, true);

		return getTime((getTimeMs() - state.getStarted()) * timeMultiplier, true);
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

		Long startTime = contest.getStartStatus();
		if (startTime == null)
			return "";

		double timeMultiplier = contest.getTimeMultiplier();
		if (startTime < 0)
			return "Paused @ " + getTime(startTime * timeMultiplier, true);

		IState state = contest.getState();
		if (state.getEnded() != null)
			return "Contest is over";
		else if (state.getStarted() == null)
			return getTime((contest.getStartTime() - getTimeMs()) * timeMultiplier + contest.getDuration(), false);

		return getTime((state.getStarted() - getTimeMs()) * timeMultiplier + contest.getDuration(), false);
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
		return list.toArray(new String[list.size()]);
	}

	protected void paintHelp(Graphics2D g, String[] message) {
		paintHelp(g, message, "");
	}

	protected void paintHelp(Graphics2D g, String[] message, String subs) {
		Dimension d = getSize();
		g.setColor(Color.WHITE);
		Font f = ICPCFont.getMasterFont().deriveFont(16f);
		g.setFont(f);
		FontMetrics fm = g.getFontMetrics();
		for (int i = 0; i < message.length; i++) {
			String s = message[i].replace("{0}", subs);
			g.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 2 + (i - message.length / 2) * fm.getHeight());
		}
	}
}