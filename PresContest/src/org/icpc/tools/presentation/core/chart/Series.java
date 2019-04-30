package org.icpc.tools.presentation.core.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.text.NumberFormat;

import org.icpc.tools.presentation.core.internal.Animator;

public class Series {
	private static final NumberFormat valueFormat = NumberFormat.getNumberInstance();

	static {
		valueFormat.setMaximumFractionDigits(0);
	}

	public enum LinePoint {
		RECT, OVAL
	}

	private Stroke stroke = new BasicStroke(3);
	private double[] values;
	private Animator[] anim;
	private Paint[] paints;
	private String title;
	private LinePoint linePoint;

	public Series(int num, Paint paint) {
		this.paints = new Paint[] { paint };
		this.values = new double[num];
	}

	public Series(Paint[] paints) {
		this.paints = paints;
		this.values = new double[paints.length];
	}

	public void setValues(int[] values) {
		int size = values.length;
		for (int i = 0; i < size; i++)
			this.values[i] = values[i];

		if (anim == null) {
			anim = new Animator[size];
			for (int i = 0; i < size; i++)
				anim[i] = new Animator(values[i], new Animator.Movement(5, 30));
		} else {
			for (int i = 0; i < size; i++)
				anim[i].setTarget(values[i]);
		}
	}

	public void setValues(double[] values) {
		int size = values.length;
		for (int i = 0; i < size; i++)
			this.values[i] = values[i];
		if (anim == null) {
			anim = new Animator[size];
			for (int i = 0; i < size; i++)
				anim[i] = new Animator(values[i], new Animator.Movement(5, 30));
		} else {
			for (int i = 0; i < size; i++)
				anim[i].setTarget(values[i]);
		}
	}

	public void setStroke(Stroke stroke) {
		this.stroke = stroke;
	}

	protected void incrementTimeMs(long dt) {
		for (int i = 0; i < anim.length; i++)
			anim[i].incrementTimeMs(dt);
	}

	public Paint getPaint(int i) {
		if (paints.length >= i)
			return paints[i];
		return paints[0];
	}

	public Paint getOutline(int i) {
		Paint p = paints[0];
		if (paints.length > 1)
			p = paints[i];

		if (p instanceof Color) {
			Color c = ((Color) p);
			if (c.getRed() == 0 && c.getGreen() == 0 && c.getBlue() == 0)
				return AbstractChartPresentation.alpha(Color.LIGHT_GRAY, c.getAlpha());
			return c.brighter();
		}
		return p;
	}

	public String getValueLabel(int i) {
		if (getValue(i) == 0)
			return "";
		return valueFormat.format(getValue(i));
	}

	public int getLength() {
		return values.length;
	}

	public Stroke getStroke() {
		return stroke;
	}

	public double getValue(int i) {
		return values[i];
	}

	public double getAnimValue(int i) {
		return anim[i].getValue();
	}

	public void setTitle(String s) {
		title = s;
	}

	public String getTitle() {
		return title;
	}

	public void setLinePoint(LinePoint lp) {
		linePoint = lp;
	}

	public void drawPoint(Graphics2D g, int x, int y) {
		if (linePoint == null)
			return;
		else if (linePoint == LinePoint.RECT)
			g.fillRect(x - 2, y - 2, 5, 5);
		else if (linePoint == LinePoint.OVAL)
			g.fillOval(x - 3, y - 3, 7, 7);
	}
}