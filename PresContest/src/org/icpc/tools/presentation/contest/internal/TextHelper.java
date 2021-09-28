package org.icpc.tools.presentation.contest.internal;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;

/**
 * A helper class that can layout and draw a set of text and images. It handles some special cases
 * like automatically converting emojis in strings to colour images. All items are automatically
 * vertically aligned in the middle.
 */
public class TextHelper {
	abstract class Item {
		protected Dimension d;
		protected int x;
		protected int y;

		protected abstract void draw();
	}

	class ImageItem extends Item {
		protected BufferedImage img;

		@Override
		protected void draw() {
			g.drawImage(img, x - img.getWidth() / 2, y - img.getHeight() / 2, null);
		}
	}

	// same as ImageItem, but aligns with text
	class EmojiItem extends ImageItem {
		@Override
		protected void draw() {
			g.drawImage(img, x - img.getWidth() / 2, y - img.getHeight() / 2, null);
		}
	}

	class StringItem extends Item {
		protected String s;

		@Override
		protected void draw() {
			g.drawString(s, x - d.width / 2, y - fm.getHeight() / 2 + fm.getAscent());
		}
	}

	class GlyphItem extends Item {
		protected GlyphVector gv;

		@Override
		protected void draw() {
			g.drawGlyphVector(gv, x - d.width / 2, y - fm.getHeight() / 2 + fm.getAscent());
		}
	}

	class SpacerItem extends Item {
		@Override
		protected void draw() {
			// ignore
		}
	}

	private Graphics2D g;
	private FontMetrics fm;
	private Dimension bounds = new Dimension(0, 0);
	private List<Item> list = new ArrayList<>(4);

	public TextHelper(Graphics2D g, String s) {
		this.g = g;
		this.fm = g.getFontMetrics();
		addString(s);
	}

	public TextHelper(Graphics2D g) {
		this.g = g;
		this.fm = g.getFontMetrics();
	}

	public void addPlainText(String s) {
		StringItem item = new StringItem();
		item.s = s;
		item.d = new Dimension(fm.stringWidth(s), fm.getHeight());
		add(item);
	}

	public void addString(String s) {
		FontRenderContext frc = g.getFontRenderContext();
		Font font = g.getFont();

		char[] c = s.toCharArray();

		int fontBegin = -1;
		for (int i = 0; i < s.length();) {
			int codePoint = s.codePointAt(i);
			int charCount = Character.charCount(codePoint); // high Unicode code points span two chars

			// detect if we have an emoji PNG for the current position
			EmojiEntry emojiEntry = findEmoji(s, i);
			if (emojiEntry == null) {
				if (fontBegin == -1)
					fontBegin = i;
				i += charCount;
			} else {
				if (fontBegin != -1)
					addText(font.layoutGlyphVector(frc, c, fontBegin, i, 0));
				addEmoji(emojiEntry);
				fontBegin = -1;
				i += emojiEntry.raw.length();
			}
		}

		if (fontBegin != -1)
			addText(font.layoutGlyphVector(frc, c, fontBegin, c.length, 0));
	}

	private void add(Item i) {
		list.add(i);

		layout();
	}

	private void layout() {
		// find max height
		int w = 0;
		int h = 0;
		for (Item i : list) {
			h = Math.max(h, i.d.height);
			w += i.d.width;
		}
		bounds.width = w;
		bounds.height = h;

		// layout items horizontally
		w = 0;
		for (Item i : list) {
			i.x = w + i.d.width / 2;
			i.y = h / 2;
			w += i.d.width;
		}
	}

	private void addText(GlyphVector gv) {
		GlyphItem item = new GlyphItem();
		item.gv = gv;
		Rectangle2D b = gv.getLogicalBounds();
		item.d = new Dimension((int) b.getWidth(), (int) b.getHeight());
		add(item);
	}

	private static EmojiEntry findEmoji(String s, int i) {
		if (emojiMap.isEmpty())
			loadEmojis();

		int first = s.codePointAt(i);
		List<EmojiEntry> entries = emojiMap.get(first);
		if (entries == null)
			return null;

		for (EmojiEntry entry : entries) {
			if (s.regionMatches(i, entry.raw, 0, entry.raw.length()))
				return entry;
		}
		return null;
	}

	private void addEmoji(EmojiEntry emoji) {
		if (emoji.img == null)
			emoji.img = loadEmojiFromFile(emoji.hex);
		int height = fm.getHeight() - 2;
		BufferedImage img = ImageScaler.scaleImage(emoji.img, height, height);

		EmojiItem item = new EmojiItem();
		item.img = img;
		item.d = new Dimension(img.getWidth(), img.getHeight());
		add(item);
	}

