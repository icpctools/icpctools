package org.icpc.tools.cds.web;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.FileReference;
import org.icpc.tools.contest.model.util.QRCode;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Prints pages of team labels with QR codes.
 *
 * Can pass in any format, but has pre-configured support for Avery 5392 and 5393.
 *
 * Can print on A4 too, but these labels are for letter paper so will
 */
public class TeamPDFService {
	public static class Format {
		public final int numRows, numCols;

		public final float vMargin, hMargin;

		public final float vSpacing, hSpacing;

		public final float cellMargin;

		// pdfs use points (1/72") for measurements
		public Format(int numRows, int numCols, float vMargin, float hMargin, float vSpacing, float hSpacing,
				float cellMargin) {
			this.numRows = numRows;
			this.numCols = numCols;
			this.vMargin = vMargin;
			this.hMargin = hMargin;
			this.vSpacing = vSpacing;
			this.hSpacing = hSpacing;
			this.cellMargin = cellMargin;
		}
	}

	// Avery 5395: 8.5x11" paper, 8 badges (2x4) that are each 3 3/8x2 1/3"
	public static final Format Avery_5395 = new Format(4, 2, 36, 50, 14, 27, 10);

	// Avery 5392: 8.5x11" paper, 6 badges (2x3) that are each 4x3"
	public static final Format Avery_5392 = new Format(3, 2, 72, 18, 0, 0, 10);

	public static void generate(HttpServletRequest request, HttpServletResponse response, ConfiguredContest cc)
			throws IOException {
		request.setCharacterEncoding("UTF-8");
		boolean isA4 = "A4".equals(request.getParameter("paper"));

		String template = request.getParameter("template");
		Format format = Avery_5392;
		if ("Avery_5395".equals(template))
			format = Avery_5395;

		generate(response, cc, isA4, format, "true".equals(request.getParameter("debug")));
	}

	public static void generate(HttpServletResponse response, ConfiguredContest cc, boolean A4, Format format,
			boolean debug) throws IOException {
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("ICPC-Tools", "CDS");
		response.setContentType("application/pdf");

		Document document = null;
		if (A4)
			document = new Document(PageSize.A4);
		else
			document = new Document(PageSize.LETTER);
		document.setMargins(format.hMargin, format.hMargin, format.vMargin, format.vMargin);
		PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());

		IContest contest = cc.getContest();
		document.addTitle(contest.getActualFormalName());
		document.addSubject("Team printout");
		document.addAuthor("ICPC Tools");
		document.open();

		// get and sort the teams
		ITeam[] teams = contest.getTeams();
		int numTeams = teams.length;
		Arrays.sort(teams, new Comparator<ITeam>() {
			@Override
			public int compare(ITeam t1, ITeam t2) {
				try {
					Integer in1 = Integer.parseInt(t1.getLabel());
					Integer in2 = Integer.parseInt(t2.getLabel());
					return in1.compareTo(in2);
				} catch (Exception e) {
					// ignore
				}
				return t1.getLabel().compareTo(t2.getLabel());
			}
		});

