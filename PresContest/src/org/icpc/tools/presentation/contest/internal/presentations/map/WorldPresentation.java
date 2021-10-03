package org.icpc.tools.presentation.contest.internal.presentations.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Map;
import java.util.TreeMap;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;

public class WorldPresentation extends AbstractICPCPresentation {
	@Override
	public void init() {
		WorldMap.load(getClass());
	}

	Map<String, String> organizationGroups = new TreeMap<>();
	Map<String, Color> groupColors = new TreeMap<>();
	void groupColors() {
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
						System.out.println("" + org.getName() + " belongs to " + org.getName() + " and " +
								contest.getOrganizationById(organizationGroups.get(org.getId())).getName());
					}
					organizationGroups.put(org.getId(), groupId);
					memberCount++;
				}
			}
			System.out.println(group.getName() + " " + groupColors.get(groupId) + " " + memberCount + " orgs");
		}
		System.out.println("" + teams.length + " teams");
		System.out.println("" + contest.getOrganizations().length + " orgs");
	}

	@Override
	public void paint(Graphics2D g) {
		WorldMap.drawMap(g, width, height);

		if (organizationGroups.isEmpty()) {
			groupColors();
		}

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
				g.fillRect(x - 3, y - 3, 6, 6);
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
	}
}