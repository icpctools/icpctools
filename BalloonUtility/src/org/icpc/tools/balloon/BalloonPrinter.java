package org.icpc.tools.balloon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Shell;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.Path;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IMapInfo;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.NDJSONFeedParser;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.QRCode;

public class BalloonPrinter {
	public static final String[] DEFAULT_MESSAGES = new String[] { "First balloon in contest!",
			"First balloon for group {0}!", "First solution to problem {1}!", "First balloon for this team" };

	private BufferedImage balloonImage;
	private BufferedImage bannerImage;
	private Font hugeFont;
	private Font largeFont;
	private Font mediumFont;
	private Font font;
	private FloorMap map;

	public BalloonPrinter() {
		// do nothing
	}

	protected void createResources() {
		if (balloonImage != null)
			return;

		try {
			balloonImage = ImageIO.read(getClass().getResourceAsStream("/images/balloon.gif"));
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Couldn't load balloon image", e);
		}

		bannerImage = ContestSource.getInstance().getContest().getInfo().getBannerImage(1920, 300, true, true);
	}

	/**
	 * Print a balloon page. Use a null balloon for a sample.
	 *
	 * @param printerData
	 * @param bc
	 * @param b
	 * @param messages
	 * @throws Exception
	 */
	public void print(BalloonContest bc, Balloon b, String[] messages) throws Exception {
		if (b == null)
			Trace.trace(Trace.INFO, "Printing sample balloon");
		else
			Trace.trace(Trace.INFO, "Printing balloon: " + b.getId() + " " + b.getSubmissionId());

		PrinterJob job = PrinterJob.getPrinterJob();
		job.setJobName("Balloon " + (b == null ? "Sample" : b.getSubmissionId()));

		BalloonContest bc2 = bc;
		Balloon b2 = b;
		if (bc2 == null) {
			bc2 = new BalloonContest();

			try (InputStream in = getClass().getResourceAsStream("/sample/event-feed.json")) {
				Contest contest = new Contest();
				bc2.setContest(contest);
				NDJSONFeedParser parser = new NDJSONFeedParser();
				parser.parse(contest, in);
				map = new FloorMap(contest);
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error loading sample contest", e);
			}

			b2 = new Balloon("1");
			b2.setFlags(Balloon.FIRST_FOR_TEAM | Balloon.FIRST_IN_CONTEST);
			bc2.add(b2);
			b2 = new Balloon("2");
			b2.setFlags(Balloon.FIRST_FOR_PROBLEM);
			bc2.add(b2);
		}

		if (balloonImage != null)
			balloonImage = null;

		if (bannerImage != null)
			bannerImage = null;

		if (font != null) {
			font = null;
			hugeFont = null;
			largeFont = null;
			mediumFont = null;
		}

		createResources();

		final BalloonContest bc3 = bc2;
		final Balloon b3 = b2;
		Printable p = new Printable() {
			@Override
			public int print(Graphics gg, PageFormat pf, int page) throws PrinterException {
				if (page != 0)
					return Printable.NO_SUCH_PAGE;

				Graphics2D g = (Graphics2D) gg;
				g.translate(pf.getImageableX(), pf.getImageableY());
				Dimension d = new Dimension((int) pf.getImageableWidth(), (int) pf.getImageableHeight());

				printPage(bc3, b3, g, d, messages == null ? DEFAULT_MESSAGES : messages);

				if (b == null) {
					// print some sample messages so that nobody gets confused among other printouts
					g.setFont(hugeFont);
					printSample(g, d.width / 2, 50);
					printSample(g, d.width / 2, d.height / 2);
					printSample(g, d.width / 2, d.height - 50);
				}

				return Printable.PAGE_EXISTS;
			}
		};

		try {
			job.setPrintable(p);
			job.print();
		} catch (PrinterException e) {
			throw new Exception("Could not start a print job");
		}

		if (bc == null)
			map = null;
	}

	private static void printSample(Graphics g, int x, int y) {
		String s = "SAMPLE";
		FontMetrics fm = g.getFontMetrics();
		Point p = new Point(fm.stringWidth(s), fm.getHeight());

		g.setColor(Color.WHITE);
		g.drawString(s, x - p.x / 2 - 1, y - p.y / 2 - 1);
		g.drawString(s, x - p.x / 2 - 1, y - p.y / 2 + 1);
		g.drawString(s, x - p.x / 2 + 1, y - p.y / 2 - 1);
		g.drawString(s, x - p.x / 2 + 1, y - p.y / 2 + 1);

		g.setColor(Color.RED);
		g.drawString(s, x - p.x / 2, y - p.y / 2);
	}

