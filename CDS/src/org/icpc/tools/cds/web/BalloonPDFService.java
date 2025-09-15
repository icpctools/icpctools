package org.icpc.tools.cds.web;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.internal.FileReference;
import org.icpc.tools.contest.model.util.Balloon;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BalloonPDFService {
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
		PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());

		IContest contest = cc.getContest();
		document.addTitle(contest.getActualFormalName());
		document.addSubject("Balloon printouts");
		document.addAuthor("ICPC Tools");
		document.open();

		// generate balloon images
		Balloon.load(cc.getClass());
		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;
		Image[] images = new Image[numProblems];
		for (int i = 0; i < numProblems; i++) {
			BufferedImage img = Balloon.getBalloonImage(problems[i].getColorVal());
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ImageIO.write(img, "png", bout);
			images[i] = Image.getInstance(bout.toByteArray());
		}

		BufferedImage img = contest.getBannerImage(3840, 1080, FileReference.TAG_LIGHT, true, false);
		Image banner = null;
		if (img != null) {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ImageIO.write(img, "png", bout);
			banner = Image.getInstance(bout.toByteArray());
		}

		// a page or two with all problems with balloon colours
		Font font = FontFactory.getFont(FontFactory.HELVETICA, 24, Color.BLACK);
		Font font2 = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);

		PdfPTable table = new PdfPTable(3);
		table.setWidthPercentage(100);
		table.setWidths(new float[] { 3.5f, 3.5f, 3.5f });

		// add header row
		PdfPCell hCell = null;
		if (banner != null) {
			float scale = document.getPageSize().getWidth() * 80f / banner.getWidth();
			banner.scalePercent(scale);
			hCell = new PdfPCell(banner);
		} else
			hCell = new PdfPCell(new Phrase(contest.getActualFormalName(), font));

		hCell.setBorder(Rectangle.NO_BORDER);
		hCell.setPadding(10);
		hCell.setPaddingBottom(25);
		hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		hCell.setColspan(3);
		table.addCell(hCell);
		table.setHeaderRows(1);

		for (int i = 0; i < numProblems; i++) {
			IProblem p = problems[i];
			images[i].scalePercent(12);
			PdfPCell cell = new PdfPCell(images[i]);
			cell.setBorder(Rectangle.NO_BORDER);
			cell.setPadding(10);
			cell.setPaddingRight(15);
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			table.addCell(cell);

			cell = new PdfPCell(new Phrase("Problem " + p.getLabel(), font));
			cell.setBorder(Rectangle.NO_BORDER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell);

			cell = new PdfPCell(new Phrase("(" + p.getColor() + ")", font2));
			cell.setBorder(Rectangle.NO_BORDER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell);
		}
		document.add(table);

		// a horizontal page with two columns
		if (A4)
			document.setPageSize(PageSize.A4.rotate());
		else
			document.setPageSize(PageSize.LETTER.rotate());
		document.newPage();

		font = FontFactory.getFont(FontFactory.HELVETICA, 26, Color.BLACK);

		table = new PdfPTable(4);
		table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
		table.setWidthPercentage(100);
		table.setWidths(new float[] { 3.0f, 3.0f, 3.0f, 4.0f });

		// add header row
		if (banner != null) {
			float scale = document.getPageSize().getWidth() * 80f / banner.getWidth();
			banner.scalePercent(scale);
			hCell = new PdfPCell(banner);
		} else
			hCell = new PdfPCell(new Phrase(contest.getActualFormalName(), font));

		hCell.setBorder(Rectangle.NO_BORDER);
		hCell.setPadding(10);
		hCell.setPaddingBottom(25);
		hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		hCell.setColspan(4);
		table.addCell(hCell);
		table.setHeaderRows(1);

		for (int i = 0; i < numProblems; i++) {
			IProblem p = problems[i];
			images[i].scalePercent(15);
			PdfPCell cell = new PdfPCell(images[i]);
			cell.setBorder(Rectangle.NO_BORDER);
			cell.setPadding(10);
			cell.setPaddingRight(15);
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell);

			cell = new PdfPCell(new Phrase("Problem " + p.getLabel(), font));
			cell.setBorder(Rectangle.NO_BORDER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell);
		}

		if (numProblems % 2 != 0)
			table.completeRow();

		document.add(table);

		if (A4)
			document.setPageSize(PageSize.A4);
		else
			document.setPageSize(PageSize.LETTER);

		// one page for each problem with large letter
		font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 225, Color.BLACK);

		for (int i = 0; i < numProblems; i++) {
			IProblem p = problems[i];
			document.newPage();

			float width = document.getPageSize().getWidth();
			float height = document.getPageSize().getHeight();

			PdfContentByte canvas = writer.getDirectContentUnder();
			canvas.saveState();
			PdfGState state = new PdfGState();
			state.setFillOpacity(0.2f);
			canvas.setGState(state);
			canvas.addImage(images[i], width * 0.6f, 0, 0, height * 0.6f, width * 0.2f, height * 0.2f);
			canvas.restoreState();

			Paragraph para = new Paragraph(p.getLabel(), font);
			para.setAlignment(Element.ALIGN_CENTER);
			document.add(para);
		}

		document.close();
	}
}