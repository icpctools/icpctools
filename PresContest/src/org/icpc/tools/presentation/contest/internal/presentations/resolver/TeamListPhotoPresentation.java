package org.icpc.tools.presentation.contest.internal.presentations.resolver;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.TextHelper;

public class TeamListPhotoPresentation extends AbstractICPCPresentation {
	private static final Collator collator = Collator.getInstance(Locale.US);

	private IAward award;

	private ITeam[] teams;
	private Map<String, BufferedImage> logos = new HashMap<>();
	private Map<String, BufferedImage> photos = new HashMap<>();

	private Map<String, SelectType> selections = new HashMap<>();

	private Font teamFont;
	private Font titleFont;
	private int header;
	private int gap;

	private int numRows;
	private int numColumns;
	private Dimension tileDim;

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		IContest contest = getContest();
		if (contest == null)
			return;

		float dpi = 96;
		float inch = height * 72f / dpi / 10f;
		titleFont = ICPCFont.deriveFont(Font.BOLD, inch * 0.6f);

		header = height / 20;
		gap = height / 120;

		if (award != null) {
			int numTeams = award.getTeamIds().length;
			numColumns = (int) Math.ceil(Math.sqrt(numTeams));

			numRows = (numTeams + numColumns - 1) / numColumns;

			tileDim = new Dimension((width - (numColumns - 1) * gap - 1) / numColumns,
					(height - header - (numRows - 1) * gap - 1) / numRows);

			teamFont = ICPCFont.deriveFont(Font.PLAIN, inch * tileDim.height / 250);

			logos.clear();
			photos.clear();
			cacheLogos();
		}
	}

	protected void cacheLogos() {
		if (teams == null || height < 100)
			return;

		// load logos and photos in background
		ITeam[] teams2 = teams;
		execute(new Runnable() {
			@Override
			public void run() {
				if (teams2 == null)
					return;

				for (ITeam team : teams2) {
					if (team != null) {
						BufferedImage img = team.getPhotoImage(tileDim.width, tileDim.height, true, true);
						if (img != null)
							photos.put(team.getId(), img);

						IOrganization org = getContest().getOrganizationById(team.getOrganizationId());
						if (org != null) {
							BufferedImage img2 = org.getLogoImage(tileDim.height / 5, tileDim.height / 5, true, true);
							if (img2 != null)
								logos.put(team.getId(), img2);
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

		int numTeams = award.getTeamIds().length;
		numColumns = (int) Math.ceil(Math.sqrt(numTeams));

		numRows = (numTeams + numColumns - 1) / numColumns;

		tileDim = new Dimension((width - (numColumns - 1) * gap - 1) / numColumns,
				(height - header - (numRows - 1) * gap - 1) / numRows);

		float dpi = 96;
		float inch = height * 72f / dpi / 10f;
		teamFont = ICPCFont.deriveFont(Font.PLAIN, inch * tileDim.height / 250);

		cacheLogos();
	}

	@Override
	public void paint(Graphics2D g) {
		if (award == null || teams == null)
			return;

		// draw title across the top
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g.setFont(titleFont);
		g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
		FontMetrics fm = g.getFontMetrics();
		g.drawString(award.getCitation(), (width - fm.stringWidth(award.getCitation())) / 2, fm.getAscent());

		g.drawLine(0, header - 1, width, header - 1);

		// draw team list
		g.setFont(teamFont);
		g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
		fm = g.getFontMetrics();

		int x = 0;
		int y = 0;
		int size = teams.length;
		for (int i = 0; i < size; i++) {
			int xx = tileDim.width * x + gap * x;
			int yy = tileDim.height * y + gap * y + header;

			ITeam t = teams[i];
			BufferedImage photo = photos.get(t.getId());
			if (photo != null)
				g.drawImage(photo, xx, yy, null);
			else {
				g.setColor(isLightMode() ? Color.LIGHT_GRAY : Color.DARK_GRAY);
				g.drawRect(xx, yy, tileDim.width, tileDim.height);
			}

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

			TextHelper text = new TextHelper(g);
			text = new TextHelper(g);
			BufferedImage logo = photos.get(t.getId());
			if (logo != null)
				text.addImage(logo);
			text.addSpacer(8, fm.getHeight());
			text.addString(t.getActualDisplayName());

			g.translate(xx + tileDim.width / 2, 0);
			text.drawFit(-Math.min((tileDim.width - gap * 2) / 2, text.getWidth() / 2),
					yy + tileDim.height - gap - fm.getHeight(), tileDim.width - gap * 2);
			g.translate(-xx - tileDim.width / 2, 0);

			x++;
			if (x >= numColumns) {
				x = 0;
				y++;
			}
		}
	}

	@Override
	public void setProperty(String value) {
		super.setProperty(value);
		if (value == null || value.isEmpty())
			return;

		if (value.startsWith("award-id:")) {
			try {
				String awardId = value.substring(9);
				setAward(getContest().getAwardById(awardId));
			} catch (Exception e) {
				Trace.trace(Trace.INFO, "Invalid " + value);
			}
		}
	}
}
