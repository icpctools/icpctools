package org.icpc.tools.presentation.contest.internal.scoreboard;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.resolver.SelectType;
import org.icpc.tools.contest.model.resolver.SubmissionInfo;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.Legend;
import org.icpc.tools.presentation.contest.internal.ShadedRectangle;
import org.icpc.tools.presentation.contest.internal.nls.Messages;

/**
 * Official contest scoreboard.
 */
public class ScoreboardPresentation extends AbstractScrollingScoreboardPresentation {
	private static final int MARGIN = 15;
	private static final int GAP = 5;

	private boolean showSubmissionInfo;
	private boolean showLegend = true;

	@Override
	protected String getTitle() {
		return Messages.titleCurrentStandings;
	}

	public void setShowSubmissionInfo(boolean showInfo) {
		showSubmissionInfo = showInfo;
	}

	public boolean getShowSubmissionInfo() {
		return showSubmissionInfo;
	}

	/**
	 * Allows external clients (for example, a Resolver) to enable/disable display of the Legend
	 * panel which describes the meanings of the round boxes in the problem display portion of the
	 * team grid. The default value for showing legends is true.
	 *
	 * @param yesNo - boolean indicating whether or not the Legend should be displayed
	 */
	public void setShowLegend(boolean yesNo) {
		showLegend = yesNo;
	}

	/**
	 * This method overrides an inherited empty-body method from the parent
	 * AbstractScoreboardPresentation class; it is the primary method for repainting the appearance
	 * of this ScoreboardPresentation object. It draws a header, whose appearance is defined by the
	 * parent class, at the top of the presentation. It then draws a row for each team containing
	 * the team name and the team status on each problem (solved, not solved, or has pending
	 * submissions). It also draws an "extra presenter-info" box if that option was selected.
	 */
	@Override
	protected void paintImpl(Graphics2D g) {
		IContest contest = getContest();
		if (contest == null)
			return;

		int scroll2 = paintBarsAndScroll(contest, g);

		ITeam[] teams = contest.getOrderedTeams();

		// draw teams - fill in each team's bar (rectangle) with team and problem info
		// in the manner defined by the parent class's drawTeamAndProblemGrid() method
		for (int i = teams.length - 1; i >= 0; i--) {
			ITeam team = teams[i];
			double y = getTeamY(team);
			if ((y + rowHeight - scroll2) > 0 && (y - scroll2) < (height - headerHeight)) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.translate(0, (int) y);
				drawTeamAndProblemGrid(g2, team);
				g2.dispose();
			}
		}

		// draw stats row
		Graphics2D g2 = (Graphics2D) g.create();
		g2.translate(0, (int) (teams.length * rowHeight));
		drawStatsRow(g2);
		g2.dispose();

