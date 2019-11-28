package org.icpc.tools.presentation.contest.internal.presentations.old;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.StatisticsGenerator;

public class LogScreenPresentation extends AbstractICPCPresentation {
	private static final int BOTTOM_MARGIN = 78;
	private static final int LINES_PER_SCREEN = 14;
	private static final int CHARS_PER_SECOND = 20;
	private static final int CHARS_PER_SCROLL = 15;
	private static final int BORDER = 5;
	// private static final Color CONSOLE_GREEN = new Color(0, 196, 0);
	private static final Color CONSOLE_GREEN = new Color(0, 255, 0);

	private Font textFont;
	private FontMetrics fm;

	private float rowHeight = 50;

	private float logPos;
	private float lastIndex = -1;
	private StringBuffer log;
	private final StatisticsGenerator stats = new StatisticsGenerator();

	@Override
	public void init() {
		super.init();

		log = new StringBuffer();
		log.append("Initializing...\n");
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		final float dpi = 96;
		rowHeight = (height - BOTTOM_MARGIN) / (float) LINES_PER_SCREEN;
		float inch = rowHeight / dpi;
		int size = (int) (inch * 72f * 0.9f);
		textFont = new Font("Courier", Font.BOLD, size);
	}

	@Override
	public void setContest(IContest sc) {
		super.setContest(sc);
		stats.generate(sc);
	}

	@Override
	public void paint(Graphics2D g) {
		float index = getTimeMs() / 1000f;
		// g.setColor(Color.blue);
		// g.fillRect((1280-640)/2, 50, 640, 480);
		g.setFont(textFont);
		fm = g.getFontMetrics();
		int y = height - BORDER - BOTTOM_MARGIN - fm.getDescent();

		Point p = new Point(0, 0);
		if (lastIndex != -1) {
			float d = index - lastIndex;
			/*
				logPos += d * CHARS_PER_SECOND;
				if (logPos > log.length())
					logPos = log.length();
			 */
			p = index(d);
		}
		lastIndex = index;

		if (p.y > 0)
			// y -= (fm.getHeight() - 5) * (float)p.y / CHARS_PER_SCROLL;
			y -= rowHeight * p.y / CHARS_PER_SCROLL;

		String s = log.substring(0, p.x);
		// String s = log.substring(0, (int) logPos);
		String[] st = tokenize(s);
		int size = st.length;
		if (size == 0)
			return;

		//
		// g.setColor(Color.WHITE);
		g.setColor(CONSOLE_GREEN);
		g.drawString(st[size - 1], BORDER, y);

		// draw log
		if (size > 1) {
			for (int i = size - 2; i >= 0; i--) {
				// y -= fm.getHeight() - 5;
				y -= rowHeight;
				g.drawString(st[i], BORDER, y);
			}
		}

		if (size > LINES_PER_SCREEN) {
			int ind = log.indexOf("\n") + 1;
			log.delete(0, ind);
			logPos -= ind;
			logPos -= CHARS_PER_SCROLL;
			if (logPos < 0)
				logPos = 0;
		}

		if (p.x >= log.length() - 2) {
			append(stats.getStatistic());
		}
	}

	protected Point index(float d) {
		if (d > 0)
			logPos += d * CHARS_PER_SECOND;

		StringBuffer sb = new StringBuffer((int) logPos);
		float l = logPos;
		int ind = 0;
		int scroll = 0;
		while (l > 0 && ind < log.length()) {
			char c = log.charAt(ind);
			if ('\n' == c && scroll < CHARS_PER_SCROLL) {
				scroll++;
			} else if ('\n' == c) {
				ind++;
				sb.append(c);
			} else {
				ind++;
				scroll = 0;
				sb.append(c);
			}
			l -= 1;
		}

		if (ind >= log.length()) {
			logPos = logPos - l;
			if (logPos < 0)
				logPos = 0;
		}

		return new Point(ind, scroll);
	}

	protected String[] tokenize2(String s2) {
		List<String> list = new ArrayList<>();

		String s = s2;
		int l = s.length();
		while (l > 0) {
			if (fm != null) {
				// look for maximum length possible string
				while (fm.stringWidth(s.substring(0, l)) > width - BORDER * 2) {
					l--;
				}
			} else {
				while (textFont.getStringBounds(s.substring(0, l), new FontRenderContext(null, true, true))
						.getWidth() > width - BORDER * 2) {
					l--;
				}
			}

			if (l >= s.length()) {
				list.add(s);
				s = "";
				l = 0;
			} else {
				// now find last previous space
				while (l > 0 && !Character.isSpaceChar(s.charAt(l)))
					l--;

				if (l == 0)
					return null;

				list.add(s.substring(0, l));
				s = s.substring(l + 1);
				if (s.length() > 1)
					s = "   " + s;
				l = s.length();
			}
		}

		String[] st = new String[list.size()];
		list.toArray(st);
		return st;
	}

	protected String[] tokenize(String s) {
		List<String> list = new ArrayList<>();

		StringTokenizer st = new StringTokenizer(s, "\n");
		while (st.hasMoreTokens()) {
			String ss = st.nextToken();
			list.add(ss);
		}

		String[] ss = new String[list.size()];
		list.toArray(ss);
		return ss;
	}

	protected void append(String s) {
		String[] ss = tokenize2(s);
		int size = ss.length;
		for (int i = 0; i < size; i++) {
			// if (i > 0)
			// log.append(" ");
			log.append(ss[i] + "\n");
		}
	}

	public void handleRuns(ISubmission[] runs) {
		IContest contest = getContest();
		for (ISubmission run : runs) {
			ITeam team = contest.getTeamById(run.getTeamId());
			Status status = contest.getStatus(run);
			if (status == Status.SOLVED) {
				String s = team.getActualDisplayName() + " solved Problem " + run.getProblemId();
				append(s);
			} else if (status == Status.SUBMITTED) {
				String s = team.getActualDisplayName() + " submitted a run for Problem " + run.getProblemId();
				append(s);
			}
		}
	}
}