package org.icpc.tools.presentation.contest.internal.presentations.old;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.TextHelper;

public class FloorNamePresentation extends AbstractICPCPresentation {
	protected FloorMap floor;
	protected Font font;

	public FloorNamePresentation() {
		super();

		font = ICPCFont.getMasterFont().deriveFont(Font.PLAIN, 36);
	}

	@Override
	public void paint(Graphics2D g) {
		IContest contest = getContest();
		if (floor == null)
			floor = new FloorMap(contest);

		final ITeam team = contest.getTeams()[((int) (getTimeMs() / 2000f)) % contest.getNumTeams()];

		floor.drawFloor(g, new Rectangle(0, 0, width, height), new FloorMap.ScreenColors() {
			@Override
			public Color getDeskOutlineColor(String teamId) {
				if (team != null && team.getId().equals(teamId))
					return Color.WHITE;
				return Color.BLACK;
			}

			@Override
			public Color getDeskFillColor(String teamId) {
				if (team != null && team.getId().equals(teamId))
					return Color.BLACK;
				return Color.WHITE;
			}
		}, false);

		if (team != null) {
			g.setFont(font);
			g.setColor(Color.WHITE);
			FontMetrics fm = g.getFontMetrics();
			TextHelper text = new TextHelper(g, team.getActualDisplayName());
			text.draw((width - text.getWidth()) / 2, fm.getAscent() + 20);
		}
	}
}