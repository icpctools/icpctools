package org.icpc.tools.balloon;

import java.util.Arrays;
import java.util.Comparator;

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
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;

public class TeamSummaryPrint {
	protected Printer printer;

	protected Font regularFont;
	protected Font headerFont;

	public TeamSummaryPrint(Printer printer) {
		this.printer = printer;
	}

	public void print(IContest contest) throws Exception {
		boolean success = printer.startJob("Team Summary");
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

		ITeam[] teams = contest.getTeams();

		Arrays.sort(teams, new Comparator<ITeam>() {
			@Override
			public int compare(ITeam t1, ITeam t2) {
				try {
					Integer in1 = Integer.parseInt(t1.getId());
					Integer in2 = Integer.parseInt(t2.getId());
					return in1.compareTo(in2);
				} catch (Exception e) {
					// ignore
				}
				return t1.getId().compareTo(t2.getId());
			}
		});

		printImpl(contest, teams, gc, r);

		gc.dispose();
		printer.endPage();
		printer.endJob();
		headerFont.dispose();
	}

	private void printImpl(IContest contest, ITeam[] teams, GC gc, Rectangle r) {
		FontMetrics fm = gc.getFontMetrics();

		int y = r.y + fm.getAscent();
		int h = fm.getHeight();
		double aw = fm.getAverageCharacterWidth();

		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;
		int[] col = new int[1 + numProblems];
		float teamWidth = r.width * 0.45f;
		col[0] = 0;
		float problemWidth = (r.width - col[0] - teamWidth) / numProblems;
		for (int i = 0; i < numProblems; i++)
			col[i + 1] = (int) (teamWidth + (problemWidth * i));

		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));

		int i = 0;
		i += printSummary(contest, gc, r, h);
		printer.endPage();
		printer.startPage();
		i = 1;

		int page = 1;
		String date = BalloonUtility.getDateString();
		printHeader(gc, r, problems, col, (int) problemWidth, page++, date);

		for (ITeam team : teams) {
			if (contest.isTeamHidden(team))
				continue;

			if (h * (i + 5) > r.height) {
				printer.endPage();
				printer.startPage();
				printHeader(gc, r, problems, col, (int) problemWidth, page++, date);
				i = 1;
			}

			int yy = y + h * i;

			if (i % 4 == 1 && i != 1) {
				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
				gc.drawLine(r.x, yy, r.x + r.width, yy);
				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
			}

			for (int pInd = 0; pInd < numProblems; pInd++) {
				String s = null;
				for (ISubmission sub : contest.getSubmissions()) {
					if (sub.getTeamId().equals(team.getId()) && sub.getProblemId().equals(problems[pInd].getId())) {
						IJudgementType jt = contest.getJudgementType(sub);
						if (jt != null && jt.isSolved()) {
							s = "x";
							break;
						}
					}
				}
				IResult result = contest.getResult(team, pInd);

				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
				if (result.isFirstToSolve())
					s = "FTS";
				else if (result.getStatus() == Status.SUBMITTED) {
					s = "pend";
					gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
				}

				if (s != null)
					gc.drawString(s, r.x + col[pInd + 1] + ((int) problemWidth - gc.stringExtent(s).x) / 2, yy, true);
			}

			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
			gc.setFont(regularFont);

			String s = team.getId() + ":";
			gc.drawString(s, r.x + col[0] + (int) (aw * 4.0) - gc.stringExtent(s).x, yy, true);

			s = team.getActualDisplayName();
			Point ip = gc.stringExtent(s);
			int tw = col[1] - col[0] - (int) (aw * 4.5);
			if (ip.x > tw) {
				Transform oldTrans = new Transform(printer);
				gc.getTransform(oldTrans);
				float[] elem = new float[6];
				oldTrans.getElements(elem);
				Transform trans = new Transform(printer, elem);
				float scaleX = ((float) tw / (float) ip.x);
				trans.scale(scaleX, 1f);
				gc.setTransform(trans);
				gc.drawString(s, (int) ((r.x + col[0] + (int) (aw * 4.5)) / scaleX), yy, true);
				gc.setTransform(oldTrans);
				trans.dispose();
			} else
				gc.drawString(s, r.x + col[0] + (int) (aw * 4.5), yy, true);

			i++;
		}
	}

	protected int printSummary(IContest contest, GC gc, Rectangle r, int h) {
		int col = r.x + (int) (r.width * 0.16f);
		int yy = r.y;

		gc.setFont(headerFont);
		gc.drawString("Team Summary", r.x, yy, true);
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
		if (contest.getFreezeDuration() != null)
			gc.drawString("none", col, yy, true);
		else
			gc.drawString(ContestUtil.formatDuration(contest.getFreezeDuration()), col, yy, true);
		yy += h;

		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;
		int[] totals = new int[numProblems];
		for (ISubmission s : contest.getSubmissions()) {
			IJudgementType jt = contest.getJudgementType(s);
			if (jt != null && jt.isSolved())
				totals[contest.getProblemIndex(s.getProblemId())]++;
		}

		yy += h;
		gc.setFont(headerFont);
		gc.drawString("Problem", r.x, yy, true);
		gc.drawString("Color", r.x + (int) (r.width * 0.3f), yy, true);
		gc.drawString("Balloons", r.x + (int) (r.width * 0.5f), yy, true);

		Point dpi = gc.getDevice().getDPI();
		FontMetrics fm = gc.getFontMetrics();
		int y = yy + fm.getAscent() + (dpi.y / 18);
		gc.drawLine(r.x, y, r.x + r.width, y);

		yy += h * 2;

		int px = (int) (fm.getAverageCharacterWidth() * 2.5);
		gc.setFont(regularFont);
		for (int i = 0; i < numProblems; i++) {
			int x = px - gc.textExtent(problems[i].getLabel() + ":").x;
			gc.drawString(problems[i].getLabel() + ": " + problems[i].getName(), r.x + x, yy, true);
			if (problems[i].getColor() != null)
				gc.drawString(problems[i].getColor(), r.x + (int) (r.width * 0.3f), yy, true);
			else
				gc.drawString("unknown", r.x + (int) (r.width * 0.3f), yy, true);
			x = gc.textExtent("99999").x - gc.textExtent(totals[i] + "").x;
			gc.drawString(totals[i] + "", r.x + (int) (r.width * 0.5f) + x, yy, true);
			yy += h;
		}

		return 4;
	}

	protected void printHeader(GC gc, Rectangle r, IProblem[] problems, int[] col, int problemWidth, int pageNum,
			String date) {
		gc.setFont(headerFont);
		FontMetrics fm = gc.getFontMetrics();
		gc.drawString("Team", r.x + col[0], r.y, true);

		for (int i = 0; i < problems.length; i++) {
			String s = problems[i].getLabel();
			gc.drawString(s, r.x + col[i + 1] + (problemWidth - gc.stringExtent(s).x) / 2, r.y, true);
		}

		Point dpi = gc.getDevice().getDPI();
		int y = r.y + fm.getAscent() + (dpi.y / 18);
		gc.drawLine(r.x, y, r.x + r.width, y);

		gc.setFont(regularFont);
		fm = gc.getFontMetrics();
		String s = "Page " + pageNum;
		gc.drawText(s, r.x + r.width - gc.stringExtent(s).x, r.y + r.height - fm.getHeight() * 2);
		gc.drawText(date, r.x + r.width - gc.stringExtent(date).x, r.y + r.height - fm.getHeight());
	}
}