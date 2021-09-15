package org.icpc.tools.resolver;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.icpc.tools.contest.model.resolver.ResolutionUtil.TeamListStep;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

public class TeamListSidePresentation extends AbstractICPCPresentation {
	private Font titleFont;
	private Font subTitleFont;

	private TeamListStep step;

	@Override
	public void init() {
		float inch = height * 72f / 96f / 4.5f;
		titleFont = ICPCFont.deriveFont(Font.BOLD, inch);
		subTitleFont = ICPCFont.deriveFont(Font.BOLD, inch * 0.5f);
	}

	public void setTeams(TeamListStep step) {
		this.step = step;
	}

	@Override
	public void paint(Graphics2D g2) {
		if (step == null)
			return;

		// draw title across the top
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setFont(titleFont);
		g2.setColor(Color.WHITE);
		FontMetrics fm = g2.getFontMetrics();
		g2.drawString(step.title, (width - fm.stringWidth(step.title)) / 2, height / 2 - fm.getDescent() - 10);

		if (step.subTitle != null) {
			g2.setFont(subTitleFont);
			fm = g2.getFontMetrics();
			g2.drawString(step.subTitle, (width - fm.stringWidth(step.subTitle)) / 2, height / 2 + fm.getAscent());
		}
	}
}