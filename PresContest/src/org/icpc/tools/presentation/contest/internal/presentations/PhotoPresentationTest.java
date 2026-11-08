package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.icpc.tools.presentation.contest.internal.presentations.PhotoPresentation.CatalogProperty;
import org.icpc.tools.presentation.contest.internal.presentations.PhotoPresentation.PhotoLoader;
import org.icpc.tools.presentation.core.internal.PresentationInfo;
import org.icpc.tools.presentation.core.internal.PresentationsParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PhotoPresentationTest {
	@Test
	public void defaultDirectoryIsUnchangedWithoutCatalog() {
		PhotoPresentation presentation = new PhotoPresentation();
		assertEquals("/presentation/photos", presentation.getPhotoDirectory());
		assertEquals("/presentation/photos", PhotoPresentation.directoryForCatalog(null));
		assertEquals("/presentation/photos", PhotoPresentation.directoryForCatalog(""));
	}

	@Test
	public void namedCatalogSelectsPhotosNameDirectory() {
		PhotoPresentation presentation = new PhotoPresentation();
		presentation.setProperty("catalog:awards");
		assertEquals("/presentation/photos-awards", presentation.getPhotoDirectory());
		presentation.setProperty("catalog: finals");
		assertEquals("/presentation/photos-finals", presentation.getPhotoDirectory());
		assertEquals("/presentation/photos-finals_2026", PhotoPresentation.directoryForCatalog("finals_2026"));
		assertEquals("/presentation/photos-cup-1", PhotoPresentation.directoryForCatalog("cup-1"));

		String max = "a2345678901234567890123456789012";
		assertEquals(32, max.length());
		assertEquals(PhotoPresentation.CATALOG_SET, PhotoPresentation.parseCatalogProperty("catalog:" + max).action);
		assertEquals("/presentation/photos-" + max, PhotoPresentation.directoryForCatalog(max));
	}

	@Test
	public void emptyCatalogResetsToDefault() {
		PhotoPresentation presentation = new PhotoPresentation();
		presentation.setProperty("catalog:awards");
		presentation.setProperty("catalog:");
		assertEquals("/presentation/photos", presentation.getPhotoDirectory());

		presentation.setProperty("catalog:awards");
		presentation.setProperty("catalog:   ");
		assertEquals("/presentation/photos", presentation.getPhotoDirectory());

		presentation.setProperty("catalog:awards");
		presentation.setProperty("catalog");
		assertEquals("/presentation/photos", presentation.getPhotoDirectory());

		assertEquals(PhotoPresentation.CATALOG_RESET, PhotoPresentation.parseCatalogProperty("catalog:").action);
		assertEquals(PhotoPresentation.CATALOG_RESET, PhotoPresentation.parseCatalogProperty("catalog").action);
	}

	@Test
	public void invalidCatalogNamesAreRejected() {
		String[] invalid = new String[] { "../secret", "..", ".", "/abs", "foo/bar", "foo\\bar", "foo..bar", "-leading",
				"_leading", "foo bar", "foo:bar", "foo?x", "foo#y", "%2e%2e", "photos/../../etc", "a23456789012345678901234567890123",
				".hidden" };

		PhotoPresentation presentation = new PhotoPresentation();
		presentation.setProperty("catalog:awards");
		for (String name : invalid) {
			CatalogProperty parsed = PhotoPresentation.parseCatalogProperty("catalog:" + name);
			assertEquals(name, PhotoPresentation.CATALOG_INVALID, parsed.action);
			assertFalse(name, PhotoPresentation.isValidCatalogName(name));
			presentation.setProperty("catalog:" + name);
			assertEquals(name, "/presentation/photos-awards", presentation.getPhotoDirectory());
		}

		assertEquals(PhotoPresentation.NOT_CATALOG, PhotoPresentation.parseCatalogProperty(null).action);
		assertEquals(PhotoPresentation.NOT_CATALOG, PhotoPresentation.parseCatalogProperty("h").action);
		assertEquals(PhotoPresentation.NOT_CATALOG, PhotoPresentation.parseCatalogProperty("lightMode:light").action);
		assertTrue(PhotoPresentation.isValidCatalogName("A"));
		assertTrue(PhotoPresentation.isValidCatalogName("finals_2026"));
	}

	@Test
	public void catalogPropertyIsInAdminMetadata() throws Exception {
		InputStream in = PhotoPresentation.class.getClassLoader().getResourceAsStream("META-INF/presentations.xml");
		assertNotNull(in);
		PresentationsParser parser = new PresentationsParser();
		parser.load(in);
		in.close();

		PresentationInfo photos = null;
		for (PresentationInfo info : parser.getPresentations()) {
			if ("org.icpc.tools.presentation.contest.photos".equals(info.getId())) {
				photos = info;
			}
		}
		assertNotNull(photos);
		List<String> properties = Arrays.asList(photos.getProperties());
		assertTrue(properties.contains("h - bicubic interpolation"));
		assertTrue(properties.contains("l - nearest-neighbor interpolation"));
		assertTrue(properties.contains("catalog:name - use CDP/present/photos-name (letters digits _ -)"));
		assertTrue(properties.contains("catalog: - reset to CDP/present/photos"));
		assertTrue(photos.getDescription().contains("photos-name"));
	}

	@Test
	public void unsafeFileNamesAreRejected() {
		assertTrue(PhotoPresentation.isSafeFileName("team.jpg"));
		assertTrue(PhotoPresentation.isSafeFileName("my photo.png"));
		assertFalse(PhotoPresentation.isSafeFileName(null));
		assertFalse(PhotoPresentation.isSafeFileName(""));
		assertFalse(PhotoPresentation.isSafeFileName("."));
		assertFalse(PhotoPresentation.isSafeFileName(".."));
		assertFalse(PhotoPresentation.isSafeFileName("../secret.jpg"));
		assertFalse(PhotoPresentation.isSafeFileName("foo/bar.jpg"));
		assertFalse(PhotoPresentation.isSafeFileName("foo\\bar.jpg"));
		assertFalse(PhotoPresentation.isSafeFileName("foo..bar.jpg"));
		assertFalse(PhotoPresentation.isSafeFileName("pic.jpg?x=1"));
		assertFalse(PhotoPresentation.isSafeFileName("pic.jpg#frag"));
	}

	@Test
	public void propertiesKeepInterpolationAndLightMode() {
		PhotoPresentation presentation = new PhotoPresentation();
		presentation.setProperty("h");
		assertEquals("h", presentation.getInterpolationMode());

		presentation.setProperty("lightMode:light");
		assertTrue(presentation.isLightMode());
		assertEquals("h", presentation.getInterpolationMode());
		assertEquals("/presentation/photos", presentation.getPhotoDirectory());

		presentation.setProperty("catalog:finals");
		assertEquals("/presentation/photos-finals", presentation.getPhotoDirectory());
		assertEquals("h", presentation.getInterpolationMode());
		assertTrue(presentation.isLightMode());
		assertEquals(0, presentation.getGeneration());

		presentation.setProperty("l");
		assertEquals("l", presentation.getInterpolationMode());
		assertEquals("/presentation/photos-finals", presentation.getPhotoDirectory());

		presentation.setProperty("name:Site 2");
		assertEquals("name:Site 2", presentation.getInterpolationMode());
		assertEquals("/presentation/photos-finals", presentation.getPhotoDirectory());
		assertTrue(presentation.isLightMode());

		presentation.setProperty("catalog:");
		assertEquals("/presentation/photos", presentation.getPhotoDirectory());
		assertEquals("name:Site 2", presentation.getInterpolationMode());

		presentation.setProperty("catalog:finals");
		int generation = presentation.getGeneration();
		presentation.setProperty("catalog:finals");
		assertEquals(generation, presentation.getGeneration());

		presentation.setProperty(null);
		assertNull(presentation.getInterpolationMode());
		assertEquals("/presentation/photos-finals", presentation.getPhotoDirectory());
		assertTrue(presentation.isLightMode());
	}

	@Test
	public void catalogChosenBeforeInitIsLoaded() throws Exception {
		ScriptedLoader loader = new ScriptedLoader();
		BufferedImage image = loader.add("/presentation/photos-finals", "podium.png", 0x112233);
		PhotoPresentation presentation = new PhotoPresentation();
		presentation.setPhotoLoader(loader);
		presentation.setProperty("h");
		presentation.setProperty("catalog:finals");
		presentation.setProperty("lightMode:dark");
		presentation.init();

		waitUntil("finals image", () -> presentation.isShowing(image) && !presentation.isLoading());
		assertEquals("/presentation/photos-finals", presentation.getPhotoDirectory());
		assertEquals(Arrays.asList("/presentation/photos-finals"), loader.listed);
		assertTrue(loader.opened.get(0).path.endsWith("/podium.png"));
		assertEquals("h", presentation.getInterpolationMode());
		assertFalse(presentation.isLightMode());
	}

	@Test
	public void switchDropsPreviousImagesAndRepeatedValueDoesNotReload() throws Exception {
		ScriptedLoader loader = new ScriptedLoader();
		BufferedImage first = loader.add("/presentation/photos", "a.png", 0xFF0000);
		BufferedImage second = loader.add("/presentation/photos-cup", "b.png", 0x0000FF);
		PhotoPresentation presentation = new PhotoPresentation();
		presentation.setPhotoLoader(loader);
		presentation.setProperty("h");
		presentation.init();

		waitUntil("default image", () -> presentation.isShowing(first) && !presentation.isLoading());
		assertEquals("/presentation/photos", presentation.getPhotoDirectory());
		int generation = presentation.getGeneration();
		int opens = loader.opened.size();

		presentation.setProperty("catalog:cup");
		waitUntil("named image", () -> presentation.isShowing(second) && !presentation.isLoading());
		assertFalse(presentation.isShowing(first));
		assertEquals("/presentation/photos-cup", presentation.getPhotoDirectory());
		assertTrue(presentation.getGeneration() > generation);
		assertEquals("h", presentation.getInterpolationMode());

		int afterSwitch = loader.opened.size();
		int generationAfter = presentation.getGeneration();
		BufferedImage showing = second;
		presentation.setProperty("catalog:cup");
		Thread.sleep(50);
		assertEquals(generationAfter, presentation.getGeneration());
		assertEquals(afterSwitch, loader.opened.size());
		assertTrue(presentation.isShowing(showing));
		assertFalse(presentation.isShowing(first));
		assertTrue(opens > 0);
	}

	@Test
	public void resetAndMissingDirectoryThenRecover() throws Exception {
		ScriptedLoader loader = new ScriptedLoader();
		BufferedImage awards = loader.add("/presentation/photos-awards", "trophy.jpg", 0x00FF00);
		BufferedImage fallback = loader.add("/presentation/photos", "crowd.jpg", 0xFFFF00);
		PhotoPresentation presentation = new PhotoPresentation();
		presentation.setPhotoLoader(loader);
		presentation.setProperty("catalog:awards");
		presentation.init();
		waitUntil("awards", () -> presentation.isShowing(awards) && !presentation.isLoading());

		presentation.setProperty("catalog:missing");
		waitUntil("missing settled", () -> !presentation.isLoading());
		assertEquals("/presentation/photos-missing", presentation.getPhotoDirectory());
		assertEquals(0, presentation.getCachedImageCount());
		assertFalse(presentation.isShowing(awards));
		assertTrue(loader.listed.contains("/presentation/photos-missing"));

		presentation.setProperty("catalog:missing");
		assertEquals("/presentation/photos-missing", presentation.getPhotoDirectory());

		presentation.setProperty("catalog:");
		waitUntil("default restored", () -> presentation.isShowing(fallback) && !presentation.isLoading());
		assertEquals("/presentation/photos", presentation.getPhotoDirectory());
		assertFalse(presentation.isShowing(awards));
	}

	@Test
	public void invalidSwitchKeepsCurrentImages() throws Exception {
		ScriptedLoader loader = new ScriptedLoader();
		BufferedImage image = loader.add("/presentation/photos", "a.png", 0xABCDEF);
		PhotoPresentation presentation = new PhotoPresentation();
		presentation.setPhotoLoader(loader);
		presentation.init();
		waitUntil("loaded", () -> presentation.isShowing(image) && !presentation.isLoading());

		int generation = presentation.getGeneration();
		int opens = loader.opened.size();
		presentation.setProperty("catalog:../secret");
		presentation.setProperty("catalog:foo/bar");
		Thread.sleep(30);
		assertEquals(generation, presentation.getGeneration());
		assertEquals(opens, loader.opened.size());
		assertTrue(presentation.isShowing(image));
		assertEquals("/presentation/photos", presentation.getPhotoDirectory());
	}

	@Test
	public void lateImageFromPreviousCatalogIsDiscarded() throws Exception {
		ScriptedLoader loader = new ScriptedLoader();
		loader.blockOpenPrefix = "/presentation/photos/";
		loader.add("/presentation/photos", "old.png", 0x010101);
		loader.add("/presentation/photos-live", "new.png", 0x020202);
		PhotoPresentation presentation = new PhotoPresentation();
		presentation.setPhotoLoader(loader);
		presentation.init();

		assertTrue(loader.openEntered.await(3, TimeUnit.SECONDS));
		loader.blockListDirectory = "/presentation/photos-live";
		presentation.setProperty("catalog:live");
		assertEquals(0, presentation.getCachedImageCount());
		assertEquals("/presentation/photos-live", presentation.getPhotoDirectory());

		loader.allowOpen.countDown();
		assertTrue(loader.listEntered.await(3, TimeUnit.SECONDS));
		List<BufferedImage> stale = imagesFor(loader, "/presentation/photos/");
		assertFalse(stale.isEmpty());
		for (BufferedImage image : stale) {
			assertFalse(presentation.isShowing(image));
		}

		loader.allowList.countDown();
		waitUntil("new catalog", () -> presentation.getCachedImageCount() > 0 && presentation.getDiscardedImages() > 0
				&& !presentation.isLoading());
		assertTrue(presentation.getDiscardedImages() > 0);
		for (BufferedImage image : stale) {
			assertFalse(presentation.isShowing(image));
		}
		assertTrue(anyShowing(presentation, imagesFor(loader, "/presentation/photos-live/")));
		assertNotNull(presentation.getPhotoDirectory());
	}

	@Test
	public void unsafeListingEntriesAreNotOpened() throws Exception {
		ScriptedLoader loader = new ScriptedLoader();
		loader.names.put("/presentation/photos", new String[] { "../secret.png", "ok.png", "nested/bad.png" });
		BufferedImage ok = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		ok.setRGB(0, 0, 0x445566);
		loader.images.put("/presentation/photos/ok.png", ok);
		PhotoPresentation presentation = new PhotoPresentation();
		presentation.setPhotoLoader(loader);
		presentation.init();

		waitUntil("safe image", () -> presentation.isShowing(ok) && !presentation.isLoading());
		for (ScriptedLoader.Opened opened : loader.opened) {
			assertTrue(opened.path, opened.path.endsWith("/ok.png"));
		}
	}

	@Test
	public void catalogListingIsReservedBeforeARepeatCanStartAnotherLoad() throws Exception {
		ScriptedLoader loader = new ScriptedLoader();
		BufferedImage first = loader.add("/presentation/photos", "a.png", 0xFF0000);
		BufferedImage second = loader.add("/presentation/photos-cup", "b.png", 0x0000FF);
		Graphics2D imageGraphics = second.createGraphics();
		imageGraphics.setColor(Color.BLUE);
		imageGraphics.fillRect(0, 0, second.getWidth(), second.getHeight());
		imageGraphics.dispose();

		List<Runnable> pending = new ArrayList<>();
		Field imagesField = PhotoPresentation.class.getDeclaredField("images");
		imagesField.setAccessible(true);
		PhotoPresentation presentation = new PhotoPresentation() {
			@Override
			protected Job execute(Runnable task) {
				try {
					// This lock must still cover the new generation and its listing reservation.
					assertTrue("Repeat must not reserve a cache-only load before the listing",
							Thread.holdsLock(imagesField.get(this)));
				} catch (IllegalAccessException e) {
					throw new AssertionError(e);
				}
				pending.add(task);
				return () -> false;
			}
		};
		presentation.setSize(new Dimension(32, 32));
		presentation.setPhotoLoader(loader);
		presentation.init();
		presentation.aboutToShow();
		assertEquals(1, pending.size());

		presentation.setProperty("catalog:cup");
		presentation.aboutToShow();
		assertEquals(2, pending.size());
		assertTrue(presentation.isLoading());
		assertEquals(0, presentation.getCachedImageCount());
		assertTrue(loader.listed.isEmpty());

		// Help can be painted while both workers are waiting to run.
		BufferedImage frame = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = frame.createGraphics();
		try {
			presentation.paint(g);
			for (Runnable task : pending) {
				task.run();
			}
			assertFalse(presentation.isShowing(first));
			assertTrue(presentation.isShowing(second));
			assertFalse(presentation.isLoading());
			presentation.paint(g);
			assertEquals(Color.BLUE.getRGB(), frame.getRGB(16, 16));
		} finally {
			g.dispose();
		}
	}

	@Test
	public void catalogSwitchDuringFadeDropsBothOldPhotosAndKeepsTiming() throws Exception {
		ScriptedLoader loader = new ScriptedLoader();
		BufferedImage first = loader.add("/presentation/photos", "a.png", 0xFF0000);
		BufferedImage second = loader.add("/presentation/photos", "b.png", 0x00FF00);
		BufferedImage next = loader.add("/presentation/photos-cup", "cup.png", 0x0000FF);
		Graphics2D pixels = next.createGraphics();
		pixels.setColor(Color.BLUE);
		pixels.fillRect(0, 0, next.getWidth(), next.getHeight());
		pixels.dispose();
		PhotoPresentation presentation = new PhotoPresentation();
		presentation.setSize(new Dimension(32, 32));
		presentation.setPhotoLoader(loader);
		presentation.setProperty("fade:1000");
		presentation.init();
		waitUntil("old photos", () -> presentation.getCachedImageCount() >= 3 && !presentation.isLoading());
		BufferedImage frame = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = frame.createGraphics();
		try {
			presentation.paint(g);
			presentation.setRepeatTimeMs(11_500);
			presentation.paint(g);
			Field outgoing = PhotoPresentation.class.getDeclaredField("outgoing");
			outgoing.setAccessible(true);
			assertNotNull(outgoing.get(presentation));
			presentation.setProperty("catalog:cup");
			waitUntil("new photos", () -> presentation.isShowing(next) && !presentation.isLoading());
			assertNull(outgoing.get(presentation));
			assertFalse(presentation.isShowing(first));
			assertFalse(presentation.isShowing(second));
			assertEquals(1000, presentation.fadeMs());
			assertEquals(12_000, presentation.durationMs());
			presentation.setRepeatTimeMs(0);
			presentation.paint(g);
			assertEquals(Color.BLUE.getRGB(), frame.getRGB(16, 16));
		} finally {
			g.dispose();
			presentation.dispose();
		}
	}

	@Test
	public void disposeRejectsAnImageStillBeingLoaded() throws Exception {
		ScriptedLoader loader = new ScriptedLoader();
		loader.add("/presentation/photos", "a.png", 0xFF0000);
		loader.blockOpenPrefix = "/presentation/photos/";
		PhotoPresentation presentation = new PhotoPresentation();
		presentation.setPhotoLoader(loader);
		presentation.init();
		assertTrue(loader.openEntered.await(2, TimeUnit.SECONDS));
		try {
			presentation.dispose();
		} finally {
			loader.allowOpen.countDown();
		}
		waitUntil("discard disposed load", () -> presentation.getDiscardedImages() > 0);
		assertEquals(0, presentation.getCachedImageCount());
	}

	private static boolean anyShowing(PhotoPresentation presentation, List<BufferedImage> images) {
		for (BufferedImage image : images) {
			if (presentation.isShowing(image)) {
				return true;
			}
		}
		return false;
	}

	private static List<BufferedImage> imagesFor(ScriptedLoader loader, String prefix) {
		List<BufferedImage> images = new ArrayList<>();
		for (ScriptedLoader.Opened opened : loader.opened) {
			if (opened.path.startsWith(prefix)) {
				images.add(opened.image);
			}
		}
		return images;
	}

	private static void waitUntil(String message, Check check) throws Exception {
		long end = System.currentTimeMillis() + 3000;
		while (System.currentTimeMillis() < end) {
			if (check.ok()) {
				return;
			}
			Thread.sleep(10);
		}
		fail(message);
	}

	private interface Check {
		boolean ok() throws Exception;
	}

	private static final class ScriptedLoader implements PhotoLoader {
		final Map<String, String[]> names = new HashMap<>();
		final Map<String, BufferedImage> images = new HashMap<>();
		final List<String> listed = new ArrayList<>();
		final List<Opened> opened = new ArrayList<>();
		volatile String blockOpenPrefix;
		volatile String blockListDirectory;
		final CountDownLatch openEntered = new CountDownLatch(1);
		final CountDownLatch allowOpen = new CountDownLatch(1);
		final CountDownLatch listEntered = new CountDownLatch(1);
		final CountDownLatch allowList = new CountDownLatch(1);

		BufferedImage add(String directory, String name, int rgb) {
			String[] existing = names.get(directory);
			if (existing == null) {
				names.put(directory, new String[] { name });
			} else {
				String[] next = Arrays.copyOf(existing, existing.length + 1);
				next[existing.length] = name;
				names.put(directory, next);
			}
			BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
			image.setRGB(0, 0, rgb);
			images.put(directory + "/" + name, image);
			return image;
		}

		@Override
		public String[] list(String directory) throws Exception {
			synchronized (listed) {
				listed.add(directory);
			}
			if (directory.equals(blockListDirectory)) {
				listEntered.countDown();
				if (!allowList.await(3, TimeUnit.SECONDS)) {
					throw new IllegalStateException("list was not released");
				}
			}
			String[] files = names.get(directory);
			return files == null ? null : files.clone();
		}

		@Override
		public BufferedImage open(String path) throws Exception {
			BufferedImage image = images.get(path);
			if (image == null) {
				throw new IllegalStateException("no image for " + path);
			}
			Opened record = new Opened(path, image);
			synchronized (opened) {
				opened.add(record);
			}
			if (blockOpenPrefix != null && path.startsWith(blockOpenPrefix)) {
				openEntered.countDown();
				if (!allowOpen.await(3, TimeUnit.SECONDS)) {
					throw new IllegalStateException("open was not released");
				}
			}
			return image;
		}

		static final class Opened {
			final String path;
			final BufferedImage image;

			Opened(String path, BufferedImage image) {
				this.path = path;
				this.image = image;
			}
		}
	}
}
