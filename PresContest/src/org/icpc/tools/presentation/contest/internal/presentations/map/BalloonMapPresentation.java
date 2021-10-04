package org.icpc.tools.presentation.contest.internal.presentations.map;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

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

public class BalloonMapPresentation extends AbstractICPCPresentation {
	private static final double DEFAULT_LONGITUDE = 55.75376;
	private static final double DEFAULT_LATITUDE = 37.61225;
	private static final long TIME_TO_KEEP_SOLVED = 11000;
	private static final long TIME_TO_KEEP_FAILED = 8000;
	private static final long TIME_TO_KEEP_RECENT = 14000;
	private static final long TIME_TO_FADE_RECENT = 2000;
	private static final int NUM_SEGMENTS = 15;

	private static final Movement SUBMISSION_MOVEMENT = new Movement(0.1, 0.1);

	enum Action {
		INCOMING, OUTGOING, HIGHLIGHT, FADE_OUT
	}

	protected class SubmissionRecord {
		public ISubmission submission;
		protected Animator anim;
		protected long fullAge;
		protected long actionAge;

		protected int problem;
		protected IOrganization org;
		protected BufferedImage logo;
		protected BufferedImage smLogo;

		protected double[] lat;
		protected double[] lon;

		protected Action action;

		@Override
		public String toString() {
			return "Submission: " + submission.getId();
		}
	}

	protected IContestListener listener = (contest, obj, d) -> {
		if (obj instanceof ISubmission) {
			handleSubmission((ISubmission) obj);
		}
	};

	protected List<SubmissionRecord> submissions = new ArrayList<>();

	protected BufferedImage[] balloonImages;
	protected BufferedImage balloonSolvedImage;
	protected BufferedImage balloonFailedImage;

	private double c_lat = 0;
	private double c_lon = 0;
	private double c_zoom = 1;

	protected Font font;

	protected int logoSize;

