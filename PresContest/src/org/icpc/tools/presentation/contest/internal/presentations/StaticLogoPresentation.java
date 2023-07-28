package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;

public class StaticLogoPresentation extends AbstractICPCPresentation {
	protected BufferedImage image;

	@Override
	public long getDelayTimeMs() {
		if (image == null)
			return 250;

		return 5000;
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		image = getContest().getLogoImage((int) (width * 0.8), (int) (height * 0.8), true, true);
	}

	@Override
	public void paintImpl(Graphics2D g) {
		if (image != null)
			g.drawImage(image, (width - image.getWidth()) / 2, (height - image.getHeight()) / 2, null);
	}
}