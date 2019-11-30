package org.icpc.tools.presentation.contest.internal.scoreboard;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.contest.model.internal.Result;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.ImageHelper;
import org.icpc.tools.presentation.contest.internal.ImageScaler;
import org.icpc.tools.presentation.contest.internal.ShadedRectangle;
import org.icpc.tools.presentation.contest.internal.TextImage;

public class FirstSolutionPresentation extends AbstractScoreboardPresentation {
	private static final String FIRSTSOLUTIONS_TXT = "firstsolutions.txt";

	private static final long TIME_TO_KEEP_SOLVED = 8000;

	static class FirstSolution {
		int year;
		String name;
		int time;
		BufferedImage logo;
		BufferedImage smLogo;
	}

	class SubmissionRecord {
		protected ISubmission submission;
		protected Animator anim;
		protected long age;

		@Override
		public String toString() {
			return "Submission: " + submission.getId();
		}
	}

	protected IContestListener listener = (contest, obj, d) -> {
		if (obj instanceof ISubmission) {
			handleSubmission((ISubmission) obj);
		} else if (obj instanceof IJudgement) {
			IJudgement sj = (IJudgement) obj;
			ISubmission s = contest.getSubmissionById(sj.getSubmissionId());
			handleSubmission(s);
		}
	};

	protected List<SubmissionRecord> submissions = new ArrayList<>();

	private List<FirstSolution> historical = new ArrayList<>();
	private SubmissionRecord first = null;

	public FirstSolutionPresentation() {
		teamsPerScreen = 10;

		historical = readData();

		execute(new Runnable() {
			@Override
			public void run() {
				for (FirstSolution fs2 : historical) {
					if (fs2.time < 0)
						return;

					try {
						fs2.logo = ImageHelper.loadImage("/presentation/fts/" + fs2.year + ".png");
					} catch (Exception e) {
						// ignore - no image
					}
				}
				loadTeamLogos();
			}
		});
	}