	@Override
	public void init() {
		WorldMap.load(getClass());

		IContest contest = getContest();
		if (contest == null)
			return;

		contest.addListener(listener);

		if (balloonImages != null)
			return;

		Balloon.load(getClass());

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
	public void dispose() {
		super.dispose();

		getContest().removeListener(listener);
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		final float dpi = 96;
		font = ICPCFont.deriveFont(Font.BOLD, height * 72f * 0.04f / dpi);
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

		findCenter();
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

		// sr.anim = new Animator(Math.max(0, sr.path.getDistance()), SUBMISSION_MOVEMENT);
		submissions.add(sr);

		IContest contest = getContest();

		double o_lon = contest.getLongitude();
		if (Double.isNaN(o_lon))
			o_lon = DEFAULT_LONGITUDE;
		double o_lat = contest.getLatitude();
		if (Double.isNaN(o_lat))
			o_lat = DEFAULT_LATITUDE;

		ITeam team = contest.getTeamById(submission.getTeamId());

		sr.anim = new Animator(0, SUBMISSION_MOVEMENT);
		sr.anim.setTarget(0.95);
		sr.problem = contest.getProblemIndex(submission.getProblemId());
		IOrganization org = contest.getOrganizationById(team.getOrganizationId());
		if (org != null) {
			sr.org = org;
			sr.logo = org.getLogoImage(height / 6, height / 6, true, true);
			sr.smLogo = org.getLogoImage(height * 4 / 6 / 10, height * 4 / 6 / 10, true, true);
			sr.lat = new double[NUM_SEGMENTS];
			sr.lon = new double[NUM_SEGMENTS];
			sr.lat[0] = org.getLatitude();
			sr.lon[0] = org.getLongitude();
			double h = 40.0 * (Math.abs(sr.lat[0] - o_lat) + Math.abs(sr.lon[0] - o_lon)) / 180.0;
			for (int i = 1; i < NUM_SEGMENTS; i++) {
				double d = 1.0 - i / (NUM_SEGMENTS - 1.0);
				sr.lon[i] = sr.lon[0] * d + o_lon * (1.0 - d);
				sr.lat[i] = sr.lat[0] * d + o_lat * (1.0 - d) + Math.sin(i * Math.PI / (NUM_SEGMENTS - 1.0)) * h;
			}
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

					if (!contest.getState().isFrozen() && contest.isJudged(sr.submission)) {
						sr.actionAge += dt;
						sr.anim.setTarget(0.05);

						boolean solved = contest.isSolved(sr.submission);
						if (solved && sr.actionAge > TIME_TO_KEEP_SOLVED)
							sr.action = Action.HIGHLIGHT;
						else if (!solved && sr.actionAge > TIME_TO_KEEP_FAILED)
							sr.action = Action.FADE_OUT;
						if (sr.action != null)
							sr.actionAge = 0;
					} else if (contest.getState().isFrozen() && sr.fullAge > TIME_TO_KEEP_RECENT)
						sr.action = Action.FADE_OUT;
				} else
					sr.actionAge += dt;

				if (sr.action == Action.HIGHLIGHT && sr.actionAge > TIME_TO_KEEP_SOLVED)
					remove.add(sr);
				else if (sr.action == Action.FADE_OUT && sr.actionAge > TIME_TO_FADE_RECENT)
					remove.add(sr);
			}

			for (SubmissionRecord sr : remove) {
				submissions.remove(sr);
				sr.logo = null;
				sr.smLogo = null;
			}
		}
	}

	protected void findCenter() {
		IContest contest = getContest();
		if (contest == null)
			return;

		double minLat = 90;
		double maxLat = -90;
		double minLon = 180;
		double maxLon = -180;

		ITeam[] teams = contest.getTeams();
		for (ITeam t : teams) {
			IOrganization org = contest.getOrganizationById(t.getOrganizationId());
			if (org != null) {
				double lat = org.getLatitude();
				if (!Double.isNaN(lat)) {
					minLat = Math.min(minLat, lat);
					maxLat = Math.max(maxLat, lat);
				}

				double lon = org.getLongitude();
				if (!Double.isNaN(lon)) {
					minLon = Math.min(minLon, lon - 3);
					maxLon = Math.max(maxLon, lon + 3);
				}
			}
		}
		c_lat = (minLat + maxLat) / 2.0;
		c_lon = (minLon + maxLon) / 2.0;

		c_zoom = Math.min(360.0 / Math.abs(minLon - maxLon), 180.0 / Math.abs(minLat - maxLat)) * 0.92;
		// System.out.println(360.0 / Math.abs(minLon - maxLon) + " " + 180.0 / Math.abs(minLat -
		// maxLat) + " " + c_zoom);
		// c_zoom = 1.2;
	}

	@Override
	public void paint(Graphics2D g) {
		IContest contest = getContest();
		if (contest == null)
			return;

		double scm = c_zoom;
		double oX = width / 2.0 - (width * (c_lon + 180) * scm / 360.0);
		double oY = height / 2.0 - (height * (90 - c_lat) * scm / 180.0);

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		WorldMap.drawMap(g, (int) oX, (int) oY, width, height, scm);

		Graphics2D g2 = (Graphics2D) g.create();
		g2.translate((int) oX, (int) oY);

		// draw paths
		// int ox = (int) (width * (o_lon + 180.0) * scm / 360.0);
		// int oy = (int) (height * (90 - o_lat) * scm / 180.0);

		Stroke stroke = g2.getStroke();
		g2.setStroke(new BasicStroke(3));
		g2.setColor(Color.DARK_GRAY);

		SubmissionRecord[] srs = submissions.toArray(new SubmissionRecord[0]);
		for (SubmissionRecord sr : srs) {
			Graphics2D gg = (Graphics2D) g2.create();
			double d = sr.anim.getValue();
			if (d > 0.06) {
				if (d < 0.16)
					gg.setComposite(AlphaComposite.SrcOver.derive((float) ((d - 0.06) * 10.0)));
				// else if (d > 0.9)
				// gg.setComposite(AlphaComposite.SrcOver.derive((float) (1.0 - (d - 0.9) * 10)));

				gg.setColor(Color.GRAY);
				for (int i = 0; i < NUM_SEGMENTS - 1; i++) {
					int x1 = (int) (width * (sr.lon[i] + 180.0) * scm / 360.0);
					int y1 = (int) (height * (90 - sr.lat[i]) * scm / 180.0);
					int x2 = (int) (width * (sr.lon[i + 1] + 180.0) * scm / 360.0);
					int y2 = (int) (height * (90 - sr.lat[i + 1]) * scm / 180.0);
					gg.drawLine(x1, y1, x2, y2);
				}
			}
			gg.dispose();
		}
		g2.setStroke(stroke);

		// draw logos
		List<String> orgIds = new ArrayList<>();
		for (int i = srs.length - 1; i >= 0; i--) {
			SubmissionRecord sr = srs[i];
			if (orgIds.contains(sr.org.getId()))
				continue;
			orgIds.add(sr.org.getId());

			int x = (int) (width * (sr.lon[0] + 180.0) * scm / 360.0);
			int y = (int) (height * (90 - sr.lat[0]) * scm / 180.0);
			int yy = 4;

			BufferedImage img = sr.logo;
			BufferedImage smImg = sr.smLogo;
			if (img != null) {
				if (sr.fullAge < 3000) {
					g2.drawImage(img, (int) (x - img.getWidth() / 2.0), (int) (y - img.getHeight() / 2.0), null);
					yy = img.getHeight() / 2 + 4;
				} else if (sr.fullAge < 4000) {
					double sc = 1.0 - 0.6 * (sr.fullAge - 3000f) / 1000f;
					g2.drawImage(img, (int) (x - img.getWidth() * sc / 2.0), (int) (y - img.getHeight() * sc / 2.0),
							(int) (img.getWidth() * sc), (int) (img.getHeight() * sc), null);
					yy = (int) (img.getHeight() * sc / 2) + 4;
				} else {
					g2.drawImage(smImg, (int) (x - smImg.getWidth() / 2.0), (int) (y - smImg.getHeight() / 2.0), null);
					yy = smImg.getHeight() / 2 + 4;
				}

			}

			String s = sr.org.getName();
			g2.setColor(Color.WHITE);
			g2.setFont(font);
			FontMetrics fm = g2.getFontMetrics();
			g2.drawString(s, x - fm.stringWidth(s) / 2, y + yy + fm.getAscent());
		}

		// draw balloons
		for (SubmissionRecord sr : srs) {
			if (sr.problem >= balloonImages.length)
				continue;

			double d = sr.anim.getValue();
			int i = (int) (d * 0.999 * (NUM_SEGMENTS - 1));
			double dd = (d - i * 1.0 / (NUM_SEGMENTS - 1)) * (NUM_SEGMENTS - 1);

			double lat = sr.lat[i] * (1.0 - dd) + sr.lat[i + 1] * dd;// NPE
			double lon = sr.lon[i] * (1.0 - dd) + sr.lon[i + 1] * dd;
			int x = (int) (width * (lon + 180.0) * scm / 360.0);
			int y = (int) (height * (90 - lat) * scm / 180.0);

			BufferedImage img = balloonImages[sr.problem];
			g2.drawImage(img, (int) (x - img.getWidth() / 2.0), (int) (y - img.getHeight() / 2.0), null);

			if (contest.isSolved(sr.submission)) {
				g2.drawImage(balloonSolvedImage, (int) (x - img.getWidth() / 2.0), (int) (y - img.getHeight() / 2.0), null);
			} else if (contest.isJudged(sr.submission)) {
				g2.drawImage(balloonFailedImage, (int) (x - img.getWidth() / 2.0), (int) (y - img.getHeight() / 2.0), null);
			}
		}

		g2.dispose();
	}
}