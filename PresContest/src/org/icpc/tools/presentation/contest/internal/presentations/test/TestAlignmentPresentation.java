package org.icpc.tools.presentation.contest.internal.presentations.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.core.Presentation;

public class TestAlignmentPresentation extends Presentation {
	private Font largeFont;
	private String name = "";

	@Override
	public long getDelayTimeMs() {
		// paint every 5s
		return 5000;
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		largeFont = ICPCFont.deriveFont(Font.BOLD, height * 0.25f * 72f / 96f);
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		int NUM = 40;

		g.setColor(isLightMode() ? Color.LIGHT_GRAY : Color.DARK_GRAY);
		for (int i = 0; i < NUM; i++) {
			for (int j = 0; j < NUM; j++) {
				int x1 = ((width - 1) * i / NUM);
				int y1 = ((height - 1) * j / NUM);
				int x2 = ((width - 1) * (i + 1) / NUM);
				int y2 = ((height - 1) * (j + 1) / NUM);
				g.drawRect(x1, y1, x2 - x1, y2 - y1);
			}
		}

		NUM = 20;
		g.setColor(isLightMode() ? Color.DARK_GRAY : Color.LIGHT_GRAY);
		for (int i = 0; i < NUM; i++) {
			for (int j = 0; j < NUM; j++) {
				int x1 = ((width - 1) * i / NUM);
				int y1 = ((height - 1) * j / NUM);
				int x2 = ((width - 1) * (i + 1) / NUM);
				int y2 = ((height - 1) * (j + 1) / NUM);
				g.drawRect(x1, y1, x2 - x1, y2 - y1);
			}
		}

		NUM = 10;
		g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
		for (int i = 0; i < NUM; i++) {
			for (int j = 0; j < NUM; j++) {
				int x1 = ((width - 1) * i / NUM);
				int y1 = ((height - 1) * j / NUM);
				int x2 = ((width - 1) * (i + 1) / NUM);
				int y2 = ((height - 1) * (j + 1) / NUM);
				g.drawRect(x1, y1, x2 - x1, y2 - y1);
			}
		}

		FontMetrics fm = g.getFontMetrics();
		for (int i = 0; i < NUM; i++) {
			for (int j = 0; j < NUM; j++) {
				int x = (int) ((width - 1) * (i + 0.5f) / NUM);
				int y = (int) ((height - 1) * (j + 0.5f) / NUM);
				String s = i + "-" + j;
				g.setColor(isLightMode() ? Color.WHITE : Color.BLACK);
				g.fillRect(x - fm.stringWidth(s) / 2, y - fm.getAscent() / 2, fm.stringWidth(s), fm.getAscent());
				g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
				g.drawString(s, x - fm.stringWidth(s) / 2, y + fm.getAscent() / 2);
			}
		}

		g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
		g.setFont(largeFont);
		fm = g.getFontMetrics();

		g.drawString(name, (width - fm.stringWidth(name)) / 2, (height - fm.getHeight()) / 2 + fm.getAscent());
	}

	@Override
	public void setProperty(String value) {
		super.setProperty(value);
		if (value != null && value.startsWith("name:"))
			this.name = value.substring(5);
	}
}