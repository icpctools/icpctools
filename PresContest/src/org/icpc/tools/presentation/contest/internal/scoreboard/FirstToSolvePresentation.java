package org.icpc.tools.presentation.contest.internal.scoreboard;

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
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.ShadedRectangle;
import org.icpc.tools.presentation.contest.internal.TextImage;

public class FirstToSolvePresentation extends AbstractScoreboardPresentation {
	private static final long TIME_TO_KEEP_SOLVED = 8000;

	class SubmissionRecord {
		protected ISubmission submission;
		protected Animator anim;
		protected long age;

		@Override
		public String toString() {
			return "Submission: " + submission.getId();
		}
	}

	protected List<String> fts = new ArrayList<>();
	protected List<SubmissionRecord> submissions = new ArrayList<>();

	protected IContestListener listener = (contest, e, d) -> {
		if (e instanceof IJudgement) {
			handleSubmission((IJudgement) e);
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
		IContest contest = getContest();
		synchronized (submissions) {
			ISubmission[] submissions2 = contest.getSubmissions();
			for (ISubmission submission : submissions2) {
				if (contest.isSolved(submission) && contest.isFirstToSolve(submission)) {
					IJudgement[] judgements = contest.getJudgementsBySubmissionId(submission.getId());
					if (judgements != null) {
						for (IJudgement judgement : judgements)
							handleSubmission(judgement);
					}
				}
			}
		}
		updateRecords(1, true);
	}

	protected void handleFTS(SubmissionRecord run) {
		if (run == null || run.submission == null)
			return;

		IContest contest = getContest();
		if (!contest.isJudged(run.submission) || !contest.isFirstToSolve(run.submission))
			return;

		// filter out problems that have already been added
		String probId = run.submission.getProblemId();
		for (String id : fts) {
			if (id.equals(probId))
				return;
		}

		// insert in order, or just add to the end
		/*for (int i = 0; i < fts.size(); i++) {
			Integer in = fts.get(i);
			if (probId < in) {
				fts.add(i, probId);
				return;
			}
		}*/
		fts.add(probId);
	}

	protected void handleSubmission(IJudgement judgement) {
		ISubmission submission = getContest().getSubmissionById(judgement.getSubmissionId());
		// check for existing record first
		synchronized (submissions) {
			for (SubmissionRecord sr : submissions) {
				if (sr.submission.getId().equals(submission.getId())) {
					sr.submission = submission;
					handleFTS(sr);
					return;
				}
			}

			// filter out problems that have already been solved
			String probId = submission.getProblemId();
			for (String id : fts) {
				if (id.equals(probId))
					return;
			}

			SubmissionRecord sr = new SubmissionRecord();
			sr.submission = submission;
			double target = 0;
			if (submissions.size() > 0)
				target = Math.min(submissions.get(submissions.size() - 1).anim.getValue() + 1, teamsPerScreen * 2);
			sr.anim = new Animator(Math.max(target, teamsPerScreen + 2), AbstractScoreboardPresentation.ROW_MOVEMENT);
			submissions.add(sr);
			handleFTS(sr);
		}
	}

	@Override
	public void incrementTimeMs(long dt) {
		updateRecords(dt, false);
		super.incrementTimeMs(dt);
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
		g.drawString("Time", BORDER + (fm.stringWidth("199") - fm2.stringWidth("Time")) / 2, y);
		g.setFont(headerFont);
		g.drawString("Name", BORDER + fm.stringWidth("199 ") + rowHeight, y);
		g.setFont(headerFont);
		g.drawString("Problem", width - BORDER - (fm.stringWidth("9999") + fm2.stringWidth("Problem")) / 2, y);
	}

	@Override
	protected String getTitle() {
		return "First to Solve";
	}

	@Override
	protected void paintImpl(Graphics2D g) {
		SubmissionRecord[] runs2 = null;
		synchronized (submissions) {
			runs2 = submissions.toArray(new SubmissionRecord[0]);
		}

		// draw backgrounds
		int size = fts.size();
		for (int i = 0; i < teamsPerScreen; i += 2)
			drawRowBackground(g, (int) (i * rowHeight));

		if (size > 0) {
			g.setColor(Color.WHITE);
			int y = (int) (size * rowHeight);
			g.drawLine(0, y, width, y);
		}

		g.setFont(problemFont);
		for (SubmissionRecord sr : runs2) {
			double yy = sr.anim.getValue() * rowHeight;
			if (yy < height - headerHeight) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.translate(0, (int) yy);
				drawSubmission(g2, sr);
				g2.dispose();
			}
		}
	}

