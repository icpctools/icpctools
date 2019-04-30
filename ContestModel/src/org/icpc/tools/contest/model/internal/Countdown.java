package org.icpc.tools.contest.model.internal;

import java.util.Map;

import org.icpc.tools.contest.model.ICountdown;
import org.icpc.tools.contest.model.feed.JSONEncoder;

public class Countdown extends ContestObject implements ICountdown {
	private static final String STATUS = "status";
	private static final int STATUS_SIZE = 9;

	private boolean[] status = new boolean[STATUS_SIZE];

	public Countdown() {
		// create an empty countdown
	}

	public Countdown(Countdown c) {
		status = c.status;
	}

	@Override
	public ContestType getType() {
		return ContestType.COUNTDOWN;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public boolean[] getStatus() {
		return status;
	}

	public void setStatus(boolean[] s) {
		status = s;
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (STATUS.equals(name)) {
			try {
				for (int i = 0; i < STATUS_SIZE; i++) {
					status[i] = (((String) value).charAt(i) == 'Y');
				}
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
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < STATUS_SIZE; i++) {
			if (status[i])
				sb.append("Y");
			else
				sb.append("N");
		}
		props.put(STATUS, sb.toString());
	}

	@Override
	public void write(JSONEncoder je) {
		je.open();
		je.encode(ID, id);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < STATUS_SIZE; i++) {
			if (status[i])
				sb.append("Y");
			else
				sb.append("N");
		}
		je.encode(STATUS, sb.toString());
		je.close();
	}
}