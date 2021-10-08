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
import org.icpc.tools.presentation.contest.internal.nls.Messages;

/**
 * Timeline version of the scoreboard.
 */
public class TimelinePresentation extends AbstractScrollingScoreboardPresentation {
	private static final Color FREEZE_COLOR = org.icpc.tools.contest.model.ICPCColors
			.alphaDarker(ICPCColors.PENDING_COLOR, 64, 1f);
	private static final int MS_PER_HOUR = 1000 * 60 * 60;
	protected Font cubeFont;
	protected Image arrowImg;
	protected int start;
	protected double scale;

	@Override
	protected String getTitle() {
		return Messages.titleCurrentStandings;
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		cubeFont = ICPCFont.deriveFont(Font.BOLD, rowFont.getSize() * 0.8f);
		arrowImg = null;
	}

	protected void drawTimeArrow(Graphics2D g2, int y) {
		int sx = getX(0);
		if (arrowImg == null) {
			arrowImg = new BufferedImage(width - sx - 16, 18, Transparency.TRANSLUCENT);
			Graphics2D g = (Graphics2D) arrowImg.getGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g.translate(-sx + 2, 9);
			g.setColor(Color.LIGHT_GRAY);
			g.setStroke(new BasicStroke(2f));
			IContest contest = getContest();
			g.drawLine(sx, 0, width - 20, 0);
			g.drawLine(width - 27, -7, width - 20, 0);
			g.drawLine(width - 27, +7, width - 20, 0);

			int numHours = contest.getDuration() / MS_PER_HOUR;

			int hour = 0;
			while (hour < numHours) {
				int x = getX(hour * MS_PER_HOUR);
				g.drawLine(x, -4, x, 4);
				hour++;
			}

			g.dispose();
		}
		g2.drawImage(arrowImg, sx - 2, y - 9, null);
	}

	@Override
	protected void drawHeader(Graphics2D g) {
		g.setFont(rowFont);
		FontMetrics fm = g.getFontMetrics();
		g.setFont(headerFont);
		FontMetrics fm2 = g.getFontMetrics();

		g.setColor(isLightMode() ? Color.WHITE : Color.BLACK);
		g.fillRect(0, 0, width, headerHeight + 2);
		g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
		g.drawLine(0, headerHeight - 1, width, headerHeight - 1);
		int y = headerHeight - 3;

		g.setFont(headerItalicsFont);
		g.drawString(Messages.rank, BORDER + (fm.stringWidth("199") - fm2.stringWidth(Messages.rank)) / 2, y);
		g.setFont(headerFont);
		g.drawString(Messages.name, BORDER + fm.stringWidth("199 ") + (int) rowHeight, y);
		g.setFont(headerItalicsFont);
		g.drawString(Messages.solved,
				width - BORDER - fm.stringWidth(" 9999") - (fm2.stringWidth(Messages.solved) + fm.stringWidth("99")) / 2,
				y);
		g.setFont(headerFont);
		g.drawString(Messages.time, width - BORDER - (fm2.stringWidth(Messages.time) + fm.stringWidth("9999")) / 2, y);
	}

	@Override
	protected void paintImpl(Graphics2D g) {
		IContest contest = getContest();
		if (contest == null)
			return;

		g.setFont(rowFont);
		FontMetrics fm = g.getFontMetrics();
		start = BORDER + fm.stringWidth("199 ") + (int) rowHeight;

		scale = (width - start - 20) / (double) contest.getDuration();

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
			currentTime = (int) ((getTimeMs() - state.getStarted()) * contest.getTimeMultiplier());
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
				drawTimeArrow(g4, (int) (rowHeight * 3 / 4f) - 1);

				drawTeamGrid(g4, team);

				g4.setFont(cubeFont);
				g4.translate(0, (int) (rowHeight * 3 / 4f) - 1);

				int up = 1;

				ISubmission[] submissions = contest.getSubmissions();

				for (ISubmission submission : submissions) {
					if (submission.getTeamId().equals(team.getId())) {
						boolean closeToNeighbor = false;
						for (ISubmission s : submissions) {
							if (submission != s && s.getTeamId().equals(team.getId())
									&& Math.abs(submission.getContestTime() - s.getContestTime()) * scale < cubeHeight)
								closeToNeighbor = true;
						}

						double xx = getX(submission.getContestTime()) - cubeHeight / 2.0;

						if (closeToNeighbor)
							g4.translate(0, -offset * up);

						IProblem p = contest.getProblemById(submission.getProblemId());
						ShadedRectangle.drawRoundRect(g4, (int) xx, -cubeHeight / 2, cubeHeight, cubeHeight, contest,
								submission, getTimeMs(), p.getLabel());

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
		return start + (int) (contestTimeMs * scale);
	}

	@Override
	protected void paintLegend(Graphics2D g) {
		g.translate(width - 30, height - 230);
		Legend.drawLegend(g, isLightMode());
		g.translate(30 - width, -(height - 230));
	}
}