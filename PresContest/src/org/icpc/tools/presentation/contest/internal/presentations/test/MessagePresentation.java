package org.icpc.tools.presentation.contest.internal.presentations.test;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import org.icpc.tools.presentation.core.Presentation;

public class MessagePresentation extends Presentation {
	protected String text = "This is a test";
	protected Font font;

	public MessagePresentation() {
		// do nothing
	}

	@Override
	public void setProperty(String value) {
		if (value == null || value.startsWith("lightMode:"))
			return;

		this.text = value;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public void paint(Graphics2D g) {
		g.setColor(Color.RED);
		int xx = (int) (40 * getTimeMs()) / 1000 % 1000;
		g.fillRect(50 + xx, 50, 100, 600);

		if (font == null)
			font = new Font("Arial", Font.BOLD, 25);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth(text);
		float x = width / 2f - w / 2f;
		float y = height / 2f + fm.getDescent();

		g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
		g.drawString(text, x, y);
	}
}