package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.ImageScaler;
import org.icpc.tools.presentation.core.Presentation;

public class MohamedPresentation extends Presentation {
	protected static final String[] text = new String[] { "Are", "you", "READY?" };

	protected Font font;

	protected BufferedImage image;
	protected BufferedImage scImage;

	@Override
	public void init() {
		if (image != null)
			return;

		ClassLoader cl = getClass().getClassLoader();
		try {
			image = ImageIO.read(cl.getResource("images/mohamed.jpg"));
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading images", e);
		}
	}

	@Override
	public long getRepeat() {
		return 11000L;
	}

	@Override
	public long getDelayTimeMs() {
		return 100;
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		if (image == null)
			return;

		if (scImage == null || scImage.getWidth() != width || scImage.getHeight() != height) {
			if (scImage != null)
				scImage.flush();
			scImage = ImageScaler.scaleImage(image, width, height);
		}
		g.drawImage(scImage, (width - scImage.getWidth()) / 2, (height - scImage.getHeight()) / 2, null);

		int border = width / 20;

		StringBuffer sb = new StringBuffer();
		sb.append(" ");
		for (String s : text)
			sb.append(s + " ");
		String fullText = sb.toString();

		if (font == null) {
			int n = 80;
			int w = 50000;

			do {
				font = ICPCFont.deriveFont(Font.BOLD, n);
				n -= 2;

				g.setFont(font);
				FontMetrics fm = g.getFontMetrics();
				w = fm.stringWidth(fullText);
			} while (w > width - border * 2);
		}
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		sb = new StringBuffer();
		sb.append(" ");
		int n = (int) Math.min(text.length, (getRepeatTimeMs() / 1000));
		for (int i = 0; i < n; i++)
			sb.append(text[i] + " ");

		g.setColor(Color.white);
		g.drawString(sb.toString(), (width - fm.stringWidth(fullText)) / 2, height - border);
	}
}