	private static String getLabelColor(IProblem p) {
		if (p == null)
			return "";
		if (p.getColor() == null)
			return p.getLabel();
		return p.getLabel() + " - " + p.getColor();
	}

	private static String getGroupLabel(IContest contest, ITeam team) {
		IGroup[] groups = contest.getGroupsByIds(team.getGroupIds());
		if (groups == null || groups.length == 0)
			return "<unknown>";

		return Arrays.stream(groups).map(g -> g.getName()).collect(Collectors.joining(", "));
	}

	/**
	 *
	 * @param bc
	 * @param b
	 * @param device
	 * @param gc
	 * @param r
	 * @param messages First to solve
	 */
	private void printPage(BalloonContest bc, Balloon b, Graphics2D g, Dimension r, String[] messages) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		// create fonts
		Font tempFont = g.getFont();
		if (font == null) {
			font = tempFont.deriveFont(Font.PLAIN, 9f);
			mediumFont = tempFont.deriveFont(Font.PLAIN, 12f);
			largeFont = tempFont.deriveFont(Font.PLAIN, 20f);
			hugeFont = tempFont.deriveFont(Font.PLAIN, 64f);
		}

		IContest c = bc.getContest();
		String submisssionId = b.getSubmissionId();
		ISubmission submisssion = c.getSubmissionById(submisssionId);
		ITeam team = c.getTeamById(submisssion.getTeamId());
		IProblem problem = c.getProblemById(submisssion.getProblemId());
		if (problem == null) {
			Trace.trace(Trace.ERROR, "Submission for a missing problem; likely invalid contest");
			return;
		}
		int bId = b.getId();

		int gap = r.width / 75;
		g.setColor(Color.WHITE);

		// size the box - 1 medium line, 1 huge, 4 normal
		g.setFont(mediumFont);
		FontMetrics fm = g.getFontMetrics();
		// Point p = new Point(fm.stringWidth(getLabelColor(problem)), fm.getHeight());
		int boxY = fm.getHeight();

		g.setFont(font);
		fm = g.getFontMetrics();
		boxY += fm.getHeight() * 4;

		g.setFont(hugeFont);
		fm = g.getFontMetrics();
		int px = fm.stringWidth("199") + gap * 2;
		boxY += fm.getHeight() + gap * 2;

		g.setColor(Color.BLACK);
		g.fillRect(0, 0, px, boxY);

		String s = "Team";
		g.setFont(font);
		fm = g.getFontMetrics();
		int y = gap;
		g.setColor(Color.WHITE);
		g.drawString(s, (px - fm.stringWidth(s)) / 2, y + fm.getAscent());
		y += fm.getHeight();

		s = team.getLabel() + "";
		g.setFont(hugeFont);
		fm = g.getFontMetrics();
		if (fm.stringWidth(s) > px - 10) {
			AffineTransform old = g.getTransform();
			float scaleX = ((float) (px - 10) / fm.stringWidth(s));
			g.transform(AffineTransform.getScaleInstance(scaleX, 1));
			g.drawString(s, (int) ((px / 2.0) / scaleX - fm.stringWidth(s) / 2.0), y + fm.getAscent());
			g.setTransform(old);
		} else
			g.drawString(s, (px - fm.stringWidth(s)) / 2, y + fm.getAscent());
		y += fm.getHeight();

		// solved problem
		s = getLabelColor(problem);
		g.setFont(mediumFont);
		fm = g.getFontMetrics();
		g.drawString(s, (px - fm.stringWidth(s)) / 2, y + fm.getAscent());
		y += fm.getHeight();

		// team details
		s = "Submission " + submisssion.getId();
		g.setFont(font);
		fm = g.getFontMetrics();
		g.drawString(s, (px - fm.stringWidth(s)) / 2, y + fm.getAscent());
		y += fm.getHeight();

		s = "Time " + ContestUtil.getTime(submisssion.getContestTime());
		g.drawString(s, (px - fm.stringWidth(s)) / 2, y + fm.getAscent());
		y += fm.getHeight();

		s = "Balloon " + bId;
		g.drawString(s, (px - fm.stringWidth(s)) / 2, y + fm.getAscent());
		y += fm.getHeight();

