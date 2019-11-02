package org.icpc.tools.contest.model.internal;

import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITestData;
import org.icpc.tools.contest.model.feed.JSONEncoder;

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
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		props.put(PROBLEM_ID, problemId);
		props.put(ORDINAL, ordinal);
		if (isSample != null)
			props.put(SAMPLE, isSample.booleanValue());
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);
		je.encode(PROBLEM_ID, problemId);
		je.encode(ORDINAL, ordinal);
		if (isSample != null)
			je.encode(SAMPLE, isSample.booleanValue());
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