package org.icpc.tools.contest.model.internal;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.feed.Decimal;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class Problem extends ContestObject implements IProblem {
	private static final String ORDINAL = "ordinal";
	private static final String LABEL = "label";
	private static final String NAME = "name";
	private static final String COLOR = "color";
	private static final String RGB = "rgb";
	private static final String TEST_DATA_COUNT = "test_data_count";
	private static final String LOCATION = "location";
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
	private int testDataCount = Integer.MIN_VALUE;
	private double x = Double.NaN;
	private double y = Double.NaN;
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

	public void setLocation(double x, double y) {
		this.x = x;
		this.y = y;
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
		switch (name2) {
			case ORDINAL: {
				ordinal = parseInt(value);
				return true;
			}
			case LABEL: {
				label = (String) value;
				return true;
			}
			case NAME: {
				this.name = (String) value;
				return true;
			}
			case COLOR: {
				this.color = (String) value;
				return true;
			}
			case RGB: {
				this.rgb = (String) value;
				colorVal = null;
				return true;
			}
			case TEST_DATA_COUNT: {
				testDataCount = parseInt(value);
				return true;
			}
			case TIME_LIMIT: {
				timeLimit = Decimal.parse((String) value);
				return true;
			}
			case MAX_SCORE: {
				maxScore = parseDouble(value);
				return true;
			}
			case LOCATION: {
				JsonObject obj = JSONParser.getOrReadObject(value);
				x = obj.getDouble(X);
				y = obj.getDouble(Y);
				return true;
			}
			default:
				return false;
		}
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
		if (testDataCount != Integer.MIN_VALUE)
			props.put(TEST_DATA_COUNT, testDataCount);
		if (timeLimit > 0)
			props.put(TIME_LIMIT, Decimal.format(timeLimit));
		if (maxScore != null)
			props.put(MAX_SCORE, round(maxScore));

		if (!Double.isNaN(x) || !Double.isNaN(y)) {
			List<String> attrs = new ArrayList<>(3);
			if (!Double.isNaN(x))
				attrs.add("\"" + X + "\":" + round(x));
			if (!Double.isNaN(y))
				attrs.add("\"" + Y + "\":" + round(y));
			props.put(LOCATION, "{" + String.join(",", attrs) + "}");
		}
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
		if (testDataCount != Integer.MIN_VALUE)
			je.encode(TEST_DATA_COUNT, testDataCount);
		if (timeLimit > 0)
			je.encodePrimitive(TIME_LIMIT, Decimal.format(timeLimit));
		if (maxScore != null)
			je.encode(MAX_SCORE, Math.round(maxScore * 10000.0) / 10000.0); // round to 4 decimals

		if (!Double.isNaN(x) || !Double.isNaN(y)) {
			List<String> attrs = new ArrayList<>(3);
			if (!Double.isNaN(x))
				attrs.add("\"" + X + "\":" + round(x));
			if (!Double.isNaN(y))
				attrs.add("\"" + Y + "\":" + round(y));
			je.encodePrimitive(LOCATION, "{" + String.join(",", attrs) + "}");
		}
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

		if (testDataCount == Integer.MIN_VALUE)
			errors.add("Test data count missing");

		if (errors.isEmpty())
			return null;
		return errors;
	}
}