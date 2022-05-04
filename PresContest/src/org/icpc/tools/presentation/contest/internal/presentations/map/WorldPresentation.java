package org.icpc.tools.presentation.contest.internal.presentations.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.TreeMap;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;

public class WorldPresentation extends AbstractICPCPresentation {
	private boolean drawLogos = true;
	private int logoPercentSize = 33;
	private boolean drawGroupLines = false;

	private Map<String, String> organizationGroups = new TreeMap<>();
	private Map<String, Color> groupColors = new TreeMap<>();
	private TeamIntroPresentation.GroupZoom worldLogos;

	@Override
	public void init() {
		WorldMap.load(getClass());
	}

	@Override
	public void setProperty(String value) {
		if (value == null || value.isEmpty())
			return;
		if ("logosOn".equals(value))
			drawLogos = true;
		else if ("logosOff".equals(value))
			drawLogos = false;
		else if (value.startsWith("logoSize:"))
			logoPercentSize = Integer.parseInt(value.replace("logoSize:", ""));
		else if ("groupLinesOn".equals(value))
			drawGroupLines = true;
		else if ("groupLinesOff".equals(value))
			drawGroupLines = false;
		else
			super.setProperty(value);
	}

	protected void groupColors() {
		IContest contest = getContest();

		ITeam[] teams = contest.getTeams();
		IGroup[] groups = contest.getGroups();
		float hue = 0f;
		for (IGroup group : groups) {
			String groupId = group.getId();
			groupColors.put(groupId, Color.getHSBColor(hue, 1f, 1f));
			hue += 1f / groups.length;
			int memberCount = 0;
			for (ITeam t : teams) {
				IOrganization org = contest.getOrganizationById(t.getOrganizationId());
				String[] groupIds = t.getGroupIds();
				if (org != null && GroupPresentation.belongsToGroup(groupIds, groupId)) {
					if (organizationGroups.containsKey(org.getId())) {
						Trace.trace(Trace.INFO, org.getName() + " belongs to " + org.getName() + " and "
								+ contest.getOrganizationById(organizationGroups.get(org.getId())).getName());
					}
					organizationGroups.put(org.getId(), groupId);
					memberCount++;
				}
			}
			Trace.trace(Trace.INFO, group.getName() + " " + groupColors.get(groupId) + " " + memberCount + " orgs");
		}
		Trace.trace(Trace.INFO, teams.length + " teams");
		Trace.trace(Trace.INFO, contest.getOrganizations().length + " orgs");
	}

	private static float hue(Color c) {
		return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null)[0];
	}

	@Override
	public void incrementTimeMs(long dt) {
		if (drawLogos) {
			if (worldLogos == null)
				worldLogos = TeamIntroPresentation.setTargets(getContest(), null, logoPercentSize * height / 100);

			BubbleOut.bubbleOut(worldLogos.instPos, worldLogos.instPos.length, width, height, 1, dt);
		}
	}

	@Override
	public void paint(Graphics2D g) {
		WorldMap.drawMap(g, width, height);

		if (organizationGroups.isEmpty())
			groupColors();

		IContest contest = getContest();
		if (contest == null)
			return;

		g.setColor(Color.RED);
		IOrganization[] orgs = contest.getOrganizations();
		for (IOrganization org : orgs) {
			double lat = org.getLatitude();
			double lon = org.getLongitude();
			Color groupColor = groupColors.get(organizationGroups.get(org.getId()));
			g.setColor(groupColor);
			if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
				int x = (int) (width * (lon + 180.0) / 360.0);
				int y = (int) (height * (90 - lat) / 180.0);
				if (!drawLogos || worldLogos == null) {
					g.fillRect(x - 3, y - 3, 6, 6);
				}

				if (drawGroupLines) {
					double a = hue(groupColor) * Math.PI * 2;
					a += (getTimeMs() % 10000) * Math.PI * 2 / 10000.0;
					double w2 = (width - 1) / 2.0, h2 = (height - 1) / 2.0;
					double m2 = Math.min(w2, h2);
					g.drawLine(x, y, (int) (w2 + m2 * Math.cos(a)), (int) (h2 + m2 * Math.sin(a)));
				}
			}
		}

		g.setColor(Color.GREEN);
		double lat = contest.getLatitude();
		double lon = contest.getLongitude();
		if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
			int x = (int) (width * (lon + 180.0) / 360.0);
			int y = (int) (height * (90 - lat) / 180.0);
			g.fillRect(x - 3, y - 3, 6, 6);
		}

		if (drawLogos && worldLogos != null) {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			for (TeamIntroPresentation.Position p : worldLogos.instPos) {
				if (p.smImage == null)
					continue;

				BufferedImage im = p.smImage;
				AffineTransform at = AffineTransform.getTranslateInstance((width * (p.x + 180.0) / 360.0),
						(height * (90 - p.y) / 180.0));
				g.drawImage(im, at, null);
			}
		}
	}
}