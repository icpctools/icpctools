package org.icpc.tools.presentation.contest.internal.presentations.clock;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IStartStatus;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

public class StatusCountdownPresentation extends CountdownPresentation {
	private static final int YES = 100;
	private static final int UNDECIDED = 50;
	private static final int NO = 0;
	private static final Movement SLIDER_ANIM = new Movement(150, 300);

	private Map<String, Animator> animMap = new HashMap<>();

	private static boolean showStatus = true;

	private Font font;

	public StatusCountdownPresentation() {
		// do nothing
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		font = ICPCFont.deriveFont(Font.BOLD, height * 36f / 10f / 96f);
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
		super.incrementTimeMs(dt);

		if (getContest() == null)
			return;

		IStartStatus[] startStatus = getContest().getStartStatuses();
		if (startStatus == null)
			return;

		for (IStartStatus ss : startStatus) {
			Animator an = animMap.get(ss.getLabel());
			if (an == null) {
				an = new Animator(0, SLIDER_ANIM);
				animMap.put(ss.getLabel(), an);
			}
			int status = ss.getStatus();
			if (status == 0)
				an.setTarget(NO);
			else if (status == 1)
				an.setTarget(UNDECIDED);
			else if (status == 2)
				an.setTarget(YES);

			an.incrementTimeMs(dt);
		}
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();

		if (getContest() == null)
			return;

		IStartStatus[] startStatus = getContest().getStartStatuses();
		if (startStatus == null)
			return;

		for (IStartStatus ss : startStatus) {
			Animator an = animMap.get(ss.getId());
			if (an == null) {
				an = new Animator(0, SLIDER_ANIM);
				animMap.put(ss.getId(), an);
			}
			int status = ss.getStatus();
			if (status == 0)
				an.reset(NO);
			else if (status == 1)
				an.reset(UNDECIDED);
			else if (status == 2)
				an.reset(YES);
		}
	}

	protected void drawStatus(Graphics2D g, int px, int py, IStartStatus ss) {
		// int o = 7;
		int o = height / 42;
		g.setColor(Color.GRAY);
		int x = px - o / 2;
		int y = py - o / 2;
		g.drawOval(x, y, o, o);
		g.drawOval(x + o * 3, y, o, o);
		// g.drawLine(px + o / 2, py, px + 50 - o / 2, py);

		double val = 50;
		Animator an = animMap.get(ss.getLabel());
		if (an != null)
			val = an.getValue();
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
		g.drawString(ss.getLabel(), px + o * 5, py + (int) (fm.getAscent() / 2.2f));
	}

	@Override
	public void paint(Graphics2D g) {
		super.paint(g);

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

		if (!showStatus)
			return;

		if (getContest() == null)
			return;

		IStartStatus[] startStatus = getContest().getStartStatuses();
		if (startStatus == null)
			return;

		int n = 0;
		g.setFont(font);
		for (IStartStatus ss : startStatus) {
			int x = width / 20 + (n % 3) * width * 4 / 14;
			int y = 40 + (n / 3) * (int) (height / 18f);
			drawStatus(g, x, y, ss);
			n++;
		}
	}
}