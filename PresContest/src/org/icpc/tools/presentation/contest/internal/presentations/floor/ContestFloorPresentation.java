package org.icpc.tools.presentation.contest.internal.presentations.floor;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

public class ContestFloorPresentation extends AbstractICPCPresentation {
	private static final int MS_PER_TEAM = 2000;

	protected FloorMap floor;

	protected Font font;

	public ContestFloorPresentation() {
		super();
	}

	@Override
	public void init() {
		font = ICPCFont.getMasterFont().deriveFont(Font.PLAIN, 36);
	}

	@Override
	public long getRepeat() {
		IContest contest = getContest();
		if (contest == null)
			return 0;
		return contest.getNumTeams() * MS_PER_TEAM;
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		IContest contest = getContest();
		if (contest == null)
			return;
		if (floor == null)
			floor = new FloorMap(contest);
		ITeam[] teams = contest.getTeams();
		ContestUtil.sort(teams);
		ITeam team = teams[(int) (getRepeatTimeMs() / MS_PER_TEAM)];
		int h = 0;
		Rectangle r = new Rectangle(0, h, width, height - h);
		int mx = Math.min(width / 10, height / 10);
		// r = new Rectangle(width / 2, h, width / 2, height - h);
		floor.drawFloor(g, r, new FloorMap.ScreenColors() {
			@Override
			public BufferedImage getTeamLogo(String teamId) {
				if (team != null && team.getId().equals(teamId)) {
					ITeam team2 = contest.getTeamById(teamId);
					if (team2 == null)
						return null;
					IOrganization org = contest.getOrganizationById(team2.getOrganizationId());
					if (org == null)
						return null;
					return org.getLogoImage(mx, mx, true, true);
				}
				return null;
			}

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
				return Color.GRAY;
			}
		}, false);

		if (team != null) {
			g.setFont(font);
			g.setColor(Color.WHITE);
			String s = team.getActualDisplayName();
			FontMetrics fm = g.getFontMetrics();
			int x = 0;
			IOrganization org = contest.getOrganizationById(team.getOrganizationId());
			if (org != null) {
				BufferedImage img = org.getLogoImage(fm.getHeight() + 10, fm.getHeight() + 10, true, true);
				if (img != null) {
					x = img.getWidth();
					g.drawImage(img, 10, 20 + (fm.getHeight() - img.getHeight()) / 2, null);
				}
			}

			g.drawString(s, x + 20, fm.getAscent() + 20);
		}
	}
}