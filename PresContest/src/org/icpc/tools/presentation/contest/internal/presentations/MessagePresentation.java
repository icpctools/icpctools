package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.TextHelper;

public class MessagePresentation extends AbstractICPCPresentation {
	private Font font;
	private static String message = null;
	private TextHelper text;
	private BufferedImage image;

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		final float dpi = 96;
		font = ICPCFont.deriveFont(Font.BOLD, height * 36f / 3f / dpi);
		image = getContest().getBannerImage((int) (width * 0.8), (int) (height * 0.25), getModeTag(), true, true);

		text = null;
	}

	@Override
	public long getDelayTimeMs() {
		// paint twice a second, whether we need to or not
		return 500;
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int h = 0;
		if (image != null) {
			int w = image.getWidth();
			h = image.getHeight();
			g.drawImage(image, (width - w) / 2, height - h - 20, null);
		}

		g.setFont(font);
		g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
		TextHelper curText = text;
		if (curText == null) {
			curText = new TextHelper(g, false);
			curText.addICPCString(getContest(), message);

			TextHelper.Layout layout = new TextHelper.Layout();
			layout.align = TextHelper.Alignment.CENTER;
			layout.wrapWidth = (int) (width * 0.9);
			curText.layout(layout);
			text = curText;
		}

		int y = (int) ((height - h - text.getHeight()) / 2f) - 10;
		curText.setGraphics(g);
		curText.draw((int) (width * 0.05), y);
	}

	public void setMessage(String value) {
		synchronized (this) {
			message = value;
			text = null;
		}
	}

	@Override
	public void setProperty(String value) {
		super.setProperty(value);
		if (value == null || value.startsWith("lightMode:"))
			return;

		setMessage(value);
	}
}