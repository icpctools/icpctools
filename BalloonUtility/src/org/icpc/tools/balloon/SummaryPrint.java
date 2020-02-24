package org.icpc.tools.balloon;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.printing.Printer;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;

public class SummaryPrint {
	protected Printer printer;

	protected Font regularFont;
	protected Font headerFont;

	public SummaryPrint(Printer printer) {
		this.printer = printer;
	}

	public void print(BalloonContest bc, List<Balloon> list) throws Exception {
		boolean success = printer.startJob("Balloon Summary");
		if (!success)
			throw new Exception("Could not start a print job");
		success = printer.startPage();

		Rectangle r = printer.getClientArea();

		GC gc = new GC(printer);
		gc.setTextAntialias(SWT.ON);
		Font font = gc.getFont();
		FontData[] fontData = font.getFontData();
		for (int i = 0; i < fontData.length; i++) {
			fontData[i].setHeight(12);
			fontData[i].setStyle(SWT.BOLD);
		}
		headerFont = new Font(printer, fontData);

		for (int i = 0; i < fontData.length; ++i) {
			fontData[i].setHeight(9);
			fontData[i].setStyle(SWT.NORMAL);
		}
		regularFont = new Font(printer, fontData);

		Balloon[] balloons = null;
		if (list == null)
			balloons = bc.getBalloons();
		else
			balloons = list.toArray(new Balloon[0]);

		Arrays.sort(balloons, new Comparator<Balloon>() {
			@Override
			public int compare(Balloon b1, Balloon b2) {
				if (b1.getId() > b2.getId())
					return 1;
				if (b1.getId() < b2.getId())
					return -1;
				return 0;
			}
		});

		printImpl(bc, balloons, gc, r);

		gc.dispose();
		printer.endPage();
		printer.endJob();
		headerFont.dispose();
	}

