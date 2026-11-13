package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.presentation.contest.internal.ImageHelper;
import org.icpc.tools.presentation.core.Presentation;

public class PhotoPresentation extends Presentation {
	private static final String[] HELP = new String[] { "This presentation will pan across a series of photos.",
			"Place photos on the CDS in {icpc.cds.config}/present/photos." };

	private static final int SECONDS_PER_PHOTO = 12;
	private static final int MIN_DURATION_MS = 100;
	private static final int CACHE_SIZE = 2;

	private String mode = null;
	private int durationMs = SECONDS_PER_PHOTO * 1000;
	private int fadeMs = 0;

	private PhotoPan current;
	private PhotoPan outgoing;
	private BufferedImage blend;
	private final List<BufferedImage> images = new ArrayList<>(4);
	private int num;
	private String[] filenames;
	private Presentation.Job job;

	// Avoid fading a preloaded image that was replaced before the first paint.
	private boolean hasPainted;
	private boolean switchedBeforeRepeat;

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
		// The next photo may already be fading in before the repeat boundary.
		if (!switchedBeforeRepeat) {
			switchToNextImage();
		}
		switchedBeforeRepeat = false;
	}

	private void preloadImages() {
		if (images.size() >= CACHE_SIZE) {
			return;
		}

		if (filenames == null || filenames.length == 0) {
			return;
		}

		if (job != null && !job.isComplete()) {
			return;
		}

		job = execute(new Runnable() {
			@Override
			public void run() {
				while (images.size() < CACHE_SIZE) {
					num++;
					if (num >= filenames.length) {
						num = 0;
					}

					try {
						images.add(ImageHelper.loadImage("/presentation/photos/" + filenames[num]));
					} catch (Exception e) {
						Trace.trace(Trace.ERROR, "Could not load image " + filenames[num] + ": " + e.getMessage());
					}

					if (current == null) {
						switchToNextImage();
					}
				}
			}
		});
	}

	private boolean switchToNextImage() {
		if (images.isEmpty()) {
			return false;
		}

		if (outgoing != null) {
			outgoing.release();
			outgoing = null;
		}
		if (current != null) {
			if (fadeMs() > 0 && hasPainted) {
				outgoing = current;
			} else {
				current.release();
			}
		}
		current = createPan(images.remove(0));

		preloadImages();
		return true;
	}

	@Override
	public long getRepeat() {
		return durationMs;
	}

	private PhotoPan createPan(BufferedImage image) {
		Dimension sd = getSize();
		Dimension id = new Dimension(image.getWidth(), image.getHeight());

		// determine the widest possible scaling for the final view
		float s2 = Math.max((float) sd.width / id.width, (float) sd.height / id.height);

		// zoom in by 10% to start, unless it is greater than the pixel resolution
		float s1 = s2 * 1.1f;

		// pick any corner to start
		Point2D.Float p1 = new Point2D.Float();
		double r = Math.random();
		if (r >= 0.5) {
			p1.x = id.width - (int) (sd.width / s1);
		}
		if ((r >= 0.25 && r < 0.5) || r >= 0.75) {
			p1.y = id.height - (int) (sd.height / s1);
		}

		// pick any corner to finish
		Point2D.Float p2 = new Point2D.Float();
		r = Math.random();
		if (r >= 0.5) {
			p2.x = id.width - (int) (sd.width / s2);
		}
		if ((r >= 0.25 && r < 0.5) || r >= 0.75) {
			p2.y = id.height - (int) (sd.height / s2);
		}
		return new PhotoPan(image, p1, p2, s1, s2);
	}

	@Override
	public void paint(Graphics2D g) {
		if (current == null) {
			paintHelp(g, HELP, "");
			return;
		}

		int duration = durationMs;
		int fade = fadeMs();
		// Center each fade on the repeat boundary. The first photo is fully visible
		// because there is no predecessor, but its outgoing fade follows the same beat.
		double visible = getRepeatTimeMs() + fade / 2.0;
		if (fade > 0 && visible >= duration && !switchedBeforeRepeat) {
			switchedBeforeRepeat = switchToNextImage();
		}
		if (switchedBeforeRepeat) {
			visible -= duration;
		}
		if (outgoing != null && (fade <= 0 || visible >= fade)) {
			outgoing.release();
			outgoing = null;
		}

		if (outgoing == null || AlphaComposite.SrcOver.equals(g.getComposite())) {
			paintPhotos(g, visible, duration, fade);
		} else {
			// Blend the photos first, then apply the caller's composite to the whole frame.
			Dimension size = getSize();
			if (blend == null || blend.getWidth() != size.width || blend.getHeight() != size.height) {
				if (blend != null) {
					blend.flush();
				}
				blend = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
			}
			Graphics2D bg = blend.createGraphics();
			try {
				bg.setComposite(AlphaComposite.Clear);
				bg.fillRect(0, 0, size.width, size.height);
				bg.setComposite(AlphaComposite.SrcOver);
				paintPhotos(bg, visible, duration, fade);
			} finally {
				bg.dispose();
			}
			g.drawImage(blend, 0, 0, null);
		}
		hasPainted = true;
	}

	private void paintPhotos(Graphics2D g, double visible, int duration, int fade) {
		current.draw(g, PhotoPan.motionDt(visible, duration, fade), mode, 1f);
		if (outgoing != null) {
			outgoing.draw(g, PhotoPan.motionDt(duration + visible, duration, fade), mode,
					PhotoPan.fadeAlpha(visible, fade));
		}
	}

	@Override
	public void dispose() {
		if (current != null) {
			current.release();
		}
		if (outgoing != null) {
			outgoing.release();
		}
		if (blend != null) {
			blend.flush();
		}
		super.dispose();
	}

	/**
	 * One photo's Ken Burns state. {@code dt} runs from 0 at the start of its fade-in to 1 at the
	 * end of its fade-out. The span is {@code duration} plus the effective fade. The default fade
	 * is 0, a hard cut across the cycle; set {@code fade:} to opt in.
	 */
	static final class PhotoPan {
		final BufferedImage image;
		final Point2D.Float p1;
		final Point2D.Float p2;
		final float s1;
		final float s2;

		PhotoPan(BufferedImage image, Point2D.Float p1, Point2D.Float p2, float s1, float s2) {
			this.image = image;
			this.p1 = p1;
			this.p2 = p2;
			this.s1 = s1;
			this.s2 = s2;
		}

		void release() {
			image.flush();
		}

		void draw(Graphics2D g, float dt, String mode, float alpha) {
			if (image == null || p1 == null || p2 == null || alpha <= 0f) {
				return;
			}

			double dx = p1.x * (1f - dt) + p2.x * dt;
			double dy = p1.y * (1f - dt) + p2.y * dt;
			double ds = s1 * (1f - dt) + s2 * dt;

			Graphics2D gg = (Graphics2D) g.create();
			gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			if ("h".equals(mode)) {
				gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			} else if ("l".equals(mode)) {
				gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			} else {
				gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			}
			gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			if (alpha < 1f) {
				gg.setComposite(AlphaComposite.SrcOver.derive(alpha));
			}
			gg.scale(ds, ds);
			gg.translate(-dx, -dy);
			gg.drawImage(image, 0, 0, null);
			gg.dispose();
		}

		static long motionSpanMs(int durationMs, int fadeMs) {
			int fade = fadeMs < 0 ? 0 : fadeMs;
			return (long) durationMs + fade;
		}

		static float motionDt(double visibleMs, int durationMs, int fadeMs) {
			long span = motionSpanMs(durationMs, fadeMs);
			if (span <= 0) {
				return 1f;
			}
			if (visibleMs <= 0) {
				return 0f;
			}
			if (visibleMs >= span) {
				return 1f;
			}
			return (float) (visibleMs / span);
		}

		static float fadeAlpha(double elapsedMs, int fadeMs) {
			if (fadeMs <= 0) {
				return 0f;
			}
			if (elapsedMs <= 0) {
				return 1f;
			}
			if (elapsedMs >= fadeMs) {
				return 0f;
			}
			return (float) (1.0 - elapsedMs / fadeMs);
		}
	}

	int durationMs() {
		return durationMs;
	}

	/**
	 * Successive transitions start {@code duration} milliseconds apart. Finish each fade
	 * before the next transition starts so that only two photos are needed.
	 */
	int fadeMs() {
		int fade = fadeMs;
		if (fade > durationMs) {
			fade = durationMs;
		}
		return fade;
	}

	/**
	 * Unparseable text leaves {@code current} unchanged.
	 */
	static int parseMs(String text, int current) {
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			return current;
		}
	}

	static int clampDurationMs(int requested) {
		if (requested < MIN_DURATION_MS) {
			return MIN_DURATION_MS;
		}
		return requested;
	}

	@Override
	public void setProperty(String value) {
		if (value != null && value.startsWith("fade:")) {
			fadeMs = parseMs(value.substring("fade:".length()), fadeMs);
			return;
		}
		if (value != null && value.startsWith("duration:")) {
			durationMs = clampDurationMs(parseMs(value.substring("duration:".length()), durationMs));
			return;
		}
		mode = value;
	}
}
