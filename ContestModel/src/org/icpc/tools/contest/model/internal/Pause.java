package org.icpc.tools.contest.model.internal;

import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IPause;

public class Pause extends ContestObject implements IPause {
	private static final String START = "start";
	private static final String END = "end";

	private long start;
	private long end;

	public Pause() {
		// create an empty pause
	}

	@Override
	public ContestType getType() {
		return ContestType.PAUSE;
	}

	@Override
	public long getStart() {
		return start;
	}

	@Override
	public long getEnd() {
		return end;
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (START.equals(name)) {
			try {
				start = Long.parseLong((String) value);
			} catch (Exception e) {
				// ignore
			}
			return true;
		} else if (END.equals(name)) {
			try {
				end = Long.parseLong((String) value);
			} catch (Exception e) {
				// ignore
			}
			return true;
		}

		return false;
	}

	@Override
	protected void getProperties(Properties props) {
		props.addLiteralString(ID, id);
		props.addLiteralString(START, start + "");
		if (end != 0)
			props.addLiteralString(END, end + "");
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (start <= 0)
			errors.add("Invalid start time");

		if (end < 0)
			errors.add("Invalid end time");

		if (errors.isEmpty())
			return null;
		return errors;
	}
}