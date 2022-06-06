package org.icpc.tools.balloon;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class UPCa {
	private final long upc;

	private static final byte[][] DATA = new byte[][] { { 0, 0, 1, 1, 0 }, // 0
			{ 0, 1, 1, 0, 0 }, // 1
			{ 0, 1, 0, 0, 1 }, // 2
			{ 1, 1, 1, 1, 0 }, // 3
			{ 1, 0, 0, 0, 1 }, // 4
			{ 1, 1, 0, 0, 0 }, // 5
			{ 1, 0, 1, 1, 1 }, // 6
			{ 1, 1, 1, 0, 1 }, // 7
			{ 1, 1, 0, 1, 1 }, // 8
			{ 0, 0, 1, 0, 1 } }; // 9

	// 1 23456 78999
	// 3 60002 9145
	public UPCa(long upc) {
		if (upc < 0 || upc > 99999999999l)
			throw new IllegalArgumentException("UPC-A must be a positive integer with a maximum of 11 digits");
		this.upc = upc;
	}

	public void draw(Graphics2D g, Rectangle r) {
		byte[] b = new byte[12];
		long u = upc;
		int c = 10;
		while (u > 0) {
			b[c--] = (byte) (u % 10);
			u /= 10;
		}

		// add checksum
		int cs = 0;
		for (int i = 0; i < 6; i++)
			cs += b[i * 2];
		cs *= 3;
		for (int i = 0; i < 6; i++)
			cs += b[i * 2 + 1];
		cs %= 10;
		if (cs == 0)
			b[11] = (byte) cs;
		else
			b[11] = (byte) (10 - cs);

		// determine size
		int dx = r.width / 102;
		int sx = r.x + (r.width - dx * 102) / 2 + dx * 9;
		int bh = r.height - dx * 5;

		// draw S
		g.setColor(Color.BLACK);
		g.fillRect(sx, r.y, dx + 1, r.height + 1);
		sx += dx * 2;
		g.fillRect(sx, r.y, dx + 1, r.height + 1);
		sx += dx;

		// draw left side
		for (int i = 0; i < 6; i++) {
			sx += dx;
			for (int j = 0; j < 5; j++) {
				if (DATA[b[i]][j] == 1)
					g.fillRect(sx, r.y, dx + 1, bh);
				sx += dx;
			}
			g.fillRect(sx, r.y, dx + 1, bh);
			sx += dx;
		}

		// draw M
		sx += dx;
		g.fillRect(sx, r.y, dx + 1, r.height + 1);
		sx += dx * 2;
		g.fillRect(sx, r.y, dx + 1, r.height + 1);
		sx += dx * 2;

		// draw right side
		for (int i = 6; i < 12; i++) {
			g.fillRect(sx, r.y, dx + 1, bh);
			sx += dx;
			for (int j = 0; j < 5; j++) {
				if (DATA[b[i]][j] == 0) // <---- exception
					g.fillRect(sx, r.y, dx + 1, bh);
				sx += dx;
			}
			sx += dx;
		}

		// draw E
		g.fillRect(sx, r.y, dx + 1, r.height + 1);
		sx += dx * 2;
		g.fillRect(sx, r.y, dx + 1, r.height + 1);

		sx = r.x + (r.width - dx * 102) / 2 + dx * 9;
		FontMetrics fm = g.getFontMetrics();
		int yy = r.y + bh + fm.getAscent();
		String s = b[0] + "";
		g.drawString(s, sx - fm.stringWidth(s) - dx, yy);
		sx += dx * 3;

		for (int i = 1; i < 6; i++) {
			s = b[i] + "";
			g.drawString(s, sx + (int) (dx * (i - 1f) * (35f / 4f)) + (dx * 7 - fm.stringWidth(s)) / 2, yy);
		}
		sx += dx * 42;
		sx += dx * 5;

		for (int i = 6; i < 11; i++) {
			s = b[i] + "";
			g.drawString(s, sx + (int) (dx * (i - 6f) * (35f / 4f)) + (dx * 7 - fm.stringWidth(s)) / 2, yy);
		}
		sx += dx * 42;

		sx += dx * 3;
		s = b[11] + "";
		g.drawString(s, sx + dx, yy);
	}
}