package org.icpc.tools.presentation.contest.internal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ICPCColors;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IPerson;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Contest;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGDocument;

/**
 * A helper class that can layout and draw a set of text and images. It handles some special cases
 * like automatically converting emojis in strings to colour images. All items are automatically
 * vertically aligned in the middle.
 */
public class TextHelper {
	private static final boolean DEBUG_BOUNDING_BOXES = false;

	public static abstract class Item {
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

		@Override
		public String toString() {
			return "[image]";
		}
	}

	class NewLineItem extends Item {
		public NewLineItem() {
			d = new Dimension(0, 0);
		}

		@Override
		protected void draw() {
			// ignore
		}

		@Override
		public String toString() {
			return "\\n";
		}
	}

	class StringItem extends Item {
		protected String s;

		public StringItem(String s) {
			this.s = s;
			d = new Dimension(fm.stringWidth(s), fm.getHeight());
		}

		@Override
		protected void draw() {
			g.setColor(Color.WHITE);
			g.drawString(s, x - d.width / 2, y - fm.getHeight() / 2 + fm.getAscent());
		}

		@Override
		public String toString() {
			return s;
		}
	}

	class GlyphItem extends Item {
		protected GlyphVector gv;

		@Override
		protected void draw() {
			g.setColor(Color.WHITE);
			g.drawGlyphVector(gv, x - d.width / 2, y - fm.getHeight() / 2 + fm.getAscent());
		}
	}

	class SpacerItem extends Item {
		@Override
		protected void draw() {
			// ignore
		}
	}

	protected class ProblemItem extends Item {
		protected IProblem problem;
		protected Font font;

		protected ProblemItem(IProblem problem) {
			this.problem = problem;

			String s = problem.getName();
			d = new Dimension((int) (fm.getHeight() * 1.5) + fm.stringWidth(s), fm.getHeight());

			Font f = g.getFont();
			font = f.deriveFont(f.getSize2D() * 0.75f);
		}

		@Override
		protected void draw() {
			Color c = problem.getColorVal();
			Color cc = ICPCColors.getContrastColor(c);
			int w = (int) (fm.getHeight() * 1.35);
			int h = (fm.getAscent());
			ShadedRectangle.drawRoundRect(g, x - d.width / 2, y - d.height / 2 + fm.getAscent() / 2 - h / 2, w, h, c,
					Color.WHITE, "");

			g.setColor(cc);
			Font curFont = g.getFont();
			g.setFont(font);
			FontMetrics fm2 = g.getFontMetrics();
			g.drawString(problem.getLabel(), x - d.width / 2 + w / 2 - fm2.stringWidth(problem.getLabel()) / 2,
					y - fm2.getHeight() / 2 + fm2.getAscent());

			String s = problem.getName();
			g.setColor(Color.LIGHT_GRAY);
			g.setFont(curFont);
			g.drawString(s, x + d.width / 2 - fm.stringWidth(s), y - fm.getHeight() / 2 + fm.getAscent());
		}
	}

	protected class ImageTextItem extends Item {
		protected BufferedImage img;
		protected String text;

		protected ImageTextItem(BufferedImage img, String text) {
			this.img = img;
			int w = 0;
			int fh = fm.getHeight();
			if (img != null)
				w = img.getWidth() + fh / 10;

			this.text = text;
			d = new Dimension(w + fm.stringWidth(text), fh);
		}

		protected ImageTextItem() {
			// subclass must set image and text
		}

		@Override
		protected void draw() {
			if (img != null)
				g.drawImage(img, x - d.width / 2, y - d.height / 2 + fm.getAscent() / 2 - img.getHeight() / 2, null);

			g.setColor(Color.LIGHT_GRAY);
			g.drawString(text, x + d.width / 2 - fm.stringWidth(text), y - fm.getHeight() / 2 + fm.getAscent());
		}

		@Override
		public String toString() {
			return text;
		}
	}

	protected class TeamItem extends ImageTextItem {
		protected TeamItem(IContest contest, ITeam team) {
			IOrganization org = contest.getOrganizationById(team.getOrganizationId());
			int w = 0;
			int fh = fm.getHeight();
			if (org != null) {
				img = org.getLogoImage(fh, fh, true, true);
				if (img != null)
					w = img.getWidth() + fh / 10;
			}

			text = team.getActualDisplayName();
			d = new Dimension(w + fm.stringWidth(text), fh);
		}
	}

