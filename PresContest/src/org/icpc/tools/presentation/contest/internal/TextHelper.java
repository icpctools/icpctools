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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;

public class TextHelper {
	private static final int ZERO_WIDTH_JOINER = 8205; // 200d

	class Text {
		GlyphVector gv;
		BufferedImage img;
		float dx;
	}

	private Graphics2D g;
	private Dimension bounds;
	private List<Text> list = new ArrayList<>();
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

		int textLength = s.length();
		char[] c = s.toCharArray();

		int lastSegment = -1;
		int fontBegin = 0;
		List<Integer> emojiList = new ArrayList<>();
		for (int i = 0; i < s.length();) {
			int codePoint = s.codePointAt(i);
			int charCount = Character.charCount(codePoint); // high Unicode code points span two chars

			// detect if the font can be used to display the current character
			int curSegment = -1;
			if (codePoint != ZERO_WIDTH_JOINER && font.canDisplay(codePoint)) {
				curSegment = 1;
			} else {
				curSegment = 2;
				emojiList.add(codePoint);
			}

			if (curSegment != lastSegment && lastSegment != -1) {
				// segment type has changed, output last segment
				if (lastSegment == 1) {
					addText(font.layoutGlyphVector(frc, c, fontBegin, i, 0));
				} else if (lastSegment == 2) {
					addEmojis(emojiList, fm.getHeight());
				}

				fontBegin = i;
			}

			i += charCount;
			lastSegment = curSegment;
		}

		if (fontBegin < textLength) {
			if (lastSegment == 1) {
				addText(font.layoutGlyphVector(frc, c, fontBegin, c.length, 0));
			} else if (lastSegment == 2) {
				addEmojis(emojiList, fm.getHeight());
			}
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

	private void addEmojis(List<Integer> emojiList, int height) {
		int n = emojiList.size();
		while (n > 0) {
			// find the longest emoji set that we have an image for
			String hex = getEmojiHex(emojiList, n);
			while (n > 0 && !hasEmojis(hex)) {
				n--;
				hex = getEmojiHex(emojiList, n);
			}

			//
			if (n == 0) {
				// we don't have any matching emoji :(
				emojiList.remove(0);
			} else {
				// add the emoji
				addEmoji(height, hex);
				while (n > 0) {
					emojiList.remove(0);
					n--;
				}

			}
			n = emojiList.size();
		}
	}

	private void addEmoji(double height, String hex) {
		BufferedImage img = getEmoji(hex);
		Text t = new Text();
		// TODO should resize emojis when we scale text
		t.img = ImageScaler.scaleImage(img, height, height);
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
		for (Text t : list) {
			if (t.gv != null)
				g.drawGlyphVector(t.gv, x + t.dx, y);
			else
				g.drawImage(t.img, x + (int) t.dx, y - t.img.getHeight(), null);
		}
	}

	public void draw(float x, float y) {
		for (Text t : list) {
			if (t.gv != null)
				g.drawGlyphVector(t.gv, x + t.dx, y);
			else
				g.drawImage(t.img, (int) (x + t.dx), (int) y - t.img.getHeight(), null);
		}
	}

	private static List<String> emojis;
	private static ConcurrentHashMap<String, BufferedImage> map = new ConcurrentHashMap<>();

	private static String loadEmojis() {
		emojis = new ArrayList<String>();
		String filename = "font/emoji.txt";
		Trace.trace(Trace.USER, "loading: " + filename);
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(ICPCFont.class.getClassLoader().getResourceAsStream(filename)))) {
			String s = br.readLine();
			while (s != null) {
				emojis.add(s);
				s = br.readLine();
			}
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	private static String getEmojiHex(List<Integer> codePoints, int max) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		int count = 0;
		for (int cp : codePoints) {
			if (!first)
				sb.append("-");
			first = false;
			sb.append(Integer.toHexString(cp));
			count++;
			if (count >= max)
				break;
		}
		return sb.toString();
	}

	private static boolean hasEmojis(String hex) {
		if (emojis == null)
			loadEmojis();

		return emojis.contains(hex);
	}

	private static BufferedImage getEmoji(String hex) {
		BufferedImage img = map.get(hex);
		if (img != null)
			return img;

		if (emojis == null)
			loadEmojis();

		if (!emojis.contains(hex))
			return null;
		img = getEmojiFromFile(hex);
		if (img != null)
			map.put(hex, img);
		return img;
	}

	private static BufferedImage getEmojiFromFile(String hex) {
		String filename = "font/twemoji/" + hex + ".png";
		Trace.trace(Trace.USER, "loading: " + filename);
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
	 *
	 * @param g
	 * @param s
	 * @param x
	 * @param y
	 * @param width
	 */
	public static void drawString(Graphics2D g, String s, int x, int y, int width) {
		TextHelper text = new TextHelper(g, s, width);
		text.draw(x, y);
	}

	/**
	 * An equivalent to g.drawString(), except that it takes emojis into account.
	 *
	 * @param g
	 * @param s
	 * @param x
	 * @param y
	 * @param width
	 */
	public static void drawString(Graphics2D g, String s, int x, int y) {
		TextHelper text = new TextHelper(g, s);
		text.draw(x, y);
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