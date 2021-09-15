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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

public class StaffPresentation extends AbstractICPCPresentation {
	private static final int BORDER = 15;
	private static final Color BG = new Color(0, 0, 0, 127);
	private Font nameFont;
	private Font roleFont;
	protected List<Staff> staff;

	protected Staff current;
	protected BufferedImage photo;
	protected BufferedImage logo;

	class Staff {
		String role;
		String firstName;
		String lastName;
	}

	@Override
	public void init() {
		if (staff != null)
			return;

		try {
			staff = new ArrayList<>();

			IContest contest = getContest();
			File f = ContestSource.getInstance().getFile("/contests/" + contest.getId() + "/staff-members.tsv");
			if (f == null || !f.exists()) {
				Trace.trace(Trace.WARNING, "Staff member file (staff-members.tsv) not found");
				return;
			}

			BufferedReader br = new BufferedReader(new FileReader(f));

			try {
				// read header
				br.readLine();

				String s = br.readLine();
				while (s != null) {
					String[] st = s.split("\\t");
					if (st != null && st.length > 0) {
						Staff sta = new Staff();
						sta.role = st[0];
						sta.firstName = st[1];
						sta.lastName = st[2];
						staff.add(sta);
					}
					s = br.readLine();
				}
			} finally {
				try {
					br.close();
				} catch (Exception e) {
					// ignore
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading staff list", e);
		}
	}

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
		if (staff == null)
			return;

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		if (photo != null) {
			// TODO paint here
		} else if (logo != null) {
			g.drawImage(logo, (width - logo.getWidth()) / 2, (height - logo.getHeight()) / 2 - BORDER * 4, null);
		}

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

		if (current != null && current.role != null) {
			float nn = 1f;
			while (fm.stringWidth(current.role) > width - BORDER * 2) {
				nn -= 0.02f;
				Font f = roleFont.deriveFont(AffineTransform.getScaleInstance(nn, 1.0));
				g.setFont(f);
				fm = g.getFontMetrics();
			}

			g.drawString(current.role, BORDER, y);
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
			String st = current.firstName + " " + current.lastName;

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
		if (value == null || value.isEmpty() || staff == null)
			return;

		Trace.trace(Trace.INFO, "Looking for staff: " + value);
		if (value.contains(" ")) {
			int ind = value.indexOf(" ");
			String first = value.substring(0, ind).toLowerCase();
			String last = value.substring(ind + 1, value.length()).toLowerCase();
			for (Staff s : staff) {
				if (s.firstName != null && s.firstName.toLowerCase().contains(first) && s.lastName != null
						&& s.lastName.toLowerCase().contains(last))
					current = s;
			}
		} else {
			String name = value.toLowerCase();
			for (Staff s : staff) {
				if (s.lastName != null && s.lastName.toLowerCase().contains(name))
					current = s;
			}
		}
		photo = null;
		if (current == null)
			Trace.trace(Trace.INFO, "No staff selected");
		else {
			Trace.trace(Trace.INFO, "Staff: " + current.firstName + " - " + current.lastName);
			// TODO load photo
		}
	}
}