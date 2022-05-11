package org.icpc.tools.contest.model.internal;

import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.feed.RelativeTime;
import org.icpc.tools.contest.model.feed.Timestamp;

public abstract class TimedEvent extends ContestObject {
	private static final String CONTEST_TIME = "contest_time";
	private static final String TIME = "time";

	protected int contestTime = Integer.MIN_VALUE;
	protected long time = Long.MIN_VALUE;

	public TimedEvent() {
		super();
	}

	public TimedEvent(String id) {
		super(id);
	}

	public int getContestTime() {
		return contestTime;
	}

	public long getTime() {
		return time;
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (CONTEST_TIME.equals(name)) {
			contestTime = parseRelativeTime(value);
			return true;
		} else if (TIME.equals(name)) {
			time = parseTimestamp(value);
			return true;
		}
		return false;
	}

	@Override
	protected void getProperties(Properties props) {
		if (contestTime != Integer.MIN_VALUE)
			props.addLiteralString(CONTEST_TIME, RelativeTime.format(contestTime));
		if (time != Long.MIN_VALUE)
			props.addLiteralString(TIME, Timestamp.format(time));
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (contestTime == Integer.MIN_VALUE)
			errors.add("Invalid contest time " + contestTime);

		if (time == Long.MIN_VALUE)
			errors.add("Invalid time " + time);

		return errors;
	}
}