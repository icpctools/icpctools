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
import org.icpc.tools.presentation.contest.internal.TextHelper;

public class ContestFloorPresentation extends AbstractICPCPresentation {
	private static final int MS_PER_TEAM = 2000;
	private static final Color LIGHTER_GRAY = new Color(240, 240, 240);
	private static final Color DARKISH_GRAY = new Color(96, 96, 96);
	private static final Color DARKER_GRAY = new Color(40, 40, 40);
	private static final Color DARKEST_GRAY = new Color(25, 25, 25);

	protected FloorMap floor;

	protected Font font;

	public ContestFloorPresentation() {
		super();
	}

	@Override
	public void init() {
		font = ICPCFont.deriveFont(Font.PLAIN, 36);
	}

	@Override
	public long getRepeat() {
		IContest contest = getContest();
		if (contest == null)
			return 0;
		return contest.getNumTeams() * MS_PER_TEAM;
	}

	@Override
	public void paintImpl(Graphics2D g) {
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
			public Color getTeamAreaFillColor() {
				return isLightMode() ? Color.WHITE : Color.BLACK;
			}

			@Override
			public Color getTeamAreaOutlineColor() {
				return isLightMode() ? Color.LIGHT_GRAY : Color.GRAY;
			}

			@Override
			public Color getSeatFillColor() {
				return isLightMode() ? Color.LIGHT_GRAY : DARKEST_GRAY;
			}

			@Override
			public Color getSeatOutlineColor() {
				return isLightMode() ? Color.GRAY : DARKISH_GRAY;
			}

			@Override
			public Color getTextColor() {
				return isLightMode() ? Color.BLACK : Color.WHITE;
			}

			@Override
			public Color getDeskOutlineColor(String teamId) {
				return isLightMode() ? Color.DARK_GRAY : Color.GRAY;
			}

			@Override
			public Color getDeskFillColor(String teamId) {
				return isLightMode() ? LIGHTER_GRAY : DARKER_GRAY;
			}
		});

		if (team != null) {
			g.setFont(font);
			g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
			String s = team.getActualDisplayName();
			FontMetrics fm = g.getFontMetrics();
			int hh = fm.getHeight() + 10;
			TextHelper text = new TextHelper(g);
			IOrganization org = contest.getOrganizationById(team.getOrganizationId());
			if (org != null) {
				BufferedImage img = org.getLogoImage(hh, hh, true, true);
				if (img != null) {
					text.addImage(img);
					text.addSpacer(10);
				}
			}

			text.addString(s);
			text.drawFit(20, 20 + (hh - text.getHeight()) / 2, width - 40);
		}
	}
}