package org.icpc.tools.resolver;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.TeamUtil;
import org.icpc.tools.presentation.contest.internal.scoreboard.JudgePresentation;

public class JudgePresentation2 extends JudgePresentation {
	public JudgePresentation2() {
		setProperty("clockOff");
		timeToKeepFailed = 1250;
		timeToKeepSolved = 8000;
		reservedRows = 4;
	}

	@Override
	public IContest getContest() {
		return super.getContest();
	}

	@Override
	public void dispose() {
		super.dispose();

		getContest().removeListener(listener);
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();

		synchronized (submissions) {
			submissions.clear();
		}
		updateTargets(true);
	}

	public void handleSubmissions(String[] submissionIds) {
		if (submissionIds == null)
			return;

		synchronized (submissions) {
			List<ISubmission> last = new ArrayList<>();
			ISubmission lastSubmission = null;
			for (String submissionId : submissionIds) {
				ISubmission submission = getContest().getSubmissionById(submissionId);
				if (submission == null)
					Trace.trace(Trace.ERROR, "Couldn't find submission: " + submissionId);
				else if (lastSubmission != null && (!lastSubmission.getTeamId().equals(submission.getTeamId())
						|| !lastSubmission.getProblemId().equals(submission.getProblemId()))) {
					SubmissionRecord sr = createSubmissionRecord(lastSubmission);
					last.remove(lastSubmission);
					sr.related = last.toArray(new ISubmission[last.size()]);
					last.clear();
				}
				last.add(submission);

				lastSubmission = submission;
			}

			if (lastSubmission != null) {
				SubmissionRecord sr = createSubmissionRecord(lastSubmission);
				last.remove(lastSubmission);
				sr.related = last.toArray(new ISubmission[last.size()]);
			}
		}
	}

	public void handleSubmission(String submissionId) {
		ISubmission submission = getContest().getSubmissionById(submissionId);
		if (submission == null)
			System.err.println("Couldn't find submission: " + submissionId);
		else
			handleSubmission(submission);
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
		int y = headerHeight - 5;

		g.setFont(headerFont);
		g.drawString("Name", BORDER + (int) rowHeight, y);
		g.setFont(headerItalicsFont);
		g.drawString("Solved",
				width - BORDER - fm.stringWidth(" 9999") - (fm2.stringWidth("Solved") + fm.stringWidth("99")) / 2, y);
		g.setFont(headerFont);
		g.drawString("Time", width - BORDER - (fm2.stringWidth("Time") + fm.stringWidth("9999")) / 2, y);
	}

	@Override
	protected void drawSubmission(Graphics2D g, SubmissionRecord submission) {
		if (selectedSubmissionId != null && selectedSubmissionId.equals(submission.submission.getId())) {
			g.setColor(ICPCColors.SELECTION_COLOR);
			g.fillRect(0, 0, width, (int) (rowHeight));
		}

		if (selectedSubmissionId != null) {
			if (submission.related != null) {
				for (ISubmission srr : submission.related) {
					if (selectedSubmissionId.equals(srr.getId())) {
						g.setColor(ICPCColors.SELECTION_COLOR);
						g.fillRect(0, 0, width, (int) (rowHeight));
					}
				}
			}
		}

		g.setFont(rowFont);
		FontMetrics fm = g.getFontMetrics();

		IContest contest = getContest();
		ITeam team = contest.getTeamById(submission.submission.getTeamId());
		IStanding standing = contest.getStanding(team);

		BufferedImage img = getSmallTeamLogo(team, true);
		if (img != null) {
			int nx = (int) ((rowHeight - img.getWidth()) / 2f);
			int ny = (int) ((rowHeight - img.getHeight()) / 2f);
			g.drawImage(img, BORDER + nx, ny, null);
		}

		String s = TeamUtil.getTeamName(style, contest, team);
		g.setColor(Color.white);
		g.setFont(rowFont);
		fm = g.getFontMetrics();
		float nn = 1f;
		int xx = BORDER + fm.stringWidth("199 ") + (int) rowHeight;
		float wid = width - BORDER * 2 - fm.stringWidth("199 9 9999") - rowHeight;
		while (fm.stringWidth(s) > wid) {
			nn -= 0.025f;
			Font f = rowFont.deriveFont(AffineTransform.getScaleInstance(nn, 1.0));
			g.setFont(f);
			fm = g.getFontMetrics();
		}
		g.drawString(s, xx, fm.getAscent() + 5);

		drawRight(g, contest, team, standing, submission);
	}
}