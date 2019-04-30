package org.icpc.tools.contest.model.internal;

import org.icpc.tools.contest.model.Status;

public class Recent {
	public long time; // exact time in ms
	public Status status;

	public Recent(long time, Status status) {
		this.time = time;
		this.status = status;
	}
}