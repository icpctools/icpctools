package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.ImageHelper;
import org.icpc.tools.presentation.contest.internal.ImageScaler;

public class CCSPresentation extends AbstractICPCPresentation {
	protected static final int BORDER = 10;
	protected static final String PRIMARY_TEXT = "Contest Control System";
	protected static final String SECONDARY_TEXT = "Shadow Verification";
	protected Font font;

	protected BufferedImage primaryImage;
	protected BufferedImage shadowImage;

	@Override
	public void init() {
		if (primaryImage != null)
			return;

		try {
			primaryImage = ImageHelper.loadImage("/presentation/ccs/primary.png");
		} catch (Exception e) {
			e.printStackTrace();
			Trace.trace(Trace.ERROR, "Error loading primary image", e);
		}

		try {
			shadowImage = ImageHelper.loadImage("/presentation/ccs/shadow.png");
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading secondary image", e);
		}

		if (shadowImage == null) {
			primaryImage = ImageScaler.scaleImage(primaryImage, width * 0.8, height * 0.7);
		} else {
			primaryImage = ImageScaler.scaleImage(primaryImage, width * 0.4, height * 0.7);
			shadowImage = ImageScaler.scaleImage(shadowImage, width * 0.4, height * 0.7);
		}

		final float dpi = 96;
		font = ICPCFont.getMasterFont().deriveFont(Font.BOLD, height * 4f / dpi);
	}

	@Override
	public long getDelayTimeMs() {
		return 200;
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		if (primaryImage == null)
			return;

		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		if (shadowImage == null) {
			g.drawImage(primaryImage, (width - primaryImage.getWidth()) / 2,
					(height - primaryImage.getHeight() - BORDER) / 2 - fm.getHeight(), null);
		} else {
			g.drawImage(primaryImage, (width / 2 - primaryImage.getWidth()) / 2,
					(height - primaryImage.getHeight() - BORDER) / 2 - fm.getHeight(), null);
			g.drawImage(shadowImage, (width * 3 / 2 - shadowImage.getWidth()) / 2,
					(height - shadowImage.getHeight() - BORDER) / 2 - fm.getHeight(), null);
		}

		g.setColor(Color.WHITE);
		if (shadowImage == null) {
			g.drawString(PRIMARY_TEXT, (width - fm.stringWidth(PRIMARY_TEXT)) / 2, height - BORDER - fm.getDescent());
		} else {
			g.drawString(PRIMARY_TEXT, (width / 2 - fm.stringWidth(PRIMARY_TEXT)) / 2, height - BORDER - fm.getDescent());
			g.drawString(SECONDARY_TEXT, (width * 3 / 2 - fm.stringWidth(SECONDARY_TEXT)) / 2,
					height - BORDER - fm.getDescent());
		}
	}
}