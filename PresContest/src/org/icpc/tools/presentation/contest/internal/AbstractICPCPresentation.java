package org.icpc.tools.presentation.contest.internal;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IState;
import org.icpc.tools.presentation.contest.internal.nls.Messages;
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
			return NLS.bind(Messages.pausedAt, getTime(startTime * timeMultiplier, true));

		IState state = contest.getState();
		if (state.getEnded() != null)
			return Messages.contestOver;
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
			return NLS.bind(Messages.pausedAt, getTime(startTime * timeMultiplier, true));

		IState state = contest.getState();
		if (state.getEnded() != null)
			return Messages.contestOver;
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
		return list.toArray(new String[0]);
	}

	/**
	 * @return A TextLayout that wraps the String <code>text</code> into lines of a given
	 *         </code>width</code>. If some characters in the text are not supported by the first
	 *         font, one of the other fonts is used, in order.
	 */
	public static TextLayout renderString(Graphics2D g, String text, int width, Font[] fonts) {
		AttributedString attributedString = createFallbackString(text, fonts);
		// https://stackoverflow.com/a/21357419
		AttributedCharacterIterator characterIterator = attributedString.getIterator();
		FontRenderContext fontRenderContext = g.getFontRenderContext();
		LineBreakMeasurer lbm = new LineBreakMeasurer(characterIterator, fontRenderContext);
		return lbm.nextLayout(width);
	}

	/**
	 * @return An AttributedString that attributes the String <code>text</code> with font
	 *         attributes. If some characters in <code>text</code> are not supported by the first
	 *         font, one of the other fonts is used, in order.
	 */
	protected static AttributedString createFallbackString(String s, Font[] fonts) {
		// https://stackoverflow.com/a/9482676, adapted to support Unicode values > 0x10000 and
		// multiple fallback fonts
		AttributedString result = new AttributedString(s);

		int textLength = s.length();
		result.addAttribute(TextAttribute.FONT, fonts[0], 0, textLength);

		int lastFont = -1;
		int fontBegin = 0;
		for (int i = 0; i < s.length();) {
			int codePoint = s.codePointAt(i);
			int charCount = Character.charCount(codePoint); // high-valued Unicode code points span
																			// two chars

			// detect which font (if any) can be used to display the current character
			int curFont = 0;
			for (int j = 0; j < fonts.length; j++) {
				if (fonts[j].canDisplay(codePoint)) {
					curFont = j;
					break;
				}
			}

			// if the font changes, add attribute for last region
			if (curFont != lastFont) {
				if (lastFont >= 0)
					result.addAttribute(TextAttribute.FONT, fonts[lastFont], fontBegin, i);

				fontBegin = i;
				lastFont = curFont;
			}

			i += charCount;
		}

		if (lastFont >= 0)
			result.addAttribute(TextAttribute.FONT, fonts[lastFont], fontBegin, textLength);

		return result;
	}
}