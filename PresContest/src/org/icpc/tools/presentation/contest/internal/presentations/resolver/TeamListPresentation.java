package org.icpc.tools.presentation.contest.internal.presentations.resolver;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.resolver.SelectType;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.TextHelper;

public class TeamListPresentation extends AbstractICPCPresentation {
	private static final Collator collator = Collator.getInstance(Locale.US);

	private static final float SPACING = 1.2f;
	private static final int GAP = 8;
	private static final int ROWS_PER_SCREEN = 15;

	private Animator scroll = new Animator(0, new Movement(0.5, 0.75));
	private boolean scrollPause = false;
	private IAward award;

	private ITeam[] teams;
	private Map<String, BufferedImage> logos = new HashMap<>();

	private Map<String, SelectType> selections = new HashMap<>();

	private Font teamFont;
	private Font titleFont;
	private int rowHeight;
	private double bottom = 0;

	@Override
	public void init() {
		rowHeight = (height - 40) / ROWS_PER_SCREEN;
		float dpi = 96;
		float inch = height * 72f / dpi / 10f;
		teamFont = ICPCFont.deriveFont(Font.PLAIN, inch * 0.62f);
		titleFont = ICPCFont.deriveFont(Font.BOLD, inch);
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();
		scroll.resetToTarget();
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		IContest contest = getContest();
		if (contest == null)
			return;

		rowHeight = (height - 40) / ROWS_PER_SCREEN;

		// selections = step.selections;

		if (award != null) {
			bottom = teams.length + height / rowHeight;

			logos.clear();
			cacheLogos();
		}
	}

	protected void cacheLogos() {
		if (teams == null || height < 100)
			return;

		// load logos in background
		ITeam[] teams2 = teams;
		execute(new Runnable() {
			@Override
			public void run() {
				if (teams2 == null)
					return;

				for (ITeam team : teams2) {
					if (team != null) {
						IOrganization org = getContest().getOrganizationById(team.getOrganizationId());
						if (org != null) {
							BufferedImage img = org.getLogoImage(rowHeight, rowHeight, true, true);
							if (img != null)
								logos.put(team.getId(), img);
						}
					}
				}
			}
		});
	}

	public void setAward(IAward award) {
		this.award = award;
		Trace.trace(Trace.INFO, "Set award: " + award);

		if (award == null) {
			teams = new ITeam[0];
			return;
		}

		String[] teamIds = award.getTeamIds();
		final int size = teamIds.length;
		teams = new ITeam[size];
		for (int i = 0; i < size; i++) {
			teams[i] = getContest().getTeamById(teamIds[i]);
		}

		// sort teams alphabetically
		Arrays.sort(teams, new Comparator<ITeam>() {
			@Override
			public int compare(ITeam t1, ITeam t2) {
				String n1 = t1.getActualDisplayName();
				String n2 = t2.getActualDisplayName();
				return collator.compare(n1, n2);
			}
		});

		cacheLogos();

		// TODO figure out selections
		// selections = step.selections;

		if (rowHeight > 0)
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
	public void paintImpl(Graphics2D g2) {
		if (award == null || teams == null)
			return;

		// draw title across the top
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2.setFont(titleFont);
		g2.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
		FontMetrics fm = g2.getFontMetrics();
		g2.drawString(award.getCitation(), (width - fm.stringWidth(award.getCitation())) / 2, fm.getAscent());

		int headerHeight = fm.getHeight();
		/*if (step.subTitle != null) {
			g2.setFont(subTitleFont);
			fm = g2.getFontMetrics();
			g2.drawString(step.subTitle, (width - fm.stringWidth(step.subTitle)) / 2, headerHeight + fm.getAscent());
			headerHeight += fm.getHeight();
		}*/

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
			ITeam t = teams[i];

			TextHelper text = new TextHelper(g);
			text = new TextHelper(g);
			BufferedImage img = logos.get(t.getId());
			if (img != null)
				text.addImage(img);
			text.addSpacer(GAP, rowHeight);
			text.addString(t.getActualDisplayName());

			int y = height - headerHeight - scr + (int) (rowHeight * i * SPACING);
			if (y + text.getHeight() < headerHeight || y >= height)
				continue;

			SelectType sel = selections.get(t.getId());
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

	@Override
	public void setProperty(String value) {
		super.setProperty(value);
		if (value == null || value.isEmpty())
			return;

		if ("speed".equals("0"))
			scrollPause = true;
		if ("speed".equals("1"))
			scrollPause = false;
		if (value.startsWith("award-id:")) {
			try {
				String awardId = value.substring(9);
				setAward(getContest().getAwardById(awardId));
			} catch (Exception e) {
				Trace.trace(Trace.INFO, "Invalid " + value);
			}
			scrollPause = true;
		}
	}
}