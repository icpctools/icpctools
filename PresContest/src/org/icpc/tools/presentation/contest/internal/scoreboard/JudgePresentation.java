package org.icpc.tools.presentation.contest.internal.scoreboard;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.contest.model.resolver.SubmissionInfo;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ShadedRectangle;
import org.icpc.tools.presentation.contest.internal.TeamUtil;
import org.icpc.tools.presentation.contest.internal.TextImage;
import org.icpc.tools.presentation.contest.internal.Utility;

/**
 * Judge queue, shows incoming submissions and what judgement they receive.
 */
public class JudgePresentation extends AbstractScoreboardPresentation {
	private static final long TIME_TO_KEEP_SOLVED = 11000;
	private static final long TIME_TO_KEEP_FAILED = 8000;
	private static final long TIME_TO_KEEP_RECENT = 14000;
	private static final long TIME_TO_FADE_RECENT = 2000;

	private static final Movement SUBMISSION_MOVEMENT = new Movement(5, 9);

	protected int reservedRows = 0;

	enum Action {
		MOVE_UP, MOVE_DOWN, MOVE_OUT
	}

	protected class SubmissionRecord {
		public ISubmission submission;
		public ISubmission[] related;
		protected Animator anim;
		protected long fullAge;
		protected long actionAge;

		protected Action action;

		@Override
		public String toString() {
			return "Submission: " + submission.getId();
		}
	}

	protected List<SubmissionRecord> submissions = new ArrayList<>();
	protected String selectedSubmissionId;
	protected long timeToKeepFailed = TIME_TO_KEEP_FAILED;
	protected long timeToKeepSolved = TIME_TO_KEEP_SOLVED;

	protected IContestListener listener = (contest, obj, d) -> {
		if (obj instanceof ISubmission) {
			handleSubmission((ISubmission) obj);
		}
	};