		// print logo and large team name across the top
		int indent = 0;
		g.setFont(largeFont);
		fm = g.getFontMetrics();
		try {
			IOrganization org = c.getOrganizationById(team.getOrganizationId());
			if (org != null) {
				BufferedImage logo = org.getLogoImage(fm.getHeight() * 2, fm.getHeight(), true, true);
				int h = fm.getHeight();
				int w = (logo.getWidth() * h) / logo.getHeight();
				g.drawImage(logo, px + gap, 0, w, h, null);
				indent = w + gap;
			}
		} catch (Exception e) {
			// ignore
		}
		String name = team.getActualDisplayName();
		g.setColor(Color.BLACK);
		fm = g.getFontMetrics();
		int ip = fm.stringWidth(name);
		if (px + ip + gap + indent > r.width) {
			AffineTransform old = g.getTransform();
			float scaleX = ((float) (r.width - px - gap - indent) / ip);
			g.transform(AffineTransform.getScaleInstance(scaleX, 1));
			g.drawString(name, (int) ((px + gap + indent) / scaleX), fm.getAscent());
			g.setTransform(old);
		} else
			g.drawString(name, px + gap + indent, fm.getAscent());
		int iy = fm.getHeight() + gap;

		// draw balloons
		int numProblems = c.getNumProblems();
		int maxBalloonsPerLine = 8;
		int wid = (int) ((r.width - px - gap) / (float) Math.min(numProblems, maxBalloonsPerLine));
		int nbw = (int) (wid * 0.9f);
		Point nbp = new Point(nbw, (int) ((float) balloonImage.getHeight() * nbw / balloonImage.getWidth()));

		// build problem array
		IProblem[] probs = c.getProblems();

		// other balloons
		boolean[] solved = new boolean[numProblems];
		int solvedProblem = -1;
		for (int i = 0; i < numProblems; i++) {
			if (probs[i].equals(problem)) {
				solvedProblem = i;
				solved[i] = true;
			}
		}
		List<Balloon> otherBalloons = new ArrayList<>();
		for (Balloon ob : bc.getBalloons()) {
			ISubmission os = c.getSubmissionById(ob.getSubmissionId());
			if (os.getContestTime() < submisssion.getContestTime() && ob.getFlags() >= 0) {
				// only include prior solved submissions from the same team
				if (submisssion.getTeamId().equals(os.getTeamId())) {
					otherBalloons.add(ob);
					IProblem pr = c.getProblemById(os.getProblemId());

					for (int i = 0; i < numProblems; i++) {
						if (probs[i].equals(pr))
							solved[i] = true;
					}
				}
			}
		}

		// list other balloons
		g.setColor(Color.BLACK);
		g.setFont(font);
		fm = g.getFontMetrics();
		if (otherBalloons.size() == 0)
			s = "Team has no other balloons";
		else {
			s = "Team has " + otherBalloons.size() + " other balloon";
			if (otherBalloons.size() != 1)
				s += "s";
		}
		y += gap * 2 + fm.getAscent();
		g.drawString(s, (px - fm.stringWidth(s)) / 2, y);
		y += fm.getHeight();

		for (int i = 0; i < numProblems; i++) {
			if (solved[i] && i != solvedProblem) {
				IProblem op = probs[i];
				s = getLabelColor(op);
				g.drawString(s, (px - fm.stringWidth(s)) / 2, y);
				y += fm.getHeight();
			}
		}
		y += gap;

