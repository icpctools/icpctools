package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

@RunWith(JUnit4.class)
public class PhotoFadeTest {
	@Test
	public void fadeAlphaFallsFromOpaqueToClear() {
		assertEquals(0f, PhotoPresentation.PhotoPan.fadeAlpha(0, 0), 0f);
		assertEquals(0f, PhotoPresentation.PhotoPan.fadeAlpha(500, 0), 0f);

		assertEquals(1f, PhotoPresentation.PhotoPan.fadeAlpha(0, 1000), 0f);
		assertEquals(1f, PhotoPresentation.PhotoPan.fadeAlpha(-20, 1000), 0f);
		assertEquals(0.5f, PhotoPresentation.PhotoPan.fadeAlpha(500, 1000), 0.0001f);
		assertEquals(0f, PhotoPresentation.PhotoPan.fadeAlpha(1000, 1000), 0f);
		assertEquals(0f, PhotoPresentation.PhotoPan.fadeAlpha(2500, 1000), 0f);
	}

	@Test
	public void kenBurnsCoversThePhotoAndTheFollowingFade() {
		long span = PhotoPresentation.PhotoPan.motionSpanMs(12_000, 0);
		assertEquals(12_000L, span);
		assertEquals(0f, PhotoPresentation.PhotoPan.motionDt(0, 12_000, 0), 0f);
		assertEquals(1f, PhotoPresentation.PhotoPan.motionDt(12_000L, 12_000, 0), 0f);
		assertEquals(1f, PhotoPresentation.PhotoPan.motionDt(span + 50, 12_000, 0), 0f);

		long faded = PhotoPresentation.PhotoPan.motionSpanMs(12_000, 1000);
		assertEquals(12_000L + 1000L, faded);
		float atCut = PhotoPresentation.PhotoPan.motionDt(12_000L, 12_000, 1000);
		assertEquals(12_000f / faded, atCut, 0.0001f);
		assertTrue(atCut < 1f);
		assertEquals(1f, PhotoPresentation.PhotoPan.motionDt(faded, 12_000, 1000), 0f);
	}

	@Test
	public void customFadeFollowsTheProperty() {
		PhotoPresentation photos = new PhotoPresentation();
		photos.setProperty("fade:2500");
		assertEquals(2500, photos.fadeMs());

		long span = PhotoPresentation.PhotoPan.motionSpanMs(12_000, photos.fadeMs());
		assertEquals(12_000L + 2500L, span);
		assertEquals(1f, PhotoPresentation.PhotoPan.fadeAlpha(0, 2500), 0f);
		assertEquals(0.5f, PhotoPresentation.PhotoPan.fadeAlpha(1250, 2500), 0.0001f);
		assertEquals(0f, PhotoPresentation.PhotoPan.fadeAlpha(2500, 2500), 0f);
		assertEquals(12_000f / span, PhotoPresentation.PhotoPan.motionDt(12_000L, 12_000, 2500), 0.0001f);
		assertEquals(1f, PhotoPresentation.PhotoPan.motionDt(span, 12_000, 2500), 0f);

		photos.setProperty("fade:nope");
		assertEquals(2500, photos.fadeMs());
		photos.setProperty("fade:-30");
		assertEquals(-30, photos.fadeMs());
		assertEquals(0f, PhotoPresentation.PhotoPan.fadeAlpha(0, photos.fadeMs()), 0f);
		assertEquals(12_000L, PhotoPresentation.PhotoPan.motionSpanMs(12_000, photos.fadeMs()));
		photos.setProperty("fade:50000");
		assertEquals(12_000, photos.fadeMs());
		assertEquals(12_000L + 12_000L, PhotoPresentation.PhotoPan.motionSpanMs(12_000, photos.fadeMs()));
	}

	@Test
	public void defaultDurationIsTwelveSeconds() {
		PhotoPresentation photos = new PhotoPresentation();
		assertEquals(12_000, photos.durationMs());
		assertEquals(0, photos.fadeMs());
		assertEquals(12_000L, photos.getRepeat());
		assertEquals(12_000L, PhotoPresentation.PhotoPan.motionSpanMs(photos.durationMs(), photos.fadeMs()));
	}

