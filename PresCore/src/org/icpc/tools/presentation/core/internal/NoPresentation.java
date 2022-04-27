package org.icpc.tools.presentation.core.internal;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.internal.NetworkUtil;
import org.icpc.tools.presentation.core.Presentation;

public class NoPresentation extends Presentation {
	private static final Dimension FULL_DIM = new Dimension(1143, 1200);
	private static final Dimension WRENCH_DIM = new Dimension(545, 330);

	private BufferedImage leftBracket;
	private BufferedImage rightBracket;
	private BufferedImage gear;
	private BufferedImage wrench;
	private BufferedImage icpc;
	private BufferedImage tools;
	private double scale;
	private Point origin;

	@Override
	public void init() {
		if (leftBracket != null)
			return;

		ClassLoader cl = getClass().getClassLoader();
		try {
			leftBracket = ImageIO.read(cl.getResource("images/tools/left-bracket.png"));
			rightBracket = ImageIO.read(cl.getResource("images/tools/right-bracket.png"));
			gear = ImageIO.read(cl.getResource("images/tools/gear.png"));
			wrench = ImageIO.read(cl.getResource("images/tools/wrench.png"));
			icpc = ImageIO.read(cl.getResource("images/tools/icpc.png"));
			tools = ImageIO.read(cl.getResource("images/tools/tools.png"));
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading images", e);
		}
	}

	@Override
	public long getRepeat() {
		return 18000L;
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		scale = Math.min(width * 0.6 / FULL_DIM.width, (height * 0.6 - 50) / FULL_DIM.height);
		origin = new Point((int) (width - FULL_DIM.width * scale) / 2, (int) (height - FULL_DIM.height * scale) / 2 - 20);
	}

	private void drawImage(Graphics2D g, BufferedImage img, int x, int y) {
		g.drawImage(img, x, y, (int) (img.getWidth() * scale), (int) (img.getHeight() * scale), null);
	}

	@Override
	public void paint(Graphics2D g) {
		long ms = getRepeatTimeMs();
		if (ms > 9000)
			ms = 18000 - ms;

		// gear - fade in during first 3s
		if (ms >= 3000)
			drawImage(g, gear, origin.x, origin.y);
		else {
			Graphics2D gg = (Graphics2D) g.create();
			gg.setComposite(AlphaComposite.SrcOver.derive(ms / 3000f));

			gg.translate(origin.x + (int) (WRENCH_DIM.width * scale), origin.y + (int) (WRENCH_DIM.height * scale));
			gg.rotate((3000 - ms) * 2.0 / 2500);

			drawImage(gg, gear, -(int) (WRENCH_DIM.width * scale), -(int) (WRENCH_DIM.height * scale));
			gg.dispose();
		}

		// wrench - fade in and rotate into position between 1s and 3.5s
		if (ms >= 3000)
			drawImage(g, wrench, origin.x, origin.y);
		else if (ms > 500) {
			Graphics2D gg = (Graphics2D) g.create();
			if (ms < 1500)
				gg.setComposite(AlphaComposite.SrcOver.derive((ms - 500) / 1000f));
			gg.translate(origin.x + (int) (WRENCH_DIM.width * scale), origin.y + (int) (WRENCH_DIM.height * scale));
			gg.rotate(-(3000 - ms) * 2.0 / 2500);
			drawImage(gg, wrench, -(int) (WRENCH_DIM.width * scale), -(int) (WRENCH_DIM.height * scale));
			gg.dispose();
		}

		// brackets - fade and slide in between 1.5s and 3.5s
		if (ms >= 3500) {
			drawImage(g, leftBracket, origin.x, origin.y);
			drawImage(g, rightBracket, origin.x + (int) ((FULL_DIM.width - rightBracket.getWidth()) * scale), origin.y);
		} else if (ms > 1500) {
			Graphics2D gg = (Graphics2D) g.create();
			int dx = -(int) (FULL_DIM.width * scale * (ms - 3500) / 2000.0 / 4.0);
			gg.setComposite(AlphaComposite.SrcOver.derive((ms - 1500) / 2000f));
			drawImage(gg, leftBracket, origin.x - dx, origin.y);
			drawImage(gg, rightBracket, origin.x + dx + (int) ((FULL_DIM.width - rightBracket.getWidth()) * scale),
					origin.y);
			gg.dispose();
		}

		// icpc tools - slide from left and right between 2.5s and 4.5s
		if (ms >= 4500) {
			drawImage(g, icpc, origin.x, origin.y);
			drawImage(g, tools, origin.x, origin.y + (int) ((FULL_DIM.height - tools.getHeight()) * scale));
		} else if (ms > 2500) {
			Graphics2D gg = (Graphics2D) g.create();
			int dx = -(int) (FULL_DIM.width * scale * (ms - 4500) / 2000.0 / 4.0);
			gg.setComposite(AlphaComposite.SrcOver.derive((ms - 2500) / 2000f));
			drawImage(gg, icpc, origin.x - dx, origin.y);
			drawImage(gg, tools, origin.x + dx, origin.y + (int) ((FULL_DIM.height - tools.getHeight()) * scale));
			gg.dispose();
		}

		Dimension d = getSize();
		g.setColor(isLightMode() ? Color.BLACK : Color.WHITE);
		FontMetrics fm = g.getFontMetrics();
		String s = "No presentation assigned";
		g.drawString(s, (d.width - fm.stringWidth(s)) / 2, (d.height) * 7 / 8);

		g.setColor(isLightMode() ? Color.LIGHT_GRAY : Color.DARK_GRAY);
		s = NetworkUtil.getLocalAddress();
		g.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height - fm.getHeight() - 20);

		s = Trace.getVersion();
		g.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height - 20);
	}
}