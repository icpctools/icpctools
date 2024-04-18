package org.icpc.tools.presentation.contest.internal.presentations.resolver;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ListAwardStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ResolutionStep;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.ImageScaler;
import org.icpc.tools.presentation.contest.internal.TextHelper;

public class TeamListPhotoPresentation extends AbstractICPCPresentation {
	private static final Collator collator = Collator.getInstance(Locale.US);

	private static final Color BG_COLOR = new Color(0, 0, 0, 196);

	private IAward award;

	private ITeam[] teams;

	private Font titleFont;
	private int header;
	private int gap;

	class Cache {
		private List<String> teamIds = new ArrayList<>();
		private IAward award2;
		private int numRows;
		private int numColumns;
		private Dimension tileDim;
		private Font teamFont;
		private BufferedImage contestLogo;
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

		float dpi = 96;
		float inch = height * 72f / dpi / 10f;
		titleFont = ICPCFont.deriveFont(Font.BOLD, inch * 0.6f);

		header = height / 20;
		gap = height / 120;

		updateCacheSizes();

		if (award != null) {
			updateCache(0);
		}
	}

	protected void updateCacheSizes() {
		if (cache == null)
			return;

		for (int i = 0; i < cache.length; i++) {
			Cache c = cache[i];

			int numTeams = c.award2.getTeamIds().length;
			c.numColumns = (int) Math.ceil(Math.sqrt(numTeams));

			c.numRows = (numTeams + c.numColumns - 1) / c.numColumns;

			c.tileDim = new Dimension((width - (c.numColumns - 1) * gap - 1) / c.numColumns,
					(height - header - gap - (c.numRows - 1) * gap - 1) / c.numRows);

			float dpi = 96;
			float inch = height * 72f / dpi / 10f;
			c.teamFont = ICPCFont.deriveFont(Font.PLAIN, inch * c.tileDim.height / 485);

			// for now, never flush contest logo if size changes
			// if (c.contestLogo != null)
			// c.contestLogo.flush();

			c.contestLogo = getContest().getLogoImage((int) (c.tileDim.width * 0.8), (int) (c.tileDim.height * 0.7), true,
					true);
			if (c.contestLogo == null) {
				ClassLoader cl = getClass().getClassLoader();
				try {
					c.contestLogo = ImageScaler.scaleImage(ImageIO.read(cl.getResource("images/id.png")),
							(int) (c.tileDim.width * 0.8), (int) (c.tileDim.height * 0.7));
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error loading images", e);
				}
			}
		}
	}

	public void cacheAwards(List<ResolutionStep> steps) {
		execute(new Runnable() {
			@Override
			public void run() {
				// create cache of awards in correct order
				List<Cache> list = new ArrayList<>();

				for (ResolutionStep step : steps) {
					if (step instanceof ListAwardStep && ((ListAwardStep) step).photos) {
						ListAwardStep as = (ListAwardStep) step;
						Cache c = new Cache();
						c.teamIds = Arrays.stream(as.teams).map(IContestObject::getId).collect(Collectors.toList());
						c.award2 = as.award;

						list.add(c);
					}
				}

				cache = list.toArray(new Cache[0]);

				updateCacheSizes();

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
					c.teamPhotos.computeIfAbsent(c.teamIds.get(j), k -> c.contestLogo);
				}
			} else {
				for (int j = 0; j < c.teamIds.size(); j++) {
					String teamId = c.teamIds.get(j);
					if (c.teamPhotos.get(teamId) != null) {
						if (!c.teamPhotos.get(teamId).equals(c.contestLogo))
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
		for (int i = 0; i < c.teamIds.size(); i++) {
			String teamId = c.teamIds.get(i);
			ITeam team = getContest().getTeamById(c.teamIds.get(i));
			if (team == null)
				return;

			if (c.teamPhotos.get(teamId) == null)
				c.teamPhotos.put(teamId, team.getPhotoImage(c.tileDim.width, c.tileDim.height, true, true));

			IContest contest = getContest();
			IOrganization org = contest.getOrganizationById(team.getOrganizationId());
			if (org == null)
				return;

			if (c.teamLogos.get(teamId) == null)
				c.teamLogos.put(teamId, org.getLogoImage(c.tileDim.height / 8, c.tileDim.height / 8, true, true));
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
			if (c.award2.equals(award)) {
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
		g.setFont(c.teamFont);
		g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
		fm = g.getFontMetrics();

		int x = 0;
		int y = 0;
		int size = teams.length;
		for (int i = 0; i < size; i++) {
			int xx = x * (c.tileDim.width + gap);
			int yy = y * (c.tileDim.height + gap) + header + gap;

			if (y == c.numRows - 1 && size % c.numColumns != 0) {
				xx += (c.numColumns - size % c.numColumns) * (c.tileDim.width + gap) / 2;
			}

			ITeam t = teams[i];
			BufferedImage photo = c.teamPhotos.get(t.getId());
			if (photo != null)
				g.drawImage(photo, xx + (c.tileDim.width - photo.getWidth()) / 2,
						yy + (c.tileDim.height - photo.getHeight()) / 2, null);
			else {
				g.setColor(isLightMode() ? Color.LIGHT_GRAY : Color.DARK_GRAY);
				g.drawRect(xx, yy, c.tileDim.width, c.tileDim.height);

				if (c.contestLogo != null) {
					g.drawImage(c.contestLogo, xx + (c.tileDim.width - c.contestLogo.getWidth()) / 2,
							yy + (c.tileDim.height - c.contestLogo.getHeight()) / 2, null);
				}
			}

			g.setColor(BG_COLOR);
			g.fillRect(xx, yy + c.tileDim.height - 2 * gap - Math.max(c.tileDim.height / 8, fm.getHeight()),
					c.tileDim.width, 2 * gap + Math.max(c.tileDim.height / 8, fm.getHeight()));

			TextHelper text = new TextHelper(g);
			text = new TextHelper(g);
			BufferedImage logo = c.teamLogos.get(t.getId());
			if (logo != null)
				text.addImage(logo);
			text.addSpacer(8, fm.getHeight());
			text.addString(t.getActualDisplayName());
			Dimension b = text.getBounds();

			g.translate(xx + c.tileDim.width / 2, 0);
			text.drawFit(-Math.min((c.tileDim.width - gap * 2) / 2, text.getWidth() / 2),
					yy + c.tileDim.height - gap - c.tileDim.height / 16 - b.height / 2, c.tileDim.width - gap * 2);
			g.translate(-xx - c.tileDim.width / 2, 0);

			x++;
			if (x >= c.numColumns) {
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