	private static List<FirstSolution> readData() {
		List<FirstSolution> list = new ArrayList<FirstSolution>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				Thread.currentThread().getContextClassLoader().getResourceAsStream("data/" + FIRSTSOLUTIONS_TXT)))) {
			String line = br.readLine();
			while (line != null) {
				String[] values = line.split("\\t");
				if (values != null && values.length != 0 && !values[0].startsWith("#")) {
					try {
						FirstSolution fs = new FirstSolution();
						fs.year = Integer.parseInt(values[0]);
						fs.name = values[1];
						fs.time = Integer.parseInt(values[2]);
						list.add(fs);
					} catch (Exception e) {
						Trace.trace(Trace.ERROR, "Problem processing " + FIRSTSOLUTIONS_TXT + ": " + e.getMessage());
					}
				}
				line = br.readLine();
			}
		} catch (IOException e) {
			Trace.trace(Trace.ERROR, "Problem processing " + FIRSTSOLUTIONS_TXT + ": " + e.getMessage());
		}
		return list;
	}

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

		if (first == null) {
			synchronized (submissions) {
				submissions.clear();

				ISubmission[] runs2 = getContest().getSubmissions();
				for (ISubmission submission : runs2) {
					handleSubmission(submission);
				}
			}
		}
		updateRecords(1, true);

		for (FirstSolution fs : historical) {
			fs.smLogo = null;
		}
	}

	protected void checkForFirst() {
		if (first != null)
			return;

		IContest contest = getContest();
		ISubmission[] subs = contest.getSubmissions();
		for (ISubmission s : subs) {
			ITeam team = contest.getTeamById(s.getTeamId());
			if (!contest.isTeamHidden(team)) {
				IJudgementType jt = contest.getJudgementType(s);
				if (jt == null)
					return;

				if (jt.isSolved()) {
					for (SubmissionRecord sr : submissions) {
						if (sr.submission.equals(s)) {
							first = sr;
							return;
						}
					}
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
					checkForFirst();
					return;
				}
			}

			if (first != null)
				return;

			SubmissionRecord sr = new SubmissionRecord();
			sr.submission = submission;
			double y = 0;
			if (submissions.size() > 0)
				y = Math.min(submissions.get(submissions.size() - 1).anim.getValue() + 1, teamsPerScreen * 2);
			sr.anim = new Animator(Math.max(y, teamsPerScreen + 2), AbstractScoreboardPresentation.ROW_MOVEMENT);
			submissions.add(sr);

			checkForFirst();
		}
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
		g.drawString("Year", BORDER + (fm.stringWidth("2999") - fm2.stringWidth("Year")) / 2, y);
		g.setFont(headerFont);
		g.drawString("Name", BORDER + fm.stringWidth("29999 ") + rowHeight, y);
		g.setFont(headerFont);
		g.drawString("Time", width - BORDER - (fm.stringWidth("9999") + fm2.stringWidth("Time")) / 2, y);
	}

	@Override
	protected String getTitle() {
		return "First Solution in Contest";
	}

	@Override
	protected void paintImpl(Graphics2D g) {
		SubmissionRecord[] runs2 = null;
		synchronized (submissions) {
			runs2 = submissions.toArray(new SubmissionRecord[0]);
		}

		// draw backgrounds
		for (int i = 0; i < teamsPerScreen; i += 2)
			drawRowBackground(g, (int) (i * rowHeight));

		int count = 0;
		for (FirstSolution fs : historical) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.translate(0, (int) (rowHeight * count));
			drawFirstSolution(g2, fs);
			g2.dispose();
			count++;
		}

		g.setColor(Color.WHITE);
		int y2 = (int) (historical.size() * rowHeight);
		g.drawLine(0, y2, width, y2);

		g.setFont(problemFont);
		for (SubmissionRecord run : runs2) {
			double yy = run.anim.getValue() * rowHeight;
			if (yy < height - headerHeight) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.translate(0, (int) yy);
				drawSubmission(g2, run);
				g2.dispose();
			}
		}
	}

	@Override
	public void incrementTimeMs(long dt) {
		updateRecords(dt, false);
		super.incrementTimeMs(dt);
	}

	protected void updateRecords(long dt, boolean force) {
		if (submissions == null)
			return;

		List<SubmissionRecord> remove = new ArrayList<>();
		int count = historical.size();
		synchronized (submissions) {
			for (SubmissionRecord sr : submissions) {
				double target = count;
				if (sr == first)
					target = (historical.size() - 1);
				else if (sr.age > TIME_TO_KEEP_SOLVED || first != null)
					target = teamsPerScreen * 2;
				else if (count < teamsPerScreen * 2)
					count++;

				if (force)
					sr.anim.reset(target);
				else {
					sr.anim.setTarget(target);
					sr.anim.incrementTimeMs(dt);
				}

				if (getContest().isJudged(sr.submission) && sr.submission != first) {
					sr.age += dt;
					if (sr.age > TIME_TO_KEEP_SOLVED && sr.anim.getValue() > teamsPerScreen) {
						remove.add(sr);
					}
				}
			}

			for (SubmissionRecord sr : remove) {
				submissions.remove(sr);
			}
		}
	}

	protected void drawSubmission(Graphics2D g, SubmissionRecord run) {
		g.setFont(rowFont);
		FontMetrics fm = g.getFontMetrics();

		IContest contest = getContest();
		ITeam team = contest.getTeamById(run.submission.getTeamId());

		int col1 = fm.stringWidth("29999: ");

		BufferedImage img = getSmallTeamLogo(team, true);
		if (img != null) {
			int nx = (int) ((rowHeight - img.getWidth()) / 2f);
			int ny = (int) ((rowHeight - img.getHeight()) / 2f);
			g.drawImage(img, BORDER + col1 + nx, ny, null);
		}

		String s = team.getActualDisplayName();
		g.setColor(Color.white);
		g.setFont(rowFont);
		fm = g.getFontMetrics();

		float n = 1f;
		while (fm.stringWidth(s) > width - BORDER * 2 - col1 - rowHeight) {
			n -= 0.025f;
			Font f = rowFont.deriveFont(AffineTransform.getScaleInstance(n, 1.0));
			g.setFont(f);
			fm = g.getFontMetrics();
		}
		g.drawString(s, BORDER + col1 + (int) rowHeight, fm.getAscent() + 5);

		g.setFont(rowFont);
		fm = g.getFontMetrics(); // row font

		s = ContestUtil.getTime(run.submission.getContestTime());

		ShadedRectangle.drawRoundRect(g, BORDER + col1 + (int) rowHeight, (int) rowHeight / 2 + CUBE_INSET / 2 + 3,
				(int) (cubeWidth * 1.5), cubeHeight, contest, run.submission, 0, s);

		g.setColor(Color.white);
		g.setFont(rowFont);
		TextImage.drawString(g, s, width - BORDER - (fm.stringWidth("9999") + fm.stringWidth(s)) / 2, 5);
	}

	protected void drawFirstSolution(Graphics2D g, FirstSolution fs) {
		g.setFont(rowFont);
		FontMetrics fm = g.getFontMetrics();

		IContest contest = getContest();

		int col1 = fm.stringWidth("29999: ");
		BufferedImage img = fs.smLogo;
		if (img == null && fs.logo != null) {
			fs.smLogo = ImageScaler.scaleImage(fs.logo, rowHeight - 10, rowHeight - 10);
			img = fs.smLogo;
		}
		if (img != null) {
			int nx = (int) ((rowHeight - img.getWidth()) / 2f);
			int ny = (int) ((rowHeight - img.getHeight()) / 2f);
			g.drawImage(img, BORDER + col1 + nx, ny, null);
		}

		String s = fs.year + "";
		g.setColor(Color.white);
		g.setFont(rowFont);
		TextImage.drawString(g, s, BORDER, 5);

		s = fs.name;
		if (first != null && "?".equals(s))
			s = "";
		g.setColor(Color.white);
		g.setFont(rowFont);
		fm = g.getFontMetrics();

		float n = 1f;
		while (fm.stringWidth(s) > width - BORDER * 2 - col1 - rowHeight) {
			n -= 0.025f;
			Font f = rowFont.deriveFont(AffineTransform.getScaleInstance(n, 1.0));
			g.setFont(f);
			fm = g.getFontMetrics();
		}

		g.drawString(s, BORDER + col1 + (int) rowHeight, fm.getAscent() + 5);

		if (fs.time < 0)
			return;

		Result result = new Result() {
			@Override
			public boolean isFirstToSolve() {
				return true;
			}

			@Override
			public Status getStatus() {
				return Status.SOLVED;
			}
		};

		s = fs.time + "";
		ShadedRectangle.drawRoundRect(g, BORDER + col1 + (int) rowHeight, (int) rowHeight / 2 + CUBE_INSET / 2 + 3,
				(int) (cubeWidth * 1.5), cubeHeight, contest, result, 0, s);

		g.setColor(Color.white);
		g.setFont(rowFont);
		fm = g.getFontMetrics();
		TextImage.drawString(g, s, width - BORDER - (fm.stringWidth("9999") + fm.stringWidth(s)) / 2, 5);
	}
}