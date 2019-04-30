package org.icpc.tools.presentation.core.chart;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;

import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.internal.Animator;
import org.icpc.tools.presentation.core.transition.SmoothUtil;

public abstract class AbstractChartPresentation extends Presentation {
	private static final int MARGIN = 10;
	private static final int GAP = 5;
	private static final int DEPTH = 20;
	private static final long INITIAL_DELAY = 250;
	private static final long FADE_IN_TIME = 3000;
	private static final long UPDATE_TIME = 800;

	protected static final int NO_DATA = -9999;

	public static final Color GRAD = new Color(50, 50, 50);
	public static final Color GRAD2 = new Color(170, 170, 170);
	public static final Color GRAD25 = new Color(230, 230, 230);
	public static final Color GRAD3 = new Color(70, 70, 70);
	private static final Color SHADOW = new Color(0, 0, 0, 192);

	public enum Type {
		LINE, BAR, BAR_3D
	}

	private Font titleFont;
	private Font labelFont;
	private Font horizontalAxisFont;
	private Font verticalAxisFont;

	private Rectangle rect;
	protected boolean showValueLabels = true;
	protected Type type = Type.LINE;
	private Font font;

	protected String title;
	protected String horizontalAxisTitle;
	protected String verticalAxisTitle;
	protected String[] horizontalLabels;

	private Series[] dataSeries;
	private int[] xPoints;
	private int[] lines;
	private Animator maximum = new Animator(0, new Animator.Movement(5, 20));
	private int lineDelta;
	private long initialTime;
	private long lastUpdateTime;

	public AbstractChartPresentation(Type type, String title, String verticalAxisTitle) {
		this.type = type;
		this.title = title;
		this.verticalAxisTitle = verticalAxisTitle;
	}

	protected void setSeries(Series... ser) {
		dataSeries = ser;
	}

	protected Series[] getSeries() {
		return dataSeries;
	}

	protected void setTitle(String title) {
		this.title = title;
	}

	protected void setVerticalAxisTitle(String verticalAxisTitle) {
		this.verticalAxisTitle = verticalAxisTitle;
	}

	protected void setHorizontalAxisTitle(String horizontalAxisTitle) {
		this.horizontalAxisTitle = horizontalAxisTitle;
	}

	protected void setHorizontalLabels(String[] horizontalLabels) {
		this.horizontalLabels = horizontalLabels;
	}