		// draw an info box if that option was selected
		if (showSubmissionInfo) {
			// check the current SubmissionInfo object, which is set in Resolver.resolveToRow() to be
			// a
			// pending submission on the currently selected team row
			SubmissionInfo runInfo = selectedSubmission;
			if (runInfo != null) {
				// there is a pending submission; calculate the vertical position for the box
				ITeam team = runInfo.getTeam();
				double y = getTeamY(team);
				Graphics2D g4 = (Graphics2D) g.create();
				g4.translate(0, (int) y);
				// draw the info box with the specified SubmissionInfo object data in it
				paintSubmissionInfo(g4, runInfo, y > height / 2);
				g4.dispose();
			} else {
				// there's no pending submission; see if maybe we should draw a FTS info box instead
				if (selectedTeams != null && (selectType == SelectType.FTS || selectType == SelectType.FTS_HIGHLIGHT)) {
					for (ITeam selectedTeam : selectedTeams) {
						double y = getTeamY(selectedTeam);
						Graphics2D g4 = (Graphics2D) g.create();
						g4.translate(0, (int) y);
						paintFTSInfoBox(g4, selectedTeam, y > height / 2);
						g4.dispose();
					}
				}
			}
		}
	}

	@Override
	protected void paintLegend(Graphics2D g) {
		if (showLegend) {
			g.translate(width - 30, height - 200);
			Legend.drawLegend(g, isLightMode());
			g.translate(30 - width, 200 - height);
		}
	}

	/**
	 * This method draws a rectangle containing the "what-if" info data for a given pending
	 * submission and current selected team status. This data is contained in an object of type
	 * SubmissionInfo.
	 *
	 * @param g2 - the graphics object to be used to draw the info box
	 * @param submissionInfo - the SubmissionInfo object containing the info data regarding what
	 *           happens when the current pending submission is resolved
	 * @param alignTop - boolean flag specifying how to manage vertical alignment of the info box
	 *           with respect to the row containing the current pending submission
	 */
	private void paintSubmissionInfo(Graphics2D g2, SubmissionInfo submissionInfo, boolean alignTop) {
		// g2.setFont(rowFont);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setFont(AbstractScoreboardPresentation.statusFont);

		FontMetrics fm = g2.getFontMetrics();

		IStanding[] ifSolved = submissionInfo.getStandingIfSolved();
		IStanding[] bestCase = submissionInfo.getStandingBestCase();
		IStanding[] worstCase = submissionInfo.getStandingWorstCase();

		int tw = fm.stringWidth("Guaranteed position:");
		if (bestCase != null)
			tw = fm.stringWidth("Best case with other submission:");

		ITeam team = submissionInfo.getTeam();
		String teamId = team.getId();
		int problemInd = submissionInfo.getProblemIndex();

		int numRuns = 0;
		int numRunsBefore = 0;
		IContest contest = getContest();
		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission submission : submissions) {
			if (submission.getTeamId().equals(teamId)
					&& submission.getProblemId().equals(contest.getProblems()[problemInd].getId())) {
				numRuns++;
				if (contest.isJudged(submission))
					numRunsBefore++;
			}
		}

		// TODO skip submissions if there are too many
		int skipBefore = 0;
		int skipAfter = 0;
		int skipAfter2 = 0;
		if (numRunsBefore > 3)
			skipBefore = numRunsBefore - 3;
		if (numRuns - numRunsBefore > 7) {
			skipAfter = (numRuns - numRunsBefore) - 7;
			skipAfter2 = skipAfter - 1;
		}

		ISubmission[] submissions2 = new ISubmission[numRuns];
		int c = 0;
		for (ISubmission submission : submissions) {
			if (submission.getTeamId().equals(teamId)
					&& submission.getProblemId().equals(contest.getProblems()[problemInd].getId())) {
				submissions2[c] = submission;
				c++;
			}
		}

		// g2.setColor(new Color(0, 0, 0, 220));
		int w = tw + (cubeWidth + GAP) * (numRuns - skipBefore - skipAfter2);
		int h = (cubeHeight + GAP) * 4;
		if (bestCase != null)
			h += (cubeHeight + GAP) * 2;

		// if (ifSolved.getNumSolved() != highlight.getStandingBestCase().getNumSolved())
		// h += fm.getHeight();
		// int y = headerHeight + MARGIN;
		// float y2 = getTeamY(team);
		// int y = (int) (y2 - height - MARGIN);
		int y = (int) rowHeight + MARGIN;
		if (alignTop)
			y = -(h + MARGIN);
		/*try {
			if (Integer.parseInt(highlight.getTeam().getStanding().getRank()) < TEAMS_PER_SCREEN - 4)
				y = height - h - MARGIN * 2;
		} catch (Exception e) {
			// ignore
		}*/

		g2.setColor(ICPCColors.SELECTION_COLOR);
		g2.fillRect((width - w) / 2 - MARGIN, y - MARGIN, w + MARGIN * 2, h + MARGIN * 2);
		// y += fm.getAscent();

		// draw text
		int xx = (width - w) / 2;
		int yy = y + cubeHeight + fm.getAscent() + GAP;

		g2.setColor(Color.WHITE);
		g2.drawString("If solved:", xx, yy);
		yy += (fm.getHeight() + GAP) * 2;
		if (bestCase != null) {
			g2.drawString("Best case with other submissions:", xx, yy);
			yy += (fm.getHeight() + GAP) * 2;
		}
		g2.drawString("Guaranteed position:", xx, yy);

		xx = (width - w) / 2 + tw + GAP;

		for (int i = 0; i < numRunsBefore; i++) {
			String s = ContestUtil.getTime(submissions2[i].getContestTime()) + "";

			if (i == 11 && numRunsBefore > 12)
				s = "+" + (numRunsBefore - 11);

			ShadedRectangle.drawRoundRect(g2, xx + (i % 3) * (cubeWidth + GAP), y + (cubeHeight + GAP) * (i / 3),
					cubeWidth, cubeHeight, contest, submissions2[i], 0, s);
			if (i == 11)
				break;
		}

		xx = (width - w) / 2 + Math.min(numRunsBefore, 3) * (cubeWidth + GAP) + tw + GAP;

		g2.drawLine(xx - GAP / 2 - 1, y, xx - GAP / 2 - 1, y + cubeHeight);
		g2.drawLine(xx - GAP / 2, y, xx - GAP / 2, y + cubeHeight);

		int jj = 0;
		for (int i = numRunsBefore; i < numRuns - skipAfter; i++) {
			String s = ContestUtil.getTime(submissions2[i].getContestTime()) + "";

			ShadedRectangle.drawRoundRect(g2, xx, y, cubeWidth, cubeHeight, contest, submissions2[i], 0, s);

			yy = y + cubeHeight + fm.getAscent() + GAP;
			if (!contest.isJudged(submissions2[i])) {
				if (ifSolved != null) {
					IStanding standing = ifSolved[jj];
					s = "# " + standing.getRank();
					g2.drawString(s, xx + (cubeWidth - fm.stringWidth(s)) / 2, yy);
					yy += fm.getHeight() + GAP;
					s = standing.getNumSolved() + " - " + standing.getTime();
					g2.drawString(s, xx + (cubeWidth - fm.stringWidth(s)) / 2, yy);
				}

				if (bestCase != null) {
					IStanding standing = bestCase[jj];
					s = "# " + standing.getRank();
					yy += fm.getHeight() + GAP;
					g2.drawString(s, xx + (cubeWidth - fm.stringWidth(s)) / 2, yy);
					s = standing.getNumSolved() + " - " + standing.getTime();
					yy += fm.getHeight() + GAP;
					g2.drawString(s, xx + (cubeWidth - fm.stringWidth(s)) / 2, yy);
				}

				if (worstCase != null && worstCase[jj] != null) {
					IStanding standing = worstCase[jj];
					s = standing.getRank();
					yy += fm.getHeight() + GAP;
					g2.drawString(s, xx + (cubeWidth - fm.stringWidth(s)) / 2, yy);
				}

				jj++;
			}

			xx += cubeWidth + GAP;
		}

		if (skipAfter > 0) {
			String s = "+" + skipAfter;
			ShadedRectangle.drawRoundRect(g2, xx, y, cubeWidth, cubeHeight, contest, submissions2[numRunsBefore], 0, s);
		}

		g2.drawRect((width - w) / 2 - MARGIN, y - MARGIN, w + MARGIN * 2, h + MARGIN * 2);
	}

	/**
	 * Paints an info box containing FTS information (a list of the problems which the specified
	 * team was first-to-solve).
	 *
	 * @param g2 - the graphics object to be used to do the drawing
	 * @param selected - the currently selected team
	 * @param alignTop - a flag indicating whether to display the FTS Info box above or below the
	 *           team grid row
	 */
	private void paintFTSInfoBox(Graphics2D g2, ITeam selected, boolean alignTop) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setFont(AbstractScoreboardPresentation.statusFont);

		FontMetrics fm = g2.getFontMetrics();

		String ss = "First to solve problem ";

		// build an arraylist of strings, one for each FTS problem for the team
		List<String> list = new ArrayList<>();
		IContest contest = getContest();
		IProblem[] p = contest.getProblems();
		for (int i = 0; i < p.length; i++) {
			if (contest.getResult(selected, i).isFirstToSolve())
				list.add(ss + p[i].getLabel());
		}

		// calculate the info box dimensions from the number and size of the strings
		int w = fm.stringWidth(ss + "XX");
		int h = (list.size() + GAP) * 3 - GAP;

		int y = (int) rowHeight + MARGIN;
		if (alignTop)
			y = -(h + MARGIN);

		// draw infobox background rectangle in FTS color
		g2.setColor(ICPCColors.FIRST_TO_SOLVE_COLOR);
		g2.fillRect((width - w) / 2 - MARGIN, y - MARGIN, w + MARGIN * 2, h + MARGIN * 2);

		// draw a white highlight rectangle around the edge of the infobox
		g2.setColor(Color.WHITE);
		g2.drawRect((width - w) / 2 - MARGIN, y - MARGIN, w + MARGIN * 2, h + MARGIN * 2);

		// draw each of the problem strings into the infobox
		y += fm.getAscent();
		for (String s : list) {
			g2.drawString(s, (width - w) / 2, y);
			y += fm.getHeight() + GAP;
		}
	}
}