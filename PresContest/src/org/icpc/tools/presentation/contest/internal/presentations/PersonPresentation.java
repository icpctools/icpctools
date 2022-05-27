package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IPerson;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

public class PersonPresentation extends AbstractICPCPresentation {
	private static final int BORDER = 15;
	private static final Color BG = new Color(0, 0, 0, 127);
	private Font nameFont;
	private Font roleFont;

	protected IPerson current;
	protected BufferedImage photo;
	protected BufferedImage logo;

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		float dpi = 96;
		float size = height * 36f / dpi;
		nameFont = ICPCFont.deriveFont(Font.BOLD, size * 0.25f);
		roleFont = ICPCFont.deriveFont(Font.PLAIN, size * 0.15f);

		logo = getContest().getLogoImage((int) (width * 0.7), (int) ((height - BORDER * 4) * 0.7), true, true);
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		if (photo != null)
			g.drawImage(photo, (width - logo.getWidth()) / 2, (height - logo.getHeight()) / 2 - BORDER * 4, null);
		else if (logo != null)
			g.drawImage(logo, (width - logo.getWidth()) / 2, (height - logo.getHeight()) / 2 - BORDER * 4, null);

		g.setFont(nameFont);
		FontMetrics fm = g.getFontMetrics();
		int h = fm.getHeight() + BORDER * 4;
		g.setFont(roleFont);
		fm = g.getFontMetrics();
		g.setColor(BG);
		h += fm.getHeight();
		g.fillRect(0, height - h, width, h);

		int y = height - BORDER;
		g.setColor(Color.WHITE);
		y -= fm.getDescent();

		if (current != null && current.getRole() != null) {
			float nn = 1f;
			while (fm.stringWidth(current.getRole()) > width - BORDER * 2) {
				nn -= 0.02f;
				Font f = roleFont.deriveFont(AffineTransform.getScaleInstance(nn, 1.0));
				g.setFont(f);
				fm = g.getFontMetrics();
			}

			g.drawString(current.getRole(), BORDER, y);
		}
		y -= fm.getAscent();

		y -= BORDER;
		g.setColor(ICPCColors.RED);
		Stroke s = g.getStroke();
		g.setStroke(new BasicStroke(2f));
		g.drawLine(BORDER, y, width - BORDER, y);
		g.setStroke(s);
		y -= BORDER;

		g.setColor(Color.WHITE);
		g.setFont(nameFont);
		fm = g.getFontMetrics();
		y -= fm.getDescent();
		if (current != null) {
			String st = current.getName();

			float nn = 1f;
			while (fm.stringWidth(st) > width - BORDER * 2) {
				nn -= 0.02f;
				Font f = nameFont.deriveFont(AffineTransform.getScaleInstance(nn, 1.0));
				g.setFont(f);
				fm = g.getFontMetrics();
			}

			g.drawString(st, BORDER, y);
		}
	}

	@Override
	public void setProperty(String value) {
		super.setProperty(value);
		if (value == null || value.isEmpty())
			return;

		Trace.trace(Trace.INFO, "Looking for person: " + value);
		for (IPerson p : getContest().getPersons()) {
			if (p.getName() != null && p.getName().toLowerCase().contains(value.toLowerCase()))
				current = p;
		}

		if (current == null) {
			Trace.trace(Trace.INFO, "No person selected");
			photo = null;
		} else {
			Trace.trace(Trace.INFO, "Person: " + current.getName());
			photo = current.getPhotoImage((int) (width * 0.7), (int) ((height - BORDER * 4) * 0.7), true, true);
		}
	}
}