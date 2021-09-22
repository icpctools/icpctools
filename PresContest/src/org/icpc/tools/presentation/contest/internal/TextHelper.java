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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;

public class TextHelper {

	static class Text {
		GlyphVector gv;
		BufferedImage img;
		float dx;
	}

	private final Graphics2D g;
	private final List<Text> list = new ArrayList<>();
	private Dimension bounds;
	private double xx;
	private double h;

	public TextHelper(Graphics2D g, String s, int width) {
		this.g = g;
		Font font = g.getFont();
		layout(s);
		float nn = 1;
		while (bounds.getWidth() > width) {
			list.clear();
			bounds = null;
			xx = 0;
			h = 0;

			nn -= 0.025f;
			g.setFont(font.deriveFont(AffineTransform.getScaleInstance(nn, 1.0)));
			layout(s);
		}
		g.setFont(font);
	}

	public TextHelper(Graphics2D g, String s) {
		this.g = g;
		layout(s);
	}

	private void layout(String s) {
		FontRenderContext frc = g.getFontRenderContext();
		Font font = g.getFont();
		FontMetrics fm = g.getFontMetrics();

		char[] c = s.toCharArray();

		int fontBegin = -1;
		for (int i = 0; i < s.length(); ) {
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
				addEmoji(fm.getHeight(), emojiEntry.hex);
				fontBegin = -1;
				i += emojiEntry.raw.length();
			}
		}

		if (fontBegin != -1) {
			addText(font.layoutGlyphVector(frc, c, fontBegin, c.length, 0));
		}

		bounds = new Dimension((int) xx, (int) h);
	}

	private void addText(GlyphVector gv) {
		Text t = new Text();
		t.gv = gv;
		t.dx = (int) xx;
		Rectangle2D b = t.gv.getLogicalBounds();
		xx += b.getWidth();
		h = Math.max(h, b.getHeight());
		list.add(t);
	}

	private void addEmoji(double height, String hex) {
		BufferedImage img = getEmoji(hex);
		Text t = new Text();
		// TODO should resize emojis when we scale text
		t.img = ImageScaler.scaleImage(img, height - 2, height - 2);
		t.dx = (int) xx;
		xx += t.img.getWidth();
		h = Math.max(h, t.img.getHeight());
		list.add(t);
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
		FontMetrics fm = g.getFontMetrics();
		for (Text t : list) {
			if (t.gv != null)
				g.drawGlyphVector(t.gv, x + t.dx, y);
			else
				g.drawImage(t.img, x + (int) t.dx, y + fm.getDescent() - t.img.getHeight() - 1, null);
		}
	}

	public void draw(float x, float y) {
		FontMetrics fm = g.getFontMetrics();
		for (Text t : list) {
			if (t.gv != null)
				g.drawGlyphVector(t.gv, x + t.dx, y);
			else
				g.drawImage(t.img, (int) (x + t.dx), (int) y + fm.getDescent() - t.img.getHeight() - 1, null);
		}
	}

	private static final class EmojiEntry {
		final String hex;
		final String raw;

		public EmojiEntry(String hex, String raw) {
			this.hex = hex;
			this.raw = raw;
		}

		@Override public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			EmojiEntry that = (EmojiEntry) o;
			return Objects.equals(hex, that.hex) && Objects.equals(raw, that.raw);
		}

		@Override public int hashCode() {
			return Objects.hash(hex, raw);
		}

		@Override public String toString() {
			return '[' + hex + '=' + raw + ']';
		}
	}

	private static final Set<String> emojis = new HashSet<>();
	private static final Map<Integer, List<EmojiEntry>> emojiMap = new HashMap<>();
	private static final ConcurrentHashMap<String, BufferedImage> map = new ConcurrentHashMap<>();

	private static void loadEmojis() {
		String filename = "font/emoji.txt";
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(ICPCFont.class.getClassLoader().getResourceAsStream(filename)))) {
			String s;
			while ((s = br.readLine()) != null) {
				emojis.add(s);
				String[] hexCodes = s.split("-");
				int[] codePoints = Arrays.stream(hexCodes).mapToInt(hex -> Integer.parseInt(hex, 16)).toArray();
				String raw = new String(codePoints, 0, hexCodes.length);
				emojiMap.putIfAbsent(codePoints[0], new ArrayList<>());
				emojiMap.get(codePoints[0]).add(new EmojiEntry(s, raw));
			}
			for (List<EmojiEntry> entryList : emojiMap.values()) {
				// Sort entries from long to short, so that combined emoji (with e.g. Zero-Width Joiner) are found first
				entryList.sort(Comparator.<EmojiEntry>comparingInt(l -> l.raw.length()).reversed());
			}
		} catch (Exception e) {
			// ignore
		}
	}

	private EmojiEntry findEmoji(String s, int i) {
		if (emojis.isEmpty())
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

	private static boolean hasEmoji(String hex) {
		if (emojis.isEmpty())
			loadEmojis();

		return emojis.contains(hex);
	}

	private static BufferedImage getEmoji(String hex) {
		return map.computeIfAbsent(hex, key -> hasEmoji(key) ? getEmojiFromFile(key) : null);
	}

	private static BufferedImage getEmojiFromFile(String hex) {
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
		new TextHelper(g, s, width).draw(x, y);
	}

	/**
	 * An equivalent to g.drawString(), except that it takes emojis into account.
	 */
	public static void drawString(Graphics2D g, String s, int x, int y) {
		new TextHelper(g, s).draw(x, y);
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