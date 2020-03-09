package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.ImageScaler;
import org.icpc.tools.presentation.core.Presentation;

public class SinglePhotoPresentation extends Presentation {
	private static final String[] HELP = new String[] { "This presentation will show one image with a caption.",
			"Place the image on the CDS in {icpc.cds.config}/present/photo.jpg." };
	protected Font font;
	protected BufferedImage image;
	protected String message;

	private BufferedImage getImage() {
		if (image != null)
			return image;

		try {
			File f = ContestSource.getInstance().getFile("present/photo.jpg");
			if (f != null && f.exists())
				image = ImageScaler.scaleImage(ImageIO.read(f), width, height);
		} catch (IOException e) {
			Trace.trace(Trace.ERROR, "Error reading image", e);
		}
		return image;
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		BufferedImage img = getImage();
		if (img == null)
			paintHelp(g, HELP);
		else
			g.drawImage(img, (width - img.getWidth()) / 2, (height - img.getHeight()) / 2, null);

		if (font == null)
			font = ICPCFont.getMasterFont().deriveFont(Font.BOLD, 55);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		String s = message;
		if (s != null) {
			g.setColor(Color.WHITE);
			g.drawString(s, (width - fm.stringWidth(s)) / 2, height - 20 - fm.getDescent());
		}
	}

	@Override
	public void setProperty(String value) {
		message = value;
	}
}