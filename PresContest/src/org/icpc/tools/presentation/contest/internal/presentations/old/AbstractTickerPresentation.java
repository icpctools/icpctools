package org.icpc.tools.presentation.contest.internal.presentations.old;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.Dimension2D;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;

/**
 * blue: 75,130,195 yellow: 249,199,28 red: 178,44,28
 */
public class AbstractTickerPresentation extends AbstractICPCPresentation {
	protected int count2 = 0;
	protected boolean proportional;
	protected boolean verticalTicker;
	protected long currentTicker;

	interface ITicker {
		public float getWidth(Graphics2D g);

		public void paint(Graphics2D g, float x, float time);

		public void paint(Graphics2D g, Dimension2D d, float count);
	}

	public class StringTicker implements ITicker {
		protected String s;
		protected Color c;

		public StringTicker(String s) {
			this(s, Color.WHITE);
		}

		public StringTicker(String s, Color c) {
			this.s = s;
			this.c = c;
		}

		public String getText() {
			return s;
		}

		@Override
		public float getWidth(Graphics2D g) {
			FontMetrics fm = g.getFontMetrics();
			return fm.stringWidth(s);
		}

		@Override
		public void paint(Graphics2D g, float x, float time2) {
			FontMetrics fm = g.getFontMetrics();
			g.setColor(c);
			g.drawString(s, x, getSize().height - BORDER - fm.getDescent());
		}

		@Override
		public void paint(Graphics2D g, Dimension2D d, float count) {
			g.setColor(c);

			FontMetrics fm = g.getFontMetrics();
			int y = fm.getAscent() + 6;

			if (d.getWidth() > 0 && fm.stringWidth(s) > d.getWidth()) {
				StringTokenizer st = new StringTokenizer(s);
				String t = "";
				String next = st.nextToken();
				while (st.hasMoreTokens()) {
					while (fm.stringWidth(t + " " + next) < d.getWidth() && st.hasMoreTokens()) {
						t += next + " ";
						next = st.nextToken();
					}
					if (fm.stringWidth(t + " " + next) < d.getWidth()) {
						t += next;
						next = null;
					}
					g.drawString(t, 0, y);
					y += fm.getHeight();

					t = next;
					if (t != null && !st.hasMoreTokens())
						g.drawString(t, 0, y);

					next = "";
				}
			} else
				g.drawString(s, 0, y);
		}
	}

	class ImageTicker implements ITicker {
		private final Image image;

		public ImageTicker(Image image) {
			this.image = image;
		}

		@Override
		public float getWidth(Graphics2D g) {
			return image.getWidth(null);
		}

		@Override
		public void paint(Graphics2D g, float x, float time2) {
			g.translate(x, 0);
			g.drawImage(image, 0, getSize().height, null);
			g.translate(-x, 0);
		}

		@Override
		public void paint(Graphics2D g, Dimension2D d, float count) {
			paint(g, 0, count);
		}
	}

	class GapTicker implements ITicker {
		private final int gap;

		public GapTicker(int gap) {
			this.gap = gap;
		}

		@Override
		public float getWidth(Graphics2D g) {
			return gap;
		}

		@Override
		public void paint(Graphics2D g, float x, float time2) {
			// ignore
		}

		@Override
		public void paint(Graphics2D g, Dimension2D d, float count) {
			d.setSize(gap, gap);
		}
	}

	private static final int DEFAULT_SPEED = 8; // seconds to cross screen
	private int speed = DEFAULT_SPEED;
	protected static final int BORDER = 5;

	protected Font textFont;
	protected int height2;

	private float logPos;
	private float lastIndex = -1;
	private final List<ITicker> ticker = new ArrayList<>();

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		float rowHeight = height / 2f;
		float size = rowHeight - 4;
		textFont = ICPCFont.getMasterFont().deriveFont(Font.BOLD, size);
		height2 = (int) textFont.getLineMetrics("AjyqJ", new FontRenderContext(null, true, true)).getHeight();
	}

	public boolean hasContent() {
		return !ticker.isEmpty();
	}

	@Override
	public void incrementTimeMs(long dt) {
		super.incrementTimeMs(dt);
		currentTicker += (dt / 1000f);
	}

	public void paintBackground(Graphics2D g) {
		// do nothing
	}

	public void paintForeground(Graphics2D g) {
		// do nothing
	}

	@Override
	public void paint(Graphics2D g) {
		paintBackground(g);

		if (verticalTicker)
			paintVerticalTicker(g);
		else
			paintHorizontalTicker(g);

		paintForeground(g);
	}

	protected void paintVerticalTicker(Graphics2D g) {
		if (currentTicker > 7f) {
			currentTicker = 0;
			if (!ticker.isEmpty())
				remove();
		} // TODO handle negative time

		if (ticker.isEmpty())
			newContent();

		ITicker current = ticker.get(0);

		Shape oldClip = g.getClip();
		g.setClip(0, 0, width, height);
		g.setFont(textFont);

		Dimension2D d = new Dimension(width - 10, height);

		if (currentTicker > 6f) {
			float dt = (currentTicker - 6f);
			float h = height * dt;
			g.translate(5, -h);
			current.paint(g, d, currentTicker);
			g.translate(0, height);
			if (ticker.size() < 2)
				newContent();
			ITicker next = ticker.get(1);
			next.paint(g, d, 0);
			g.translate(-5, h - height);
		} else {
			g.translate(5, 0);
			current.paint(g, d, currentTicker);
			g.translate(-5, 0);
		}

		g.setClip(oldClip);
	}

	protected void paintHorizontalTicker(Graphics2D g) {
		Shape oldClip = g.getClip();
		g.setClip(0, 0, width, height);

		g.setFont(textFont);

		float time = getTimeMs() / 1000f;
		float index = time;
		if (lastIndex != -1) {
			float d = index - lastIndex;
			if (d > 0)
				logPos += d * width / speed;
		}
		lastIndex = index;

		float x = width - logPos;

		int n = 0;
		boolean over = false;
		while (x < width && n < ticker.size()) {
			ITicker tick = ticker.get(n);
			tick.paint(g, x, time);

			x += tick.getWidth(g);
			if (n == 0 && x < 0)
				over = true;
			n++;
		}

		g.setClip(oldClip);

		if (over) {
			ITicker tick = ticker.get(0);
			logPos -= tick.getWidth(g);
			remove();
		}

		if (x < width + 20)
			newContent();
	}

	protected void newContent() {
		// ignore
	}

	/**
	 * Sets the speed, in seconds for each element to cross the screen.
	 *
	 * @param speed
	 */
	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public void remove() {
		synchronized (ticker) {
			ticker.remove(0);
		}
	}

	public void append(ITicker tick) {
		synchronized (ticker) {
			ticker.add(tick);
		}
	}
}