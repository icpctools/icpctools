package org.icpc.tools.contest.model.internal;

import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.feed.JSONEncoder;

public class JudgementType extends ContestObject implements IJudgementType {
	private static final String NAME = "name";
	private static final String PENALTY = "penalty";
	private static final String SOLVED = "solved";

	private String name;
	private boolean penalty;
	private boolean solved;

	@Override
	public ContestType getType() {
		return ContestType.JUDGEMENT_TYPE;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isPenalty() {
		return penalty;
	}

	@Override
	public boolean isSolved() {
		return solved;
	}

	@Override
	protected boolean addImpl(String name2, Object value) throws Exception {
		if (NAME.equals(name2)) {
			name = (String) value;
			return true;
		} else if (PENALTY.equals(name2)) {
			penalty = parseBoolean(value);
			return true;
		} else if (SOLVED.equals(name2)) {
			solved = parseBoolean(value);
			return true;
		}

		return false;
	}

	@Override
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		props.put(NAME, name);
		props.put(PENALTY, penalty);
		props.put(SOLVED, solved);
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);
		je.encode(NAME, name);
		je.encode(PENALTY, penalty);
		je.encode(SOLVED, solved);
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (name == null || name.isEmpty())
			errors.add("Name missing");

		if (errors.isEmpty())
			return null;
		return errors;
	}
}