package org.icpc.tools.presentation.contest.internal.presentations.map;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator3D;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

public class GroupPresentation extends AbstractICPCPresentation {
	private static final long FADE_IN_TIME = 1250;
	private static final long FADE_OUT_TIME = 1000;
	private static final long MAX_TIME = 12000;

	protected Animator3D anim = new Animator3D(0.0, new Animator.Movement(60, 180), 0.0, new Animator.Movement(30, 90),
			1.0, new Animator.Movement(0.5, 1.5));
	protected int groupNum = -1;

	protected Map<String, BufferedImage> images = new HashMap<>();
	protected Font groupFont;

	@Override
	public void init() {
		WorldMap.load(getClass());
	}

	@Override
	public long getRepeat() {
		return MAX_TIME;
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		images.clear();

		final float dpi = 96;
		groupFont = ICPCFont.deriveFont(Font.BOLD, height * 72f * 0.07f / dpi);
	}

	@Override
	public void aboutToShow() {
		groupNum++;
		if (groupNum >= getContest().getGroups().length)
			groupNum = -1;

		setTargets(getContest());
	}

	protected static boolean belongsToGroup(String[] groupIds, String groupId) {
		if (groupIds == null || groupId == null)
			return false;
		for (String s : groupIds) {
			if (groupId.equals(s))
				return true;
		}
		return false;
	}

	protected void setTargets(IContest contest) {
		String groupId = null;
		if (groupNum >= 0)
			groupId = contest.getGroups()[groupNum].getId();
		double minLat = 90;
		double maxLat = -90;
		double minLon = 180;
		double maxLon = -180;

		ITeam[] teams = contest.getTeams();
		for (ITeam team : teams) {
			IOrganization org = contest.getOrganizationById(team.getOrganizationId());
			String[] groupIds = team.getGroupIds();
			if (org != null && (groupId == null || belongsToGroup(groupIds, groupId))) {
				double lat = org.getLatitude();
				if (lat != Double.MIN_VALUE) {
					minLat = Math.min(minLat, lat);
					maxLat = Math.max(maxLat, lat);
				}

				double lon = org.getLongitude();
				if (lon != Double.MIN_VALUE) {
					minLon = Math.min(minLon, lon);
					maxLon = Math.max(maxLon, lon);
				}
			}
		}

		if (minLat == 90 || minLon == 180) {
			anim.setTarget(0, 0, 1);
			return;
		}

		double dLon = maxLon - minLon;
		double dLat = maxLat - minLat;

		double sc = Math.min(5, Math.min(360.0 / (dLon + 20.0), 180.0 / (dLat + 10.0)));
		anim.setTarget(minLon + dLon / 2.0, minLat + dLat / 2.0, sc);
		// anim.setTargetAtTime(minLon + dLon / 2.0, minLat + dLat / 2.0, sc, 2000);
	}

	@Override
	public void incrementTimeMs(long dt) {
		anim.incrementTimeMs(dt);
	}

	@Override
	public void paint(Graphics2D g) {
		IContest contest = getContest();
		if (contest == null)
			return;

		long time = getRepeatTimeMs();
		double sc = anim.getZValue();
		double oX = width / 2.0 - (width * (anim.getXValue() + 180) * sc / 360.0);
		double oY = height / 2.0 - (height * (90 - anim.getYValue()) * sc / 180.0);

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		WorldMap.drawMap(g, (int) oX, (int) oY, width, height, sc);

		Graphics2D g2 = (Graphics2D) g.create();
		g2.translate(oX, oY);

		IGroup group = null;
		String groupId = null;
		if (groupNum >= 0) {
			group = contest.getGroups()[groupNum];
			groupId = group.getId();
		}

		if (time > 5000) {
			int logoSize = height / 12;
			g2.setColor(Color.RED);
			float count = 0;
			ITeam[] teams = contest.getTeams();
			for (ITeam team : teams) {
				String[] groupIds = team.getGroupIds();
				if (group == null || belongsToGroup(groupIds, groupId)) {
					count++;
				}
			}

			if (time > MAX_TIME - FADE_OUT_TIME && time < MAX_TIME)
				g2.setComposite(
						AlphaComposite.SrcOver.derive(1f - (time - (float) (MAX_TIME - FADE_OUT_TIME)) / FADE_OUT_TIME));

			if (time < 8000)
				count = ((time - 5000f) * count / 3000f);

			for (ITeam team : teams) {
				String[] groupIds = team.getGroupIds();
				if ((group == null || belongsToGroup(groupIds, groupId)) && count > 0) {
					IOrganization inst = contest.getOrganizationById(team.getOrganizationId());
					if (inst == null)
						continue;
					double lat = inst.getLatitude();
					double lon = inst.getLongitude();
					if (lat != Double.MIN_VALUE && lon != Double.MIN_VALUE) {
						int x = (int) (width * (lon + 180.0) * sc / 360.0);
						int y = (int) (height * (90 - lat) * sc / 180.0);

						if (x + oX > -20 && x + oX < width + 20 && y + oY > -20 && y + oY < height + 20) {
							if (count < 1)
								try {
									g2.setComposite(AlphaComposite.SrcOver.derive(count));
								} catch (Exception e) {
									System.err.println(e.getMessage() + " " + count);
								}

							String id = inst.getId();
							BufferedImage img = images.get(id);
							if (img == null) {
								img = inst.getLogoImage(logoSize, logoSize, getModeTag(), true, true);
								if (img != null)
									images.put(id, img);
							}
							if (img != null)
								g2.drawImage(img, x - 20, y - 20, null);
							else
								g2.fillRect(x - 2, y - 2, 5, 5);
						}
					}
					count--;
				}
			}
		}
		g2.dispose();

		if (time > 4000) {
			if (time < 4000 + FADE_IN_TIME)
				g.setComposite(AlphaComposite.SrcOver.derive((time - 4000f) / FADE_IN_TIME));
			else if (time > MAX_TIME - FADE_OUT_TIME)
				g.setComposite(
						AlphaComposite.SrcOver.derive(1f - (time - (float) (MAX_TIME - FADE_OUT_TIME)) / FADE_OUT_TIME));
			g.setFont(groupFont);
			String s = "All teams";
			if (group != null)
				s = group.getName();
			FontMetrics fm = g.getFontMetrics();
			float border = height / 40f;
			g.setColor(Color.DARK_GRAY);
			g.drawString(s, (int) (width - border - fm.stringWidth(s)) + 1, (int) (height - border) + 1);
			g.setColor(Color.WHITE);
			g.drawString(s, (int) (width - border - fm.stringWidth(s)), (int) (height - border));
		}
	}
}