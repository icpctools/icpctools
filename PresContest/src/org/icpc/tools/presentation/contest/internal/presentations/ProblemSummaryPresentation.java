package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.Comparator;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;

public class ProblemSummaryPresentation extends AbstractICPCPresentation {
	private static final int MARGIN = 5;
	private static final int FLY_IN = 250;
	private ProblemSummary[] probs;

	@Override
	public long getRepeat() {
		if (probs != null)
			return probs.length * 1000 + 5000L;
		return 5000L;
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		IContest contest = getContest();
		if (contest == null)
			return;

		IProblem[] problems = contest.getProblems();
		Arrays.sort(problems, new Comparator<IProblem>() {
			@Override
			public int compare(IProblem p1, IProblem p2) {
				return Integer.compare(p1.getOrdinal(), p2.getOrdinal());
			}
		});

		// determine size
		int GAP = height / 50;
		int numProblems = problems.length;
		if (numProblems == 0) {
			probs = null;
			return;
		}

		int rows = 1;
		if (numProblems > 12)
			rows = 4;
		else if (numProblems > 9)
			rows = 3;
		else if (numProblems > 4)
			rows = 2;

		int numPerRow = (int) Math.ceil((double) numProblems / rows);
		int wid = (width - MARGIN * 2 - GAP * (numPerRow - 1)) / numPerRow;
		int hei = (height - MARGIN * 2 - GAP * (rows - 1)) / rows;

		probs = new ProblemSummary[numProblems + 1];
		for (int i = 0; i < numProblems + 1; i++) {
			Rectangle r = new Rectangle(MARGIN + (i % numPerRow) * (wid + GAP), MARGIN + (i / numPerRow) * (hei + GAP),
					wid, hei);
			if (i < numProblems)
				probs[i] = new ProblemSummary(r, problems[i]);
			else
				probs[i] = new ProblemSummary(r, null);
		}
	}

	@Override
	public void paintImpl(Graphics2D g) {
		IContest contest = getContest();
		if (contest == null)
			return;

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		long timeMs = getRepeatTimeMs();
		if (probs != null) {
			for (int i = 0; i < probs.length; i++) {
				if (timeMs <= i * FLY_IN)
					break;

				long[] stats = new long[4]; // attempted, solved, failed, fts
				stats[3] = -1;

				if (probs[i].getProblem() != null) {
					for (ITeam team : contest.getTeams()) {
						IResult r = contest.getResult(team, i);
						stats[0] += r.getNumSubmissions();

						if (r.isFirstToSolve())
							stats[3] = r.getContestTime();
						if (r.getStatus() == Status.SOLVED) {
							stats[1]++;
							if (r.getNumJudged() > 1)
								stats[2] += r.getNumJudged() - 1;
						} else if (r.getStatus() == Status.FAILED)
							stats[2] += r.getNumJudged();
					}
					probs[i].setStats(stats);
				}

				Graphics2D gg = (Graphics2D) g.create();
				if (timeMs < (i + 1) * FLY_IN) {
					float f = (timeMs - (float) i * FLY_IN) / FLY_IN; // goes from 0 to 1
					gg.setComposite(AlphaComposite.SrcOver.derive(f));
					int dx = (int) ((1f - f) * width / 30);
					gg.translate(dx, dx);
				} else
					gg.setComposite(AlphaComposite.SrcOver);
				probs[i].paint(gg);
				gg.dispose();
			}
		}
	}
}