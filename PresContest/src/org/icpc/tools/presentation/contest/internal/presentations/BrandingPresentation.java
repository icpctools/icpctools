package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.core.Presentation;

public class BrandingPresentation extends AbstractICPCPresentation {
	private int border = 10;
	private int topBorder = 10;
	protected int header = 20;
	protected Font font;

	protected Presentation childPresentation;

	@Override
	public void init() {
		if (childPresentation != null)
			childPresentation.init();
	}

	public void setChildPresentation(Presentation p) {
		childPresentation = p;
	}

	@Override
	public long getDelayTimeMs() {
		if (childPresentation == null)
			return super.getDelayTimeMs();
		return childPresentation.getDelayTimeMs();
	}

	@Override
	public long getRepeat() {
		if (childPresentation == null)
			return super.getRepeat();
		return childPresentation.getRepeat();
	}

	@Override
	public long getRepeatTimeMs() {
		if (childPresentation == null)
			return super.getRepeatTimeMs();
		return childPresentation.getRepeatTimeMs();
	}

	private static String getPresentationKey(String className) {
		int ind = className.lastIndexOf(".");
		return "property[" + className.substring(ind + 1) + "|" + className.hashCode() + "]";
	}

	@Override
	public void setProperty(String s) {
		super.setProperty(s);
		if (childPresentation != null) {
			String k = getPresentationKey(childPresentation.getClass().getName()) + ":";
			if (s.startsWith(k)) {
				childPresentation.setProperty(s.substring(k.length()));
			}
		}
	}

	@Override
	public void setRepeatTimeMs(long l) {
		super.setRepeatTimeMs(l);
		if (childPresentation != null)
			childPresentation.setRepeatTimeMs(l);
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		header = height / 20;
		border = height / 150;
		topBorder = height / 300;
		final float dpi = 96;
		font = ICPCFont.deriveFont(Font.BOLD, height * 3.5f / dpi);

		Dimension dd = new Dimension(d.width - border * 2, d.height - header - topBorder - border);
		if (childPresentation != null)
			childPresentation.setSize(dd);
	}

	@Override
	public void setTimeMs(long ms) {
		super.setTimeMs(ms);
		if (childPresentation != null)
			childPresentation.setTimeMs(ms);
	}

	@Override
	public void aboutToShow() {
		if (childPresentation != null)
			childPresentation.aboutToShow();
	}

	@Override
	public void setContest(IContest newContest) {
		super.setContest(newContest);
		if (childPresentation != null && childPresentation instanceof AbstractICPCPresentation)
			((AbstractICPCPresentation) childPresentation).setContest(newContest);
	}

	@Override
	public void dispose() {
		if (childPresentation != null)
			childPresentation.dispose();
	}

	public static void drawStringWithOutline(Graphics2D g, String s, int x, int y) {
		g.setColor(Color.BLACK);
		g.drawString(s, x - 1, y - 1);
		g.drawString(s, x - 1, y + 1);
		g.drawString(s, x + 1, y - 1);
		g.drawString(s, x + 1, y + 1);

		g.setColor(Color.WHITE);
		g.drawString(s, x, y);
	}

	@Override
	public void paint(Graphics2D g) {
		if (childPresentation != null) {
			Graphics2D gg = (Graphics2D) g.create(border, header + topBorder, width - border * 2,
					height - header - topBorder - border);
			childPresentation.paint(gg);
			gg.dispose();
		}

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		if (font != null)
			g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		IContest contest = getContest();
		if (contest != null) {
			g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);

			Color contestColor = contest.getColorVal();
			if (contestColor != null) {
				g.setColor(contestColor);
				g.fillRect(0, 0, width, header - topBorder);

				if (border > 0) {
					g.fillRect(0, header - topBorder, border, height - header + topBorder);
					g.fillRect(width - border, header - topBorder, border, height - header + topBorder);
					g.fillRect(border, height - border, width - border * 2, border);
				}
			}

			String name = contest.getActualFormalName();
			if (name != null)
				drawStringWithOutline(g, name, (width - fm.stringWidth(name)) / 2, fm.getAscent() + height / 140);
		}
	}
}