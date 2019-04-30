package org.icpc.tools.resolver;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;

public class TeamLogoPresentation extends AbstractICPCPresentation {
	private BufferedImage logo;

	public void setTeam(String teamId) {
		logo = null;

		IContest contest = getContest();
		ITeam team = contest.getTeamById(teamId);
		if (team == null)
			return;

		IOrganization org = contest.getOrganizationById(team.getOrganizationId());
		if (org == null)
			return;

		logo = org.getLogoImage(width, height, true, true);
	}

	@Override
	public void paint(Graphics2D g) {
		if (logo != null)
			g.drawImage(logo, (width - logo.getWidth()) / 2, (height - logo.getHeight()) / 2, null);
	}
}