	@Test
	public void parsesDurationAndIgnoresInvalidValues() {
		PhotoPresentation photos = new PhotoPresentation();
		photos.setProperty("duration:15000");
		assertEquals(15_000, photos.durationMs());
		assertEquals(15_000L, photos.getRepeat());
		assertEquals(0, photos.fadeMs());

		photos.setProperty("duration:nope");
		assertEquals(15_000, photos.durationMs());
		photos.setProperty("duration:");
		assertEquals(15_000, photos.durationMs());
		photos.setProperty("duration:-5");
		assertEquals(100, photos.durationMs());
		assertEquals(0, photos.fadeMs());
		photos.setProperty("duration:5");
		assertEquals(100, photos.durationMs());
		assertEquals(0, photos.fadeMs());
		photos.setProperty("duration:999999");
		assertEquals(999_999, photos.durationMs());
		assertEquals(999_999L, photos.getRepeat());
		assertEquals(0, photos.fadeMs());

		photos.setProperty("fade:1000");
		photos.setProperty("duration:10");
		assertEquals(100, photos.durationMs());
		assertEquals(100, photos.fadeMs());
	}

	@Test
	public void fadeIsClippedToTheDuration() {
		PhotoPresentation photos = new PhotoPresentation();
		photos.setProperty("duration:8000");
		photos.setProperty("fade:4000");
		assertEquals(8000, photos.durationMs());
		assertEquals(4000, photos.fadeMs());

		photos.setProperty("fade:9000");
		assertEquals(8000, photos.fadeMs());

		long span = PhotoPresentation.PhotoPan.motionSpanMs(photos.durationMs(), photos.fadeMs());
		assertEquals(8000L + 8000L, span);
		assertEquals(8000f / span, PhotoPresentation.PhotoPan.motionDt(8000L, 8000, 8000), 0.0001f);
		assertEquals(1f, PhotoPresentation.PhotoPan.motionDt(span, 8000, 8000), 0f);

		photos.setProperty("duration:20000");
		assertEquals(9000, photos.fadeMs());
		photos.setProperty("h");
		assertEquals(20_000, photos.durationMs());
		assertEquals(9000, photos.fadeMs());
	}

	@Test
	public void zeroFadeIsAHardCut() {
		PhotoPresentation photos = new PhotoPresentation();
		photos.setProperty("fade:0");
		assertEquals(0, photos.fadeMs());
		assertEquals(12_000L, PhotoPresentation.PhotoPan.motionSpanMs(12_000, 0));
		assertEquals(0f, PhotoPresentation.PhotoPan.motionDt(0, 12_000, 0), 0f);
		assertEquals(1f, PhotoPresentation.PhotoPan.motionDt(12_000L, 12_000, 0), 0.0001f);
		assertEquals(0f, PhotoPresentation.PhotoPan.fadeAlpha(0, 0), 0f);
		assertEquals(0f, PhotoPresentation.PhotoPan.fadeAlpha(1, 0), 0f);

		photos.setProperty("duration:8000");
		photos.setProperty("fade:0");
		assertEquals(0, photos.fadeMs());
		assertEquals(8000L, photos.getRepeat());
		assertEquals(8000L, PhotoPresentation.PhotoPan.motionSpanMs(photos.durationMs(), photos.fadeMs()));
	}

	@Test
	public void outgoingDrawFadesTowardTheImageUnderneath() {
		BufferedImage photo = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
		Graphics2D pg = photo.createGraphics();
		pg.setColor(Color.WHITE);
		pg.fillRect(0, 0, 4, 4);
		pg.dispose();

		Point2D.Float origin = new Point2D.Float();
		PhotoPresentation.PhotoPan pan = new PhotoPresentation.PhotoPan(photo, origin, origin, 1f, 1f);

		BufferedImage dest = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = dest.createGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, 2, 2);
		pan.draw(g, 0f, null, 0f);
		assertEquals(Color.BLACK.getRGB(), dest.getRGB(0, 0));