	public void addImage(BufferedImage img) {
		ImageItem item = new ImageItem();
		item.img = img;
		item.d = new Dimension(img.getWidth(), img.getHeight());
		add(item);
	}

	public void addImage(BufferedImage img, int width, int height) {
		ImageItem item = new ImageItem();
		item.img = img;
		item.d = new Dimension(width, height);
		add(item);
	}

	public void addSpacer(int width) {
		SpacerItem item = new SpacerItem();
		item.d = new Dimension(width, 0);
		add(item);
	}

	public void addSpacer(int width, int height) {
		SpacerItem item = new SpacerItem();
		item.d = new Dimension(width, height);
		add(item);
	}

	public int getWidth() {
		return bounds.width;
	}

	public int getHeight() {
		return bounds.height;
	}

	public Dimension getBounds() {
		return bounds;
	}

	public void draw(int x, int y) {
		g.translate(x, y);
		for (Item i : list)
			i.draw();

		g.translate(-x, -y);
	}

	public void draw(float x, float y) {
		draw((int) x, (int) y);
	}

	public void drawFit(int x, int y, int width) {
		if (bounds.width <= width) {
			draw(x, y);
			return;
		}
		AffineTransform old = g.getTransform();
		g.translate(x, 0);
		double sc = width / (double) bounds.width;
		g.transform(AffineTransform.getScaleInstance(sc, 1.0));
		draw(0, y);

		g.setTransform(old);
	}

	private static final Map<Integer, List<EmojiEntry>> emojiMap = new HashMap<>();

	private static final class EmojiEntry {
		final String hex;
		final String raw;
		BufferedImage img;

		public EmojiEntry(String hex, String raw) {
			this.hex = hex;
			this.raw = raw;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			EmojiEntry that = (EmojiEntry) o;
			return Objects.equals(hex, that.hex) && Objects.equals(raw, that.raw);
		}

		@Override
		public int hashCode() {
			return Objects.hash(hex, raw);
		}

		@Override
		public String toString() {
			return '[' + hex + '=' + raw + ']';
		}
	}

	private static void loadEmojis() {
		String filename = "font/emoji.txt";
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(ICPCFont.class.getClassLoader().getResourceAsStream(filename)))) {
			String s;
			while ((s = br.readLine()) != null) {
				String[] hexCodes = s.split("-");
				int[] codePoints = Arrays.stream(hexCodes).mapToInt(hex -> Integer.parseInt(hex, 16)).toArray();
				String raw = new String(codePoints, 0, hexCodes.length);
				emojiMap.putIfAbsent(codePoints[0], new ArrayList<>());
				emojiMap.get(codePoints[0]).add(new EmojiEntry(s, raw));
			}
			for (List<EmojiEntry> entryList : emojiMap.values()) {
				// sort entries from long to short, so that combined emoji (with e.g. Zero-Width
				// Joiner) are found first
				entryList.sort(Comparator.<EmojiEntry> comparingInt(l -> l.raw.length()).reversed());
			}
		} catch (Exception e) {
			// ignore
		}
	}

	private static BufferedImage loadEmojiFromFile(String hex) {
		String filename = "font/twemoji/" + hex + ".png";
		try (InputStream in = ICPCFont.class.getClassLoader().getResourceAsStream(filename)) {
			return ImageIO.read(in);
		} catch (Exception e) {
			// in case we are not in a jar, re-try as a file
			try {
				return ImageIO.read(new File(filename));
			} catch (Exception e1) {
				Trace.trace(Trace.ERROR, "Error loading font", e1);
				return null;
			}
		}
	}

	/**
	 * An equivalent to g.drawString(), except that it takes emojis and maximum width into account.
	 */
	public static void drawString(Graphics2D g, String s, int x, int y, int width) {
		TextHelper text = new TextHelper(g, s);
		text.drawFit(x, y - text.fm.getAscent(), width);
	}

	/**
	 * An equivalent to g.drawString(), except that it takes emojis into account.
	 */
	public static void drawString(Graphics2D g, String s, int x, int y) {
		TextHelper text = new TextHelper(g, s);
		new TextHelper(g, s).draw(x, y - text.fm.getAscent());
	}

	public static void main(String[] args) {
		File folder = new File("src/font/twemoji");
		if (!folder.exists())
			return;

		File[] files = folder.listFiles();
		if (files == null)
			return;

		for (File f : files) {
			String name = f.getName();
			if (name.endsWith(".png"))
				System.out.println(name.substring(0, name.length() - 4));
		}
	}
}