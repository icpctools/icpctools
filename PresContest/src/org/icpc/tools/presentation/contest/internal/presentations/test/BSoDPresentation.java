package org.icpc.tools.presentation.contest.internal.presentations.test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.presentation.core.Presentation;

public class BSoDPresentation extends Presentation {
	protected BufferedImage image;

	@Override
	public long getDelayTimeMs() {
		// paint every 10s
		return 10000;
	}

	@Override
	public void init() {
		ClassLoader cl = getClass().getClassLoader();
		try {
			image = ImageIO.read(cl.getResource("images/bsod.png"));
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading image", e);
		}
	}

	@Override
	public void paint(Graphics2D g) {
		if (image == null)
			return;

		g.drawImage(image, 0, 0, width, height, 0, 0, image.getWidth(), image.getHeight(), null);
	}
}