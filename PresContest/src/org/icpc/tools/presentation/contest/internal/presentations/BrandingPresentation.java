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
	protected int header = 20;
	protected Font font;

	protected Presentation childPresentation;

	@Override
	public void init() {
		header = height / 20;
		final float dpi = 96;
		font = ICPCFont.deriveFont(Font.BOLD, height * 3.5f / dpi);

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

	@Override
	public void setProperty(String s) {
		super.setProperty(s);
		if (childPresentation != null)
			childPresentation.setProperty(s);
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
		final float dpi = 96;
		font = ICPCFont.deriveFont(Font.BOLD, height * 3.5f / dpi);

		Dimension dd = new Dimension(d.width, d.height - header);
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

	private static Color foregroundColor(Color background) {
		int r = background.getRed();
		int g = background.getGreen();
		int b = background.getBlue();
		// http://www.w3.org/TR/AERT#color-contrast
		int brightness = (int) Math.round(((r * 299) + (g * 587) + (b * 114)) / 1000.0);

		return brightness > 125 ? Color.black : Color.white;
	}

	@Override
	public void paint(Graphics2D g) {
		if (childPresentation != null) {
			Graphics2D gg = (Graphics2D) g.create(0, header, width, height - header);
			childPresentation.paint(gg);
			gg.dispose();
		}

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		IContest contest = getContest();
		if (contest != null) {
			g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);

			Color contestColor = contest.getColorVal();
			if (contestColor != null) {
				g.setColor(contestColor);
				g.fillRect(0, 0, width, header - height / 100);
				g.setColor(foregroundColor(contestColor));
			}

			g.drawString(contest.getActualFormalName(), (width - fm.stringWidth(contest.getActualFormalName())) / 2,
					fm.getAscent() + height / 160);
		}
	}
}