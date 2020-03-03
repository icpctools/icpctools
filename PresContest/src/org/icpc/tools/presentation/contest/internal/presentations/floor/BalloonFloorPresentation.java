package org.icpc.tools.presentation.contest.internal.presentations.floor;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.Path;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.util.Balloon;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.ImageScaler;

public class BalloonFloorPresentation extends AbstractICPCPresentation {
	private static final long TIME_TO_KEEP_SOLVED = 11000;
	private static final long TIME_TO_KEEP_FAILED = 8000;
	private static final long TIME_TO_KEEP_RECENT = 14000;
	private static final long TIME_TO_FADE_RECENT = 2000;

	private static final Movement SUBMISSION_MOVEMENT = new Movement(5.0, 8.0);

	protected FloorMap floor;

	enum Action {
		SOLVED, FAILED, FADE_OUT
	}

	protected class SubmissionRecord {
		public ISubmission submission;
		protected Animator anim;
		protected long fullAge;
		protected long actionAge;
		protected Path path;
		protected int problem;
		protected IOrganization org;
		protected ITeam team;
		protected BufferedImage logo;
		protected BufferedImage smLogo;

		protected Action action;

		@Override
		public String toString() {
			return "Submission: " + submission.getId();
		}
	}

	protected List<SubmissionRecord> submissions = new ArrayList<>();

	protected BufferedImage[] balloonImages;
	protected BufferedImage balloonSolvedImage;
	protected BufferedImage balloonFailedImage;
	protected Font font;

	protected IContestListener listener = (contest, obj, d) -> {
		if (obj instanceof ISubmission) {
			handleSubmission((ISubmission) obj);
		}
	};

