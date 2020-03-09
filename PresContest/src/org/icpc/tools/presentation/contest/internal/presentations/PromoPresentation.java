package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.presentation.contest.internal.ImageHelper;
import org.icpc.tools.presentation.contest.internal.ImageScaler;
import org.icpc.tools.presentation.core.Presentation;

public class PromoPresentation extends Presentation {
	private static final String[] HELP = new String[] {
			"This presentation will show one promotional image each time it is shown.",
			"Place images on the CDS in {icpc.cds.config}/present/promo." };
	private static final int SECONDS_PER_PROMO = 6;

	private List<BufferedImage> images;
	private int num;

	@Override
	public void init() {
		execute(new Runnable() {
			@Override
			public void run() {
				String[] filenames = null;
				try {
					filenames = ContestSource.getInstance().getDirectory("/presentation/promo");
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Could not load present/promo contents: " + e.getMessage());
				}
				if (filenames == null || filenames.length == 0)
					return;

				List<BufferedImage> list = new ArrayList<>();
				for (String s : filenames) {
					try {
						if (s.startsWith("."))
							continue;
						BufferedImage img = ImageHelper.loadImage("/presentation/promo/" + s);
						if (img != null) {
							list.add(ImageScaler.scaleImage(img, width, height));
						}
					} catch (Exception e) {
						Trace.trace(Trace.ERROR, "Could not load image " + s + ": " + e.getMessage());
					}
				}
				images = list;
				Trace.trace(Trace.INFO, "Promo loaded " + images.size() + " images");
			}
		});
	}

	@Override
	public long getRepeat() {
		return SECONDS_PER_PROMO * 1000L;
	}

	@Override
	public long getDelayTimeMs() {
		if (images == null)
			return 500;

		return SECONDS_PER_PROMO * 1000L;
	}

	@Override
	public void aboutToShow() {
		if (images == null) {
			num = 0;
			return;
		}
		num++;
		num %= images.size();
	}

	@Override
	public void paint(Graphics2D g) {
		if (images == null || images.isEmpty()) {
			paintHelp(g, HELP);
			return;
		}

		BufferedImage image = images.get(num);
		int w = image.getWidth();
		int h = image.getHeight();

		g.drawImage(image, (width - w) / 2, (height - h) / 2, w, h, null);
	}
}