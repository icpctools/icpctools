package org.icpc.tools.contest.model.internal;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.feed.Decimal;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class Problem extends ContestObject implements IProblem {
	private static final String ORDINAL = "ordinal";
	private static final String LABEL = "label";
	private static final String NAME = "name";
	private static final String UUID = "uuid";
	private static final String COLOR = "color";
	private static final String RGB = "rgb";
	private static final String TEST_DATA_COUNT = "test_data_count";
	private static final String LOCATION = "location";
	private static final String X = "x";
	private static final String Y = "y";
	private static final String TIME_LIMIT = "time_limit";
	private static final String MEMORY_LIMIT = "memory_limit";
	private static final String OUTPUT_LIMIT = "output_limit";
	private static final String CODE_LIMIT = "code_limit";
	private static final String MAX_SCORE = "max_score";
	private static final String PACKAGE = "package";
	private static final String STATEMENT = "statement";

	private int ordinal = Integer.MIN_VALUE;
	private String label;
	private String name;
	private String uuid;
	private String color;
	private String rgb;
	private Color colorVal;
	private int testDataCount = Integer.MIN_VALUE;
	private double x = Double.NaN;
	private double y = Double.NaN;
	private int timeLimit;
	private int memoryLimit = Integer.MIN_VALUE;
	private int outputLimit = Integer.MIN_VALUE;
	private int codeLimit = Integer.MIN_VALUE;
	private Double maxScore;
	private FileReferenceList package_;
	private FileReferenceList statement;

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
	public String getUUID() {
		return uuid;
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

	public void clearTestDataCount() {
		testDataCount = Integer.MIN_VALUE;
	}

	@Override
	public int getTimeLimit() {
		return timeLimit;
	}

	@Override
	public int getMemoryLimit() {
		return memoryLimit;
	}

	@Override
	public int getOutputLimit() {
		return outputLimit;
	}

	@Override
	public int getCodeLimit() {
		return codeLimit;
	}

	@Override
	public Double getMaxScore() {
		return maxScore;
	}

	public void setPackage(FileReferenceList list) {
		package_ = list;
	}

	public FileReferenceList getPackage() {
		return package_;
	}

	@Override
	public File getPackage(boolean force) {
		return getFile(package_.first(), PACKAGE, force);
	}

	public void setStatement(FileReferenceList list) {
		statement = list;
	}

	public FileReferenceList getStatement() {
		return statement;
	}

	@Override
	public File getStatement(boolean force) {
		return getFile(statement.first(), STATEMENT, force);
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
				name = (String) value;
				return true;
			}
			case UUID: {
				uuid = (String) value;
				return true;
			}
			case COLOR: {
				color = (String) value;
				return true;
			}
			case RGB: {
				rgb = (String) value;
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
			case MEMORY_LIMIT: {
				memoryLimit = parseInt(value);
				return true;
			}
			case OUTPUT_LIMIT: {
				outputLimit = parseInt(value);
				return true;
			}
			case CODE_LIMIT: {
				codeLimit = parseInt(value);
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
			case PACKAGE: {
				package_ = parseFileReference(value);
				return true;
			}
			case STATEMENT: {
				statement = parseFileReference(value);
				return true;
			}
			default:
				return false;
		}
	}

	@Override
	public IContestObject clone() {
		Problem p = new Problem();
		p.id = id;
		p.ordinal = ordinal;
		p.name = name;
		p.uuid = uuid;
		p.label = label;
		p.color = color;

		p.rgb = rgb;
		p.testDataCount = testDataCount;
		p.x = x;
		p.y = y;
		p.timeLimit = timeLimit;
		p.memoryLimit = memoryLimit;
		p.outputLimit = outputLimit;
		p.codeLimit = codeLimit;
		p.maxScore = maxScore;

		p.package_ = package_;
		p.statement = statement;
		return p;
	}

	@Override
	protected void getProperties(Properties props) {
		props.addLiteralString(ID, id);
		props.addString(LABEL, label);
		props.addString(NAME, name);
		props.addString(UUID, uuid);
		if (ordinal != Integer.MIN_VALUE)
			props.addInt(ORDINAL, ordinal);
		props.addString(COLOR, color);
		props.addLiteralString(RGB, rgb);
		if (testDataCount != Integer.MIN_VALUE)
			props.addInt(TEST_DATA_COUNT, testDataCount);
		if (timeLimit > 0)
			props.add(TIME_LIMIT, Decimal.format(timeLimit));
		if (memoryLimit != Integer.MIN_VALUE)
			props.addInt(MEMORY_LIMIT, memoryLimit);
		if (outputLimit != Integer.MIN_VALUE)
			props.addInt(OUTPUT_LIMIT, outputLimit);
		if (codeLimit != Integer.MIN_VALUE)
			props.addInt(CODE_LIMIT, codeLimit);
		if (maxScore != null)
			props.addDouble(MAX_SCORE, round(maxScore));

		if (!Double.isNaN(x) || !Double.isNaN(y)) {
			List<String> attrs = new ArrayList<>(3);
			if (!Double.isNaN(x))
				attrs.add("\"" + X + "\":" + round(x));
			if (!Double.isNaN(y))
				attrs.add("\"" + Y + "\":" + round(y));
			props.add(LOCATION, "{" + String.join(",", attrs) + "}");
		}

		props.addFileRef(PACKAGE, package_);
		props.addFileRef(STATEMENT, statement);
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

	@Override
	public File resolveFileReference(String url2) {
		return FileReferenceList.resolve(url2, statement, package_);
	}
}