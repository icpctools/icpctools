package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

public class ICPCTeamPresentation extends AbstractICPCPresentation {
	private Font font;
	private BufferedImage image;
	private static final String text = "I C P C      ";

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		font = ICPCFont.deriveFont(Font.BOLD, height * 0.8f);
		image = getContest().getBannerImage((int) (width * 0.8), (int) (height * 0.3), true, true);
	}

	@Override
	public long getDelayTimeMs() {
		return 50;
	}

	@Override
	public void paint(Graphics2D g) {
		int h = 0;
		if (image != null) {
			int w = image.getWidth();
			h = image.getHeight();
			g.drawImage(image, (width - w) / 2, height - h - 20, null);
		}

		g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		int DT = 260;
		int ch = (int) (getTimeMs() % (text.length() * DT)) / DT;
		if (ch < 0 || ch >= text.length())
			return;

		String s = text.charAt(ch) + "";
		g.drawString(s, (width - fm.stringWidth(s)) / 2, (height - fm.getHeight()) / 2 + fm.getAscent());
	}
}