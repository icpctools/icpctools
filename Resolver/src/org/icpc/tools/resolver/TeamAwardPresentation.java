package org.icpc.tools.resolver;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IAward.AwardType;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.ITeamMember;
import org.icpc.tools.contest.model.internal.Award;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.AwardStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ResolutionStep;
import org.icpc.tools.contest.model.util.Messages;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.ImageScaler;

public class TeamAwardPresentation extends AbstractICPCPresentation {
	private static final int BORDER = 20;
	private static final int TEAM_SPACING = 5;

	class Cache {
		private String teamId;
		private List<IAward> awards = new ArrayList<>(3);
		private BufferedImage teamImage;
		private BufferedImage teamLogo;
		private BufferedImage groupLogo;
		private boolean mergedAwards;

		private String[] name;
		private String[] members;
	}

	private Cache[] cache;
	private Map<String, String[]> citations = new HashMap<>();
	private Cache currentCache;
	private Font teamFont;
	private Font groupFont;
	private Font memberFont;
	private BufferedImage logo;
	private boolean showInfo;

	private static final Color BG_COLOR = new Color(0, 0, 0, 196);

	@Override
	public void init() {
		float dpi = 96;
		float inch = height * 72f / dpi / 10f;
		teamFont = ICPCFont.deriveFont(Font.BOLD, inch * 0.9f);
		groupFont = ICPCFont.deriveFont(Font.BOLD, inch * 0.7f);
		memberFont = ICPCFont.deriveFont(Font.BOLD, inch * 0.3f);
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		IContest contest = getContest();
		if (contest == null)
			return;

		logo = getContest().getLogoImage((int) (width * 0.8), (int) (height * 0.7), true, true);
		if (logo == null) {
			ClassLoader cl = getClass().getClassLoader();
			try {
				logo = ImageScaler.scaleImage(ImageIO.read(cl.getResource("images/id.png")), (int) (width * 0.8),
						(int) (height * 0.7));
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error loading images", e);
			}
		}
	}

	public void setShowInfo(boolean b) {
		showInfo = b;
	}

	public void cacheAwards(List<ResolutionStep> steps) {
		execute(new Runnable() {
			@Override
			public void run() {
				// create cache of awards in correct order
				List<Cache> list = new ArrayList<>();

				for (ResolutionStep step : steps) {
					if (step instanceof AwardStep) {
						AwardStep as = (AwardStep) step;
						Cache c = new Cache();
						c.teamId = as.teamId;

						for (IAward a : as.awards) {
							if (a.getTeamIds() != null) {
								for (String tId : a.getTeamIds()) {
									if (tId.equals(c.teamId))
										c.awards.add(a);
								}
							}
						}

						list.add(c);
					}
				}

				cache = list.toArray(new Cache[0]);

				// load initial images
				updateCache(0);
			}
		});
	}

	public void setTeam(String teamId) {
		currentCache = null;
		int index = -1;
		for (int i = 0; i < cache.length; i++) {
			Cache c = cache[i];
			if (c.teamId.equals(teamId)) {
				index = i;
				currentCache = c;
			}
		}

		if (index < 0)
			return;

		mergeAwards();

		ITeamMember[] members = getContest().getTeamMembersByTeamId(currentCache.teamId);
		if (members != null) {
			Arrays.sort(members, new Comparator<ITeamMember>() {
				@Override
				public int compare(ITeamMember m1, ITeamMember m2) {
					String r1 = m1.getRole();
					String r2 = m2.getRole();
					if (r1 == null || r2 == null)
						return 0;
					return -r1.compareTo(r2);
				}
			});
		}
		if (members == null)
			currentCache.members = new String[0];
		else {
			int size = members.length;
			currentCache.members = new String[size];
			for (int i = 0; i < size; i++) {
				currentCache.members[i] = members[i].getFirstName() + " " + members[i].getLastName() + " ("
						+ members[i].getRole() + ")";
			}
		}

		final int index2 = index;

		execute(new Runnable() {
			@Override
			public void run() {
				updateCache(index2);
			}
		});
	}

