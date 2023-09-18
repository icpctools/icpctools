package org.icpc.tools.contest.model.internal;

import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IPause;
import org.icpc.tools.contest.model.feed.Timestamp;

public class Pause extends ContestObject implements IPause {
	private static final String START = "start";
	private static final String END = "end";

	private Long start;
	private Long end;

	public Pause() {
		// create an empty pause
	}

	@Override
	public ContestType getType() {
		return ContestType.PAUSE;
	}

	@Override
	public Long getStart() {
		return start;
	}

	@Override
	public Long getEnd() {
		return end;
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (START.equals(name)) {
			try {
				start = parseTimestamp(value);
			} catch (Exception e) {
				// ignore
			}
			return true;
		} else if (END.equals(name)) {
			try {
				end = parseTimestamp(value);
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
		if (start != null)
			props.addLiteralString(START, Timestamp.format(start));
		if (end != null)
			props.addLiteralString(END, Timestamp.format(end));
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