package org.icpc.tools.presentation.contest.internal.presentations.test;

import java.awt.Color;
import java.text.NumberFormat;

import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.core.chart.AbstractChartPresentation;
import org.icpc.tools.presentation.core.chart.Series;

public class TestChartPresentation extends AbstractChartPresentation {
	private static final Color[] ICPC_COLORS = new Color[] { ICPCColors.BLUE, ICPCColors.YELLOW, ICPCColors.RED };
	private static final String LABEL = "ABCDEFGHIJKL";
	private static final int NUM = 9;
	private static final NumberFormat format = NumberFormat.getNumberInstance();

	public TestChartPresentation() {
		super(Type.BAR, "Test Chart", "Widgets");
		setFont(ICPCFont.getMasterFont());
		format.setMinimumFractionDigits(1);
		format.setMaximumFractionDigits(1);
	}

	@Override
	protected void setupChart() {
		String[] labels = new String[NUM];
		Color[] colors = new Color[NUM];
		for (int i = 0; i < NUM; i++) {
			labels[i] = LABEL.charAt(i) + "";
			colors[i] = ICPC_COLORS[i % ICPC_COLORS.length];
		}

		setHorizontalLabels(labels);
		setSeries(new Series[] { new Series(colors) {
			@Override
			public String getValueLabel(int i) {
				return format.format(getValue(i));
			}
		} });
	}

	@Override
	protected void updateData() {
		long time = getRepeatTimeMs();

		double[] values = new double[NUM];
		for (int i = 0; i < NUM; i++)
			values[i] = 30 + Math.sin(i - time / 5000.0) * 15.0 + Math.sin(time / 10000.0) * 12.0;

		getSeries()[0].setValues(values);
	}
}