		// draw problem balloons
		for (int i = 0; i < numProblems; i++) {
			int ii = i;
			if (i > maxBalloonsPerLine - 1)
				ii -= maxBalloonsPerLine;

			if (i == maxBalloonsPerLine) {
				g.setFont(font);
				fm = g.getFontMetrics();
				iy += nbp.y + fm.getHeight() + gap;
			}

			org.icpc.tools.contest.model.util.Balloon.load(this.getClass());
			BufferedImage tmpImg = null;
			if (i == solvedProblem)
				tmpImg = org.icpc.tools.contest.model.util.Balloon.getBalloonImage(probs[i].getColorVal());
			else if (solved[i]) {
				Color cc = probs[i].getColorVal();
				cc = new Color((cc.getRed() + 1275) / 6, (cc.getGreen() + 1275) / 6, (cc.getBlue() + 1275) / 6);
				tmpImg = org.icpc.tools.contest.model.util.Balloon.getBalloonImage(cc);
			} else
				tmpImg = org.icpc.tools.contest.model.util.Balloon.getBalloonImage(Color.WHITE);

			g.drawImage(tmpImg, px + gap + wid * ii, iy, nbp.x, nbp.y, null);
			tmpImg.flush();

			s = probs[i].getLabel();
			if (s == null)
				s = "";
			g.setFont(mediumFont);
			fm = g.getFontMetrics();
			int bx = px + gap + nbp.x / 2 + wid * ii;

			if (i == solvedProblem) {
				g.setFont(largeFont);
				fm = g.getFontMetrics();
				Color cc = probs[i].getColorVal();
				if (cc.getRed() + cc.getGreen() + cc.getBlue() < 255 * 2)
					g.setColor(Color.WHITE);
				else
					g.setColor(Color.BLACK);
			} else if (solved[i])
				g.setColor(Color.DARK_GRAY);
			else
				g.setColor(Color.GRAY);
			g.drawString(s, bx - fm.stringWidth(s) / 2, iy + nbp.y / 2);

			if (i == solvedProblem)
				g.setColor(Color.BLACK);
			else if (solved[i])
				g.setColor(Color.DARK_GRAY);
			else
				g.setColor(Color.GRAY);
			g.setFont(font);
			fm = g.getFontMetrics();
			s = probs[i].getColor();
			if (s != null) {
				Graphics2D gg = (Graphics2D) g.create();
				gg.translate(bx - fm.stringWidth(s) / 2, iy + nbp.y + gap + fm.getAscent());
				gg.rotate(-0.15);
				gg.drawString(s, 0, 0);
				gg.dispose();
			}
		}

		g.setFont(font);
		fm = g.getFontMetrics();
		iy += nbp.y + fm.getHeight() + gap;

		g.setColor(Color.BLACK);
		int yy = y;
		if (iy > yy)
			yy = iy;

		int w = (int) (r.width * 0.1);
		QRCode.drawQRCode(g, "icpc-balloon-" + b.getId(), r.width - w - r.width / 50, yy + r.width / 60, w);

		// draw messages
		if (messages != null && messages.length == 4) {
			String groupName = getGroupLabel(c, team);
			if (b.isFirstInContest())
				yy = addMessage(g, yy, gap, subs(messages[0], groupName, problem.getLabel(), problem.getColor()));

			if (b.isFirstInGroup())
				yy = addMessage(g, yy, gap, subs(messages[1], groupName, problem.getLabel(), problem.getColor()));

			if (b.isFirstForProblem())
				yy = addMessage(g, yy, gap, subs(messages[2], groupName, problem.getLabel(), problem.getColor()));

			if (b.isFirstForTeam())
				yy = addMessage(g, yy, gap, subs(messages[3], groupName, problem.getLabel(), problem.getColor()));
		}

		// draw banner and contest name at the bottom
		g.setColor(Color.BLACK);
		g.setFont(font);
		fm = g.getFontMetrics();

		try {
			if (map == null)
				map = new FloorMap(c);

			IMapInfo mapInfo = c.getMapInfo();
			if (mapInfo != null) {
				Path path1 = null;
				if (mapInfo.getPrinter() != null)
					path1 = map.getPath(mapInfo.getPrinter(), problem);
				Path path2 = map.getPath(problem, team);
				map.drawFloor(g, new Rectangle(0, yy, r.width, r.height - yy), team.getId(), false, path1, path2);
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error drawing map", e);
		}

		g.setFont(font);
		String contestTitle = c.getName();
		g.drawString(contestTitle, 0, r.height - fm.getDescent());

		s = "Printed " + BalloonUtility.getDateString();
		g.drawString(s, (r.width - fm.stringWidth(s)) / 2, r.height - fm.getDescent());

		// draw delivery signature line
		s = "Delivered";
		g.setFont(mediumFont);
		fm = g.getFontMetrics();
		int dw = (int) (r.width * 0.84);
		g.drawString(s, dw - fm.stringWidth(s), r.height - fm.getDescent());
		g.drawLine(dw, r.height, r.width, r.height);

		if (bannerImage != null) {
			float scale = r.width * 0.5f / bannerImage.getWidth();
			int bw = (int) (bannerImage.getWidth() * scale);
			int bh = (int) (bannerImage.getHeight() * scale);
			g.drawImage(bannerImage, (r.width - bw) / 2, r.height - fm.getHeight() - bh, bw, bh, null);
		}
	}