	@Override
	public void init() {
		super.init();

		getContest().addListener(listener);

		if (balloonImages != null)
			return;

		Balloon.load(getClass());

		IContest contest = getContest();
		if (contest == null)
			return;

		IProblem[] problems = contest.getProblems(); // TODO update at contest start
		BufferedImage[] temp = new BufferedImage[problems.length];
		int count = 0;
		for (IProblem p : problems) {
			Color c = p.getColorVal();
			BufferedImage img = Balloon.getBalloonImage(c);
			int h = height / 15;
			int w = h * img.getWidth() / img.getHeight();
			temp[count++] = ImageScaler.scaleImage(img, w, h);

			if (count == 1) {
				balloonSolvedImage = ImageScaler.scaleImage(Balloon.getBalloonSolvedImage(), w, h);
				balloonFailedImage = ImageScaler.scaleImage(Balloon.getBalloonFailedImage(), w, h);
			}
		}
		balloonImages = temp;
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		final float dpi = 96;
		font = ICPCFont.getMasterFont().deriveFont(Font.BOLD, height * 72f * 0.04f / dpi);
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

	private void createSubmissionRecord(ISubmission submission) {
		SubmissionRecord sr = new SubmissionRecord();
		sr.submission = submission;

		IContest contest = getContest();
		IProblem problem = contest.getProblemById(submission.getProblemId());
		ITeam team = contest.getTeamById(submission.getTeamId());
		if (floor == null)
			floor = FloorMap.getInstance(contest);

		sr.path = floor.getPath(problem, team);
		sr.anim = new Animator(0, SUBMISSION_MOVEMENT);
		sr.anim.setTarget(sr.path.getDistance());
		sr.problem = contest.getProblemIndex(submission.getProblemId());
		sr.team = team;
		sr.org = contest.getOrganizationById(team.getOrganizationId());
		if (sr.org != null) {
			sr.logo = sr.org.getLogoImage(height / 6, height / 6, true, true);
			sr.smLogo = sr.org.getLogoImage(height * 4 / 6 / 10, height * 4 / 6 / 10, true, true);
		}

		submissions.add(sr);
	}

	@Override
	public void incrementTimeMs(long dt) {
		updateRecords(dt);
		super.incrementTimeMs(dt);
	}

	protected void updateRecords(long dt) {
		List<SubmissionRecord> remove = new ArrayList<>();
		synchronized (submissions) {
			for (SubmissionRecord sr : submissions) {
				sr.anim.incrementTimeMs(dt);
				sr.fullAge += dt;

				if (sr.action == null) {
					IContest contest = getContest();
					if (contest.getState().isFrozen() && contest.getState().isRunning()) {
						if (sr.fullAge > TIME_TO_KEEP_RECENT)
							sr.action = Action.FADE_OUT;
					} else {
						if (contest.isJudged(sr.submission)) {
							if (contest.isSolved(sr.submission))
								sr.action = Action.SOLVED;
							else
								sr.action = Action.FAILED;
						}
					}
				} else
					sr.actionAge += dt;

				if (sr.action == Action.SOLVED && sr.actionAge > TIME_TO_KEEP_SOLVED)
					remove.add(sr);
				if (sr.action == Action.FAILED && sr.actionAge > TIME_TO_KEEP_FAILED)
					remove.add(sr);
				else if (sr.action == Action.FADE_OUT && sr.actionAge > TIME_TO_FADE_RECENT)
					remove.add(sr);
			}

			for (SubmissionRecord sr : remove) {
				submissions.remove(sr);
				sr.path = null;
				sr.logo = null;
				sr.smLogo = null;
			}
		}
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		IContest contest = getContest();
		if (floor == null)
			floor = FloorMap.getInstance(contest);
		if (balloonImages == null)
			return;

		Rectangle r = new Rectangle(0, 0, width, height);
		floor.drawFloor(g, r, new FloorMap.ScreenColors() {
			@Override
			public Color getDeskFillColor(String teamId) {
				return Color.BLACK;
			}

			@Override
			public Color getTextColor() {
				return Color.GRAY;
			}
		}, false);

		SubmissionRecord[] srs = submissions.toArray(new SubmissionRecord[0]);

		// draw paths
		Stroke stroke = g.getStroke();
		g.setStroke(new BasicStroke(3));
		g.setColor(Color.DARK_GRAY);
		for (SubmissionRecord sr : srs) {
			Graphics2D gg = (Graphics2D) g.create();
			if (sr.fullAge < 1000)
				gg.setComposite(AlphaComposite.SrcOver.derive(sr.fullAge / 1000f));
			else if (sr.anim.getValue() > sr.anim.getTarget() - 2) {
				float f = (float) ((sr.anim.getTarget() - sr.anim.getValue()) / 2.0);
				gg.setComposite(AlphaComposite.SrcOver.derive(f));
			}
			Path path = sr.path;
			if (path != null && sr.anim.getValue() + 0.02f < sr.anim.getTarget())
				floor.drawPath(gg, r, path);
			gg.dispose();
		}
		g.setStroke(stroke);

		// draw logos
		List<String> teamIds = new ArrayList<>();
		for (int i = srs.length - 1; i >= 0; i--) {
			SubmissionRecord sr = srs[i];
			BufferedImage img = sr.logo;
			BufferedImage smImg = sr.smLogo;
			int yy = 4;
			if (img != null) {
				if (teamIds.contains(sr.team.getId()))
					continue;
				teamIds.add(sr.team.getId());
				ITeam team = floor.getTeamById(sr.submission.getTeamId());
				if (team != null) {
					Point2D p2 = floor.getPosition(r, team.getX(), team.getY());
					if (sr.fullAge < 3000) {
						g.drawImage(img, (int) (p2.getX() - img.getWidth() / 2.0), (int) (p2.getY() - img.getHeight() / 2.0),
								null);
						yy = img.getHeight() / 2 + 4;
					} else if (sr.fullAge < 4000) {
						double sc = 1.0 - 0.6 * (sr.fullAge - 3000f) / 1000f;
						g.drawImage(img, (int) (p2.getX() - img.getWidth() * sc / 2.0),
								(int) (p2.getY() - img.getHeight() * sc / 2.0), (int) (img.getWidth() * sc),
								(int) (img.getHeight() * sc), null);
						yy = (int) (img.getHeight() * sc / 2) + 4;
					} else {
						g.drawImage(smImg, (int) (p2.getX() - smImg.getWidth() / 2.0),
								(int) (p2.getY() - smImg.getHeight() / 2.0), null);
						yy = smImg.getHeight() / 2 + 4;
					}

					String s = sr.org.getName();
					g.setColor(Color.WHITE);
					g.setFont(font);
					FontMetrics fm = g.getFontMetrics();
					g.drawString(s, (int) p2.getX() - fm.stringWidth(s) / 2, (int) p2.getY() + yy + fm.getAscent());
				}
			}
		}

		// draw balloons
		for (SubmissionRecord sr : srs) {
			if (sr.problem >= balloonImages.length)
				continue;

			double d = sr.anim.getValue();
			Point2D p = sr.path.getInterimPosition(d);
			Point2D p2 = floor.getPosition(r, p);

			BufferedImage img = balloonImages[sr.problem];
			g.drawImage(img, (int) (p2.getX() - img.getWidth() / 2.0), (int) (p2.getY() - img.getHeight() / 2.0), null);

			if (contest.isSolved(sr.submission)) {
				g.drawImage(balloonSolvedImage, (int) (p2.getX() - img.getWidth() / 2.0),
						(int) (p2.getY() - img.getHeight() / 2.0), null);
			} else if (contest.isJudged(sr.submission)) {
				g.drawImage(balloonFailedImage, (int) (p2.getX() - img.getWidth() / 2.0),
						(int) (p2.getY() - img.getHeight() / 2.0), null);
			}
		}
	}
}