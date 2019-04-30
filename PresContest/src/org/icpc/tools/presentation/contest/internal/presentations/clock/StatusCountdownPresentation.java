package org.icpc.tools.presentation.contest.internal.presentations.clock;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ICountdown;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

public class StatusCountdownPresentation extends CountdownPresentation {
	private static final int YES = 100;
	private static final int NO = 0;
	private static final Movement SLIDER_ANIM = new Movement(150, 300);

	static class Status {
		String name;
		Animator m = new Animator(0, SLIDER_ANIM);

		public Status(String name) {
			this.name = name;
		}
	}

	private final Status[] status = new Status[] { new Status("Security"), new Status("Sysops"),
			new Status("Contest Control"), new Status("Judges"), new Status("Network Control"), new Status("Marshalls"),
			new Status("Operations"), new Status("Executive Director"), new Status("Contest Director") };

	private static boolean showStatus = true;

	protected Font font;

	public StatusCountdownPresentation() {
		//
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		font = ICPCFont.getMasterFont().deriveFont(Font.BOLD, height * 36f / 10f / 96f);
	}

	@Override
	public void setProperty(String value) {
		if (value == null || value.isEmpty())
			return;
		if ("statusOn".equals(value))
			showStatus = true;
		else if ("statusOff".equals(value))
			showStatus = false;
		else
			super.setProperty(value);
	}

	@Override
	public void incrementTimeMs(long dt) {
		try {
			if (getContest() != null) {
				ICountdown countdown = getContest().getCountdown();
				if (countdown != null) {
					boolean[] b = countdown.getStatus();
					for (int i = 0; i < b.length; i++) {
						if (b[i])
							status[i].m.setTarget(YES);
						else
							status[i].m.setTarget(NO);
					}
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Problem updating status targets: " + e.getMessage());
		}

		for (Status s : status)
			s.m.incrementTimeMs(dt);

		super.incrementTimeMs(dt);
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();

		try {
			if (getContest() != null) {
				ICountdown countdown = getContest().getCountdown();
				if (countdown != null) {
					boolean[] b = countdown.getStatus();
					for (int i = 0; i < b.length; i++) {
						if (b[i])
							status[i].m.reset(YES);
						else
							status[i].m.reset(NO);
					}
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Problem updating status targets: " + e.getMessage());
		}
	}

	protected void drawStatus(Graphics2D g, int px, int py, Status s) {
		// int o = 7;
		int o = height / 42;
		g.setColor(Color.GRAY);
		int x = px - o / 2;
		int y = py - o / 2;
		g.drawOval(x, y, o, o);
		g.drawOval(x + o * 3, y, o, o);
		// g.drawLine(px + o / 2, py, px + 50 - o / 2, py);

		double val = s.m.getValue();
		if (val < 5)
			g.setColor(Color.RED);
		else if (val > 95)
			g.setColor(Color.GREEN);
		else
			g.setColor(Color.YELLOW);

		x = px - o / 2 + (int) (val * o * 3 / 100);
		y = py - o / 2;
		g.fillOval(x, y, o, o);
		g.setColor(Color.WHITE);
		g.drawOval(x, y, o, o);

		FontMetrics fm = g.getFontMetrics();
		g.drawString(s.name, px + o * 5, py + (int) (fm.getAscent() / 2.2f));
	}

	@Override
	public void paint(Graphics2D g) {
		super.paint(g);

		if (!showStatus)
			return;

		int n = 0;
		int px = width / 20;
		int py = 40;
		g.setFont(font);
		for (Status s : status) {
			drawStatus(g, px, py, s);
			py += (int) (height / 18f);
			n++;
			if (n == 3 || n == 6) {
				px += width * 4 / 14;
				py = 40;
			}
		}

		g.setFont(font);
		String s = "Status: Go";
		IContest contest = getContest();
		if (contest != null) {
			Long startStatus = contest.getStartStatus();
			if (startStatus == null)
				s = "Start time undefined";
			else if (startStatus < 0)
				s = "Status: Paused";
		}

		FontMetrics fm = g.getFontMetrics();
		g.drawString(s, (width - fm.stringWidth(s)) / 2, (int) ((height + height / 2.5f) / 2f) + fm.getHeight() * 2);
	}
}