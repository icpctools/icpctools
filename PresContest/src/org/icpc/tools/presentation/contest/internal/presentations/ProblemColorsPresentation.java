package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.ImageScaler;

public class ProblemColorsPresentation extends AbstractICPCPresentation {
	private static final int TIME_FADE_IN = 200;
	private static final int TIME_BIG = 1000;
	private static final int TIME_FADE_OUT = 600;
	private static final int TIME_PER_PROBLEM = TIME_FADE_IN + TIME_BIG + TIME_FADE_OUT;
	private static final int TIME_END = 6000;

	protected ProblemInfo[] problemInfos;

	class ProblemInfo {
		int x;
		int y;
		BufferedImage bigImage;
		BufferedImage smallImage;
	}

	@Override
	public long getRepeat() {
		IContest contest = getContest();
		if (contest == null)
			return 0;

		return TIME_PER_PROBLEM * contest.getNumProblems() + TIME_END;
	}

	@Override
	public void init() {
		if (problemInfos != null)
			return;

		Balloon.load(getClass());

		IContest contest = getContest();
		if (contest == null)
			return;

		int h = height * 4 / 5;

		float dpi = 96;
		Font font = ICPCFont.getMasterFont().deriveFont(Font.BOLD, h * 45f / dpi);

		IProblem[] problems = contest.getProblems();
		problemInfos = new ProblemInfo[problems.length];
		int count = 0;
		for (IProblem p : problems) {
			Color c = p.getColorVal();
			BufferedImage img = Balloon.getBalloonImage(c);
			int w = h * img.getWidth() / img.getHeight();
			BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D g = (Graphics2D) bi.getGraphics();
			g.drawImage(img, 0, 0, w, h, 0, 0, img.getWidth(), img.getHeight(), null);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setColor(org.icpc.tools.contest.model.ICPCColors.getContrastColor(c));
			g.setFont(font);
			String s = p.getLabel();
			FontMetrics fm = g.getFontMetrics();
			g.drawString(s, (w - fm.stringWidth(s)) / 2, (int) (h * 0.575));
			ProblemInfo pi = new ProblemInfo();
			pi.bigImage = bi;
			problemInfos[count++] = pi;
		}

		createSmallImages();
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		if (problemInfos == null)
			return;

		createSmallImages();
	}

	private void createSmallImages() {
		if (problemInfos == null || problemInfos.length == 0)
			return;

		// figure out layers
		int numProblems = problemInfos.length;

		BufferedImage img = problemInfos[0].bigImage;

		if (width > height * 2) {
			// really wide display
			double scale = img.getHeight() / (height / 2.0);
			int w = (int) (img.getWidth() / scale);
			int h = (int) (img.getHeight() / scale);

			int numTopRow = (numProblems + 1) / 2;
			int topRowOffset = (width - w * numTopRow) / 2;
			int bottomRowOffset = topRowOffset;
			if (numProblems % 2 == 1)
				bottomRowOffset += w / 2;

			for (int i = 0; i < numProblems; i++) {
				ProblemInfo pi = problemInfos[i];
				pi.smallImage = ImageScaler.scaleImage(pi.bigImage, w - 10, h - 10);

				if (i < numTopRow) {
					pi.x = topRowOffset + w * i;
					pi.y = 0;
				} else {
					pi.x = bottomRowOffset + w * (i - numTopRow);
					pi.y = h;
				}
				pi.x += 5;
				pi.y += 5;
			}
			return;
		}

		double scale = img.getHeight() / (height / 3.0);
		int w = (int) (img.getWidth() / scale);
		int h = (int) (img.getHeight() / scale);

		int dif = numProblems % 3;
		int numTopBottomRow = numProblems / 3;
		int numMiddleRow = numProblems / 3;
		if (dif == 0 || dif == 1)
			numMiddleRow++;
		else
			numTopBottomRow++;
		int longestRow = Math.max(numTopBottomRow, numMiddleRow);
		int topBottomRowOffset = (width - w * longestRow) / 2;
		int middleRowOffset = topBottomRowOffset;
		if (dif == 0 || dif == 1)
			topBottomRowOffset += w / 2;
		else
			middleRowOffset += w / 2;

		for (int i = 0; i < numProblems; i++) {
			ProblemInfo pi = problemInfos[i];
			pi.smallImage = ImageScaler.scaleImage(pi.bigImage, w - 10, h - 10);

			if (i < numTopBottomRow) {
				pi.x = topBottomRowOffset + w * i;
				pi.y = 0;
			} else if (i < numTopBottomRow + numMiddleRow) {
				pi.x = middleRowOffset + w * (i - numTopBottomRow);
				pi.y = h;
			} else {
				pi.x = topBottomRowOffset + w * (i - numTopBottomRow - numMiddleRow);
				pi.y = h * 2;
			}
			pi.x += 5;
			pi.y += 5;
		}
	}

	@Override
	public void paint(Graphics2D g) {
		if (problemInfos == null)
			return;

		long time = getRepeatTimeMs();
		int balloonNum = (int) (time / TIME_PER_PROBLEM + 0.5);
		long balloonTime = time - TIME_PER_PROBLEM * balloonNum;

		// paint background balloons
		if (balloonNum > problemInfos.length)
			balloonNum = problemInfos.length;

		for (int i = 0; i < balloonNum; i++) {
			ProblemInfo pi = problemInfos[i];
			BufferedImage image = pi.smallImage;
			if (image != null)
				g.drawImage(image, pi.x, pi.y, null);
		}

		if (balloonNum >= problemInfos.length)
			return;

		// paint foreground balloon
		if (balloonTime < TIME_FADE_IN)
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, balloonTime / (float) TIME_FADE_IN));

		ProblemInfo pi = problemInfos[balloonNum];
		BufferedImage image = pi.bigImage;
		int x = (width - image.getWidth()) / 2;
		int y = (height - image.getHeight()) / 2;
		if (balloonTime > TIME_FADE_IN + TIME_BIG) {
			float t = (balloonTime - TIME_FADE_IN - TIME_BIG) / (float) TIME_FADE_OUT;
			x = (int) (x * (1f - t) + pi.x * t);
			y = (int) (y * (1f - t) + pi.y * t);
			int iw = image.getWidth();
			int ih = image.getHeight();
			int siw = pi.smallImage.getWidth();
			int sih = pi.smallImage.getHeight();
			int w = (int) (iw * (1f - t) + siw * t);
			int h = (int) (ih * (1f - t) + sih * t);
			g.drawImage(image, x, y, x + w, y + h, 0, 0, iw, ih, null);
		} else
			g.drawImage(image, x, y, null);
	}
}