package org.icpc.tools.presentation.contest.internal.presentations.test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.core.Presentation;

public class FPSPresentation extends Presentation {
	private static final Color darkGray = new Color(32, 32, 32);
	private static final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
	private static final int AVG = 15;
	private static final int NUM_FLIPS = 10;

	private Font largeFont;
	private List<Long> times = new ArrayList<>();
	private long count;

	static {
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(1);
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		largeFont = ICPCFont.deriveFont(Font.BOLD, height * 0.25f * 72f / 96f);
	}

	@Override
	public void incrementTimeMs(long dt) {
		super.incrementTimeMs(dt);

		while (times.size() > AVG)
			times.remove(0);

		long time = getRepeatTimeMs();
		times.add(time);
	}

	private double getFPS() {
		int size = times.size();
		if (size < AVG)
			return 0;

		long time = getRepeatTimeMs();
		long last = times.get(size - AVG);
		return Math.min(60, (1000.0 / ((double) (time - last) / AVG)));
	}

	private void drawFlipBoxes(Graphics2D g) {
		int fl = Math.min(width, height) / 16;
		g.setColor(Color.GRAY);
		for (int i = 0; i < NUM_FLIPS; i++) {
			int bb = (int) Math.pow(2, i + 1);
			int y = height / 2 + (NUM_FLIPS / 2 - i - 1) * fl;
			if (count % bb == 0)
				g.fillRect(10, y, fl, fl);
			if (count % bb >= bb / 2)
				g.fillRect(width - fl - 10, y, fl, fl);
		}
		g.setColor(Color.WHITE);
		for (int i = 0; i < NUM_FLIPS; i++) {
			int y = height / 2 + (NUM_FLIPS / 2 - i - 1) * fl;
			g.drawRect(10, y, fl, fl);
			g.drawRect(width - fl - 10, y, fl, fl);
		}
		count++;
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		drawFlipBoxes(g);

		int r = Math.min(width, height) / 2 - 30;
		int w = r / 5;
		double fps = getFPS();

		g.setColor(darkGray);
		g.fillArc(width / 2 - r, height / 2 - r, r * 2, r * 2, 225, -270);
		g.setColor(Color.BLACK);
		g.fillArc(width / 2 - r + w, height / 2 - r + w, (r - w) * 2, (r - w) * 2, 225, -270);

		// 5s - small ticks
		g.setColor(Color.GRAY);
		int d = w / 4;
		for (int i = 5; i <= 60; i += 10) {
			double rad = Math.toRadians(225 - i * 270 / 60);
			int x1 = (int) (width / 2 + Math.cos(rad) * (r - d));
			int y1 = (int) (height / 2 - Math.sin(rad) * (r - d));
			int x2 = (int) (width / 2 + Math.cos(rad) * (r - w + d));
			int y2 = (int) (height / 2 - Math.sin(rad) * (r - w + d));
			g.drawLine(x1, y1, x2, y2);
		}
		// 10s - bigger ticks
		g.setColor(Color.WHITE);
		g.setStroke(new BasicStroke(2));
		d = 1;
		for (int i = 0; i <= 60; i += 10) {
			double rad = Math.toRadians(225 - i * 270 / 60);
			int x1 = (int) (width / 2 + Math.cos(rad) * (r - d));
			int y1 = (int) (height / 2 - Math.sin(rad) * (r - d));
			int x2 = (int) (width / 2 + Math.cos(rad) * (r - w + d));
			int y2 = (int) (height / 2 - Math.sin(rad) * (r - w + d));
			g.drawLine(x1, y1, x2, y2);
		}

		g.setColor(Color.RED);
		g.setStroke(new BasicStroke(4));
		double rad = Math.toRadians(225 - fps * 270 / 60);
		int x = (int) (width / 2 + Math.cos(rad) * r);
		int y = (int) (height / 2 - Math.sin(rad) * r);
		g.drawLine(x, y, width / 2, height / 2);

		g.setColor(Color.WHITE);
		g.setFont(largeFont);
		FontMetrics fm = g.getFontMetrics();
		String fpsStr = nf.format(fps);
		g.drawString(fpsStr, (width - fm.stringWidth(fpsStr)) / 2, height / 2 + r);
	}
}