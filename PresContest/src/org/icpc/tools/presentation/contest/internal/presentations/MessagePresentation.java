package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

public class MessagePresentation extends AbstractICPCPresentation {
	private Font font;
	private static String message = null;
	private static List<String> messageList = null;
	private BufferedImage image;

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		final float dpi = 96;
		font = ICPCFont.deriveFont(Font.BOLD, height * 36f / 3f / dpi);
		image = getContest().getBannerImage((int) (width * 0.8), (int) (height * 0.3), true, true);

		messageList = null;
	}

	@Override
	public long getDelayTimeMs() {
		// paint twice a second, whether we need to or not
		return 500;
	}

	protected List<String> wrapText(String s, FontMetrics fm, int w) {
		List<String> list = new ArrayList<>();
		if (s == null || s.trim().length() == 0 || w < 10)
			return list;

		String ss = s;
		while (fm.stringWidth(ss) > w) {
			// find last space
			String t = ss;
			int ind = -1;
			do {
				ind = t.lastIndexOf(" ");
				if (ind <= 0) {
					// no space, just cut string wherever it lands and insert "-"
					ind = t.length() - 1;
					while (ind > 0 && fm.stringWidth(t.substring(0, ind) + "-") > w) {
						ind--;
					}
					// add dash
					ss = ss.substring(0, ind) + "-" + ss.substring(ind);
					ind++;
				} else {
					// try cutting at the space and see if it fits. if it doesn't, try the first part of
					// the string again
					t = t.substring(0, ind);
					if (fm.stringWidth(t) > w) {
						ind = -1;
					}
				}
			} while (ind < 0);

			// add first line into list and keep what's left
			list.add(ss.substring(0, ind));
			ss = ss.substring(ind).trim();
		}

		// add final line and return
		list.add(ss);
		return list;
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
		FontMetrics fm = g.getFontMetrics();
		synchronized (this) {
			if (messageList == null)
				messageList = wrapText(message, fm, (int) (width * 0.9));
		}

		int y = (int) ((height - h - 20f) / 2f - (fm.getHeight() + 10) * messageList.size() / 2f) + fm.getAscent();

		g.setColor(Color.WHITE);
		for (String s : messageList) {
			g.drawString(s, (width - fm.stringWidth(s)) / 2, y);
			y += fm.getHeight() + 10;
		}
	}

	@Override
	public void setProperty(String value) {
		synchronized (this) {
			message = value;
			messageList = null;
		}
	}
}