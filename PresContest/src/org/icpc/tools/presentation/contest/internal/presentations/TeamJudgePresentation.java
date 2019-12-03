package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.AlphaComposite;
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
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.ShadedRectangle;
import org.icpc.tools.presentation.contest.internal.TextImage;

/**
 * Judge queue, shows incoming submissions and what judgement they receive.
 */
public class TeamJudgePresentation extends AbstractICPCPresentation {
	private static final long TIME_TO_KEEP_SOLVED = 8000;
	private static final long TIME_TO_KEEP_FAILED = 5000;
	private static final long TIME_TO_KEEP_RECENT = 7000;
	private static final long TIME_TO_FADE_RECENT = 2000;
	private static final int BORDER = 5;

	private static final int ROWS = 3;
	private static final int COLUMNS = 7;

	private int lastPosition = 0;

	protected Font titleFont;

	enum Action {
		STAY, MOVE_UP, MOVE_DOWN, MOVE_OUT
	}

	protected class SubmissionRecord {
		public ISubmission submission;
		protected long fullAge;
		protected long actionAge;
		protected int position;
		protected Animator anim;
		protected BufferedImage image;
		protected String text;

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
	public void setSize(Dimension d) {
		super.setSize(d);
		final float dpi = 96;

		float size = (int) (height * 72.0 * 0.028 / dpi);

		size = height * 36f * 0.06f / dpi;
		titleFont = ICPCFont.getMasterFont().deriveFont(Font.BOLD, size * 1.2f);
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
			}
			createSubmissionRecord(submission);
		}
	}

	protected void createSubmissionRecord(ISubmission submission) {
		if (submissions.size() >= ROWS * COLUMNS)
			return;

		synchronized (submissions) {
			SubmissionRecord sr = new SubmissionRecord();
			sr.submission = submission;
			sr.position = lastPosition;
			lastPosition++;
			if (lastPosition >= ROWS * COLUMNS)
				lastPosition = 0;
			sr.anim = new Animator(0, new Movement(0.5, 1));

			IContest contest = getContest();
			IProblem prob = contest.getProblemById(sr.submission.getProblemId());
			sr.text = prob.getLabel() + " - " + ContestUtil.getTime(sr.submission.getContestTime());

			String teamId = submission.getTeamId();
			for (SubmissionRecord srr : submissions) {
				if (srr.image != null && teamId.equals(srr.submission.getTeamId())) {
					sr.image = srr.image;
					break;
				}
			}
			if (sr.image == null) {
				ITeam team = getContest().getTeamById(teamId);
				if (team != null) {
					IOrganization org = contest.getOrganizationById(team.getOrganizationId());
					if (org != null)
						sr.image = org.getLogoImage((int) (width * 0.85 / COLUMNS), (int) (height * 0.6 / ROWS), true, true);
				}
			}

			submissions.add(sr);

			updateTargets(false);
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
	public void paint(Graphics2D g) {
		SubmissionRecord[] submissions2 = null;
		synchronized (submissions) {
			submissions2 = submissions.toArray(new SubmissionRecord[0]);
		}

		for (SubmissionRecord sr : submissions2) {
			int x = width * (sr.position % COLUMNS) / COLUMNS;
			int y = height * (sr.position / COLUMNS) / ROWS;

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setClip(x + 1, y + 1, width / COLUMNS - 2, height / ROWS - 2);
			g2.translate(x, y + (int) (sr.anim.getValue() * height / 3.0));
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

			if (sr.action == Action.MOVE_DOWN) {
				float tr = (float) Math.min(1.0,
						Math.max(0.1, 1.0 - (sr.actionAge - timeToKeepFailed) * 2.0 / timeToKeepFailed));
				g2.setComposite(AlphaComposite.SrcOver.derive(tr));
			} else if (sr.action == Action.MOVE_OUT) {
				float tr = (float) (1.0 - sr.actionAge / (double) TIME_TO_FADE_RECENT);
				g2.setComposite(AlphaComposite.SrcOver.derive(tr));
			}
			paintSubmission(g2, sr);
			g2.dispose();
		}
	}

	protected void updateRecords(long dt) {
		List<SubmissionRecord> remove = new ArrayList<>();
		synchronized (submissions) {
			for (SubmissionRecord sr : submissions) {
				sr.anim.incrementTimeMs(dt);
				sr.fullAge += dt;

				if (sr.action == null) {
					IContest contest = getContest();
					if (!contest.getState().isFrozen() && contest.isJudged(sr.submission)) {
						sr.actionAge += dt;

						boolean solved = contest.isSolved(sr.submission);
						if (solved && sr.actionAge > timeToKeepSolved)
							sr.action = Action.MOVE_UP;
						else if (!solved && sr.actionAge > timeToKeepFailed)
							sr.action = Action.MOVE_DOWN;
						if (sr.action != null)
							sr.actionAge = 0;
					} else if (contest.getState().isFrozen() && sr.fullAge > TIME_TO_KEEP_RECENT)
						sr.action = Action.MOVE_OUT;
				} else
					sr.actionAge += dt;

				if (sr.action == Action.MOVE_UP && sr.actionAge > timeToKeepSolved + 4000)
					remove.add(sr);
				else if (sr.action == Action.MOVE_DOWN && sr.actionAge > timeToKeepFailed + 4000)
					remove.add(sr);
				else if (sr.action == Action.MOVE_OUT && sr.actionAge > timeToKeepSolved + 4000)
					remove.add(sr);
			}

			for (SubmissionRecord s : remove) {
				submissions.remove(s);
			}
		}
	}

	protected void updateTargets(boolean force) {
		if (submissions == null)
			return;

		synchronized (submissions) {
			for (SubmissionRecord sr : submissions) {
				double target = 0;
				if (sr.action == Action.MOVE_UP)
					target = -2;
				else if (sr.action == Action.MOVE_DOWN)
					target = 2;

				if (force)
					sr.anim.reset(target);
				else
					sr.anim.setTarget(target);
			}
		}
	}

	protected void paintSubmission(Graphics2D g, SubmissionRecord sr) {
		g.setFont(titleFont);
		FontMetrics fm = g.getFontMetrics();

		IContest contest = getContest();
		String probId = sr.submission.getProblemId();
		IProblem problem = contest.getProblemById(probId);
		Color c = problem.getColorVal();
		g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 127));
		g.fillRect(0, 0, width / COLUMNS, height / ROWS);

		String teamId = sr.submission.getTeamId();
		ITeam team = contest.getTeamById(teamId);
		if (team == null)
			return;

		if (sr.image != null)
			g.drawImage(sr.image, width / COLUMNS / 2 - sr.image.getWidth() / 2,
					height / ROWS / 2 - sr.image.getHeight() / 2, null);

		int cubeWidth = (int) (width / COLUMNS / 2.0);
		int cubeHeight = (int) (height / ROWS / 8.0);

		ShadedRectangle.drawRoundRect(g, width / COLUMNS / 2 - cubeWidth / 2, height / ROWS - cubeHeight - BORDER,
				cubeWidth, cubeHeight, contest, sr.submission, 0, sr.text);

		String s = team.getActualDisplayName();
		g.setColor(Color.white);
		// g.setFont(rowItalicsFont);
		TextImage.drawString(g, s, width / COLUMNS / 2 - fm.stringWidth(s) / 2, BORDER);
	}
}