	protected class OrganizationItem extends ImageTextItem {
		protected OrganizationItem(IContest contest, IOrganization org) {
			int w = 0;
			int fh = fm.getHeight();
			img = org.getLogoImage(fh, fh, true, true);
			if (img != null)
				w = img.getWidth() + fh / 10;

			text = org.getActualFormalName();
			d = new Dimension(w + fm.stringWidth(text), fh);
		}
	}

	protected class PersonItem extends ImageTextItem {
		protected PersonItem(IContest contest, IPerson person) {
			int w = 0;
			int fh = fm.getHeight();
			img = person.getPhotoImage(fh, fh, true, true);
			if (img != null)
				w = img.getWidth() + fh / 10;

			text = person.getName();
			d = new Dimension(w + fm.stringWidth(text), fh);
		}
	}

	public enum Alignment {
		LEFT, CENTER, RIGHT
	}

	public static class Layout {
		public Alignment align = Alignment.LEFT;
		public int wrapWidth = 500;
		public int firstLineIndent = 0;
		public int indent = 0;
		public double verticalSpacing = 1.25;
	}

	private Graphics2D g;
	private FontMetrics fm;
	private Dimension bounds = new Dimension(0, 0);
	private List<Item> list = new ArrayList<>(4);
	private boolean autoLayout = true;

	/**
	 * Create the simplest text helper that will render a single-line string containing text or
	 * emojis.
	 *
	 * @param g
	 * @param s
	 */
	public TextHelper(Graphics2D g, String s) {
		this.g = g;
		this.fm = g.getFontMetrics();
		addString(s);
	}

	/**
	 * Create a single-line helper that automatically lays any added text or items.
	 *
	 * @param g
	 */
	public TextHelper(Graphics2D g) {
		this.g = g;
		this.fm = g.getFontMetrics();
	}

	/**
	 * Create a more complex helper, capable of handling any type of item and supporting multiple
	 * lines. layout(Layout) must be called before drawing.
	 *
	 * @param g
	 * @param autoLayout
	 */
	public TextHelper(Graphics2D g, boolean autoLayout) {
		this.g = g;
		this.fm = g.getFontMetrics();
		this.autoLayout = autoLayout;
	}

	public void setGraphics(Graphics2D g) {
		this.g = g;
	}

	public void addPlainText(String s) {
		String ss = s;
		int ind = ss.indexOf("\n");
		while (ind >= 0) {
			if (ind > 0) {
				add(new StringItem(ss.substring(0, ind)));
			}
			add(new NewLineItem());
			if (ind == ss.length() - 1)
				return;

			ss = ss.substring(ind + 1);
			ind = ss.indexOf("\n");
		}
		add(new StringItem(ss));
	}

	public void addICPCString(IContest contest, String s) {
		if (s == null)
			return;

		int pos = 0;
		int last = 0;
		int start = s.indexOf("{", pos);
		while (start >= 0) {
			int end = s.indexOf("}", start + 2);
			if (end >= 0) {
				String sub = s.substring(start + 1, end);
				int mid = sub.indexOf(":");
				// String type = null;
				IContestObject.ContestType ct = null;
				String id = null;
				if (mid < 0) {
					ct = IContestObject.getTypeByName(sub);
					if (ct != null && !IContestObject.isSingleton(ct))
						ct = null;
				} else if (mid > 1) {
					ct = IContestObject.getTypeByName(sub.substring(0, mid));
					id = sub.substring(mid + 1);
					if (IContestObject.isSingleton(ct))
						ct = null;
				}
				if (ct != null) {
					if (start > 0)
						addString(s.substring(last, start));
					IContestObject obj = ((Contest) contest).getObjectByTypeAndId(ct, id);
					if (obj == null)
						addPlainText("[missing " + ct.name().toLowerCase() + "]");
					else
						addContestObject(contest, obj);
					last = end + 1;
					pos = end;
				}
			}

			pos++;
			start = s.indexOf("{", pos);
		}
		if (last < s.length())
			addString(s.substring(last));
	}

