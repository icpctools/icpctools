package org.icpc.tools.presentation.contest.internal.presentations.clock;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.presentation.contest.internal.ICPCColors;

public class PolarCountdownPresentation extends CountdownPresentation {
	private static final int NUM_ARCS = 3;
	private static final int ARC_GAP = 8;
	private Rectangle[] arcDim = new Rectangle[NUM_ARCS];
	private final Color[] arcColors = new Color[] { ICPCColors.BLUE, ICPCColors.YELLOW, ICPCColors.RED };
	private int arcWidth = 25;

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		verticalOffset = d.height / 15;

		int diam = Math.min(d.width, d.height) - verticalOffset - 5;

		for (int i = 0; i < NUM_ARCS; i++) {
			int r = diam * (NUM_ARCS + 1 - i) / (NUM_ARCS + 1) / 2;
			arcDim[i] = new Rectangle(d.width / 2 - r, d.height / 2 - r - verticalOffset, r * 2, r * 2);
		}
		arcWidth = diam / (NUM_ARCS + 1) / 2 - ARC_GAP;
	}

	@Override
	public Color getTextBackgroundColor() {
		return null;
	}

	@Override
	public void paint(Graphics2D g) {
		IContest contest = getContest();
		if (contest == null)
			return;

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		Long l = getClock();
		if (l != null) {
			int[] angle = new int[NUM_ARCS];

			int s = (int) Math.floor(l.longValue() / 1000.0);
			int maxHours = contest.getDuration() / 3600000;
			if (maxHours != 0) {
				if (s < 0) {
					angle[0] = -(59 + (s % 120)) * 6;
					angle[1] = -(60 + ((s / 60) % 120)) * 6;
					angle[2] = -(maxHours + ((s / 3600) % maxHours)) * (360 / maxHours);
				} else {
					angle[0] = -(s % 120) * 6;
					angle[1] = -((s / 60) % 120) * 6;
					angle[2] = -((s / 3600) % maxHours) * (360 / maxHours);
				}
			}

			for (int i = 0; i < NUM_ARCS; i++)
				drawArc(g, i, angle[NUM_ARCS - i - 1], arcColors[i]);
		}

		paintClock(g);
	}

	protected void paintClock(Graphics2D g) {
		super.paint(g);
	}

	private void drawArc(Graphics2D g, int arc, int ang, Color c) {
		g.setColor(c);
		if (ang >= -360)
			g.fillArc(arcDim[arc].x, arcDim[arc].y, arcDim[arc].width, arcDim[arc].height, 90, ang);
		else
			g.fillArc(arcDim[arc].x, arcDim[arc].y, arcDim[arc].width, arcDim[arc].height, 450 + ang, -720 - ang);

		g.setColor(Color.BLACK);
		g.fillOval(arcDim[arc].x + arcWidth, arcDim[arc].y + arcWidth, arcDim[arc].width - arcWidth * 2,
				arcDim[arc].height - arcWidth * 2);
	}
}