package org.icpc.tools.balloon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.AisleIntersection;
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

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;

public class BalloonPrinter {
	public static final String[] DEFAULT_MESSAGES = new String[] { "First balloon in contest!",
			"First balloon for group {0}!", "First solution to problem {1}!", "First balloon for this team" };

	private Image balloonImage;
	private Image bannerImage;
	private Font hugeFont;
	private Font largeFont;
	private Font mediumFont;
	private Font font;
	private FloorMap map;

	public BalloonPrinter() {
		// do nothing
	}

	protected void createResources(final Device device) {
		if (balloonImage != null)
			return;

		balloonImage = new Image(device, getClass().getResourceAsStream("/images/balloon.gif"));

		Thread t = new Thread("Image loader") {
			@Override
			public void run() {
				try {
					File f = ContestSource.getInstance().getContest().getInfo().getBanner(1920, 300, true);
					bannerImage = loadImage(device, f);
				} catch (Exception e) {
					// ignore
				}
			}
		};
		t.setDaemon(true);
		t.start();
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
	public void print(PrinterData printerData, BalloonContest bc, Balloon b, String[] messages) throws Exception {
		Printer printer = new Printer(printerData);

		String jobName = "Balloon " + (b == null ? "Sample" : b.getSubmissionId());
		boolean success = printer.startJob(jobName);
		if (!success)
			throw new Exception("Could not start a print job");
		success = printer.startPage();
		if (!success)
			throw new Exception("Could not start a print job page");

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

		GC gc = null;
		try {
			if (balloonImage != null) {
				balloonImage.dispose();
				balloonImage = null;
			}
			if (bannerImage != null) {
				bannerImage.dispose();
				bannerImage = null;
			}
			if (font != null) {
				font.dispose();
				font = null;
				hugeFont.dispose();
				hugeFont = null;
				largeFont.dispose();
				largeFont = null;
				mediumFont.dispose();
				mediumFont = null;
			}

			createResources(printer);

			Rectangle r = printer.getClientArea();

			gc = new GC(printer);

			printPage(bc2, b2, printer, gc, r, messages == null ? DEFAULT_MESSAGES : messages);

			if (b == null) {
				// print some sample messages so that nobody gets confused among other printouts
				gc.setFont(hugeFont);
				printSample(gc, printer, r.x + r.width / 2, r.y + 50);
				printSample(gc, printer, r.x + r.width / 2, r.y + r.height / 2);
				printSample(gc, printer, r.x + r.width / 2, r.y + r.height - 50);
			}
		} finally {
			if (gc != null)
				gc.dispose();
		}

		printer.endPage();
		printer.endJob();
		printer.dispose();

		if (bc == null)
			map = null;
	}

	private static void printSample(GC gc, Device device, int x, int y) {
		String s = "SAMPLE";
		Point p = gc.stringExtent(s);

		gc.setForeground(device.getSystemColor(SWT.COLOR_WHITE));
		gc.drawString(s, x - p.x / 2 - 1, y - p.y / 2 - 1, true);
		gc.drawString(s, x - p.x / 2 - 1, y - p.y / 2 + 1, true);
		gc.drawString(s, x - p.x / 2 + 1, y - p.y / 2 - 1, true);
		gc.drawString(s, x - p.x / 2 + 1, y - p.y / 2 + 1, true);

		gc.setForeground(device.getSystemColor(SWT.COLOR_RED));
		gc.drawString(s, x - p.x / 2, y - p.y / 2, true);
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

	// Note that this method modifies fontData
	private static Font createFont(int height, Device device, FontData[] fontData) {
		for (FontData fontDatum : fontData) {
			fontDatum.setHeight(height);
			fontDatum.setStyle(SWT.NORMAL);
		}
		return new Font(device, fontData);
	}

	private static Image loadImage(Device device, File f) throws Exception {
		if (f == null)
			return null;

		if (!f.getName().endsWith("svg"))
			return new Image(device, new FileInputStream(f));

		// SVG image. Start by loading diagram
		SVGUniverse sRenderer = new SVGUniverse();
		URI uri = sRenderer.loadSVG(f.toURI().toURL());
		SVGDiagram diagram = sRenderer.getDiagram(uri);
		diagram.setIgnoringClipHeuristic(true);

		// create a BufferedImage from it
		BufferedImage img = new BufferedImage((int) diagram.getWidth(), (int) diagram.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		diagram.render(g);
		g.dispose();

		// then convert to ImageData
		DirectColorModel colorModel = (DirectColorModel) img.getColorModel();
		PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(),
				colorModel.getBlueMask());
		ImageData data = new ImageData(img.getWidth(), img.getHeight(), colorModel.getPixelSize(), palette);
		for (int y = 0; y < data.height; y++) {
			for (int x = 0; x < data.width; x++) {
				int rgb = img.getRGB(x, y);
				int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
				data.setPixel(x, y, pixel);
				if (colorModel.hasAlpha()) {
					data.setAlpha(x, y, (rgb >> 24) & 0xFF);
				}
			}
		}
		return new Image(device, data);
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
	protected void printPage(BalloonContest bc, Balloon b, Device device, GC gc, Rectangle r, String[] messages) {
		gc.setTextAntialias(SWT.ON);

		// create fonts
		Font tempFont = gc.getFont();
		if (font == null) {
			FontData[] fontData = tempFont.getFontData();

			font = createFont(10, device, fontData);
			mediumFont = createFont(16, device, fontData);
			largeFont = createFont(28, device, fontData);
			hugeFont = createFont(90, device, fontData);
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

		Point dpi = device.getDPI();
		int gap = dpi.x / 20;
		gc.setForeground(device.getSystemColor(SWT.COLOR_WHITE));
		gc.setBackground(device.getSystemColor(SWT.COLOR_BLACK));

		// size the box - 1 medium line, 1 huge, 4 normal
		gc.setFont(mediumFont);
		Point p = gc.stringExtent(getLabelColor(problem));
		int boxY = p.y;

		gc.setFont(font);
		p = gc.stringExtent("Team");
		boxY += p.y;
		p = gc.stringExtent("Submission " + submisssion.getId());
		boxY += p.y;
		p = gc.stringExtent("Time " + ContestUtil.getTime(submisssion.getContestTime()));
		boxY += p.y;
		p = gc.stringExtent("Balloon " + bId);
		boxY += p.y;

		gc.setForeground(device.getSystemColor(SWT.COLOR_WHITE));

		gc.setFont(hugeFont);
		p = gc.stringExtent("199");
		p.x += gap * 2;
		boxY += p.y + gap * 2;
		gc.fillRectangle(r.x, r.y, p.x, boxY);

		int y = r.y + gap;
		String s = "Team";
		gc.setFont(font);
		Point q = gc.stringExtent(s);
		gc.drawString(s, r.x + (p.x - q.x) / 2, y, true);
		y += q.y;

		s = team.getId() + "";
		gc.setFont(hugeFont);
		q = gc.stringExtent(s);
		gc.drawString(s, r.x + (p.x - q.x) / 2, y, true);
		y += q.y;

		// solved problem
		s = getLabelColor(problem);
		gc.setFont(mediumFont);
		q = gc.stringExtent(s);
		gc.drawString(s, r.x + (p.x - q.x) / 2, y, true);
		y += q.y;

		// team details
		s = "Submission " + submisssion.getId();
		gc.setFont(font);
		q = gc.stringExtent(s);
		gc.drawString(s, r.x + (p.x - q.x) / 2, y, true);
		y += q.y;

		s = "Time " + ContestUtil.getTime(submisssion.getContestTime());
		q = gc.stringExtent(s);
		gc.drawString(s, r.x + (p.x - q.x) / 2, y, true);
		y += q.y;

		s = "Balloon " + bId;
		q = gc.stringExtent(s);
		gc.drawString(s, r.x + (p.x - q.x) / 2, y, true);
		y += q.y;

		// print logo and large team name
		int indent = 0;
		gc.setFont(largeFont);
		FontMetrics fm = gc.getFontMetrics();
		try {
			IOrganization org = c.getOrganizationById(team.getOrganizationId());
			if (org != null) {
				File f = org.getLogo(fm.getHeight(), fm.getHeight(), true);
				if (f != null) {
					Image logo = loadImage(device, f);
					Rectangle logoR = logo.getBounds();
					int h = fm.getHeight();
					int w = (logoR.width * h) / logoR.height;
					gc.drawImage(logo, 0, 0, logoR.width, logoR.height, r.x + p.x + gap, r.y, w, h);
					logo.dispose();
					indent = w + gap;
				}
			}
		} catch (Exception e) {
			// ignore
		}
		String name = team.getActualDisplayName();
		gc.setForeground(device.getSystemColor(SWT.COLOR_BLACK));
		Point ip = gc.stringExtent(name);
		if (r.x + p.x + ip.x + gap + indent > r.width) {
			Transform oldTrans = new Transform(device);
			gc.getTransform(oldTrans);
			Transform trans = new Transform(device);
			gc.getTransform(trans);
			float scaleX = ((float) (r.width - r.x - p.x - gap - indent) / (float) ip.x);
			trans.scale(scaleX, 1f);
			gc.setTransform(trans);
			gc.drawString(name, (int) ((r.x + p.x + gap + indent) / scaleX), r.y, true);
			gc.setTransform(oldTrans);
			trans.dispose();
		} else
			gc.drawString(name, r.x + p.x + gap + indent, r.y, true);
		int iy = r.y + fm.getHeight() + gap;

		// draw balloons
		int numProblems = c.getNumProblems();
		Rectangle bp = balloonImage.getBounds();
		int maxBalloonsPerLine = 8;
		int wid = (int) ((r.width - p.x - gap) / (float) Math.min(numProblems, maxBalloonsPerLine));
		int nbw = (int) (wid * 0.9f);
		Point nbp = new Point(nbw, (int) ((float) bp.height * nbw / bp.width));

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

		gc.setForeground(device.getSystemColor(SWT.COLOR_BLACK));
		gc.setFont(font);
		if (otherBalloons.size() == 0)
			s = "Team has no existing balloons";
		else {
			s = "Team has " + otherBalloons.size() + " other balloon";
			if (otherBalloons.size() != 1)
				s += "s";
		}
		y += gap * 2;
		q = gc.stringExtent(s);
		gc.drawString(s, r.x + (p.x - q.x) / 2, y, true);
		y += q.y;

		for (int i = 0; i < numProblems; i++) {
			if (solved[i] && i != solvedProblem) {
				IProblem op = probs[i];
				s = getLabelColor(op);
				q = gc.stringExtent(s);
				gc.drawString(s, r.x + (p.x - q.x) / 2, y, true);
				y += q.y;
			}
		}
		y += gap;

		// gc.setFont(mediumFont);
		// fm = gc.getFontMetrics();
		for (int i = 0; i < numProblems; i++) {
			int ii = i;
			if (i > maxBalloonsPerLine - 1)
				ii -= maxBalloonsPerLine;

			if (i == maxBalloonsPerLine) {
				gc.setFont(font);
				fm = gc.getFontMetrics();
				iy += nbp.y + fm.getHeight() + gap;
			}

			Image tmpImg = null;
			if (i == solvedProblem)
				tmpImg = replaceColor(balloonImage, probs[i].getColorVal(), false);
			else if (solved[i])
				tmpImg = replaceColor(balloonImage, probs[i].getColorVal(), true);
			else
				tmpImg = replaceColor(balloonImage, null, true);

			gc.drawImage(tmpImg, 0, 0, bp.width, bp.height, r.x + p.x + gap + wid * ii, iy, nbp.x, nbp.y);
			tmpImg.dispose();

			if (i == solvedProblem)
				gc.setForeground(device.getSystemColor(SWT.COLOR_WHITE));
			else
				gc.setForeground(device.getSystemColor(SWT.COLOR_BLACK));
			s = probs[i].getLabel();
			if (s == null)
				s = "";
			gc.setFont(mediumFont);
			fm = gc.getFontMetrics();
			int bx = r.x + p.x + gap + nbp.x / 2 + wid * ii;
			gc.drawString(s, bx - gc.stringExtent(s).x / 2, iy + nbp.y / 2 - fm.getHeight(), true);

			if (i == solvedProblem)
				gc.setForeground(device.getSystemColor(SWT.COLOR_BLACK));
			gc.setFont(font);
			s = probs[i].getColor();
			if (s != null)
				gc.drawString(s, bx - gc.stringExtent(s).x / 2, iy + nbp.y + dpi.y / 20, true);
		}

		gc.setFont(font);
		fm = gc.getFontMetrics();
		iy += nbp.y + fm.getHeight() + gap;

		gc.setForeground(device.getSystemColor(SWT.COLOR_BLACK));
		int yy = y;
		if (iy > yy)
			yy = iy;

		int w = (int) (r.width * 0.25);
		int by = yy + w / 2;
		try {
			gc.setFont(font);
			UPCa upc = new UPCa(b.getId() + 100000);
			upc.draw(gc, new Rectangle(r.x + r.width - w - r.width / 50, yy + r.width / 60, w, w / 2));
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error drawing UPC", e);
		}

		if (messages != null && messages.length == 4) {
			String groupName = getGroupLabel(c, team);
			if (b.isFirstInContest())
				yy = addMessage(gc, yy, r.x, dpi.x / 4,
						subs(messages[0], groupName, problem.getLabel(), problem.getColor()));

			if (b.isFirstInGroup())
				yy = addMessage(gc, yy, r.x, dpi.x / 4,
						subs(messages[1], groupName, problem.getLabel(), problem.getColor()));

			if (b.isFirstForProblem())
				yy = addMessage(gc, yy, r.x, dpi.x / 4,
						subs(messages[2], groupName, problem.getLabel(), problem.getColor()));

			if (b.isFirstForTeam())
				yy = addMessage(gc, yy, r.x, dpi.x / 4,
						subs(messages[3], groupName, problem.getLabel(), problem.getColor()));
		}

		if (yy < by)
			yy = by;

		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setLineWidth(1);
		gc.setForeground(device.getSystemColor(SWT.COLOR_BLACK));
		gc.setBackground(device.getSystemColor(SWT.COLOR_WHITE));

		Rectangle r2 = new Rectangle(0, 0, 0, 0);
		if (bannerImage != null) {
			Rectangle rr = bannerImage.getBounds();
			float scale = r.width * 0.5f / rr.width;
			r2 = new Rectangle(0, 0, (int) (rr.width * scale), (int) (rr.height * scale));
			gc.drawImage(bannerImage, 0, 0, rr.width, rr.height, r.x + (r.width - r2.width) / 2,
					r.y + r.height - r2.height, r2.width, r2.height);
		}

		gc.setFont(font);
		String contestTitle = c.getName();
		p = gc.stringExtent(contestTitle);
		gc.drawString(contestTitle, r.x + (r.width - p.x) / 2, r.y + r.height - r2.height - p.y - dpi.y / 20);

		s = "Delivered";
		gc.setFont(mediumFont);
		y = p.y;
		p = gc.stringExtent(s);
		// y = by + dpi.y / 4;
		y = r.y + r.height - r2.height - p.y - dpi.y / 4 - y;
		gc.drawString(s, r.x + r.width - p.x - dpi.x * 2, y);
		gc.drawLine(r.x + r.width - (int) (dpi.x * 1.95f), y + p.y, r.x + r.width, y + p.y);

		gc.setFont(font);

		try {
			if (map == null)
				map = new FloorMap(c);

			Path path1 = null;
			IMapInfo mapInfo = c.getMapInfo();
			if (mapInfo != null && mapInfo.getPrinter() != null)
				path1 = map.getPath(mapInfo.getPrinter(), problem);
			Path path2 = map.getPath(problem, team);
			drawFloorImpl(device, gc, c, map, new Rectangle(r.x, yy, r.width, r.height - yy - p.y), path1, path2,
					team.getId());
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error drawing map", e);
		}

		s = "Printed " + BalloonUtility.getDateString();
		gc.drawString(s, r.x + r.width - gc.stringExtent(s).x, r.y + r.height - p.y, true);
		y += fm.getHeight() * 2;
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

	private int addMessage(GC gc, int y, int rx, int indent, String message) {
		if (message == null || message.length() == 0)
			return y;

		String m1 = message;
		String m2 = null;
		int ind = message.indexOf("\n");
		if (ind > 0) {
			m1 = message.substring(0, ind);
			m2 = message.substring(ind + 1);
		}

		gc.setFont(largeFont);
		FontMetrics fm = gc.getFontMetrics();
		gc.drawString(m1, rx, y, true);
		int yy = y + fm.getHeight();

		if (m2 == null)
			return yy;

		gc.setFont(mediumFont);
		fm = gc.getFontMetrics();
		gc.drawString(m2, rx + indent, yy, true);
		return (yy + fm.getHeight());
	}

	protected void drawFloorImpl(Device device, GC gc, IContest c, FloorMap floor, Rectangle r, Path path1, Path path2,
			String teamId) {
		Rectangle2D.Double bounds = floor.getBounds(false);
		if (bounds == null)
			return;
		double scale = Math.min(r.width / bounds.width, r.height / bounds.height);
		int x1 = r.x - (int) (bounds.x * scale);
		int y1 = r.y - (int) (bounds.y * scale);

		if (path1 != null) {
			gc.setLineWidth(3);

			gc.setForeground(device.getSystemColor(SWT.COLOR_BLACK));
			int x = 0;
			int y = 0;
			boolean first = true;
			for (AisleIntersection ai : path1.list) {
				int nx = x1 + (int) (ai.x * scale);
				int ny = y1 + (int) (ai.y * scale);
				if (!first)
					gc.drawLine(x, y, nx, ny);
				else
					first = false;
				x = nx;
				y = ny;
			}

			x = 0;
			y = 0;
			first = true;
			for (AisleIntersection ai : path2.list) {
				int nx = x1 + (int) (ai.x * scale);
				int ny = y1 + (int) (ai.y * scale);
				if (!first)
					gc.drawLine(x, y, nx, ny);
				else
					first = false;
				x = nx;
				y = ny;
			}
			gc.setLineWidth(1);
		}

		IMapInfo mapInfo = c.getMapInfo();
		if (mapInfo == null)
			return;

		double tableWidth = mapInfo.getTableWidth();
		double tableDepth = mapInfo.getTableDepth();
		double teamAreaWidth = mapInfo.getTeamAreaWidth();
		double teamAreaDepth = mapInfo.getTeamAreaDepth();

		gc.setForeground(device.getSystemColor(SWT.COLOR_BLACK));
		for (ITeam team : c.getTeams()) {
			if (Double.isNaN(team.getX()) || Double.isNaN(team.getY()))
				continue;

			Transform oldTrans = new Transform(device);
			gc.getTransform(oldTrans);
			Transform transform = new Transform(device);
			gc.getTransform(transform);

			float xt = r.x + (int) ((team.getX() - bounds.x) * scale);
			float yt = r.y + (int) ((team.getY() - bounds.y) * scale);

			// team area
			float dx = 1f;
			transform.translate(xt, yt);
			transform.rotate((float) -team.getRotation());
			gc.setTransform(transform);

			String id = team.getId();
			Rectangle tr1 = new Rectangle((int) (-(teamAreaDepth + dx) * scale / 2f), (int) (-teamAreaWidth * scale / 2f),
					(int) (teamAreaDepth * scale), (int) (teamAreaWidth * scale));
			if (teamId != null && teamId.equals(id)) {
				gc.setBackground(device.getSystemColor(SWT.COLOR_DARK_GRAY));
				gc.fillRectangle(tr1);
			}
			gc.setForeground(device.getSystemColor(SWT.COLOR_BLACK));
			gc.drawRectangle(tr1);

			// chairs

			// table
			Rectangle tr = new Rectangle((int) (-tableDepth * scale / 2f), (int) (-tableWidth * scale / 2f),
					(int) (tableDepth * scale), (int) (tableWidth * scale));
			if (teamId != null && teamId.equals(id)) {
				gc.setBackground(device.getSystemColor(SWT.COLOR_DARK_GRAY));
				gc.fillRectangle(tr);
				gc.setForeground(device.getSystemColor(SWT.COLOR_BLACK));
				gc.drawRectangle(tr);
			} else {
				gc.setBackground(device.getSystemColor(SWT.COLOR_BLACK));
				gc.fillRectangle(tr);
				gc.setForeground(device.getSystemColor(SWT.COLOR_DARK_GRAY));
				gc.drawRectangle(tr);
			}

			// if (teamLabel != null && teamLabel.equals(label))
			gc.setForeground(device.getSystemColor(SWT.COLOR_WHITE));
			// else
			// gc.setForeground(device.getSystemColor(SWT.COLOR_WHITE));

			if (id != null) {
				transform.rotate(90);
				gc.setTransform(transform);

				Point se = gc.stringExtent(id);
				gc.drawString(id, (int) (-se.x / 2f - dx * scale / 20f), (int) (-se.y / 2f), true);
			}

			gc.setTransform(oldTrans);
			transform.dispose();
		}

		gc.setForeground(device.getSystemColor(SWT.COLOR_BLACK));
		for (IProblem b : c.getProblems()) {
			float dim = 1.7f;
			double d = dim * scale;
			int x = r.x + (int) ((b.getX() - bounds.x) * scale);
			int y = r.y + (int) ((b.getY() - bounds.y) * scale);

			gc.setBackground(device.getSystemColor(SWT.COLOR_WHITE));
			gc.fillOval(x - (int) (d / 2f), y - (int) (d / 2f), (int) d, (int) d);
			gc.setForeground(device.getSystemColor(SWT.COLOR_BLACK));
			gc.drawOval(x - (int) (d / 2f), y - (int) (d / 2f), (int) d, (int) d);

			Point se = gc.stringExtent(b.getId());
			gc.drawString(b.getLabel(), (int) (x - se.x / 2f), y - se.y / 2, true);
		}
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

		GC gc = null;
		Image img = null;

		try {
			Display d = shell.getDisplay();
			createResources(d);

			Rectangle r = new Rectangle(0, 0, 850, 1100);
			float scale = 0.75f;
			Rectangle ir = new Rectangle(0, 0, (int) (r.width * scale), (int) (r.height * scale));
			img = new Image(d, ir);

			gc = new GC(img);
			gc.setAntialias(SWT.ON);
			Transform t = new Transform(d);
			t.scale(scale, scale);
			gc.setTransform(t);

			String[] messages2 = messages;
			if (messages2 == null)
				messages2 = DEFAULT_MESSAGES;
			printPage(bc2, b2, d, gc, r, messages2);
		} catch (Exception e) {
			ErrorHandler.error("Error printing", e);
		} finally {
			if (gc != null)
				gc.dispose();
		}

		if (img == null)
			return;

		PrintPreviewDialog ptd = new PrintPreviewDialog(shell, img);
		ptd.open();
		img.dispose();
	}

	private static Image replaceColor(Image image, Color c, boolean fade) {
		RGB[] targetRGB = new RGB[3];
		if (c == null) {
			targetRGB[0] = new RGB(191, 191, 191);
			targetRGB[1] = new RGB(255, 255, 255);
			targetRGB[2] = new RGB(255, 255, 255);
		} else {
			int r = c.getRed();
			int g = c.getGreen();
			int b = c.getBlue();
			if (fade) {
				r = (r + 1275) / 6;
				g = (g + 1275) / 6;
				b = (b + 1275) / 6;

				targetRGB[0] = new RGB(127, 127, 127);
			} else
				targetRGB[0] = new RGB(0, 0, 0);
			targetRGB[1] = new RGB(r, g, b);
			if (fade)
				targetRGB[2] = new RGB(r, g, b);
			else
				targetRGB[2] = new RGB((r + 255) / 2, (g + 255) / 2, (b + 255) / 2);
		}

		ImageData srcData = image.getImageData();
		PaletteData palette = srcData.palette;
		if (palette != null && palette.colors != null) {
			palette.colors[0] = targetRGB[0];
			palette.colors[1] = targetRGB[1];
			palette.colors[2] = targetRGB[2];
		} else {
			int[] lineData = new int[srcData.width];
			for (int y = 0; y < srcData.height; y++) {
				srcData.getPixels(0, y, srcData.width, lineData, 0);
				for (int x = 0; x < lineData.length; x++) {
					int pixelValue = lineData[x];
					RGB rgb = palette.getRGB(pixelValue);
					if (rgb.red == 0)
						srcData.setPixel(x, y, palette.getPixel(targetRGB[0]));
					else if (rgb.red > 250 && rgb.green < 10)
						srcData.setPixel(x, y, palette.getPixel(targetRGB[1]));
					else if (rgb.red > 250 && rgb.green < 200)
						srcData.setPixel(x, y, palette.getPixel(targetRGB[2]));
				}
			}
		}

		ImageData newImageData = new ImageData(srcData.width, srcData.height, srcData.depth, palette, srcData.scanlinePad,
				srcData.data);

		return new Image(image.getDevice(), newImageData);
	}
}