package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.TextHelper;

public class TeamDetailPresentation extends AbstractICPCPresentation {
	private static final int MARGIN = 15;

	private Font font;
	private String prefix;

	class TeamInfo {
		BufferedImage photo;
		BufferedImage logo;
		String name;
		String id;
	}

	private BufferedImage contestImage;
	private ITeam team;
	private TeamInfo info;

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		final float dpi = 96;
		font = ICPCFont.deriveFont(Font.BOLD, height * 36f / 6f / dpi);
	}

	private void setTeam(ITeam newTeam) {
		Trace.trace(Trace.INFO, "Team: " + newTeam);
		team = newTeam;

		if (team == null) {
			info = null;
			return;
		}

		TeamInfo newInfo = new TeamInfo();
		newInfo.photo = team.getPhotoImage((int) (width * 0.7), (int) ((height - MARGIN * 2) * 0.7), true, true);
		String orgId = team.getOrganizationId();
		IOrganization org = getContest().getOrganizationById(orgId);
		if (org != null) {
			double scale = 0.32;
			if (newInfo.photo == null) {
				// if there's no photo, load a larger logo
				scale = 0.7;
			}
			newInfo.logo = org.getLogoImage((int) (width * scale), (int) ((height - MARGIN * 2) * scale), true, true);
		}
		newInfo.id = team.getId();
		newInfo.name = team.getLabel() + " – " + team.getActualDisplayName();

		TeamInfo oldInfo = info;
		info = newInfo;
		if (oldInfo != null) {
			synchronized (oldInfo) {
				if (oldInfo.logo != null) {
					oldInfo.logo.flush();
				}
				if (oldInfo.photo != null) {
					oldInfo.photo.flush();
				}
			}
		}
	}

	@Override
	public long getDelayTimeMs() {
		return 0;
	}

	@Override
	public void aboutToShow() {
		getContest();
		setTeam(team);
	}

	@Override
	public void paint(Graphics2D g) {
		if (contestImage == null)
			contestImage = getContest().getLogoImage((int) (width * 0.7), (int) ((height - MARGIN * 2) * 0.7), true, true);

		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		TeamInfo ti = info;
		if (ti != null) {
			synchronized (ti) {
				if (ti.photo != null)
					g.drawImage(ti.photo, (width - ti.photo.getWidth()) / 2, (height - ti.photo.getHeight()) / 2, null);
				if (ti.logo != null) {
					if (ti.photo != null) {
						// if there's a photo, put the logo on the top right
						int y = Math.max(0, (height - ti.photo.getHeight() - ti.logo.getHeight()) / 2);
						g.drawImage(ti.logo, (width + ti.photo.getWidth() - ti.logo.getWidth()) / 2, y, null);
					} else {
						// otherwise, center it on the screen
						g.drawImage(ti.logo, (width - ti.logo.getWidth()) / 2, (height - ti.logo.getHeight()) / 2, null);
					}
				}

				g.setColor(Color.WHITE);
				g.setFont(font);
				FontMetrics fm = g.getFontMetrics();

				if (ti.name != null) {
					String[] s = splitString(g, ti.name, width - MARGIN * 2);
					for (int i = 0; i < s.length; i++) {
						TextHelper.drawString(g, s[i], (width - fm.stringWidth(s[i])) / 2,
								height - fm.getDescent() - MARGIN - (s.length - i - 1) * fm.getHeight());
					}
				}
			}
		} else if (contestImage != null)
			g.drawImage(contestImage, (width - contestImage.getWidth()) / 2, (height - contestImage.getHeight()) / 2,
					null);
	}

	@Override
	public void setProperty(String value) {
		if (value == null) {
			return;
		}
		if (value.startsWith("prefix:")) {
			prefix = value.substring(7);
			Trace.trace(Trace.INFO, "Prefix set: " + prefix);
			return;
		}
		String val = value;
		if (prefix != null && !prefix.isEmpty()) {
			if (!value.startsWith(prefix + ":"))
				return;
			val = value.substring(prefix.length() + 1);
		}
		if (val.trim().isEmpty()) {
			setTeam(null);
			return;
		}
		ITeam newTeam = getContest().getTeamById(val);
		if (newTeam != null) {
			setTeam(newTeam);
			return;
		}
		ITeam[] teams = getContest().getTeams();
		for (ITeam t : teams) {
			if (t.getLabel().equals(val)) {
				setTeam(t);
				return;
			}
		}
	}
}
