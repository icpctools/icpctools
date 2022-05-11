package org.icpc.tools.contest.model.internal;

import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IState;
import org.icpc.tools.contest.model.feed.Timestamp;

public class State extends ContestObject implements IState {
	private static final String STARTED = "started";
	private static final String ENDED = "ended";
	private static final String FROZEN = "frozen";
	private static final String THAWED = "thawed";
	private static final String FINALIZED = "finalized";
	private static final String END_OF_UPDATES = "end_of_updates";

	private Long started;
	private Long ended;
	private Long frozen;
	private Long thawed;
	private Long finalized;
	private Long endOfUpdates;
	private boolean hasEoU;

	@Override
	public ContestType getType() {
		return ContestType.STATE;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public Long getStarted() {
		return started;
	}

	@Override
	public Long getEnded() {
		return ended;
	}

	@Override
	public Long getFrozen() {
		return frozen;
	}

	@Override
	public Long getThawed() {
		return thawed;
	}

	@Override
	public Long getFinalized() {
		return finalized;
	}

	@Override
	public Long getEndOfUpdates() {
		return endOfUpdates;
	}

	public void setStarted(long time) {
		started = time;
	}

	public void setEnded(long time) {
		ended = time;
	}

	public void setFrozen(long time) {
		frozen = time;
	}

	public void setThawed(long time) {
		thawed = time;
	}

	public void setFinalized(long time) {
		finalized = time;
	}

	public void setEndOfUpdates(long time) {
		endOfUpdates = time;
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (STARTED.equals(name)) {
			started = parseTimestamp(value);
			return true;
		} else if (ENDED.equals(name)) {
			ended = parseTimestamp(value);
			return true;
		} else if (FROZEN.equals(name)) {
			frozen = parseTimestamp(value);
			return true;
		} else if (THAWED.equals(name)) {
			thawed = parseTimestamp(value);
			return true;
		} else if (FINALIZED.equals(name)) {
			finalized = parseTimestamp(value);
			return true;
		} else if (END_OF_UPDATES.equals(name)) {
			hasEoU = true;
			endOfUpdates = parseTimestamp(value);
			return true;
		}

		return false;
	}

	@Override
	protected void getProperties(Properties props) {
		// super.getPropertiesImpl(props);
		if (started != null)
			props.addLiteralString(STARTED, Timestamp.format(started));
		if (ended != null)
			props.addLiteralString(ENDED, Timestamp.format(ended));
		if (frozen != null)
			props.addLiteralString(FROZEN, Timestamp.format(frozen));
		if (thawed != null)
			props.addLiteralString(THAWED, Timestamp.format(thawed));
		if (finalized != null)
			props.addLiteralString(FINALIZED, Timestamp.format(finalized));
		if (endOfUpdates != null)
			props.addLiteralString(END_OF_UPDATES, Timestamp.format(endOfUpdates));
	}

	/*@Override
	public void writeBody(JSONEncoder je) {
		if (started != null)
			je.encodeString(STARTED, Timestamp.format(started));
		else
			je.encode(STARTED);
		if (ended != null)
			je.encodeString(ENDED, Timestamp.format(ended));
		else
			je.encode(ENDED);
		if (frozen != null)
			je.encodeString(FROZEN, Timestamp.format(frozen));
		if (thawed != null)
			je.encodeString(THAWED, Timestamp.format(thawed));
		if (finalized != null)
			je.encodeString(FINALIZED, Timestamp.format(finalized));
		else
			je.encode(FINALIZED);
		if (endOfUpdates != null)
			je.encodeString(END_OF_UPDATES, Timestamp.format(endOfUpdates));
		else
			je.encode(END_OF_UPDATES);
	}*/

	@Override
	public boolean isRunning() {
		return (started != null && ended == null);
	}

	@Override
	public boolean isFrozen() {
		return (frozen != null && thawed == null);
	}

	@Override
	public boolean isFinal() {
		return (finalized != null);
	}

	@Override
	public boolean isDoneUpdating() {
		// check for no support for EoU for 2018 contests
		return (endOfUpdates != null || (!hasEoU && finalized != null));
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (started == null
				&& (ended != null || frozen != null || thawed != null || finalized != null || endOfUpdates != null))
			errors.add("Never started but subsequent times set");

		if (ended != null && started != null && ended < started)
			errors.add("End before start");

		if (thawed != null && frozen == null)
			errors.add("Thawed but never frozen");

		if (thawed != null && frozen != null && thawed < frozen)
			errors.add("Thawed before frozen");

		// TODO check for more invalid possibilities

		if (errors.isEmpty())
			return null;
		return errors;
	}
}