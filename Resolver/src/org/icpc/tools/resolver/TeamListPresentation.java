package org.icpc.tools.resolver;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.scoreboard.AbstractScoreboardPresentation;
import org.icpc.tools.presentation.contest.internal.scoreboard.AbstractScoreboardPresentation.SelectType;
import org.icpc.tools.resolver.ResolutionUtil.TeamListStep;

public class TeamListPresentation extends AbstractICPCPresentation {
	private static final int TEAM_SPACING = 15;
	private static final int COLUMN_GAP = 10;
	private static final int GAP = 8;
	private static final int ROWS_PER_SCREEN = 15; // multiply by 2 columns for # of teams shown

	private Animator scroll = new Animator(0, new Movement(0.4, 0.5));
	private boolean scrollPause = false;

	class TeamInfo {
		private String teamName;
		private String[] name;
		private String id;
	}

	private Map<String, BufferedImage> logos = new HashMap<>();

	private TeamListStep step;
	private TeamInfo[] teams;
	private Map<String, AbstractScoreboardPresentation.SelectType> selections;

	private Font teamFont;
	private Font titleFont;
	private Font subTitleFont;
	private int rowHeight;
	private double bottom = 0;

	@Override
	public void init() {
		rowHeight = (height - 40) / ROWS_PER_SCREEN;
		float dpi = 96;
		float inch = height * 72f / dpi / 10f;
		Font masterFont = ICPCFont.getMasterFont();
		teamFont = masterFont.deriveFont(Font.PLAIN, inch * 0.62f);
		titleFont = masterFont.deriveFont(Font.BOLD, inch);
		subTitleFont = masterFont.deriveFont(Font.BOLD, inch * 0.5f);
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();
		scroll.resetToTarget();
	}

	protected void loadCache(final List<String> teamIds) {
		rowHeight = (height - 40) / ROWS_PER_SCREEN;
		// load logos in background
		execute(new Runnable() {
			@Override
			public void run() {
				for (String id : teamIds) {
					ITeam team = getContest().getTeamById(id);
					if (team != null) {
						IOrganization org = getContest().getOrganizationById(team.getOrganizationId());
						if (org != null) {
							BufferedImage img = org.getLogoImage(rowHeight, rowHeight, true, true);
							if (img != null)
								logos.put(id, img);
						}
					}
				}
			}
		});
	}

	public void setTeams(TeamListStep step) {
		this.step = step;

		if (step == null) {
			teams = null;
			selections = new HashMap<>();
			return;
		}

		final int size = step.teams.length;
		teams = new TeamInfo[size];
		for (int i = 0; i < size; i++) {
			teams[i] = new TeamInfo();
			teams[i].teamName = step.teams[i].getActualDisplayName();
			teams[i].id = step.teams[i].getId();
		}

		selections = step.selections;
	}

	/**
	 * Set the scroll to top or bottom.
	 *
	 * @param top
	 */
	public void scrollIt(boolean top) {
		if (scroll == null)
			return;

		if (top)
			scroll.setTarget(0);
		else
			scroll.setTarget(bottom);
	}

	@Override
	public void incrementTimeMs(long dt) {
		if (!scrollPause)
			scroll.incrementTimeMs(dt);
		super.incrementTimeMs(dt);
	}

	@Override
	public void paint(Graphics2D g2) {
		if (teams == null)
			return;

		// draw title across the top
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setFont(titleFont);
		g2.setColor(Color.WHITE);
		FontMetrics fm = g2.getFontMetrics();
		g2.drawString(step.title, (width - fm.stringWidth(step.title)) / 2, fm.getAscent());

		int headerHeight = fm.getHeight();
		if (step.subTitle != null) {
			g2.setFont(subTitleFont);
			fm = g2.getFontMetrics();
			g2.drawString(step.subTitle, (width - fm.stringWidth(step.subTitle)) / 2, headerHeight + fm.getAscent());
			headerHeight += fm.getHeight();
		}

		g2.drawLine(0, headerHeight - 1, width, headerHeight - 1);

		Graphics2D g = (Graphics2D) g2.create();
		g.setClip(0, headerHeight, width, height - headerHeight);
		int scr = (int) (scroll.getValue() * rowHeight);
		g.translate(0, headerHeight - scr);

		// draw team list
		int y = height - headerHeight + TEAM_SPACING;
		g.setFont(teamFont);
		g.setColor(Color.WHITE);
		fm = g.getFontMetrics();
		int size = teams.length;
		int x = 0;
		int yy = 0;
		for (int i = 0; i < size; i++) {
			TeamInfo t = teams[i];

			if (t.name == null)
				t.name = splitString(g, t.teamName, width / 2 - TEAM_SPACING - rowHeight - COLUMN_GAP);

			int hh = Math.max(t.name.length * fm.getHeight(), rowHeight);
			if (y - scr + hh > 0 && y - scr < height - headerHeight) {
				BufferedImage img = logos.get(t.id);
				int h = 0;
				if (img != null) {
					h = hh / 2 - fm.getHeight() * (t.name.length - 1) / 2;
					g.drawImage(img, x + (rowHeight - img.getWidth()) / 2, (hh - img.getHeight()) / 2 + y, null);
				}

				AbstractScoreboardPresentation.SelectType sel = selections.get(t.id);
				if (sel != null) {
					if (sel == SelectType.FTS)
						g.setColor(ICPCColors.FIRST_TO_SOLVE_COLOR);
					else if (sel == SelectType.FTS_HIGHLIGHT)
						g.setColor(ICPCColors.SOLVED_COLOR.brighter());
					else
						g.setColor(ICPCColors.SELECTION_COLOR);
				} else
					g.setColor(Color.WHITE);

				for (int j = 0; j < t.name.length; j++) {
					if (j == 0)
						g.drawString(t.name[j], x + rowHeight + GAP * 2, y + fm.getAscent() / 2 + h);
					else
						g.drawString(t.name[j], x + rowHeight * 3 / 2 + GAP * 2,
								y + fm.getAscent() / 2 + fm.getHeight() * j + h);
				}
			}

			yy = Math.max(yy, hh);

			if (x > 1) {
				x = 0;
				y += yy + TEAM_SPACING;
				yy = 0;
			} else
				x = (width + COLUMN_GAP) / 2;
		}

		if (x > 0)
			y += yy + TEAM_SPACING;

		g.setColor(Color.WHITE);
		g.drawLine(0, y, width, y);
		bottom = (y + TEAM_SPACING * 2) / (double) rowHeight;
		g.dispose();
	}

	public void setScrollPause(boolean pause) {
		scrollPause = pause;
	}
}