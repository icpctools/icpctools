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
import org.icpc.tools.presentation.core.Presentation;

public class DoNotTouchPresentation extends Presentation {
	protected static final String[] text = new String[] { "Do ", "not ", "touch", "ANYTHING!!" };

	protected Font font;

	protected static final float[] WORDS = new float[] { 1.0f, 2.2f, 3.4f, 4.6f };
	protected static final float[] MOUTH = new float[] { 1.0f, 2.2f, 3.4f, 4.6f, 4.95f, 5.3f };
	protected static final float[] MOUTH2 = new float[] { 0.4f, 0.4f, 0.4f, 0.2f, 0.2f, 0.2f };

	protected BufferedImage john, mouth;

	@Override
	public void init() {
		if (john != null)
			return;

		ClassLoader cl = getClass().getClassLoader();
		try {
			john = ImageIO.read(cl.getResource("images/john.png"));
			mouth = ImageIO.read(cl.getResource("images/johnMouth.png"));
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

	protected int numWords(long time) {
		if (time > 9000)
			return 0;

		for (int i = WORDS.length - 1; i >= 0; i--) {
			if (time > WORDS[i] * 1000f)
				return i + 1;
		}

		return 0;
	}

	protected boolean mouthOpen(long time) {
		for (int i = 0; i < MOUTH.length; i++) {
			if (time > (MOUTH[i] * 1000f) && time < (MOUTH[i] + MOUTH2[i]) * 1000f)
				return true;
		}

		return false;
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		if (john == null || mouth == null)
			return;

		int xx = width - john.getWidth();
		g.drawImage(john, xx, height - john.getHeight(), null);

		long time = getRepeatTimeMs() - 1000;
		if (mouthOpen(time)) {
			if (mouth != null)
				g.drawImage(mouth, xx + 220, height - john.getHeight() + 300, null);
		}

		if (font == null)
			font = ICPCFont.deriveFont(Font.BOLD, 55);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		int numWords = numWords(time);
		int w = 0;
		float x = xx - 10;
		float y = height - john.getHeight() + 150;
		g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);

		for (int i = 0; i < 3; i++)
			x -= fm.stringWidth(text[i]);

		while (w < numWords && w < 4) {
			if (w == 3) {
				x = xx - 10 - fm.stringWidth(text[3]);
				y += fm.getHeight();
			}

			g.drawString(text[w], x, y);
			x += fm.stringWidth(text[w]);
			w++;
		}
	}
}