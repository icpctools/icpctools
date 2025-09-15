package org.icpc.tools.presentation.contest.internal.presentations.old;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

/**
 * Show teams that solved a problem presentation
 */
public class SolvedPresentation extends AbstractICPCPresentation {
	private static final int BORDER = 20;
	private static final int TEAM_SPACING = 5;
	private static final Color bgColor = new Color(0, 0, 0, 196);

	protected List<ISubmission> runs = new ArrayList<>();

	protected IContestListener listener = (contest, obj, d) -> {
		if (obj instanceof ISubmission)
			handleRun((ISubmission) obj);
	};

	protected ISubmission currentRun;

	private BufferedImage teamImage;
	private BufferedImage teamLogo;

	protected Font font;

	@Override
	public void aboutToShow() {
		super.aboutToShow();
		currentRun = null;

		// cull oldest if we get too many
		IContest contest = getContest();
		synchronized (runs) {
			int size = runs.size();
			if (size > 5) {
				int index = 0;
				for (int i = 0; i < size - 5 && index < size; i++) {
					ISubmission run = runs.get(index);
					if (contest.isFirstToSolve(run))
						index++;
					else
						runs.remove(index);
				}
			}
		}
	}

	@Override
	public void init() {
		super.init();
		getContest().addListener(listener);
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		final float dpi = 96;
		font = ICPCFont.getMasterFont().deriveFont(Font.BOLD, height / dpi / 10f * 72f);
	}

	@Override
	public void dispose() {
		super.dispose();
		getContest().removeListener(listener);
	}

	public void handleRun(ISubmission submission) {
		if (!getContest().isSolved(submission))
			return;

		synchronized (runs) {
			runs.add(submission);
		}
	}

	private void loadImages(ITeam team) {
		teamImage = team.getPhotoImage(width, height, true, true);

		IOrganization org = getContest().getOrganizationById(team.getOrganizationId());
		if (org == null)
			return;

		float maxSize = height / 5f;
		teamLogo = org.getLogoImage((int) maxSize, (int) maxSize, getModeTag(), true, true);
	}

	@Override
	public void paint(Graphics2D g) {
		if (currentRun == null) {
			if (runs.isEmpty())
				return;

			synchronized (runs) {
				currentRun = runs.get(0);
				runs.remove(0);
			}

			teamImage = null;
			teamLogo = null;
		}

		IContest contest = getContest();
		ITeam team = contest.getTeamById(currentRun.getTeamId());
		if (teamImage == null)
			loadImages(team);

		if (teamImage != null)
			g.drawImage(teamImage, (width - teamImage.getWidth()) / 2, 0, null);

		// calculate height
		int h = TEAM_SPACING;

		int x = BORDER;
		int logoSize = (int) (height / 5f);
		if (teamLogo != null)
			x += logoSize + BORDER;

		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		h += fm.getHeight();
		String name = team.getActualDisplayName();
		int usableWidth = width - x - BORDER;
		if (fm.stringWidth(name) > usableWidth) {
			int split = name.length() / 2 - 4;
			while (split < name.length() && !" ".equals(name.substring(split, split + 1)))
				split++;
			if (split < name.length()) {
				name = name.substring(0, split) + "\n" + name.substring(split + 1);
				h += fm.getHeight();
			}
		}

		// g.setFont(solvedFont);
		// fm = g.getFontMetrics();
		h += fm.getHeight();

		if (teamLogo != null)
			h = Math.max(h, teamLogo.getHeight());

		g.setColor(bgColor);
		g.fillRect(0, height - h - BORDER * 2, width, h + BORDER * 2);

		if (teamLogo != null)
			g.drawImage(teamLogo, BORDER + (logoSize - teamLogo.getWidth()) / 2, height - h - BORDER, null);
		// + (logoSize - teamLogo.getHeight()) / 2, null);

		int y = height - h - BORDER;
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g.setColor(Color.WHITE);
		if (name.indexOf("\n") == -1)
			g.drawString(name, x, y + fm.getAscent());
		else {
			String n = name.substring(0, name.indexOf("\n"));
			g.drawString(n, x, y + fm.getAscent());
			y += fm.getHeight();
			n = name.substring(name.indexOf("\n") + 1);
			g.drawString(n, x, y + fm.getAscent());
		}
		y += fm.getHeight() + TEAM_SPACING;

		// g.setFont(solvedFont);
		// fm = g.getFontMetrics();
		String s = "solved problem " + currentRun.getProblemId();
		if (contest.isFirstToSolve(currentRun))
			s += " first";
		s += " in " + ContestUtil.getTimeInMin(currentRun.getContestTime()) + " minutes!";
		g.drawString(s, x, y + fm.getAscent());
	}
}