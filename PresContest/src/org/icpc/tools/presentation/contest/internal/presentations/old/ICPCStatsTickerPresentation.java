package org.icpc.tools.presentation.contest.internal.presentations.old;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Dimension2D;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.presentation.contest.internal.StatisticsGenerator;

public class ICPCStatsTickerPresentation extends AbstractTickerPresentation {
	protected StatisticsGenerator stats = new StatisticsGenerator();

	class ImageStringTicker extends StringTicker {
		protected Image img;

		public ImageStringTicker(String s, Color c, Image img) {
			super(s, c);
			this.img = img;
		}

		@Override
		public void paint(Graphics2D g, Dimension2D d, float count) {
			Dimension2D dd = d;
			if (img != null) {
				g.drawImage(img, 0, 5, null);
				dd = new Dimension((int) (dd.getWidth() - img.getWidth(null) - 6), (int) dd.getHeight());
				g.translate(img.getWidth(null) + 6, 0);
				super.paint(g, dd, count);
				g.translate(-img.getWidth(null) - 6, 0);
			} else
				super.paint(g, dd, count);
		}
	}

	public ICPCStatsTickerPresentation() {
		verticalTicker = true;
	}

	@Override
	public void setContest(IContest sc) {
		// super.setContest(sc);
		stats.generate(sc);
	}

	@Override
	public void setProperty(String value) {
		append(new StringTicker(value));
	}

	@Override
	protected void newContent() {
		String s = stats.getStatistic();
		if (s != null && s.length() > 0) {
			append(new StringTicker(s));
			// Image img = Images.getImage(Images.IBM);
			// append(new ImageStringTicker(s, Color.WHITE, img));
		} else
			append(new StringTicker("No statistics available"));
	}

	public void handleRuns(ISubmission[] runs) {
		for (ISubmission run : runs) {
			Trace.trace(Trace.ERROR, run + "");
			IContest contest = getContest();
			ITeam team = contest.getTeamById(run.getTeamId());
			Status status = contest.getStatus(run);
			if (status == Status.SOLVED) {
				String s = team.getName() + " solved problem " + run.getProblemId();
				append(new StringTicker(s));
				// append(new GapTicker(20));
			} else if (status == Status.SUBMITTED) {
				String s = team.getName() + " submitted a run for problem " + run.getProblemId();
				append(new StringTicker(s));
				// append(new GapTicker(20));
			}
		}
	}
}