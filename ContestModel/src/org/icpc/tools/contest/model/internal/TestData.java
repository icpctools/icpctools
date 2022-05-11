package org.icpc.tools.contest.model.internal;

import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITestData;

public class TestData extends ContestObject implements ITestData {
	private static final String PROBLEM_ID = "problem_id";
	private static final String ORDINAL = "ordinal";
	private static final String SAMPLE = "sample";

	private String problemId;
	private int ordinal;
	private Boolean isSample;

	@Override
	public ContestType getType() {
		return ContestType.TEST_DATA;
	}

	@Override
	public String getProblemId() {
		return problemId;
	}

	@Override
	public int getOrdinal() {
		return ordinal;
	}

	@Override
	public Boolean isSample() {
		return isSample;
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (PROBLEM_ID.equals(name)) {
			problemId = (String) value;
			return true;
		} else if (ORDINAL.equals(name)) {
			ordinal = parseInt(value);
			return true;
		} else if (SAMPLE.equals(name)) {
			isSample = parseBoolean(value);
			return true;
		}

		return false;
	}

	@Override
	protected void getProperties(Properties props) {
		props.addLiteralString(ID, id);
		props.addLiteralString(PROBLEM_ID, problemId);
		props.addInt(ORDINAL, ordinal);
		if (isSample != null)
			props.add(SAMPLE, isSample.booleanValue());
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (c.getProblemById(problemId) == null)
			errors.add("Missing problem");

		if (errors.isEmpty())
			return null;
		return errors;
	}
}