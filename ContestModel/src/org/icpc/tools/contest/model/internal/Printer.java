package org.icpc.tools.contest.model.internal;

import java.util.Map;

import org.icpc.tools.contest.model.IPrinter;
import org.icpc.tools.contest.model.feed.JSONEncoder;

public class Printer extends ContestObject implements IPrinter {
	private static final String X = "x";
	private static final String Y = "y";

	private double x = Double.NaN;
	private double y = Double.NaN;

	@Override
	public ContestType getType() {
		return ContestType.PRINTER;
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
	protected boolean addImpl(String name2, Object value) throws Exception {
		if (X.equals(name2)) {
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
		if (!Double.isNaN(x))
			props.put(X, round(x));
		if (!Double.isNaN(y))
			props.put(Y, round(y));
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);
		if (!Double.isNaN(x))
			je.encode(X, round(x));
		if (!Double.isNaN(y))
			je.encode(Y, round(y));
	}

	private static String round(double d) {
		return Math.round(d * 100.0) / 100.0 + "";
	}
}
