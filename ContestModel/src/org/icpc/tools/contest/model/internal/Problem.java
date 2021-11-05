package org.icpc.tools.contest.model.internal;

import java.awt.Color;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.feed.Decimal;
import org.icpc.tools.contest.model.feed.JSONEncoder;

public class Problem extends ContestObject implements IProblem {
	private static final String ORDINAL = "ordinal";
	private static final String LABEL = "label";
	private static final String NAME = "name";
	private static final String COLOR = "color";
	private static final String RGB = "rgb";
	private static final String TEST_DATA_COUNT = "test_data_count";
	private static final String X = "x";
	private static final String Y = "y";
	private static final String TIME_LIMIT = "time_limit";
	private static final String MAX_SCORE = "max_score";

	private int ordinal = Integer.MIN_VALUE;
	private String label;
	private String name;
	private String color;
	private String rgb;
	private Color colorVal;
	private int testDataCount;
	private double x = Double.MIN_VALUE;
	private double y = Double.MIN_VALUE;
	private int timeLimit;
	private Double maxScore;

	@Override
	public ContestType getType() {
		return ContestType.PROBLEM;
	}

	@Override
	public int getOrdinal() {
		return ordinal;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getColor() {
		return color;
	}

	@Override
	public Color getColorVal() {
		if (colorVal != null)
			return colorVal;

		if (rgb == null || !(rgb.length() == 3 || rgb.length() == 4 || rgb.length() == 6 || rgb.length() == 7)) {
			colorVal = Color.BLACK;
			return colorVal;
		}

		try {
			String rgbv = rgb;
			if (rgbv.length() == 3 || rgbv.length() == 4) {
				if (rgbv.length() == 4)
					rgbv = rgbv.substring(1);
				int r = Integer.parseInt(rgbv.substring(0, 1) + rgbv.substring(0, 1), 16);
				int g = Integer.parseInt(rgbv.substring(1, 2) + rgbv.substring(1, 2), 16);
				int b = Integer.parseInt(rgbv.substring(2, 3) + rgbv.substring(2, 3), 16);
				colorVal = new Color(r, g, b);
				return colorVal;
			}
			if (rgbv.length() == 7)
				rgbv = rgbv.substring(1);
			int r = Integer.parseInt(rgbv.substring(0, 2), 16);
			int g = Integer.parseInt(rgbv.substring(2, 4), 16);
			int b = Integer.parseInt(rgbv.substring(4, 6), 16);
			colorVal = new Color(r, g, b);
			return colorVal;
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Invalid color value for problem " + id + " (" + rgb + ")");
			colorVal = Color.BLACK;
			return colorVal;
		}
	}

	@Override
	public String getRGB() {
		return rgb;
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	@Override
	public int getTestDataCount() {
		return testDataCount;
	}

	@Override
	public int getTimeLimit() {
		return timeLimit;
	}

	@Override
	public Double getMaxScore() {
		return maxScore;
	}

	@Override
	protected boolean addImpl(String name2, Object value) throws Exception {
		if (ORDINAL.equals(name2)) {
			ordinal = parseInt(value);
			return true;
		} else if (LABEL.equals(name2)) {
			label = (String) value;
			return true;
		} else if (NAME.equals(name2)) {
			this.name = (String) value;
			return true;
		} else if (COLOR.equals(name2)) {
			this.color = (String) value;
			return true;
		} else if (RGB.equals(name2)) {
			this.rgb = (String) value;
			colorVal = null;
			return true;
		} else if (TEST_DATA_COUNT.equals(name2)) {
			testDataCount = parseInt(value);
			return true;
		} else if (TIME_LIMIT.equals(name2)) {
			timeLimit = Decimal.parse((String) value);
			return true;
		} else if (MAX_SCORE.equals(name2)) {
			maxScore = parseDouble(value);
			return true;
		} else if (X.equals(name2)) {
			x = parseDouble(value);
			return true;
		} else if (Y.equals(name2)) {
			y = parseDouble(value);
			return true;
		}
		return false;
	}

	@Override
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		props.put(LABEL, label);
		props.put(NAME, name);
		if (ordinal != Integer.MIN_VALUE)
			props.put(ORDINAL, ordinal);
		if (color != null)
			props.put(COLOR, color);
		if (rgb != null)
			props.put(RGB, rgb);
		if (x != Double.MIN_VALUE)
			props.put(X, round(x));
		if (y != Double.MIN_VALUE)
			props.put(Y, round(y));
		props.put(TEST_DATA_COUNT, testDataCount);
		if (timeLimit > 0)
			props.put(TIME_LIMIT, Decimal.format(timeLimit));
		if (maxScore != null)
			props.put(MAX_SCORE, round(maxScore));
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);
		if (label != null)
			je.encode(LABEL, label);
		if (name != null)
			je.encode(NAME, name);
		if (ordinal != Integer.MIN_VALUE)
			je.encode(ORDINAL, ordinal);
		if (color != null)
			je.encode(COLOR, color);
		if (rgb != null)
			je.encode(RGB, rgb);
		if (x != Double.MIN_VALUE)
			je.encode(X, round(x));
		if (y != Double.MIN_VALUE)
			je.encode(Y, round(y));
		je.encode(TEST_DATA_COUNT, testDataCount);
		if (timeLimit > 0)
			je.encodePrimitive(TIME_LIMIT, Decimal.format(timeLimit));
		if (maxScore != null)
			je.encode(MAX_SCORE, Math.round(maxScore * 10000.0) / 10000.0); // round to 4 decimals
	}

	private static double round(double d) {
		return Math.round(d * 100.0) / 100.0;
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (name == null || name.isEmpty())
			errors.add("Name missing");

		if (label == null || label.isEmpty())
			errors.add("Label missing");

		if (testDataCount == 0)
			errors.add("Test data count missing");

		if (errors.isEmpty())
			return null;
		return errors;
	}
}