	/*
	 * Keep the cache current with 1 prior image and the next 2 that are expected.
	 */
	private void updateCache(int index) {
		for (int i = 0; i < cache.length; i++) {
			Cache c = cache[i];
			if (i > index - 2 && i < index + 3) {
				loadImages(c);
				if (c.teamImage == null)
					c.teamImage = logo;
			} else {
				if (c.teamImage != null) {
					if (!c.teamImage.equals(logo))
						c.teamImage.flush();
					c.teamImage = null;
				}
				if (c.teamLogo != null) {
					c.teamLogo.flush();
					c.teamLogo = null;
				}
				if (c.groupLogo != null) {
					c.groupLogo.flush();
					c.groupLogo = null;
				}
			}
		}
	}

	protected void loadImages(Cache c) {
		ITeam team = getContest().getTeamById(c.teamId);
		if (team == null)
			return;

		if (c.teamImage == null)
			c.teamImage = team.getPhotoImage(width, height, true, true);

		IContest contest = getContest();
		IOrganization org = contest.getOrganizationById(team.getOrganizationId());
		if (org == null)
			return;

		if (c.teamLogo == null)
			c.teamLogo = org.getLogoImage(height / 5, height / 5, true, true);

		if (c.groupLogo == null) {
			boolean hasGroupAward = false;
			for (IAward a : c.awards) {
				if (a.getId().startsWith(IAward.GROUP.getPattern("")))
					hasGroupAward = true;
			}
			String[] groupIds = team.getGroupIds();
			if (hasGroupAward && groupIds != null && groupIds.length == 1) {
				IGroup g = contest.getGroupById(groupIds[0]);
				if (g != null) {
					c.groupLogo = g.getLogoImage(height / 5, height / 5, true, true);
				}
			}
		}
	}

	@Override
	public void paint(Graphics2D g) {
		Cache c = currentCache;
		if (c == null)
			return;

		List<IAward> awards = c.awards;
		if (c.teamImage != null)
			g.drawImage(c.teamImage, (width - c.teamImage.getWidth()) / 2, 0, null);

		// calculate height
		int h = TEAM_SPACING;

		int x = BORDER;
		int logoSize = (int) (height / 5f);
		if (c.teamLogo != null)
			x += logoSize + BORDER;

		int x2 = BORDER;
		if (c.groupLogo != null)
			x2 += logoSize + BORDER;

		g.setFont(teamFont);
		FontMetrics fm = g.getFontMetrics();
		IContest contest = getContest();
		ITeam team = contest.getTeamById(c.teamId);
		int usableWidth = width - x - x2 - BORDER;

		if (c.name == null)
			c.name = splitString(g, team.getActualDisplayName(), usableWidth);

		h += fm.getHeight() * c.name.length;

		for (IAward a : awards) {
			AwardType type = a.getAwardType();
			if (type == IAward.WINNER || type == IAward.RANK || type == IAward.MEDAL) {
				g.setFont(teamFont);
				fm = g.getFontMetrics();
			} else {
				g.setFont(groupFont);
				fm = g.getFontMetrics();
			}
			String[] citation = citations.get(a.getId());
			if (citation == null) {
				citation = splitString(g, a.getCitation(), usableWidth);
				citations.put(a.getId(), citation);
			}
			h += fm.getHeight() * citation.length;
		}

		if (c.teamLogo != null)
			h = Math.max(h, c.teamLogo.getHeight());

		if (c.groupLogo != null)
			h = Math.max(h, c.groupLogo.getHeight());

		g.setColor(BG_COLOR);
		g.fillRect(0, height - h - BORDER * 2, width, h + BORDER * 2);

		if (c.teamLogo != null)
			g.drawImage(c.teamLogo, BORDER + (logoSize - c.teamLogo.getWidth()) / 2, height - h - BORDER, null);

		if (c.groupLogo != null)
			g.drawImage(c.groupLogo, width - BORDER - logoSize + (logoSize - c.groupLogo.getWidth()) / 2,
					height - h - BORDER, null);

		int y = height - h - BORDER;
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(teamFont);
		fm = g.getFontMetrics();

		g.setColor(Color.WHITE);
		for (int i = 0; i < c.name.length; i++) {
			g.drawString(c.name[i], x, y + fm.getAscent());
			y += fm.getHeight();
		}
		y += TEAM_SPACING;

		g.setFont(teamFont);
		fm = g.getFontMetrics();

		for (IAward a : awards) {
			AwardType type = a.getAwardType();
			if (type == IAward.WINNER || type == IAward.RANK || type == IAward.MEDAL) {
				g.setFont(teamFont);
				fm = g.getFontMetrics();
				g.setColor(Color.WHITE);
			} else {
				g.setFont(groupFont);
				fm = g.getFontMetrics();
				g.setColor(Color.LIGHT_GRAY);
			}

			String[] citation = citations.get(a.getId());
			if (citation == null) {
				citation = splitString(g, a.getCitation(), usableWidth);
				citations.put(a.getId(), citation);
			}

			for (int i = 0; i < citation.length; i++) {
				g.drawString(citation[i], x, y + fm.getAscent());
				y += fm.getHeight();
			}
		}

		if (showInfo && c.members.length > 0) {
			g.setColor(Color.WHITE);
			g.setFont(memberFont);
			int size = c.members.length;
			fm = g.getFontMetrics();
			for (int i = 0; i < size; i++) {
				g.drawString(c.members[i], BORDER, height - h - BORDER - fm.getHeight() * (size - i));
			}

			IStanding st = contest.getStanding(team);
			String s = "Rank: " + st.getRank();
			g.drawString(s, width - fm.stringWidth(s) - BORDER, height - h - BORDER - fm.getHeight() * 3);
			s = "Solved: " + st.getNumSolved();
			g.drawString(s, width - fm.stringWidth(s) - BORDER, height - h - BORDER - fm.getHeight() * 2);
			s = "Time: " + st.getTime();
			g.drawString(s, width - fm.stringWidth(s) - BORDER, height - h - BORDER - fm.getHeight());
		}
	}

