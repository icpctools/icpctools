package org.icpc.tools.presentation.core.transition;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.Transition;

public class SlidesTransition extends Transition {
	private Rectangle r1, r2;
	private double sc;

	@Override
	public TimeOverlap getTimeOverlap() {
		return TimeOverlap.FULL;
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		int w = width / 2;
		int h = height / 2;
		r1 = new Rectangle(0, h / 2, w, h);
		r2 = new Rectangle(w, h / 2, w, h);

		/*int w = width * 2 / 3;
		int h = height * 2 / 3;
		r1 = new Rectangle(0, 0, w, h);
		r2 = new Rectangle(width - w, height - h, w, h);*/

		sc = w / (double) width;
	}

	@Override
	public void paint(Graphics2D g, double x, Presentation p1, Presentation p2) {
		if (x <= 0.5) {
			// paint p2 small
			Graphics2D gg = (Graphics2D) g.create(r2.x, r2.y, r2.width, r2.height);
			gg.scale(sc, sc);
			g.setClip(0, 0, width, height);
			p2.paint(gg);
			gg.dispose();

			g.setColor(Color.DARK_GRAY);
			g.drawRect(r2.x, r2.y, r2.width, r2.height);

			// paint p1 shrinking
			float f = SmoothUtil.smooth(x * 2.0);
			g.translate((int) (r1.x * f), (int) (r1.y * f));
			double s = 1.0 - (1.0 - sc) * f;
			g.scale(s, s);
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.DARK_GRAY);
			g.drawRect(0, 0, width, height);
			g.setClip(1, 1, width - 2, height - 2);
			p1.paint(g);
		}

		if (x > 0.5) {
			// paint p1 small
			Graphics2D gg = (Graphics2D) g.create(r1.x, r1.y, r1.width, r1.height);
			gg.scale(sc, sc);
			g.setClip(0, 0, width, height);
			p1.paint(gg);
			gg.dispose();

			g.setColor(Color.DARK_GRAY);
			g.drawRect(r1.x, r1.y, r1.width, r1.height);

			// paint p2 growing
			float f = 1f - SmoothUtil.smooth((x - 0.5) * 2.0);
			g.translate((int) (r2.x * f), (int) (r2.y * f));
			double s = sc + (1.0 - sc) * (1.0 - f);
			g.scale(s, s);
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.DARK_GRAY);
			g.drawRect(0, 0, width, height);
			g.setClip(1, 1, width - 2, height - 2);
			p2.paint(g);
		}
	}
}