	public void addString(String s) {
		char[] c = s.toCharArray();

		int fontBegin = -1;
		for (int i = 0; i < s.length();) {
			int codePoint = s.codePointAt(i);
			int charCount = Character.charCount(codePoint); // high Unicode code points span two chars

			// detect if we have an emoji for the current position
			EmojiEntry emojiEntry = findEmoji(s, i);
			if (emojiEntry == null) {
				if (fontBegin == -1)
					fontBegin = i;
				i += charCount;
			} else {
				if (fontBegin != -1)
					addPlainText(new String(c, fontBegin, i - fontBegin));
				addEmoji(emojiEntry);
				fontBegin = -1;
				i += emojiEntry.raw.length();
			}
		}

		if (fontBegin != -1)
			addPlainText(new String(c, fontBegin, c.length - fontBegin));
	}

	private void add(Item i) {
		list.add(i);

		if (autoLayout)
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

	public void layout(Layout layout) {
		if (layout == null)
			throw new IllegalArgumentException("Missing layout");

		// TODO: support \n

		// find max height
		int maxh = 0;
		for (Item i : list) {
			maxh = Math.max(maxh, i.d.height);
		}

		// layout items horizontally
		int xx = layout.firstLineIndent;
		int yy = 0;
		int in = 0;
		while (in < list.size()) {
			Item i = list.get(in);
			if (xx + i.d.width > layout.wrapWidth && i instanceof StringItem) {
				StringItem si = (StringItem) i;
				String[] ss = tryToSplit(si.s, fm, layout.wrapWidth - xx);
				if (ss == null && xx > 0 && i.d.width > layout.wrapWidth)
					ss = tryToSplit(si.s, fm, layout.wrapWidth);
				if (ss != null) {
					list.remove(in);

					addPlainText(ss[1]);
					Item ii = list.remove(list.size() - 1);
					list.add(in, ii);

					addPlainText(ss[0]);
					ii = list.remove(list.size() - 1);
					list.add(in, ii);

					i = list.get(in);
				}
			}

			if (xx + i.d.width > layout.wrapWidth || i instanceof NewLineItem) {
				// remove trailing whitespace from previous line and reposition item
				if (in > 0) {
					Item pi = list.get(in - 1);
					if (pi instanceof StringItem) {
						StringItem si = (StringItem) pi;
						si.s = si.s.stripTrailing();
						int pw = si.d.width;
						si.d = new Dimension(fm.stringWidth(si.s), fm.getHeight());
						si.x -= (pw - si.d.width) / 2;
					}
				}

				yy += (int) (maxh * layout.verticalSpacing);
				xx = layout.indent;
			}

			// remove leading whitespace from new lines
			if (xx == 0 && i instanceof StringItem) {
				StringItem si = (StringItem) i;
				si.s = si.s.stripLeading();
				si.d = new Dimension(fm.stringWidth(si.s), fm.getHeight());
			}

			if (i instanceof NewLineItem) {
				i.x = xx;
				i.y = yy + maxh / 2;
			} else {
				i.x = xx + i.d.width / 2;
				i.y = yy + maxh / 2;
				xx += i.d.width;
				bounds.width = Math.max(bounds.width, xx);
				bounds.height = Math.max(bounds.height, i.y + i.d.height / 2);
			}
			in++;
		}

		// fix alignment
		if (layout.align != Alignment.LEFT) {
			int start = 0;
			while (start < list.size()) {
				int end = start;
				while (end < list.size() && list.get(start).y == list.get(end).y)
					end++;

				// realign
				int dx = layout.wrapWidth - (list.get(end - 1).x + list.get(end - 1).d.width / 2);
				for (int i = start; i < end; i++) {
					if (layout.align == Alignment.CENTER)
						list.get(i).x += dx / 2;
					else // Alignment.RIGHT
						list.get(i).x += dx;
				}
				start = end;
			}
		}
	}

	private static String[] tryToSplit(String src, FontMetrics fm, int width) {
		if (fm.stringWidth(src) < width || src.length() < 6)
			return null;

		Matcher m = Pattern.compile(".+?[ \\t]|.+?(?:\n)|.+?$").matcher(src);

		StringBuilder sb = new StringBuilder();

		while (m.find() && fm.stringWidth(sb.toString()) < width) {
			String word = m.group();

			if (fm.stringWidth(sb.toString() + word) > width) {
				if (sb.isEmpty()) {
					// add individual letters?
					if (word.trim().length() < 6) // not worth wrapping, just start a new line
						return null;

					// check if there's an existing '-' that works
					int ind = word.indexOf("-");
					if (ind > 0) {
						if (fm.stringWidth(sb.toString() + word.substring(ind + 1)) < width) {
							ind += sb.length();
							return new String[] { src.substring(0, ind + 1), src.substring(ind + 1) };
						}
					}

					// else just pick a spot to add a hyphen
					int i = 0;
					while (i < src.length() && fm.stringWidth(src.substring(0, i + 1) + "-") < width)
						i++;

					if (i < 3)
						return null;
					return new String[] { src.substring(0, i) + "-", src.substring(i) };
				}
				return new String[] { sb.toString(), src.substring(sb.length()) };
			}

			sb.append(word);
		}

		return null;
	}

	public void addProblem(IProblem p) {
		add(new ProblemItem(p));
	}

	public void addTeam(IContest c, ITeam t) {
		add(new TeamItem(c, t));
	}

	public void addContestObject(IContest contest, IContestObject obj) {
		int fh = fm.getHeight();
		if (obj instanceof ITeam) {
			ITeam team = (ITeam) obj;
			IOrganization org = contest.getOrganizationById(team.getOrganizationId());
			BufferedImage img = null;
			if (org != null)
				img = org.getLogoImage(fh, fh, true, true);
			add(new ImageTextItem(img, team.getActualDisplayName()));
		} else if (obj instanceof IOrganization) {
			IOrganization org = (IOrganization) obj;
			add(new ImageTextItem(org.getLogoImage(fh, fh, true, true), org.getActualFormalName()));
		} else if (obj instanceof IPerson) {
			IPerson person = (IPerson) obj;
			add(new ImageTextItem(person.getPhotoImage(fh, fh, true, true), person.getName()));
		} else if (obj instanceof IProblem) {
			IProblem p = (IProblem) obj;
			add(new ProblemItem(p));
		} else {
			// unsupported contest object
			addPlainText("contest object");
		}
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
		if (emoji.svg == null)
			emoji.svg = loadEmojiFromFile(emoji.hex);

		if (emoji.svg == null)
			return;

		int size = fm.getHeight() - 2;

		try {
			String viewBox = emoji.svg.getDocumentElement().getAttribute("viewBox");
			String[] viewBoxValues = viewBox.split(" ");
			if (viewBoxValues.length < 4)
				return;

			float w = Float.parseFloat(viewBoxValues[2]);
			float h = Float.parseFloat(viewBoxValues[3]);
			float scale = Math.min(size * 1.5f / w, size / h);

			BufferedImageTranscoder imageTranscoder = new BufferedImageTranscoder();
			imageTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, w * scale);
			imageTranscoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, h * scale);

			TranscoderInput input = new TranscoderInput(emoji.svg);
			imageTranscoder.transcode(input, null);

			BufferedImage img = imageTranscoder.getBufferedImage();
			addImage(img);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Invalid emoji", e);
		}
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
		for (Item i : list) {
			if (DEBUG_BOUNDING_BOXES) {
				g.setColor(Color.GRAY);
				g.drawRect(i.x - i.d.width / 2, i.y - i.d.height / 2, i.d.width, i.d.height);
			}
			i.draw();
		}

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
		protected final String hex;
		protected final String raw;
		protected Document svg;

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

	class BufferedImageTranscoder extends ImageTranscoder {
		private BufferedImage img = null;

		@Override
		public BufferedImage createImage(int w, int h) {
			return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		}

		@Override
		public void writeImage(BufferedImage img2, TranscoderOutput output) {
			this.img = img2;
		}

		public BufferedImage getBufferedImage() {
			return img;
		}
	}

	private static SVGDocument loadEmojiFromFile(String hex) {
		String filename = "font/twemoji/" + hex + ".svg";

		SAXSVGDocumentFactory factory = null;
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		factory = new SAXSVGDocumentFactory(parser);

		try (InputStream in = ICPCFont.class.getClassLoader().getResourceAsStream(filename)) {
			return factory.createSVGDocument(filename, in);
		} catch (Exception ex) {
			return null;
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
		text.draw(x, y - text.fm.getAscent());
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
			if (name.endsWith(".svg"))
				System.out.println(name.substring(0, name.length() - 4));
		}
	}
}