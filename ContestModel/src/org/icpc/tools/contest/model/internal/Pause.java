package org.icpc.tools.contest.model.internal;

import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IPause;
import org.icpc.tools.contest.model.feed.JSONEncoder;

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
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		props.put(START, start + "");
		if (end != 0)
			props.put(END, end + "");
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);
		je.encode(START, start + "");
		if (end != 0)
			je.encode(END, end + "");
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