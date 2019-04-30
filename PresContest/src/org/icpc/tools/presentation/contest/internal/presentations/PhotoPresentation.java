package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ImageHelper;
import org.icpc.tools.presentation.core.Presentation;

public class PhotoPresentation extends AbstractICPCPresentation {
	private static final String[] HELP = new String[] { "This presentation will pan across a series of photos.",
			"Place photos on the CDS in {icpc.cds.config}/present/photos." };

	private static final int SECONDS_PER_PHOTO = 12;
	private static final int CACHE_SIZE = 2;

	private String mode = null;

	private BufferedImage image;
	private final List<BufferedImage> images = new ArrayList<>(4);
	private int num;
	private String[] filenames;
	private Presentation.Job job;

	private Point2D.Float p1, p2;
	private float s1, s2;

	@Override
	public void init() {
		execute(new Runnable() {
			@Override
			public void run() {
				try {
					filenames = ContestSource.getInstance().getDirectory("/presentation/photos");
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Could not load present/photo contents: " + e.getMessage());
				}
				preloadImages();
			}
		});
	}

	@Override
	public void aboutToShow() {
		switchToNextImage();
	}

	private void preloadImages() {
		if (images.size() >= CACHE_SIZE)
			return;

		if (filenames == null || filenames.length == 0)
			return;

		if (job != null && !job.isComplete())
			return;

		job = execute(new Runnable() {
			@Override
			public void run() {
				while (images.size() < CACHE_SIZE) {
					num++;
					if (num >= filenames.length)
						num = 0;

					try {
						images.add(ImageHelper.loadImage("/presentation/photos/" + filenames[num]));
					} catch (Exception e) {
						Trace.trace(Trace.ERROR, "Could not load image " + filenames[num] + ": " + e.getMessage());
					}

					if (image == null)
						switchToNextImage();
				}
			}
		});
	}

	private void switchToNextImage() {
		if (images.isEmpty())
			return;

		BufferedImage old = image;
		image = images.remove(0);
		if (old != null)
			old.flush();

		setupImage();

		preloadImages();
	}

	@Override
	public long getRepeat() {
		return SECONDS_PER_PHOTO * 1000L;
	}

	protected void setupImage() {
		if (image == null)
			return;
		Dimension sd = getSize();
		Dimension id = new Dimension(image.getWidth(), image.getHeight());

		// determine the widest possible scaling for the final view
		s2 = Math.max((float) sd.width / id.width, (float) sd.height / id.height);

		// zoom in by 10% to start, unless it is greater than the pixel resolution
		s1 = s2 * 1.1f;

		// pick any corner to start
		p1 = new Point2D.Float();
		double r = Math.random();
		if (r >= 0.5)
			p1.x = id.width - (int) (sd.width / s1);
		if ((r >= 0.25 && r < 0.5) || r >= 0.75)
			p1.y = id.height - (int) (sd.height / s1);

		// pick any corner to finish
		p2 = new Point2D.Float();
		r = Math.random();
		if (r >= 0.5)
			p2.x = id.width - (int) (sd.width / s2);
		if ((r >= 0.25 && r < 0.5) || r >= 0.75)
			p2.y = id.height - (int) (sd.height / s2);
	}

	@Override
	public void paint(Graphics2D g) {
		if (image == null || p1 == null || p2 == null) {
			paintHelp(g, HELP, "");
			return;
		}

		float dt = getRepeatTimeMs() / SECONDS_PER_PHOTO / 1000f;
		if (dt > 1f)
			dt = 1f;

		double dx = p1.x * (1f - dt) + p2.x * dt;
		double dy = p1.y * (1f - dt) + p2.y * dt;
		double ds = s1 * (1f - dt) + s2 * dt;

		Graphics2D gg = (Graphics2D) g.create();
		gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if ("h".equals(mode))
			gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		else if ("l".equals(mode))
			gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		else
			gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		gg.scale(ds, ds);
		gg.translate(-dx, -dy);
		gg.drawImage(image, 0, 0, null);
		gg.dispose();
	}

	@Override
	public void setProperty(String value) {
		mode = value;
	}
}