	private static String subs(String message, String group, String pLabel, String pColor) {
		String s = message;
		if (group != null)
			s = s.replace("{0}", group);
		if (pLabel != null)
			s = s.replace("{1}", pLabel);
		if (pColor != null)
			s = s.replace("{2}", pColor.toLowerCase());
		return s;
	}

	private int addMessage(Graphics2D g, int y, int indent, String message) {
		if (message == null || message.length() == 0)
			return y;

		String m1 = message;
		String m2 = null;
		int ind = message.indexOf("\n");
		if (ind > 0) {
			m1 = message.substring(0, ind);
			m2 = message.substring(ind + 1);
		}

		g.setFont(largeFont);
		FontMetrics fm = g.getFontMetrics();
		g.drawString(m1, 0, y + fm.getAscent());
		int yy = y + fm.getHeight();

		if (m2 == null)
			return yy;

		g.setFont(mediumFont);
		fm = g.getFontMetrics();
		g.drawString(m2, indent, yy + fm.getAscent());
		return (yy + fm.getHeight());
	}

	public void printPreview(Shell shell, BalloonContest bc, Balloon b, String[] messages) {
		BalloonContest bc2 = bc;
		Balloon b2 = b;
		if (bc2 == null) {
			bc2 = new BalloonContest();

			try (InputStream in = getClass().getResourceAsStream("/sample/event-feed.json")) {
				Contest contest = new Contest();
				bc2.setContest(contest);
				NDJSONFeedParser parser = new NDJSONFeedParser();
				parser.parse(contest, in);
				map = new FloorMap(contest);
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error loading sample contest", e);
			}

			b2 = new Balloon("1");
			b2.setFlags(Balloon.FIRST_FOR_TEAM | Balloon.FIRST_IN_CONTEST);
			bc2.add(b2);
			b2 = new Balloon("2");
			b2.setFlags(Balloon.FIRST_FOR_PROBLEM);
			bc2.add(b2);
		}

		try {
			createResources();

			Dimension d = new Dimension(650, 825);
			float scale = 1f;

			BufferedImage bimg = new BufferedImage((int) (d.width * scale), (int) (d.height * scale),
					BufferedImage.TYPE_INT_RGB);
			Graphics2D g = (Graphics2D) bimg.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, bimg.getWidth(), bimg.getHeight());

			String[] messages2 = messages;
			if (messages2 == null)
				messages2 = DEFAULT_MESSAGES;
			printPage(bc2, b2, g, d, messages2);

			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ImageIO.write(bimg, "jpg", bout);
			byte[] ba = bout.toByteArray();
			bimg.flush();

			ImageLoader il = new ImageLoader();
			ImageData[] id2 = il.load(new ByteArrayInputStream(ba));
			Image img = new Image(shell.getDisplay(), id2[0]);

			PrintPreviewDialog ptd = new PrintPreviewDialog(shell, img);
			ptd.open();
			img.dispose();
		} catch (Exception e) {
			ErrorHandler.error("Error printing", e);
		}
	}

	/**
	 * Print a test page.
	 *
	 * @throws Exception
	 */
	public static void printTest(PrinterData printerData2) throws Exception {
		try {
			PrinterJob job = PrinterJob.getPrinterJob();
			job.setJobName("Balloon print test");

			// PageFormat pfDefault = PrinterJob.getPrinterJob().defaultPage();
			// Paper defaultPaper = new Paper();
			// defaultPaper.setImageableArea(0, 0, defaultPaper.getWidth(), defaultPaper.getHeight());
			// pfDefault.setPaper(defaultPaper);

			job.setPrintable(new Printable() {
				@Override
				public int print(Graphics gg, PageFormat pf, int page) throws PrinterException {
					System.out.println("Page: " + page);
					if (page != 0)
						return Printable.NO_SUCH_PAGE;

					Graphics2D g = (Graphics2D) gg;
					g.translate(pf.getImageableX(), pf.getImageableY());
					FontMetrics fm = g.getFontMetrics();
					g.drawString("Test page", 0, fm.getAscent());

					return Printable.PAGE_EXISTS;
				}
			});

			if (job.printDialog())
				job.print();
		} catch (PrinterException e) {
			throw new Exception("Could not start a print job");
		}
	}

	public static void main(String[] arg) throws Exception {
		printTest(null);
	}
}