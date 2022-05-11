package org.icpc.tools.contest.model.internal;

import org.icpc.tools.contest.model.IStartStatus;

public class StartStatus extends ContestObject implements IStartStatus {
	private static final String LABEL = "label";
	private static final String STATUS = "status";

	private String label;
	private int status;

	public StartStatus() {
		// do nothing
	}

	public StartStatus(StartStatus c) {
		status = c.status;
		label = c.label;
	}

	public StartStatus(String label, int status) {
		this.id = label;
		this.label = label;
		this.status = status;
	}

	@Override
	public ContestType getType() {
		return ContestType.START_STATUS;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (LABEL.equals(name)) {
			label = (String) value;
			return true;
		} else if (STATUS.equals(name)) {
			status = parseInt(value);
			return true;
		}

		return false;
	}

	@Override
	protected void getProperties(Properties props) {
		props.addLiteralString(ID, id);
		props.addString(LABEL, label);
		props.addInt(STATUS, status);
	}
}