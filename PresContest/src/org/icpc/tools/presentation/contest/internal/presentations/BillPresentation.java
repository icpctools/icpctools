package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;

public class BillPresentation extends AbstractICPCPresentation {
	protected BufferedImage image;

	@Override
	public long getDelayTimeMs() {
		// paint every 10s
		return 10000;
	}

	@Override
	public void init() {
		if (image != null)
			return;

		ClassLoader cl = getClass().getClassLoader();
		try {
			image = ImageIO.read(cl.getResource("images/bill.jpg"));
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading images", e);
		}
	}

	@Override
	public void paint(Graphics2D g) {
		if (image == null)
			return;

		g.drawImage(image, width - image.getWidth(), height - image.getHeight(), null);
	}
}