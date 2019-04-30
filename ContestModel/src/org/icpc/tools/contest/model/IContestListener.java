package org.icpc.tools.contest.model;

public interface IContestListener {
	public static enum Delta {
		ADD, UPDATE, DELETE, NOOP
	}

	public void contestChanged(IContest contest, IContestObject obj, Delta delta);
}