		PdfContentByte cb = writer.getDirectContent();
		BaseFont font2 = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);

		Rectangle r = document.getPageSize();
		int w = (int) ((r.getWidth() - format.hMargin * 2 - (format.hSpacing * (format.numCols - 1))) / format.numCols);
		int h = (int) ((r.getHeight() - format.vMargin * 2 - (format.vSpacing * (format.numRows - 1))) / format.numRows);
		int teamsPerPage = format.numRows * format.numCols;

		for (int i = 0; i < numTeams; i++) {
			if (i % teamsPerPage == 0) {
				document.newPage();
			}

			int x = i % format.numCols;
			int y = format.numRows - 1 - i / format.numCols % format.numRows;

			// size of each badge
			Rectangle2D or = new Rectangle2D.Double(format.hMargin + x * (w + format.hSpacing),
					format.vMargin + y * (h + format.vSpacing), w, h);

			// inner size we'll print on
			Rectangle2D rr = new Rectangle2D.Double(or.getX() + format.cellMargin, or.getY() + format.cellMargin,
					or.getWidth() - format.cellMargin * 2, or.getHeight() - format.cellMargin * 2);

			if (debug) {
				cb.rectangle((float) or.getX(), (float) or.getY(), (float) or.getWidth(), (float) or.getHeight());
				cb.stroke();

				cb.setColorStroke(Color.BLUE);
				cb.rectangle((float) rr.getX(), (float) rr.getY(), (float) rr.getWidth(), (float) rr.getHeight());
				cb.stroke();

				cb.setColorStroke(Color.BLACK);
			}
			try {
				drawTeam(cb, contest, teams[i], font2, rr);
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error adding team to pdf", e);
			}
		}

		document.newPage();
		document.close();
	}

	protected static void drawTeam(PdfContentByte cb, IContest contest, ITeam team, BaseFont font, Rectangle2D r)
			throws IOException {
		IOrganization org = contest.getOrganizationById(team.getOrganizationId());
		Image logoImg = null;
		if (org != null) {
			BufferedImage image = org.getLogoImage(1024, 1024, FileReference.TAG_LIGHT, true, false);
			if (image != null) {
				logoImg = Image.getInstance(image, null);
				float scale = (float) (72f * r.getHeight() / 2f / Math.max(logoImg.getWidth(), logoImg.getHeight()));
				logoImg.scalePercent(scale);
				logoImg.setAbsolutePosition((float) (r.getMinX() + r.getWidth() * 0.1f), (float) r.getMinY());
				cb.addImage(logoImg);
			}
		}

		cb.beginText();
		int fontSize = 20;
		cb.setFontAndSize(font, fontSize);
		String[] s = splitText(team.getActualDisplayName(), cb, r.getWidth() * 0.92);
		float y = (float) (r.getMaxY() - fontSize);
		for (String ss : s) {
			cb.showTextAligned(PdfContentByte.ALIGN_CENTER, ss, (float) (r.getX() + r.getWidth() / 2f), y, 0);
			y -= r.getHeight() / 7;
		}

		cb.setFontAndSize(font, 32);
		cb.showTextAligned(PdfContentByte.ALIGN_CENTER, team.getLabel(), (float) (r.getX() + r.getWidth() / 2),
				(float) r.getMinY(), 0);
		cb.endText();

		BufferedImage image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setColor(Color.BLACK);
		QRCode.drawQRCode(g, team.getLabel(), 0, 0, 500);
		g.dispose();
		Image qrImg = Image.getInstance(image, null);
		float scale = (float) (72f * r.getHeight() / 2f / qrImg.getWidth() * 0.7f);
		qrImg.scalePercent(scale);
		qrImg.setAbsolutePosition((float) (r.getMaxX() - qrImg.getScaledWidth() - r.getWidth() * 0.1f),
				(float) r.getMinY());
		cb.addImage(qrImg);
	}

	private static String[] splitText(String src, PdfContentByte cb, double width) {
		if (src == null) {
			return new String[] {};
		}

		if (cb.getEffectiveStringWidth(src, true) < width || src.length() < 6)
			return new String[] { src };

		Matcher m = Pattern.compile(".+?[ \\t]|.+?(?:\n)|.+?$").matcher(src);

		StringBuilder sb = new StringBuilder();
		List<String> list = new ArrayList<String>(3);

		while (m.find() && cb.getEffectiveStringWidth(sb.toString(), true) < width) {
			String word = m.group();

			// would adding this word make it too long? if so, find the best place to crop
			if (cb.getEffectiveStringWidth(sb.toString() + word, true) > width) {
				if (sb.length() == 0) {
					// just a really long word, add anyway
					list.add((sb.toString() + word).trim());
					sb = new StringBuilder();
				} else {
					list.add(sb.toString().trim());
					sb = new StringBuilder(word);
				}
			} else {
				sb.append(word);
			}
		}

		list.add(sb.toString().trim());

		return list.toArray(new String[0]);
	}
}