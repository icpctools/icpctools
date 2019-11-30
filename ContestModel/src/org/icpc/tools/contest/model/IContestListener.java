package org.icpc.tools.contest.model;

public interface IContestListener {
	enum Delta {
		ADD, UPDATE, DELETE, NOOP
	}

	void contestChanged(IContest contest, IContestObject obj, Delta delta);
}