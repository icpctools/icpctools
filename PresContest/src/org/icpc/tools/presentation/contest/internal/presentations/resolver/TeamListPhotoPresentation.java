package org.icpc.tools.presentation.contest.internal.presentations.resolver;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.*;
import org.icpc.tools.contest.model.resolver.ResolutionUtil;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.ImageScaler;
import org.icpc.tools.presentation.contest.internal.TextHelper;

import javax.imageio.ImageIO;

public class TeamListPhotoPresentation extends AbstractICPCPresentation {
	private static final Collator collator = Collator.getInstance(Locale.US);

	private static final Color BG_COLOR = new Color(0, 0, 0, 196);

	private IAward award;

	private ITeam[] teams;

	private Font teamFont;
	private Font titleFont;
	private int header;
	private int gap;
	private BufferedImage contestLogo;

	private int numRows;
	private int numColumns;
	private Dimension tileDim;

	class Cache {
		private List<String> teamIds = new ArrayList<>();
		private IAward award;
		private final Map<String, BufferedImage> teamLogos = new HashMap<>();
		private final Map<String, BufferedImage> teamPhotos = new HashMap<>();
	}

	private Cache[] cache;

	private Cache currentCache;

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		IContest contest = getContest();
		if (contest == null)
			return;

		contestLogo = getContest().getLogoImage((int) (width * 0.8), (int) (height * 0.7), true, true);
		if (contestLogo == null) {
			ClassLoader cl = getClass().getClassLoader();
			try {
				contestLogo = ImageScaler.scaleImage(ImageIO.read(cl.getResource("images/id.png")), (int) (width * 0.8),
						(int) (height * 0.7));
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error loading images", e);
			}
		}

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
					(height - header - gap - (numRows - 1) * gap - 1) / numRows);

			teamFont = ICPCFont.deriveFont(Font.PLAIN, inch * tileDim.height / 475);
		}
	}

	public void cacheAwards(List<ResolutionUtil.ResolutionStep> steps) {
		execute(new Runnable() {
			@Override
			public void run() {
				// create cache of awards in correct order
				List<Cache> list = new ArrayList<>();

				for (ResolutionUtil.ResolutionStep step : steps) {
					if (step instanceof ResolutionUtil.ListAwardStep && ((ResolutionUtil.ListAwardStep) step).photos) {
						ResolutionUtil.ListAwardStep as = (ResolutionUtil.ListAwardStep) step;
						Cache c = new Cache();
						c.teamIds = Arrays.stream(as.teams).map(IContestObject::getId).collect(Collectors.toList());
						c.award = as.award;

						list.add(c);
					}
				}

				cache = list.toArray(new Cache[0]);

				// load initial images
				updateCache(0);
			}
		});
	}

	/*
	 * Keep the cache current with 1 prior award and the next 2 that are expected.
	 */
	private void updateCache(int index) {
		for (int i = 0; i < cache.length; i++) {
			Cache c = cache[i];
			if (i > index - 2 && i < index + 3) {
				loadImages(c);
				for (int j = 0; j < c.teamIds.size(); j++) {
					c.teamPhotos.computeIfAbsent(c.teamIds.get(j), k -> contestLogo);
				}
			} else {
				for (int j = 0; j < c.teamIds.size(); j++) {
					String teamId = c.teamIds.get(j);
					if (c.teamPhotos.get(teamId) != null) {
						if (!c.teamPhotos.get(teamId).equals(contestLogo))
							c.teamPhotos.get(teamId).flush();
						c.teamPhotos.remove(teamId);
					}
				}
				for (int j = 0; j < c.teamIds.size(); j++) {
					String teamId = c.teamIds.get(j);
					if (c.teamLogos.get(teamId) != null) {
						c.teamPhotos.get(teamId).flush();
						c.teamPhotos.remove(teamId);
					}
				}
			}
		}
	}

	protected void loadImages(Cache c) {
		int numTeams = c.award.getTeamIds().length;
		numColumns = (int) Math.ceil(Math.sqrt(numTeams));

		numRows = (numTeams + numColumns - 1) / numColumns;

		tileDim = new Dimension((width - (numColumns - 1) * gap - 1) / numColumns,
				(height - header - gap - (numRows - 1) * gap - 1) / numRows);

		for (int i = 0; i < c.teamIds.size(); i++) {
			String teamId = c.teamIds.get(i);
			ITeam team = getContest().getTeamById(c.teamIds.get(i));
			if (team == null)
				return;

			if (c.teamPhotos.get(teamId) == null)
				c.teamPhotos.put(teamId, team.getPhotoImage(tileDim.width, tileDim.height, true, true));

			IContest contest = getContest();
			IOrganization org = contest.getOrganizationById(team.getOrganizationId());
			if (org == null)
				return;

			if (c.teamLogos.get(teamId) == null)
				c.teamLogos.put(teamId, org.getLogoImage(tileDim.height / 8, tileDim.height / 8, true, true));
		}
	}

	public void setAward(IAward award) {
		this.award = award;
		Trace.trace(Trace.INFO, "Set award: " + award);

		if (award == null) {
			teams = new ITeam[0];
			return;
		}

		currentCache = null;
		int index = -1;
		for (int i = 0; i < cache.length; i++) {
			Cache c = cache[i];
			if (c.award.equals(award)) {
				index = i;
				currentCache = c;
			}
		}

		if (index < 0)
			return;

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
				(height - header - gap - (numRows - 1) * gap - 1) / numRows);

		float dpi = 96;
		float inch = height * 72f / dpi / 10f;
		teamFont = ICPCFont.deriveFont(Font.PLAIN, inch * tileDim.height / 475);

		final int index2 = index;

		execute(new Runnable() {
			@Override
			public void run() {
				updateCache(index2);
			}
		});
	}

	@Override
	public void paint(Graphics2D g) {
		Cache c = currentCache;
		if (c == null)
			return;

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
			int xx = x * (tileDim.width + gap);
			int yy = y * (tileDim.height + gap) + header + gap;

			if (y == numRows - 1 && size % numColumns != 0) {
				xx += (numColumns - size % numColumns) * (tileDim.width + gap) / 2;
			}

			ITeam t = teams[i];
			BufferedImage photo = c.teamPhotos.get(t.getId());
			if (photo != null)
				g.drawImage(photo, xx + (tileDim.width - photo.getWidth()) / 2,
						yy + (tileDim.height - photo.getHeight()) / 2, null);
			else {
				g.setColor(isLightMode() ? Color.LIGHT_GRAY : Color.DARK_GRAY);
				g.drawRect(xx, yy, tileDim.width, tileDim.height);

				if (contestLogo != null) {
					g.drawImage(contestLogo, xx + (tileDim.width - contestLogo.getWidth()) / 2,
							yy + (tileDim.height - contestLogo.getHeight()) / 2, null);
				}
			}

			g.setColor(BG_COLOR);
			g.fillRect(xx, yy + tileDim.height - 2 * gap - Math.max(tileDim.height / 8, fm.getHeight()), tileDim.width - gap * 2, 2 * gap + Math.max(tileDim.height / 8, fm.getHeight()));

			TextHelper text = new TextHelper(g);
			text = new TextHelper(g);
			BufferedImage logo = c.teamLogos.get(t.getId());
			if (logo != null)
				text.addImage(logo);
			text.addSpacer(8, fm.getHeight());
			text.addString(t.getActualDisplayName(), true);

			g.translate(xx + tileDim.width / 2, 0);
			text.drawFit(-Math.min((tileDim.width - gap * 2) / 2, text.getWidth() / 2),
					yy + tileDim.height - gap - Math.max(tileDim.height / 8, fm.getHeight()), tileDim.width - gap * 2);
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