	@Override
	public void init() {
		super.init();

		getContest().addListener(listener);
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

			if (!getContest().getState().isFrozen()) {
				IContest contest = getContest();
				ISubmission[] submissions2 = contest.getSubmissions();
				for (ISubmission submission : submissions2) {
					if (!contest.isJudged(submission))
						handleSubmission(submission);
				}
			}
		}
		updateTargets(true);
	}

	protected void handleSubmission(ISubmission submission) {
		// check for existing record first
		synchronized (submissions) {
			for (SubmissionRecord sr : submissions) {
				if (sr.submission.getId().equals(submission.getId())) {
					sr.submission = submission;
					return;
				}

				if (sr.related != null) {
					for (ISubmission r : sr.related) {
						if (r.getId().equals(submission.getId()))
							return;
					}
				}
			}
			createSubmissionRecord(submission);
		}
	}

	protected SubmissionRecord createSubmissionRecord(ISubmission submission) {
		synchronized (submissions) {
			SubmissionRecord sr = new SubmissionRecord();
			sr.submission = submission;
			double initalX = 0;
			if (submissions.size() > 0)
				initalX = Math.min(submissions.get(submissions.size() - 1).anim.getValue() + 1, teamsPerScreen * 2);
			sr.anim = new Animator(Math.max(initalX, teamsPerScreen + 2), SUBMISSION_MOVEMENT);
			submissions.add(sr);

			updateTargets(false);
			return sr;
		}
	}

	@Override
	public void incrementTimeMs(long dt) {
		updateRecords(dt);
		updateTargets(false);
		super.incrementTimeMs(dt);
	}

	public void setSelectedSubmissionId(String submissionId) {
		selectedSubmissionId = submissionId;
	}

	@Override
	public void setSelectedSubmission(SubmissionInfo submission) {
		super.setSelectedSubmission(submission);
		if (submission == null)
			setSelectedSubmissionId(null);
	}

	@Override
	protected void paintImpl(Graphics2D g) {
		SubmissionRecord[] submissions2 = null;
		synchronized (submissions) {
			submissions2 = submissions.toArray(new SubmissionRecord[0]);
		}

		// draw backgrounds
		for (int i = 0; i < teamsPerScreen; i += 2)
			drawRowBackground(g, (int) (i * rowHeight));

		if (reservedRows != 0) {
			g.setColor(new Color(0, 230, 0, 64));
			g.fillRect(0, 0, width, (int) (rowHeight * reservedRows));
		}

		g.setFont(problemFont);
		for (SubmissionRecord sr : submissions2) {
			double yy = sr.anim.getValue() * rowHeight;
			if (yy < height - headerHeight) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.translate(0, (int) yy);
				if (sr.action == Action.MOVE_DOWN) {
					float tr = (float) Math.min(1.0,
							Math.max(0.1, 1.0 - (sr.actionAge - timeToKeepFailed * 0.75) * 2.0 / timeToKeepFailed));
					g2.setComposite(AlphaComposite.SrcOver.derive(tr));
				} else if (sr.action == Action.MOVE_OUT) {
					float tr = (float) (1.0 - sr.actionAge / (double) TIME_TO_FADE_RECENT);
					g2.setComposite(AlphaComposite.SrcOver.derive(tr));
				}
				drawSubmission(g2, sr);
				g2.dispose();
			}
		}
	}

	protected void updateTargets(boolean force) {
		if (submissions == null)
			return;

		int count = 0;
		int count2 = reservedRows;
		IContest contest = getContest();
		synchronized (submissions) {
			// find # of solved
			int numSolved = 0;
			for (SubmissionRecord sr : submissions) {
				if (contest.isSolved(sr.submission))
					numSolved++;
			}

			for (SubmissionRecord sr : submissions) {
				double target = 0;
				if (reservedRows == 0) {
					target = count;
					if (sr.action == Action.MOVE_UP && (sr.actionAge > timeToKeepSolved))
						target = -teamsPerScreen;
					else if (sr.action == Action.MOVE_DOWN && sr.actionAge > timeToKeepFailed)
						target = teamsPerScreen * 2;
					else if (sr.action == Action.MOVE_OUT && sr.actionAge > TIME_TO_FADE_RECENT / 3) {
						// don't move count
					} else if (count < teamsPerScreen * 2)
						count++;
				} else {
					if (sr.action == Action.MOVE_UP) {
						if (sr.actionAge > timeToKeepSolved || numSolved > reservedRows) {
							target = -teamsPerScreen;
							numSolved--;
						} else
							target = count++;
					} else if (sr.action == Action.MOVE_DOWN && sr.actionAge > TIME_TO_FADE_RECENT / 2) {
						target = teamsPerScreen * 2;
					} else if (sr.action == Action.MOVE_OUT && sr.actionAge > TIME_TO_FADE_RECENT / 3) {
						// don't move count
					} else {
						target = count2++;
					}
				}

				if (force)
					sr.anim.reset(target);
				else
					sr.anim.setTarget(target);
			}
		}
	}

	protected void updateRecords(long dt) {
		List<SubmissionRecord> remove = new ArrayList<>();

		IContest contest = getContest();
		synchronized (submissions) {
			for (SubmissionRecord sr : submissions) {
				sr.anim.incrementTimeMs(dt);
				sr.fullAge += dt;

				if (sr.action == null) {
					if (contest.getState().isFrozen() && contest.getState().isRunning()) {
						if (sr.fullAge > TIME_TO_KEEP_RECENT)
							sr.action = Action.MOVE_OUT;
					} else {
						if (contest.isJudged(sr.submission)) {
							if (contest.isSolved(sr.submission))
								sr.action = Action.MOVE_UP;
							else
								sr.action = Action.MOVE_DOWN;
						}
					}
				} else
					sr.actionAge += dt;

				if (sr.action == Action.MOVE_UP && sr.anim.getValue() < -1)
					remove.add(sr);
				else if (sr.action == Action.MOVE_DOWN && sr.anim.getValue() > teamsPerScreen)
					remove.add(sr);
				else if (sr.action == Action.MOVE_OUT && sr.actionAge > TIME_TO_FADE_RECENT)
					remove.add(sr);
			}

			for (SubmissionRecord s : remove)
				submissions.remove(s);
		}
	}

	protected void drawSubmission(Graphics2D g, SubmissionRecord sr) {
		if (selectedSubmissionId != null && selectedSubmissionId.equals(sr.submission.getId())) {
			g.setColor(ICPCColors.SELECTION_COLOR);
			g.fillRect(0, 0, width, (int) (rowHeight));
		}

		if (selectedSubmissionId != null) {
			if (sr.related != null) {
				for (ISubmission srr : sr.related) {
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
		String teamId = sr.submission.getTeamId();
		ITeam team = contest.getTeamById(teamId);
		if (team == null)
			return;
		IStanding standing = contest.getStanding(team);
		String s = standing.getRank();
		g.setColor(Color.white);
		g.setFont(rowItalicsFont);
		TextImage.drawString(g, s, BORDER + (fm.stringWidth("199") - fm.stringWidth(s)) / 2, 5);

		BufferedImage img = getSmallTeamLogo(team, true);
		if (img != null) {
			int nx = (int) ((rowHeight - img.getWidth()) / 2f);
			int ny = (int) ((rowHeight - img.getHeight()) / 2f);
			g.drawImage(img, BORDER + fm.stringWidth("199 ") + nx, ny, null);
		}

		s = TeamUtil.getTeamName(style, contest, team);
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

		drawRight(g, contest, team, standing, sr);
	}

	protected void drawRight(Graphics2D g, IContest contest, ITeam team, IStanding standing, SubmissionRecord run) {
		int n = standing.getNumSolved();

		g.setFont(rowFont);
		FontMetrics fm = g.getFontMetrics();

		g.setColor(Color.white);
		g.setFont(rowItalicsFont);
		if (n > 0) {
			String s = n + "";
			TextImage.drawString(g, s,
					width - BORDER - fm.stringWidth(" 9999") - (fm.stringWidth("99") + fm.stringWidth(s)) / 2, 5);
		}

		n = standing.getTime();
		g.setFont(rowFont);
		if (n > 0) {
			String s = n + "";
			TextImage.drawString(g, s, width - BORDER - (fm.stringWidth("9999") + fm.stringWidth(s)) / 2, 5);
		}

		fm = g.getFontMetrics(); // row font

		int indent = BORDER + fm.stringWidth("199 ") + (int) rowHeight;
		int rowH = (int) rowHeight / 2;
		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;

		for (int j = 0; j < numProblems; j++) {
			int xx = indent + j * (cubeWidth + CUBE_INSET);
			IResult r = contest.getResult(team, j);

			if (problems[j].getId().equals(run.submission.getProblemId())) {
				String s = ContestUtil.getTime(run.submission.getContestTime());
				if (run.related != null && run.related.length > 0)
					s = (run.related.length + 1) + "\u200A-\u200A" + s;
				ShadedRectangle.drawRoundRect(g, xx, rowH + CUBE_INSET / 2 + 3, cubeWidth, cubeHeight, contest,
						run.submission, 0, s);
			} else if (r.getStatus() == Status.SOLVED) {
				String s = "";
				// text was too distracting - just show the semi-transparent problem colour
				// String s = r.getNumSubmissions() + "";
				// if (r.getContestTime() > 0)
				// s += "\u200A-\u200A" + ContestUtil.getTime(r.getContestTime());

				Color c = Utility.darker(ICPCColors.SOLVED_COLOR, 0.4f);

				ShadedRectangle.drawRoundRect(g, xx, rowH + CUBE_INSET / 2 + 3, cubeWidth, cubeHeight, c, null, s);
			} else
				ShadedRectangle.drawRoundRectPlain(g, xx, rowH + CUBE_INSET / 2 + 3, cubeWidth, cubeHeight,
						problems[j].getLabel());
		}

		for (int j = 0; j < numProblems; j++) {
			int xx = indent + j * (cubeWidth + CUBE_INSET);

			if (problems[j].getId().equals(run.submission.getProblemId())) {
				if (run.related != null && !contest.isJudged(run.submission)) {
					int w = cubeHeight * 5 / 9;
					for (int d = 0; d < run.related.length; d++) {
						int dd = (run.related.length - d);
						if (j < 2)
							ShadedRectangle.drawRoundRect(g, xx + cubeWidth + (w + CUBE_INSET / 2) * (dd - 1) + CUBE_INSET / 2,
									rowH + CUBE_INSET / 2 + 3, w, cubeHeight, contest, run.related[d], 0, null);
						else
							ShadedRectangle.drawRoundRect(g, xx - (w + CUBE_INSET / 2) * dd, rowH + CUBE_INSET / 2 + 3, w,
									cubeHeight, contest, run.related[d], 0, null);
					}
				}
			}
		}
	}

	@Override
	protected String getTitle() {
		return "Judge Queue";
	}
}