package org.icpc.tools.presentation.contest.internal.presentations.map;

import java.awt.Color;
import java.awt.Graphics2D;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;

public class WorldPresentation extends AbstractICPCPresentation {
	@Override
	public void init() {
		WorldMap.load(getClass());
	}

	@Override
	public void paint(Graphics2D g) {
		WorldMap.drawMap(g, width, height);

		IContest contest = getContest();
		if (contest == null)
			return;

		g.setColor(Color.RED);
		IOrganization[] orgs = contest.getOrganizations();
		for (IOrganization org : orgs) {
			double lat = org.getLatitude();
			double lon = org.getLongitude();
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