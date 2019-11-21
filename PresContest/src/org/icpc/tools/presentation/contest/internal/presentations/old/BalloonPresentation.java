package org.icpc.tools.presentation.contest.internal.presentations.old;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

public class BalloonPresentation extends AbstractICPCPresentation {
	private static final int LINES_PER_SCREEN = 10;
	private static final int SECONDS_PER_BALLOON = 8;

	private static final int YY = 70;
	private static final int UPDATE_STEPS = 120;

	private Font textFont;
	private float rowHeight = 50;

	private Balloon balloon;
	private final List<Balloon> diffs = new ArrayList<>();

	protected BufferedImage balloonImage;

	class Balloon {
		String team;
		String problem;
	}

	public BalloonPresentation() {
		Balloon pi = new Balloon();
		pi.team = "Team 23";
		pi.problem = "A";
		diffs.add(pi);

		pi = new Balloon();
		pi.team = "Team 27";
		pi.problem = "C";
		diffs.add(pi);
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		final int dpi = 96;
		rowHeight = height / (float) LINES_PER_SCREEN;
		float inch = rowHeight / dpi;
		textFont = ICPCFont.getMasterFont().deriveFont(Font.BOLD, inch * 72f);
	}

	@Override
	public void init() {
		if (balloonImage != null)
			return;

		ClassLoader cl = getClass().getClassLoader();
		try {
			balloonImage = ImageIO.read(cl.getResource("images/balloon.png"));
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading images", e);
		}
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();
		if (diffs.isEmpty()) {
			balloon = null;
			return;
		}

		synchronized (diffs) {
			balloon = diffs.remove(0);
		}
	}

	@Override
	public long getRepeat() {
		return SECONDS_PER_BALLOON * 1000L;
	}

	@Override
	public void paint(Graphics2D g) {
		g.setFont(textFont);
		FontMetrics fm = g.getFontMetrics();

		if (balloon == null)
			return;

		int xx = (int) (getRepeatTimeMs() * UPDATE_STEPS / SECONDS_PER_BALLOON / 1000);

		g.setColor(Color.WHITE);

		if (xx < YY) {
			int x = (int) (Math.sin((YY - xx) / 4.0) * 45.0);

			int y = 824 * (YY - xx) / YY;
			g.drawImage(balloonImage, 90 + x, 200 + y, null);

			String s = balloon.problem;
			g.drawString(s, 147 - fm.stringWidth(s) / 2 + x, 280 + y);
		} else {
			g.drawImage(balloonImage, 90, 200, null);

			String s = balloon.problem;
			g.drawString(s, 147 - fm.stringWidth(s) / 2, 280);

			int y = 280;
			String[] st = tokenize(balloon.team, fm, width - 285);
			int size = st.length;
			for (int i = 0; i < size; i++) {
				g.drawString(st[i], 270, y);
				y += fm.getHeight();
			}
		}
	}

	public static String[] tokenize(String s2, FontMetrics fm, int max) {
		String s = s2;
		List<String> list = new ArrayList<>();

		int l = s.length();
		while (l > 0) {
			// look for maximum length possible string
			while (fm.stringWidth(s.substring(0, l)) > max) {
				l--;
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
				l = s.length();
			}
		}

		String[] st = new String[list.size()];
		list.toArray(st);
		return st;
	}

	public void handleRuns(ISubmission[] runs) {
		IContest contest = getContest();
		for (ISubmission run : runs) {
			if (contest.isSolved(run)) {
				Balloon b = new Balloon();
				b.problem = run.getProblemId();
				b.team = contest.getTeamById(run.getTeamId()).getActualDisplayName();
				synchronized (diffs) {
					diffs.add(b);
				}
			}
		}
	}
}