		pan.draw(g, 0f, null, 0.5f);
		int faded = dest.getRGB(0, 0) & 0xFF;
		assertTrue(faded > 64 && faded < 192);
		g.dispose();
	}

	@Test
	public void startupSkipsUnpaintedPreloadedPhotos() throws Exception {
		PhotoPresentation photos = photos(1000);
		TestImage red = new TestImage(Color.RED);
		queue(photos, red, new TestImage(Color.BLUE));
		photos.aboutToShow();
		photos.aboutToShow();
		assertTrue(red.released);
		assertNull(field(photos, "outgoing"));
		assertEquals(Color.BLUE.getRGB(), render(photos, 0, 1f).getRGB(4, 4));
	}

	@Test
	public void imageSwitchContinuesMotionBlendsAndReleasesOutgoing() throws Exception {
		PhotoPresentation photos = photos(1000);
		TestImage red = new TestImage(Color.RED);
		// A pattern makes a jump in the pan visible at the switch.
		red.setRGB(4, 4, Color.WHITE.getRGB());
		queue(photos, red, new TestImage(Color.BLUE));
		photos.aboutToShow();
		PhotoPresentation.PhotoPan first = (PhotoPresentation.PhotoPan) field(photos, "current");
		render(photos, 0, 1f);
		BufferedImage expected = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = expected.createGraphics();
		first.draw(g, PhotoPresentation.PhotoPan.motionDt(12_000, 12_000, 1000), null, 1f);
		g.dispose();
		BufferedImage atFadeStart = render(photos, 11_500, 1f);
		assertSame(first, field(photos, "outgoing"));
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				assertEquals(expected.getRGB(x, y), atFadeStart.getRGB(x, y));
			}
		}
		Object second = field(photos, "current");
		render(photos, 11_999, 1f);
		photos.setRepeatTimeMs(0);
		photos.aboutToShow();
		assertSame(second, field(photos, "current"));
		assertSame(first, field(photos, "outgoing"));
		Color middle = new Color(render(photos, 0, 1f).getRGB(0, 0));
		assertEquals(128, middle.getRed(), 1);
		assertEquals(127, middle.getBlue(), 1);
		assertFalse(red.released);
		assertEquals(Color.BLUE.getRGB(), render(photos, 500, 1f).getRGB(4, 4));
		assertNull(field(photos, "outgoing"));
		assertTrue(red.released);
	}

	@Test
	public void crossfadeAppliesParentCompositeToTheWholeBlend() throws Exception {
		PhotoPresentation photos = photos(1000);
		queue(photos, new TestImage(Color.RED), new TestImage(Color.BLUE));
		photos.aboutToShow();
		render(photos, 0, 1f);
		for (long time : new long[] { 11_500, 11_750, 11_999 }) {
			assertEquals(Color.BLACK.getRGB(), render(photos, time, 0f).getRGB(4, 4));
			Color faded = new Color(render(photos, time, 0.5f).getRGB(4, 4));
			assertEquals(128, faded.getRed() + faded.getBlue(), 1);
		}
		photos.setRepeatTimeMs(0);
		photos.aboutToShow();
		for (long time : new long[] { 0, 250, 500 }) {
			assertEquals(Color.BLACK.getRGB(), render(photos, time, 0f).getRGB(4, 4));
			Color faded = new Color(render(photos, time, 0.5f).getRGB(4, 4));
			assertEquals(128, faded.getRed() + faded.getBlue(), 1);
		}
	}

	@Test
	public void fullDurationFadeCentersTransitionsOnRepeatBoundaries() throws Exception {
		PhotoPresentation photos = photos(12_000);
		TestImage red = new TestImage(Color.RED);
		TestImage blue = new TestImage(Color.BLUE);
		queue(photos, red, blue, new TestImage(Color.GREEN));
		photos.aboutToShow();
		assertEquals(Color.RED.getRGB(), render(photos, 0, 1f).getRGB(4, 4));
		assertEquals(Color.RED.getRGB(), render(photos, 6000, 1f).getRGB(4, 4));
		render(photos, 11_999, 1f);
		photos.setRepeatTimeMs(0);
		photos.aboutToShow();
		Color middle = new Color(render(photos, 0, 1f).getRGB(4, 4));
		assertEquals(128, middle.getRed(), 1);
		assertEquals(127, middle.getBlue(), 1);
		assertEquals(Color.BLUE.getRGB(), render(photos, 6000, 1f).getRGB(4, 4));
		assertTrue(red.released);
		PhotoPresentation.PhotoPan outgoing = (PhotoPresentation.PhotoPan) field(photos, "outgoing");
		assertSame(blue, outgoing.image);
		render(photos, 11_999, 1f);
		photos.setRepeatTimeMs(0);
		photos.aboutToShow();
		middle = new Color(render(photos, 0, 1f).getRGB(4, 4));
		assertEquals(128, middle.getBlue(), 1);
		assertEquals(127, middle.getGreen(), 1);
		assertEquals(Color.GREEN.getRGB(), render(photos, 6000, 1f).getRGB(4, 4));
		assertTrue(blue.released);
	}

	@Test
	public void repeatCanStartMidFadeWhenNoFrameWasPaintedBeforeTheBoundary() throws Exception {
		PhotoPresentation photos = photos(1000);
		queue(photos, new TestImage(Color.RED), new TestImage(Color.BLUE));
		photos.aboutToShow();
		render(photos, 0, 1f);
		photos.aboutToShow();
		Color middle = new Color(render(photos, 0, 1f).getRGB(4, 4));
		assertEquals(128, middle.getRed(), 1);
		assertEquals(127, middle.getBlue(), 1);
	}

	@Test
	public void oddFadeDurationIsAlsoHalfwayAtTheBoundary() throws Exception {
		PhotoPresentation photos = photos(3);
		queue(photos, new TestImage(Color.RED), new TestImage(Color.BLUE), new TestImage(Color.GREEN));
		photos.aboutToShow();
		render(photos, 0, 1f);
		render(photos, 11_998, 1f);
		assertNull(field(photos, "outgoing"));
		render(photos, 11_999, 1f);
		Object second = field(photos, "current");
		photos.setRepeatTimeMs(0);
		photos.aboutToShow();
		assertSame(second, field(photos, "current"));
		Color middle = new Color(render(photos, 0, 1f).getRGB(4, 4));
		assertEquals(128, middle.getRed(), 1);
		assertEquals(127, middle.getBlue(), 1);
		assertEquals(Color.BLUE.getRGB(), render(photos, 2, 1f).getRGB(4, 4));
	}

	@Test
	public void zeroFadeSwitchesImmediately() throws Exception {
		PhotoPresentation photos = photos(0);
		TestImage red = new TestImage(Color.RED);
		queue(photos, red, new TestImage(Color.BLUE));
		photos.aboutToShow();
		render(photos, 11_999, 1f);
		photos.aboutToShow();
		assertTrue(red.released);
		assertNull(field(photos, "outgoing"));
		assertEquals(Color.BLUE.getRGB(), render(photos, 0, 1f).getRGB(4, 4));
	}

	private static PhotoPresentation photos(int fade) {
		PhotoPresentation photos = new PhotoPresentation();
		photos.setSize(new Dimension(8, 8));
		photos.setProperty("fade:" + fade);
		return photos;
	}

	private static Object field(PhotoPresentation photos, String name) throws Exception {
		Field field = PhotoPresentation.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(photos);
	}

	@SuppressWarnings("unchecked")
	private static void queue(PhotoPresentation photos, TestImage... images) throws Exception {
		// Seed the loader's queue without starting background work or requiring a CDS.
		List<BufferedImage> queue = (List<BufferedImage>) field(photos, "images");
		for (TestImage image : images) {
			queue.add(image);
		}
	}

	private static BufferedImage render(PhotoPresentation photos, long time, float alpha) {
		photos.setRepeatTimeMs(time);
		BufferedImage frame = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = frame.createGraphics();
		try {
			AlphaComposite composite = AlphaComposite.SrcOver.derive(alpha);
			g.setComposite(composite);
			photos.paint(g);
			assertEquals(composite, g.getComposite());
		} finally {
			g.dispose();
		}
		return frame;
	}

	private static class TestImage extends BufferedImage {
		boolean released;

		TestImage(Color color) {
			super(8, 8, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = createGraphics();
			g.setColor(color);
			g.fillRect(0, 0, 8, 8);
			g.dispose();
		}

		@Override
		public void flush() {
			released = true;
			super.flush();
		}
	}

}