	protected void updateRecords(long dt, boolean force) {
		if (submissions == null)
			return;

		List<SubmissionRecord> remove = new ArrayList<>();
		int count = fts.size();

		synchronized (submissions) {
			IContest contest = getContest();
			for (SubmissionRecord sr : submissions) {
				int target = count;
				if (contest.isJudged(sr.submission) && contest.isFirstToSolve(sr.submission)) {
					target = fts.indexOf(sr.submission.getProblemId());
				} else if (sr.age > TIME_TO_KEEP_SOLVED)
					target = teamsPerScreen * 2;
				else if (count < teamsPerScreen * 2)
					count++;

				if (force)
					sr.anim.reset(target);
				else {
					sr.anim.setTarget(target);
					sr.anim.incrementTimeMs(dt);
				}

				if (contest.isJudged(sr.submission) && !contest.isFirstToSolve(sr.submission)) {
					sr.age += dt;
					if (sr.age > TIME_TO_KEEP_SOLVED && sr.anim.getValue() > teamsPerScreen) {
						remove.add(sr);
					}
				}
			}

			for (SubmissionRecord rr : remove) {
				submissions.remove(rr);
			}
		}
	}

	protected void drawSubmission(Graphics2D g, SubmissionRecord run) {
		g.setFont(rowFont);
		FontMetrics fm = g.getFontMetrics();

		IContest contest = getContest();
		String s = ContestUtil.getTime(run.submission.getContestTime());
		g.setColor(Color.white);
		g.setFont(rowItalicsFont);
		TextImage.drawString(g, s, BORDER + (fm.stringWidth("199") - fm.stringWidth(s)) / 2, 5);

		// if (teamRowImages.isEmpty()) // TODO should do this once somewhere
		// loadTeamLogos();

		ITeam team = contest.getTeamById(run.submission.getTeamId());
		if (team == null)
			return;
		BufferedImage img = getSmallTeamLogo(team, true);
		if (img != null) {
			int nx = (int) ((rowHeight - img.getWidth()) / 2f);
			int ny = (int) ((rowHeight - img.getHeight()) / 2f);
			g.drawImage(img, BORDER + fm.stringWidth("199 ") + nx, ny, null);
		}

		s = team.getActualDisplayName();
		g.setColor(Color.white);
		g.setFont(rowFont);
		fm = g.getFontMetrics();
		float nn = 1f;
		int x = BORDER + fm.stringWidth("199 ") + (int) rowHeight;
		float wid = width - BORDER * 2 - fm.stringWidth("199 9 9999") - rowHeight;
		while (fm.stringWidth(s) > wid) {
			nn -= 0.025f;
			Font f = rowFont.deriveFont(AffineTransform.getScaleInstance(nn, 1.0));
			g.setFont(f);
			fm = g.getFontMetrics();
		}
		g.drawString(s, x, fm.getAscent() + 5);

		g.setFont(rowFont);
		fm = g.getFontMetrics();

		s = ContestUtil.getTime(run.submission.getContestTime());
		int indent = BORDER + fm.stringWidth("199 ") + (int) rowHeight;
		int rowH = (int) rowHeight / 2;
		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;
		for (int j = 0; j < numProblems; j++) {
			int xx = indent + j * (cubeWidth + CUBE_INSET);

			if (problems[j].getId().equals(run.submission.getProblemId())) {
				ShadedRectangle.drawRoundRect(g, xx, rowH + CUBE_INSET / 2 + 3, cubeWidth, cubeHeight, contest,
						run.submission, 0, s);
			} else
				ShadedRectangle.drawRoundRectPlain(g, xx, rowH + CUBE_INSET / 2 + 3, cubeWidth, cubeHeight,
						problems[j].getLabel());
		}

		IProblem prob = contest.getProblemById(run.submission.getProblemId());
		s = prob.getLabel();
		g.setColor(Color.white);
		g.setFont(rowFont);
		TextImage.drawString(g, s, width - BORDER - (fm.stringWidth("9999") + fm.stringWidth(s)) / 2, 5);
	}
}