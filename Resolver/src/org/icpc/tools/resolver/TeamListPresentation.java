package org.icpc.tools.resolver;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.TeamListStep;
import org.icpc.tools.contest.model.resolver.SelectType;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.TextHelper;

public class TeamListPresentation extends AbstractICPCPresentation {
	private static final float SPACING = 1.2f;
	private static final int GAP = 8;
	private static final int ROWS_PER_SCREEN = 15;

	private Animator scroll = new Animator(0, new Movement(0.5, 0.75));
	private boolean scrollPause = false;

	class TeamInfo {
		private String teamName;
		private String id;
	}

	private Map<String, BufferedImage> logos = new HashMap<>();

	private TeamListStep step;
	private TeamInfo[] teams;
	private Map<String, SelectType> selections;

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
		teamFont = ICPCFont.deriveFont(Font.PLAIN, inch * 0.62f);
		titleFont = ICPCFont.deriveFont(Font.BOLD, inch);
		subTitleFont = ICPCFont.deriveFont(Font.BOLD, inch * 0.5f);
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

		bottom = size + height / rowHeight;
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
		g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2.setFont(titleFont);
		g2.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
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
		int scr = (int) (scroll.getValue() * rowHeight * SPACING);

		// draw team list
		g.setFont(teamFont);
		g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
		fm = g.getFontMetrics();
		int size = teams.length;
		for (int i = 0; i < size; i++) {
			TeamInfo t = teams[i];

			TextHelper text = new TextHelper(g);
			text = new TextHelper(g);
			BufferedImage img = logos.get(t.id);
			if (img != null)
				text.addImage(img);
			text.addSpacer(GAP, rowHeight);
			text.addString(t.teamName);

			int y = height - headerHeight + rowHeight / 2 - scr + (int) (rowHeight * i * SPACING);
			if (y + text.getHeight() < headerHeight || y >= height)
				continue;

			SelectType sel = selections.get(t.id);
			if (sel != null) {
				if (sel == SelectType.FTS)
					g.setColor(ICPCColors.FIRST_TO_SOLVE_COLOR);
				else if (sel == SelectType.FTS_HIGHLIGHT)
					g.setColor(ICPCColors.SOLVED_COLOR.brighter());
				else
					g.setColor(ICPCColors.SELECTION_COLOR);
			} else
				g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);

			AffineTransform old = g.getTransform();
			if (y > height * 2f / 3f)
				g.setComposite(AlphaComposite.SrcOver.derive(1f - (y - height * 2f / 3f) / (height / 3f)));
			g.translate(width / 2, y);
			if (y > height * 3f / 4f) {
				g.translate(0, (y - height * 3f / 4f) * 1f);
				double sc = 1.0 + (y - height * 3f / 4f) / (height / 4f) * 2.0;
				g.transform(AffineTransform.getScaleInstance(sc, sc));
			}

			text.drawFit(-Math.min((width - 40) / 2, text.getWidth() / 2), 0, width - 40);
			g.setTransform(old);
		}

		g.setComposite(AlphaComposite.SrcOver);
		int y = height - headerHeight - scr + (int) (rowHeight * (size + 2) * SPACING);
		g.setColor(isLightMode() ? Color.DARK_GRAY : Color.LIGHT_GRAY);
		g.drawLine(0, y, width, y);
		g.dispose();
	}

	public void setScrollPause(boolean pause) {
		scrollPause = pause;
	}
}