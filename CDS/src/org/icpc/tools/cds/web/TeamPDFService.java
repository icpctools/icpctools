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
 * Prints pages of team labels with QR codes
 *
 * Based roughly on Avery 5395.
 */
public class TeamPDFService {
	private static final int NUM_ROWS = 4;
	private static final int NUM_COLS = 2;

	// pdfs use points (1/72") for measurements
	private static final float V_MARGIN = 30;
	private static final float H_MARGIN = 30;
	private static final float CELL_MARGIN = 10;

	public static void generate(HttpServletRequest request, HttpServletResponse response, ConfiguredContest cc)
			throws IOException {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("ICPC-Tools", "CDS");
		response.setContentType("application/pdf");

		boolean A4 = request.getPathInfo().endsWith("A4");
		Document document = null;
		if (A4)
			document = new Document(PageSize.A4);
		else
			document = new Document(PageSize.LETTER);
		document.setMargins(H_MARGIN, H_MARGIN, V_MARGIN, V_MARGIN);
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
		BaseFont font2 = BaseFont.createFont(BaseFont.HELVETICA, "UTF-8", false);

		Rectangle r = document.getPageSize();
		int w = (int) ((r.getWidth() - H_MARGIN * 2) / NUM_COLS);
		int h = (int) ((r.getHeight() - V_MARGIN * 2) / NUM_ROWS);
		int teamsPerPage = NUM_ROWS * NUM_COLS;

		for (int i = 0; i < numTeams; i++) {
			if (i % teamsPerPage == 0) {
				document.newPage();
			}

			int x = i % NUM_COLS;
			int y = NUM_ROWS - 1 - i / NUM_COLS % NUM_ROWS;

			Rectangle2D rr = new Rectangle2D.Float(H_MARGIN + x * w + CELL_MARGIN, V_MARGIN + y * h + CELL_MARGIN,
					w - CELL_MARGIN * 2, h - CELL_MARGIN * 2);
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
			BufferedImage image = org.getLogoImage(1024, 1024, true, false);
			if (image != null) {
				logoImg = Image.getInstance(image, null);
				float scale = (float) (72f * r.getHeight() / 2f / Math.max(logoImg.getWidth(), logoImg.getHeight()));
				logoImg.scalePercent(scale);
				logoImg.setAbsolutePosition((float) (r.getMinX() + r.getWidth() * 0.1f), (float) r.getMinY());
				cb.addImage(logoImg);
			}
		}

		cb.beginText();
		cb.setFontAndSize(font, 20);
		String[] s = splitText(team.getActualDisplayName(), cb, r.getWidth() * 0.92);
		float y = (float) (r.getMaxY() - r.getHeight() / 4.5f);
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
		if (src == null || cb.getEffectiveStringWidth(src, true) < width || src.length() < 6)
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