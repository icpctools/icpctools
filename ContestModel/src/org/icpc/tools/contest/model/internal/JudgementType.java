package org.icpc.tools.contest.model.internal;

import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IJudgementType;

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
	protected void getProperties(Properties props) {
		props.addLiteralString(ID, id);
		props.addString(NAME, name);
		props.add(PENALTY, penalty);
		props.add(SOLVED, solved);
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