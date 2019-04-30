package org.icpc.tools.presentation.contest.internal.presentations.old;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.Utility;

public class TeamPicturesPresentation extends AbstractICPCPresentation {
	private static final int SECONDS_PER_TEAM = 10;

	class PicInfo {
		String label;
		BufferedImage image;
	}

	private Font font;
	private final List<PicInfo> diffs = new ArrayList<>();
	private PicInfo current;

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		final float dpi = 96;
		float inch = height / dpi / 10f;
		font = ICPCFont.getMasterFont().deriveFont(Font.BOLD, inch * 72f * 0.9f);
	}

	@Override
	public long getRepeat() {
		return SECONDS_PER_TEAM * 1000L;
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();
		if (diffs.isEmpty()) {
			current = null;
			return;
		}
		synchronized (diffs) {
			current = diffs.remove(0);
		}
	}

	@Override
	public void paint(Graphics2D g) {
		if (current == null)
			return;

		g.setColor(Color.white);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		String[] st = BalloonPresentation.tokenize(current.label, fm, width - 50);
		int size = st.length;
		int h = fm.getHeight() * size;

		if (current.image != null) {
			int yy = (height - current.image.getHeight() - 90) / 2;
			if (yy < 5)
				yy = 5;
			g.drawImage(current.image, (width - current.image.getWidth()) / 2, yy, null);
		}

		int y = height - 100 - h + fm.getAscent();
		if (current.image == null)
			y = (height - h) / 2 - fm.getAscent();
		for (int i = 0; i < size; i++) {
			// g.drawString(st[i], (width - fm.stringWidth(st[i])) / 2, y);
			Utility.drawString3D(g, st[i], (width - fm.stringWidth(st[i])) / 2, y);
			y += fm.getHeight();
		}
	}

	public void handleRuns(ISubmission[] runs) {
		IContest contest = getContest();
		for (ISubmission run : runs) {
			if (contest.isSolved(run)) {
				PicInfo pi2 = new PicInfo();
				String pId = run.getProblemId();
				String t = contest.getTeamById(run.getTeamId()).getName();
				pi2.label = t + " solved problem " + pId + "!";

				try {
					ITeam team = contest.getTeamById(run.getTeamId());
					if (team != null) {
						IOrganization org = contest.getOrganizationById(team.getOrganizationId());
						if (org != null)
							pi2.image = org.getLogoImage(width, height, true, true);
					}
				} catch (Exception e) {
					// ignore - no image
				}
				synchronized (diffs) {
					diffs.add(pi2);
				}
			}
		}
	}
}