	protected static Color alpha(Color c, int alpha) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}

	/*protected static Color darker(Color c, float f) {
		return new Color(Math.max((int)(c.getRed()*f), 0),
			Math.max((int)(c.getGreen()*f), 0),
			Math.max((int)(c.getBlue()*f), 0));
	}*/

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		float dpi = 96;
		float labelHeight = height / (float) 20;
		float inch = labelHeight / dpi;
		float size = (int) (inch * 72f * 1.1f);

		if (font == null)
			font = new Font("Arial", Font.BOLD, 10);
		titleFont = font.deriveFont(Font.BOLD, (int) size);
		size = (int) (inch * 72f * 0.9f);
		labelFont = font.deriveFont(Font.PLAIN, (int) size);
		horizontalAxisFont = font.deriveFont(Font.PLAIN, (int) size);
		AffineTransform at = AffineTransform.getRotateInstance(Math.toRadians(-90));
		verticalAxisFont = horizontalAxisFont.deriveFont(at);
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();
		setupChart();
		updateData();

		updateMaxTarget();
		maximum.resetToTarget();
		initialTime = getRepeatTimeMs();
		lastUpdateTime = getTimeMs();
		initLines();
	}

	// create series, set labels, etc.
	protected abstract void setupChart();

	// update series values
	protected abstract void updateData();

	@Override
	public void incrementTimeMs(long dt) {
		long time = getTimeMs();
		if (lastUpdateTime < time - UPDATE_TIME) {
			lastUpdateTime = time;
			updateData();
		}

		if (dataSeries == null)
			return;

		updateMaxTarget();
		maximum.incrementTimeMs(dt);
		initLines();

		for (Series s : dataSeries)
			s.incrementTimeMs(dt);

		super.incrementTimeMs(dt);
	}

	/**
	 * Sets the font used for this chart. Must be called before initialization (typically via the
	 * constructor).
	 *
	 * @param font a font
	 */
	protected void setFont(Font font) {
		this.font = font;
	}

	/*protected float getMaxValue() {
		float max = 0;
		int numSeries = dataSeries.length;
		int size = dataSeries[0].getLength();
		for (int j = 0; j < numSeries; j++) {
			for (int i = 0; i < size; i++)
				max = Math.max(max, dataSeries[j].getValue(i));
		}
		//if (values2 != null)
		//	for (float f: values2)
		//		max = Math.max(max, f);
		return max;
	}*/

	/*protected static Color transparent2(Color c, int a) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
	}*/

	protected Rectangle paintBackgroundRect(Graphics2D g) {
		int numLines = 0;
		if (lines != null)
			numLines = lines.length;

		Dimension d = getSize();
		Rectangle r = new Rectangle(MARGIN, MARGIN, d.width - MARGIN * 2, d.height - MARGIN * 2);

		// add title
		if (title != null) {
			g.setFont(titleFont);
			FontMetrics fm = g.getFontMetrics();

			g.setColor(Color.WHITE);
			g.drawString(title, (width / 2) - fm.stringWidth(title) / 2, fm.getAscent() + GAP);

			int ind = fm.getHeight() + GAP * 3;
			r.y += ind;
			r.height -= ind;
		}

		if (type == Type.BAR_3D) {
			r.width -= DEPTH;
			r.height -= DEPTH;
			r.y += DEPTH;
		}

		// remove left edge
		int ind = 0;
		g.setFont(labelFont);
		FontMetrics fm = g.getFontMetrics();
		for (int i = 0; i < numLines; i++) {
			String s = "" + lines[numLines - i - 1];
			ind = Math.max(ind, fm.stringWidth(s));
		}
		ind += 5;

		if (verticalAxisTitle != null) {
			g.setFont(horizontalAxisFont);
			fm = g.getFontMetrics();
			ind += (fm.getHeight() + GAP);
			g.setFont(verticalAxisFont);
			g.setColor(Color.WHITE);
			g.drawString(verticalAxisTitle, fm.getHeight(), (height + fm.stringWidth(verticalAxisTitle)) / 2);

			r.width -= (fm.getHeight() + GAP); // right edge
		}

		r.x += ind;
		r.width -= ind;

		// raise bottom
		if (horizontalAxisTitle != null) {
			g.setFont(horizontalAxisFont);
			r.height -= (fm.getHeight() + GAP);

			g.drawString(horizontalAxisTitle, ind + (width - ind - fm.stringWidth(horizontalAxisTitle)) / 2,
					r.y + r.height + fm.getAscent() + GAP);
		}

		g.setFont(labelFont);
		fm = g.getFontMetrics();
		r.height -= (fm.getAscent() + 8);

		// draw lines
		double max = maximum.getValue();
		for (int i = 0; i < numLines; i++) {
			// int y = r.y + r.height * i / size;
			// int y = r.y + r.height;
			// if (i < size)
			int y = r.y + r.height - (int) (r.height * lines[numLines - i - 1] / max);
			// g.setColor(Color.LIGHT_GRAY);
			g.setColor(Color.DARK_GRAY);
			g.drawLine(r.x, y, r.x + r.width, y);
		}

		// draw left labels
		g.setColor(Color.WHITE);
		for (int i = 0; i < numLines + 1; i++) {
			// int y = r.y + r.height * i / size;
			int y = r.y + r.height;
			if (i < numLines)
				y = r.y + r.height - (int) (r.height * lines[numLines - i - 1] / max);
			String s = "0";
			if (i < numLines)
				s = "" + lines[numLines - i - 1];
			g.drawString(s, r.x - fm.stringWidth(s) - 7, y + fm.getAscent() / 2);
		}

		// draw bottom labels
		rect = r;
		initXPoints();
		numLines = xPoints.length;
		g.setColor(Color.WHITE);
		int h = r.y + r.height + fm.getAscent() + 8;
		for (int i = 0; i < numLines; i++) {
			String s = horizontalLabels[i];
			if (s != null)
				g.drawString(s, r.x + xPoints[i] - fm.stringWidth(s) / 2, h);
		}

		return r;
	}

	/*public static void gradientPaint(Graphics2D g, Rectangle r, Color c1, Color c3) {
		//Color c1 = Color.BLACK;
		//Color c2 = new Color(32, 32, 32);
		//Color c2 = Color.RED;
		//Color c3 = Color.DARK_GRAY;
		Color c2 = new Color((c1.getRed() + c3.getRed())/2, (c1.getGreen() + c3.getGreen())/2, (c1.getBlue() + c3.getBlue())/2);
		g.setPaint(new GradientPaint(0, r.y, c1, 0, r.y + r.height / 2, c2));
		g.fillRect(r.x, r.y, r.width, r.height / 2);
		g.setPaint(new GradientPaint(0, r.y + r.height / 2, c3, 0, r.y + r.height, c2));
		g.fillRect(r.x, r.y + r.height / 2, r.width, r.height / 2);
	}*/

	protected void updateMaxTarget() {
		if (dataSeries == null)
			return;

		double f = 0;
		int numSeries = dataSeries.length;
		int size = dataSeries[0].getLength();
		for (int j = 0; j < numSeries; j++) {
			for (int i = 0; i < size; i++)
				f = Math.max(f, dataSeries[j].getValue(i));
		}

		if (showValueLabels)
			f *= 1.1;
		else
			f *= 1.01;

		maximum.setTarget(Math.ceil(f));

	}

	protected void initLines() {
		double max = maximum.getValue();
		if (max < 1)
			return;

		int m = (int) max / 4;
		if (m % 5 != 0)
			m = m / 5 * 5;
		if (m < 1)
			m = 1;

		int numLines = (int) max / m;

		int[] y = new int[numLines];
		for (int i = 0; i < numLines; i++) {
			y[i] = m * (i + 1);
		}
		lines = y;
		lineDelta = m;
	}

	protected void updateLines() {
		if (lines == null || lines.length == 0) {
			initLines();
			return;
		}
		int numLines = lines.length;
		int lmax = lines[numLines - 1];

		double max = maximum.getValue();
		if (lmax + lineDelta < max) { // add line
			int[] newLines = new int[numLines + 1];
			System.arraycopy(lines, 0, newLines, 0, numLines);
			newLines[numLines] = lmax + lineDelta;
			lines = newLines;
		} else if (lmax > max) { // remove line
			int[] newLines = new int[numLines - 1];
			System.arraycopy(lines, 0, newLines, 0, numLines - 1);
			lines = newLines;
		}
		int newSize = -1;
		if (numLines > 5) { // remove half of lines
			newSize = numLines / 2;
			lineDelta *= 2;
		} else if (numLines < 3) { // double number of lines
			newSize = numLines * 2;
			lineDelta /= 2;
		}
		if (newSize != -1) {
			int[] newLines = new int[newSize];
			for (int i = 0; i < newSize; i++)
				newLines[i] = lineDelta * (i + 1);
			lines = newLines;
		}
	}

	protected void initXPoints() {
		if (dataSeries == null || dataSeries.length == 0 || rect == null) {
			xPoints = null;
			return;
		}

		int size = dataSeries[0].getLength();
		double w = 0;
		if (type == Type.LINE)
			w = rect.width / (double) (size - 1);
		else
			w = rect.width / (double) size;

		xPoints = new int[size];
		for (int i = 0; i < size; i++) {
			if (type == Type.LINE)
				xPoints[i] = (int) (w * i);
			else
				xPoints[i] = (int) (w * i + w / 2f);
		}
	}

	protected int[] getPoints(Series series, boolean growOnStart) {
		int size = series.getLength();

		long time = getRepeatTimeMs();
		if (time > 500000)
			time = time - initialTime;

		int[] p = new int[size];
		double max = maximum.getValue();
		for (int i = 0; i < size; i++) {
			double val = series.getAnimValue(i);
			if (val == NO_DATA)
				p[i] = NO_DATA;
			else {
				int h = (int) (rect.height * val / max);
				if (growOnStart) {
					if (time < INITIAL_DELAY)
						h = 0;
					else if (time < INITIAL_DELAY + FADE_IN_TIME)
						h = (int) (h * SmoothUtil.smooth((time - INITIAL_DELAY) / (double) FADE_IN_TIME));
				}
				p[i] = rect.height - h;
			}

		}
		return p;
	}

	protected void paintLineSeries(Graphics2D g, Series series) {
		int size = series.getLength();
		if (size == 0)
			return;

		Stroke oldStroke = g.getStroke();
		g.setStroke(series.getStroke());
		g.setPaint(series.getPaint(0));

		int[] p = getPoints(series, false);

		long growTime = getRepeatTimeMs();
		if (growTime > 500000)
			growTime = growTime - initialTime;

		if (growTime < INITIAL_DELAY)
			return;
		else if (growTime < INITIAL_DELAY + FADE_IN_TIME)
			size = (int) (size * SmoothUtil.smooth((growTime - INITIAL_DELAY) / (double) FADE_IN_TIME));

		for (int i = 1; i < size; i++) {
			if (p[i - 1] != NO_DATA && p[i] != NO_DATA)
				g.drawLine(xPoints[i - 1], p[i - 1], xPoints[i], p[i]);
		}

		// draw points
		for (int i = 0; i < size; i++)
			series.drawPoint(g, xPoints[i], p[i]);

		g.setStroke(oldStroke);
		float time = getTimeMs() / 1000f;
		if (showValueLabels && time > 3f) {
			int a = (int) Math.min(255, (time - 3f) * 125);
			g.setPaint(new Color(255, 255, 255, a));
			for (int i = 0; i < size; i++) {
				String s = series.getValueLabel(i);
				FontMetrics fm = g.getFontMetrics();
				g.drawString(s, xPoints[i] - fm.stringWidth(s) / 2, p[i] - fm.getDescent() - 7);
			}
		}
	}

	protected void paintBarSeries(Graphics2D g, Series series, int ser, int numSeries) {
		int size = series.getLength();
		if (size == 0)
			return;

		float w = rect.width / size;

		int ww = (int) (w * 0.8);
		int[] pp = getPoints(series, true);
		Point[] p = new Point[xPoints.length];
		for (int i = 0; i < xPoints.length; i++)
			p[i] = new Point(xPoints[i], pp[i]);

		if (numSeries > 1) {
			ww = (int) (w * 0.7);
			for (int j = 0; j < p.length; j++) {
				if (ser == 0)
					p[j].x = p[j].x + (int) (w * 0.1f);
				else if (ser == 1)
					p[j].x = p[j].x - (int) (w * 0.1f);
			}
		}

		for (int i = 0; i < size; i++) {
			int hh = rect.height - p[i].y;
			g.setColor(new Color(0, 0, 0, 64));
			g.fillRect(p[i].x - (ww / 2) - 1, p[i].y - 1, ww + 3, hh + 2);

			Paint c = series.getPaint(i);
			g.setPaint(c);
			g.fillRect(p[i].x - ww / 2, p[i].y, ww, hh);

			g.setStroke(new BasicStroke(2.0f));
			g.setPaint(series.getOutline(i));
			g.drawRect(p[i].x - ww / 2, p[i].y, ww, hh);
			g.setStroke(new BasicStroke(1.0f));
		}

		float time = getTimeMs() / 1000f;
		if (showValueLabels && time > 3f) {
			int a = (int) Math.min(255, (time - 3f) * 125);
			if (a < 255)
				g.setPaint(new Color(255, 255, 255, a));
			else
				g.setPaint(Color.WHITE);
			for (int i = 0; i < size; i++) {
				String s = series.getValueLabel(i);
				FontMetrics fm = g.getFontMetrics();
				// g.drawString(s, p[i].x - fm.stringWidth(s) / 2, p[i].y - fm.getDescent() - 4);
				drawString3D(g, s, p[i].x - fm.stringWidth(s) / 2, p[i].y - fm.getDescent() - 4);
			}
		}
	}

	private static void drawString3D(Graphics2D g, String s, int x, int y) {
		Color c = g.getColor();
		g.setColor(alpha(SHADOW, c.getAlpha() / 3));
		g.drawString(s, x - 1, y - 1);
		// g.drawString(s, x-1, y+1);
		// g.drawString(s, x+1, y-1);
		g.drawString(s, x + 1, y + 1);
		g.setColor(c);
		g.drawString(s, x, y);
	}

	protected void paint3DBarSeries(Graphics2D g, Series series, int ser, int numSeries) {
		int size = series.getLength();
		if (size == 0)
			return;

		float w = rect.width / size;

		int ww = (int) (w * 0.5);
		int ha = 0;
		int[] pp = getPoints(series, true);
		Point[] p = new Point[xPoints.length];
		for (int i = 0; i < xPoints.length; i++)
			p[i] = new Point(xPoints[i], pp[i]);

		if (numSeries > 1) {
			ww = (int) (w * 0.4);
			for (int j = 0; j < p.length; j++) {
				if (ser == 0) {
					p[j].x = p[j].x + (int) (w * 0.15f);
					ha = -(int) (w * 0.15f);
				} else if (ser == 1) {
					p[j].x = p[j].x - (int) (w * 0.15f);
					ha = (int) (w * 0.15f);
				}
			}
		}

		for (int i = 0; i < size; i++) {
			Paint c = series.getPaint(i);
			int hh = rect.height - p[i].y + ha;

			// g.setColor(c);
			// g.fillRect(p[i].x - ww / 2, p[i].y, ww, hh);
			final int TOP = 25;

			// cylinder
			// Paint oldPaint = g.getPaint();
			// Paint paint = new GradientPaint(p[i].x, 0, c, p[i].x + ww / 2, 0, c.darker().darker(),
			// true);
			g.setPaint(c);
			Area a = new Area(new Rectangle(p[i].x - ww / 2, p[i].y, ww, hh));
			Area aa = new Area(new Ellipse2D.Float(p[i].x - ww / 2, p[i].y - TOP / 2, ww, TOP));
			a.subtract(aa);
			aa = new Area(new Ellipse2D.Float(p[i].x - ww / 2, p[i].y + hh - TOP / 2, ww, TOP));
			a.add(aa);
			g.fill(a);

			g.setPaint(series.getOutline(i));
			g.draw(a);

			// top
			a = new Area(new Ellipse2D.Float(p[i].x - ww / 2, p[i].y - TOP / 2, ww, TOP));
			g.setPaint(c);
			g.fill(a);

			g.setPaint(series.getOutline(i));
			g.draw(a);
		}

		float time = getTimeMs() / 1000f;
		if (showValueLabels && time > 3f) {
			int a = (int) Math.min(255, (time - 3f) * 125);
			g.setPaint(new Color(255, 255, 255, a));
			for (int i = 0; i < size; i++) {
				String s = series.getValueLabel(i);
				FontMetrics fm = g.getFontMetrics();
				g.drawString(s, p[i].x - fm.stringWidth(s) / 2, p[i].y - fm.getDescent() - 7);
			}
		}
	}

	@Override
	public void paint(Graphics2D g) {
		if (dataSeries == null)
			return;

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		rect = paintBackgroundRect(g);

		// draw rectangle
		g.setColor(Color.WHITE);
		g.drawLine(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y + rect.height);

		Graphics2D g2 = (Graphics2D) g.create();
		g2.translate(rect.x, rect.y);
		if (type == Type.LINE) {
			int numSeries = dataSeries.length;
			for (int i = 0; i < numSeries; i++)
				paintLineSeries(g2, dataSeries[i]);
		} else if (type == Type.BAR) {
			int numSeries = dataSeries.length;
			for (int i = 0; i < numSeries; i++)
				paintBarSeries(g2, dataSeries[i], i, numSeries);

			g2.setColor(Color.WHITE);
			g2.drawLine(0, rect.height, rect.width, rect.height);
		} else if (type == Type.BAR_3D) {
			int numSeries = dataSeries.length;
			for (int i = 0; i < numSeries; i++)
				paint3DBarSeries(g2, dataSeries[i], i, numSeries);
		}
		g2.dispose();

		long time = getRepeatTimeMs();
		if (time > 500000)
			time = time - initialTime;
		if (time < 6000) {
			Graphics2D g3 = (Graphics2D) g.create();
			// fade legends in and out
			if (time > 0 && time < 1500)
				g3.setComposite(AlphaComposite.SrcOver.derive(time / 1500f));
			else if (time > 4500)
				g3.setComposite(AlphaComposite.SrcOver.derive((6000 - time) / 1500f));

			int right = (width - rect.x - rect.width) / 2;
			g3.translate(width - right, rect.y + right);
			paintLegend(g3);
			g3.dispose();
		}
	}

	protected void paintLegend(Graphics2D g) {
		if (dataSeries == null || dataSeries.length < 2)
			return;

		g.setFont(horizontalAxisFont);

		int size = dataSeries.length;
		String[] s = new String[size];
		for (int i = 0; i < size; i++)
			s[i] = dataSeries[i].getTitle();

		FontMetrics fm = g.getFontMetrics();

		int fh = fm.getHeight();
		int h = MARGIN * 2 + MARGIN * (size - 1) + fh * size;
		int w = 0;
		for (int i = 0; i < size; i++)
			w = Math.max(w, MARGIN * 2 + fh + MARGIN + fm.stringWidth(s[i]));

		g.translate(-w, 0);
		g.setColor(new Color(0, 0, 0, 192));
		g.fillRect(0, 0, w, h);

		g.setColor(Color.WHITE);
		g.drawRect(0, 0, w, h);

		g.translate(MARGIN, MARGIN);

		int y = 0;
		for (Series series : dataSeries) {
			int i = 0;
			Paint c = series.getPaint(i);
			g.setPaint(c);
			g.fillRect(0, y, fh, fh);

			g.setStroke(new BasicStroke(2.0f));
			g.setPaint(series.getOutline(i));
			g.drawRect(0, y, fh, fh);
			g.setColor(Color.WHITE);
			g.drawString(series.getTitle(), MARGIN + fh, y + fm.getAscent() + 1);
			y += fh + MARGIN;
		}
	}
}