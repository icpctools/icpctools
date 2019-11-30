package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ImageHelper;
import org.icpc.tools.presentation.contest.internal.ImageScaler;

public class ImageBuildPresentation extends AbstractICPCPresentation {
	private static final String[] HELP = new String[] { "This presentation will fade between a series of images.",
			"Place a series of png or jpg images on the CDS in {icpc.cds.config}/present/build",
			"with filenames starting with '{0}', e.g. '{0}0.png and {0}1.png'" };
	private static final int INITIAL_DELAY = 800;
	private static final int FADE_IN_TIME = 800;
	private static final int SHOW_TIME = 10000;
	private static final int FADE_OUT_TIME = 1000;
	private BufferedImage[] images;
	private String path;

	public ImageBuildPresentation() {
		// no path
	}

	public ImageBuildPresentation(String path) {
		this.path = path;
	}

	protected void setImagePath(String path) {
		this.path = path;

		loadImages();
	}

	private List<String> filterByPath(String[] filenames) {
		List<String> list = new ArrayList<>();

		for (String s : filenames) {
			String lcs = s.toLowerCase();
			if ((lcs.endsWith(".jpg") || lcs.endsWith(".png")) && s.startsWith(path))
				list.add(s);
		}

		return list;
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		loadImages();
	}

	@Override
	public void init() {
		if (images == null)
			loadImages();
	}

	private void loadImages() {
		Trace.trace(Trace.INFO, "Image build loading from: " + path);
		images = null;

		if (path == null || width < 10 || height < 10)
			return;

		String[] filenames = null;
		try {
			filenames = ContestSource.getInstance().getDirectory("/presentation/build");
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not load images: " + e.getMessage());
		}

		if (filenames == null)
			return;

		Trace.trace(Trace.INFO, "Image build loaded: " + filenames.length);

		Arrays.sort(filenames);

		List<String> filteredFiles = filterByPath(filenames);

		Trace.trace(Trace.INFO, "Image build filtered: " + filteredFiles.size());

		List<BufferedImage> list = new ArrayList<>();
		for (String s : filteredFiles) {
			try {
				BufferedImage img = ImageHelper.loadImage("/presentation/build/" + s);
				if (img != null)
					list.add(ImageScaler.scaleImage(img, width * 0.9, height * 0.9));
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Could not load " + s + ": " + e.getMessage());
			}
		}

		if (!list.isEmpty())
			images = list.toArray(new BufferedImage[0]);
	}

	@Override
	public long getDelayTimeMs() {
		if (images == null)
			return 500;

		// we can skip painting during the initial delay
		long time = getRepeatTimeMs();
		if (time < INITIAL_DELAY)
			return INITIAL_DELAY - time;

		return 0;
	}

	@Override
	public long getRepeat() {
		if (images == null)
			return 1000L;
		return INITIAL_DELAY + FADE_IN_TIME * images.length + SHOW_TIME + FADE_OUT_TIME;
	}

	@Override
	public void paint(Graphics2D g) {
		if (images == null || images.length == 0) {
			String p = path;
			if (p == null)
				p = "{path}";
			paintHelp(g, HELP, p);
			return;
		}

		long time = getRepeatTimeMs();

		if (time < INITIAL_DELAY)
			return;

		double sc = 0.94 + 0.06 * time / getRepeat();
		time -= INITIAL_DELAY;

		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.translate(width / 2, height / 2);
		g.scale(sc, sc);
		g.translate(-width / 2, -height / 2);

		// fading in each image
		if (time < FADE_IN_TIME * images.length) {
			int count = 0;
			while (time > FADE_IN_TIME) {
				count++;
				time -= FADE_IN_TIME;
			}

			if (count > 0) {
				BufferedImage img = images[count - 1];
				g.drawImage(img, (width - img.getWidth()) / 2, (height - img.getHeight()) / 2, null);
			}

			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, time / (float) FADE_IN_TIME));

			BufferedImage img = images[count];
			g.drawImage(img, (width - img.getWidth()) / 2, (height - img.getHeight()) / 2, null);
			return;
		}

		// final image with fade out
		time -= (FADE_IN_TIME * images.length);
		if (time > SHOW_TIME) {
			g.setComposite(
					AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - (time - SHOW_TIME) / (float) FADE_OUT_TIME));
		}

		BufferedImage img = images[images.length - 1];
		g.drawImage(img, (width - img.getWidth()) / 2, (height - img.getHeight()) / 2, null);
	}

	@Override
	public void setProperty(String value) {
		path = value;

		loadImages();
	}
}