	private void printImpl(BalloonContest bc, Balloon[] balloons, GC gc, Rectangle r) {
		FontMetrics fm = gc.getFontMetrics();

		int y = r.y + fm.getAscent();
		int h = fm.getHeight();
		double aw = fm.getAverageCharacterWidth();

		int[] col = new int[] { 0, (int) (r.width * 0.04f), (int) (r.width * 0.095f), (int) (r.width * 0.55f),
				(int) (r.width * 0.73f), (int) (r.width * 0.8f), (int) (r.width * 0.875f), (int) (r.width * 0.925f) };

		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));

		int i = 0;
		i += printSummary(bc, gc, r, h);
		printer.endPage();
		printer.startPage();
		i = 0;

		int page = 1;
		String date = BalloonUtility.getDateString();
		printHeader(gc, r, col, page++, date);
		y += fm.getHeight();

		if (bc.getContest() != null) {
			for (Balloon b : balloons) {
				if (h * (i + 5) > r.height) {
					printer.endPage();
					printer.startPage();
					printHeader(gc, r, col, page++, date);
					i = 0;
				}

				IContest c = bc.getContest();
				ISubmission submission = c.getSubmissionById(b.getSubmissionId());
				ITeam team = c.getTeamById(submission.getTeamId());
				IProblem problem = c.getProblemById(submission.getProblemId());
				int yy = y + h * i;

				if (i % 4 == 0 && i != 0) {
					gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
					gc.drawLine(r.x, yy, r.x + r.width, yy);
					gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
				}

				gc.setFont(regularFont);
				gc.drawString(b.getId() + "", r.x + col[0], yy, true);

				gc.drawString(submission.getId() + "", r.x + col[1], yy, true);

				String s = team.getId() + ":";
				gc.drawString(s, r.x + col[2] + (int) (aw * 4.0) - gc.stringExtent(s).x, yy, true);

				s = team.getActualDisplayName();
				Point ip = gc.stringExtent(s);
				int tw = col[3] - col[2] - (int) (aw * 4.5);
				if (ip.x > tw) {
					Transform oldTrans = new Transform(printer);
					gc.getTransform(oldTrans);
					float[] elem = new float[6];
					oldTrans.getElements(elem);
					Transform trans = new Transform(printer, elem);
					float scaleX = ((float) tw / (float) ip.x);
					trans.scale(scaleX, 1f);
					gc.setTransform(trans);
					gc.drawString(s, (int) ((r.x + col[2] + (int) (aw * 4.5)) / scaleX), yy, true);
					gc.setTransform(oldTrans);
					trans.dispose();
				} else
					gc.drawString(s, r.x + col[2] + (int) (aw * 4.5), yy, true);

				gc.drawString(getLabelColor(problem), r.x + col[3], yy, true);
				gc.drawString(ContestUtil.getTime(submission.getContestTime()), r.x + col[4], yy, true);
				gc.drawString(b.getStatus(), r.x + col[5], yy, true);
				gc.drawString(b.isPrinted() ? "Y" : "", r.x + col[6], yy, true);
				gc.drawString(b.isDelivered() ? "Y" : "", r.x + col[7], yy, true);

				i++;
			}
		}
	}

	private static String getLabelColor(IProblem p) {
		if (p.getColor() == null)
			return p.getLabel();
		return p.getLabel() + " (" + p.getColor() + ")";
	}

	protected int printSummary(BalloonContest bc, GC gc, Rectangle r, int h) {
		int col = r.x + (int) (r.width * 0.16f);
		int yy = r.y;

		IContest contest = bc.getContest();

		gc.setFont(headerFont);
		gc.drawString("Balloon Summary", r.x, yy, true);
		yy += h * 2;

		gc.setFont(regularFont);
		gc.drawString("Name:", r.x, yy, true);
		gc.drawString(contest.getName(), col, yy, true);
		yy += h;
		gc.drawString("Start time:", r.x, yy, true);
		gc.drawString(ContestUtil.formatStartTime(contest), col, yy, true);
		yy += h;
		gc.drawString("Duration:", r.x, yy, true);
		gc.drawString(ContestUtil.formatDuration(contest.getDuration()), col, yy, true);
		yy += h;
		gc.drawString("Freeze:", r.x, yy, true);
		if (contest.getFreezeDuration() < 0)
			gc.drawString("none", col, yy, true);
		else
			gc.drawString(ContestUtil.formatDuration(contest.getFreezeDuration()), col, yy, true);
		yy += h;

		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;
		int[] totals = new int[numProblems];
		for (Balloon b : bc.getBalloons()) {
			String submissionId = b.getSubmissionId();
			ISubmission submission = contest.getSubmissionById(submissionId);
			totals[contest.getProblemIndex(submission.getProblemId())]++;
		}

		yy += h;
		gc.setFont(headerFont);
		gc.drawString("Problem", r.x, yy, true);
		gc.drawString("Color", r.x + (int) (r.width * 0.3f), yy, true);
		gc.drawString("Balloons", r.x + (int) (r.width * 0.45f), yy, true);

		Point dpi = gc.getDevice().getDPI();
		FontMetrics fm = gc.getFontMetrics();
		int y = yy + fm.getAscent() + (dpi.y / 18);
		gc.drawLine(r.x, y, r.x + r.width, y);

		yy += h * 2;

		gc.setFont(regularFont);
		for (int i = 0; i < numProblems; i++) {
			gc.drawString(problems[i].getLabel() + ": " + problems[i].getName(), r.x, yy, true);
			if (problems[i].getColor() != null)
				gc.drawString(problems[i].getColor(), r.x + (int) (r.width * 0.3f), yy, true);
			else
				gc.drawString("unknown", r.x + (int) (r.width * 0.3f), yy, true);
			gc.drawString(totals[i] + "", r.x + (int) (r.width * 0.45f), yy, true);
			yy += h;
		}

		return 4;
	}

	protected void printHeader(GC gc, Rectangle r, int[] col, int pageNum, String date) {
		gc.setFont(headerFont);
		gc.drawString("#", r.x + col[0], r.y, true);
		gc.drawString("Sub", r.x + col[1], r.y, true);
		gc.drawString("Team", r.x + col[2], r.y, true);
		gc.drawString("Problem", r.x + col[3], r.y, true);
		gc.drawString("Time", r.x + col[4], r.y, true);
		gc.drawString("Firsts", r.x + col[5], r.y, true);
		gc.drawString("Print", r.x + col[6], r.y, true);
		gc.drawString("Deliver", r.x + col[7], r.y, true);

		Point dpi = gc.getDevice().getDPI();
		FontMetrics fm = gc.getFontMetrics();
		int y = r.y + fm.getAscent() + (dpi.y / 18);
		gc.drawLine(r.x, y, r.x + r.width, y);

		gc.setFont(regularFont);
		fm = gc.getFontMetrics();
		String s = "Page " + pageNum;
		gc.drawText(s, r.x + r.width - gc.stringExtent(s).x, r.y + r.height - fm.getHeight() * 2);
		gc.drawText(date, r.x + r.width - gc.stringExtent(date).x, r.y + r.height - fm.getHeight());
	}
}