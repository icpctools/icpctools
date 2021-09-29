package org.icpc.tools.presentation.contest.internal.tile;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;

public class TilePictureScoreboardPresentation extends AbstractICPCPresentation {
	private ITeam team;

	private BufferedImage teamImage = null;
	private TeamTileHelper tileHelper;

	public void setTeam(final ITeam team) {
		teamImage = null;
		this.team = team;

		if (team != null) {
			execute(new Runnable() {
				@Override
				public void run() {
					teamImage = team.getPhotoImage(width, height, true, true);
				}
			});
		}
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		if (d.width == 0 || d.height == 0)
			return;

		tileHelper = new TeamTileHelper(new Dimension(width * 5 / 6, height / 9), getContest());
	}

	@Override
	public void paint(Graphics2D g) {
		IContest c = getContest();
		if (c == null || c.getNumTeams() == 0)
			return;

		if (teamImage != null)
			g.drawImage(teamImage, (width - teamImage.getWidth()) / 2, 0, null);

		if (team != null)
			tileHelper.paintTile(g, width / 12, height * 17 / 20, team, getTimeMs());
	}

	@Override
	public void setProperty(String value) {
		if (value.startsWith("team:")) {
			try {
				setTeam(getContest().getTeamById(value.substring(5)));
			} catch (Exception e) {
				// ignore
			}
		}
	}
}