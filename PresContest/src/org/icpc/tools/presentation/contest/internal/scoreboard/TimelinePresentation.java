package org.icpc.tools.presentation.contest.internal.scoreboard;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IState;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.Legend;
import org.icpc.tools.presentation.contest.internal.ShadedRectangle;

/**
 * Timeline version of the scoreboard.
 */
public class TimelinePresentation extends AbstractScrollingScoreboardPresentation {
	private static final Color FREEZE_COLOR = org.icpc.tools.contest.model.ICPCColors.alphaDarker(ICPCColors.PENDING_COLOR,
			64, 1f);
	private static final int MS_PER_HOUR = 1000 * 60 * 60;
	protected Font cubeFont;
	protected Image arrowImg;
	protected double scale;

	@Override
	protected String getTitle() {
		return "Current Standings";
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		cubeFont = ICPCFont.getMasterFont().deriveFont(Font.BOLD, rowFont.getSize() * 0.8f);
		arrowImg = null;
	}

	protected void drawTimeArrow(Graphics2D g2, int y) {
		if (arrowImg == null) {
			arrowImg = new BufferedImage(width - 30, 18, Transparency.TRANSLUCENT);
			Graphics2D g = (Graphics2D) arrowImg.getGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g.translate(-15, 0);
			int h = 9;
			g.setColor(Color.LIGHT_GRAY);
			// Stroke oldStroke = g.getStroke();
			g.setStroke(new BasicStroke(2f));
			g.drawLine(20, h, width - 20, h);
			g.drawLine(width - 27, h - 7, width - 20, h);
			g.drawLine(width - 27, h + 7, width - 20, h);
			// g.setStroke(oldStroke);

			IContest contest = getContest();
			int numHours = contest.getDuration() / MS_PER_HOUR;

			int hour = 0;
			while (hour < numHours) {
				int x = getX(hour * MS_PER_HOUR);
				g.drawLine(x, h - 4, x, h + 4);
				hour++;
			}

			g.dispose();
		}
		g2.drawImage(arrowImg, 15, y, null);
	}

	@Override
	protected void drawHeader(Graphics2D g) {
		g.setFont(rowFont);
		FontMetrics fm = g.getFontMetrics();
		g.setFont(headerFont);
		FontMetrics fm2 = g.getFontMetrics();

		g.setColor(Color.black);
		g.fillRect(0, 0, width, headerHeight + 2);
		g.setColor(Color.white);
		g.drawLine(0, headerHeight - 1, width, headerHeight - 1);
		int y = headerHeight - 3;

		g.setFont(headerItalicsFont);
		g.drawString("Rank", BORDER + (fm.stringWidth("199") - fm2.stringWidth("Rank")) / 2, y);
		g.setFont(headerFont);
		g.drawString("Name", BORDER + fm.stringWidth("199 ") + (int) rowHeight, y);
		g.setFont(headerItalicsFont);
		g.drawString("Solved",
				width - BORDER - fm.stringWidth(" 9999") - (fm2.stringWidth("Solved") + fm.stringWidth("99")) / 2, y);
		g.setFont(headerFont);
		g.drawString("Time", width - BORDER - (fm2.stringWidth("Time") + fm.stringWidth("9999")) / 2, y);
	}

	@Override
	protected void paintImpl(Graphics2D g) {
		IContest contest = getContest();
		if (contest == null)
			return;

		scale = (width - 45.0) / contest.getDuration();

		int scroll2 = paintBarsAndScroll(contest, g);

		// draw line for freeze time
		ITeam[] teams = contest.getOrderedTeams();
		int numTeams = teams.length;
		Stroke oldStroke = g.getStroke();
		g.setStroke(new BasicStroke(2f));
		g.setColor(FREEZE_COLOR);

		int ct = getX(contest.getDuration() - contest.getFreezeDuration());
		g.drawLine(ct, 0, ct, (int) (numTeams * rowHeight));

		// draw vertical line for current time
		g.setColor(ICPCColors.BLUE);
		int currentTime = 0;
		IState state = contest.getState();
		if (state.getEnded() != null)
			currentTime = contest.getDuration();
		else if (state.getStarted() != null)
			currentTime = (int) (getTimeMs() - state.getStarted());
		ct = getX(currentTime);
		g.drawLine(ct, 0, ct, (int) (numTeams * rowHeight));
		g.setStroke(oldStroke);

		int offset = cubeHeight * 3 / 16;

		// draw teams - fill in each team's bar (rectangle) with team and problem info
		// in the manner defined by the parent class's drawTeamAndProblemGrid() method
		for (int i = numTeams - 1; i >= 0; i--) {
			ITeam team = teams[i];
			double y = getTeamY(team);
			if ((y + rowHeight - scroll2) > 0 && (y - scroll2) < (height - headerHeight)) {
				Graphics2D g4 = (Graphics2D) g.create();
				g4.translate(0, (int) y);

				// draw timeline
				drawTimeArrow(g4, (int) (rowHeight * 3f / 4f) - 13);

				drawTeamGrid(g4, team);

				g4.setFont(cubeFont);

				int up = 1;

				ISubmission[] submissions = contest.getSubmissions();

				for (ISubmission submission : submissions) {
					if (submission.getTeamId().equals(team.getId())) {
						boolean closeToNeighbor = false;
						int dt = (int) ((cubeHeight + 2) / scale); // 12
						for (ISubmission s : submissions) {
							if (submission != s && s.getTeamId().equals(team.getId())
									&& Math.abs(submission.getContestTime() - s.getContestTime()) / 1000 / 60 < dt)
								closeToNeighbor = true;
						}

						double xx = getX(submission.getContestTime()) - cubeHeight / 2.0;

						if (closeToNeighbor)
							g4.translate(0, -offset * up);

						IProblem p = contest.getProblemById(submission.getProblemId());
						ShadedRectangle.drawRoundRect(g4, (int) xx, (int) (rowHeight / 2 + CUBE_INSET / 2) - 3, cubeHeight,
								cubeHeight, contest, submission, getTimeMs(), p.getLabel());

						if (closeToNeighbor) {
							g4.translate(0, offset * up);
							up = -up;
						} else
							up = 1;
					}
				}

				g4.dispose();
			}
		}
	}

	private int getX(int contestTimeMs) {
		return 20 + (int) (contestTimeMs * scale);
	}

	@Override
	protected void paintLegend(Graphics2D g) {
		g.translate(width - 30, height - 230);
		Legend.drawLegend(g);
		g.translate(30 - width, -(height - 230));
	}
}