	/**
	 * Attempts to merge similar awards, e.g. merge two "First to solve problem X" awards into one.
	 *
	 * @param awards
	 * @return
	 */
	protected void mergeAwards() {
		if (currentCache.mergedAwards)
			return;

		currentCache.mergedAwards = true;

		boolean show = false;
		int patternLen = IAward.FIRST_TO_SOLVE.getPattern("").length();
		List<String> fts = new ArrayList<>();
		List<IAward> list = new ArrayList<>();
		boolean hasMedal = false;
		boolean hasSolutionAward = false;
		for (IAward a : currentCache.awards) {
			if (a.getAwardType() == IAward.FIRST_TO_SOLVE) {
				String pId = a.getId().substring(patternLen);
				IProblem p = getContest().getProblemById(pId);
				if (p == null) {
					Trace.trace(Trace.WARNING, "Could not consolidate FTS award: " + a.getId());
					continue;
				}
				fts.add(p.getLabel());
				if (a.showAward())
					show = true;
			} else if (a.getId().contains("solution")) {
				hasSolutionAward = true;
				list.add(a);
			} else {
				if (a.getAwardType() == IAward.MEDAL)
					hasMedal = true;
				list.add(a);
			}
		}

		int numFTS = fts.size();
		if (numFTS == 1) {
			list.add(new Award(IAward.FIRST_TO_SOLVE, fts.get(0), new String[] { currentCache.teamId },
					"First to solve problem " + fts.get(0), show));
		} else if (numFTS >= 2) {
			fts.sort((s1, s2) -> s1.compareTo(s2));
			fts.set(numFTS - 1, "and " + fts.get(numFTS - 1));

			String citation = null;
			if (numFTS == 2)
				citation = "First to solve problems " + fts.get(0) + " " + fts.get(1);
			else
				citation = "First to solve problems " + String.join(", ", fts);

			list.add(new Award(IAward.FIRST_TO_SOLVE, String.join("_", fts), new String[] { currentCache.teamId },
					citation, show));
		} else if (fts.size() == 1) {
			list.add(new Award(IAward.FIRST_TO_SOLVE, String.join("_", fts), new String[] { currentCache.teamId },
					"First to solve problem " + fts.get(0), show));
		}

		if (hasMedal && !hasSolutionAward) {
			IContest contest = getContest();
			ITeam team = contest.getTeamById(currentCache.teamId);
			IStanding s = contest.getStanding(team);
			if (s.getNumSolved() == 1)
				list.add(new Award(IAward.OTHER, s.getNumSolved(), currentCache.teamId,
						Messages.getString("awardSolvedOne"), false));
			else if (s.getNumSolved() > 1)
				list.add(new Award(IAward.OTHER, s.getNumSolved(), currentCache.teamId,
						Messages.getString("awardSolvedMultiple").replace("{0}", s.getNumSolved() + ""), false));
		}

		currentCache.awards = list;
	}
}