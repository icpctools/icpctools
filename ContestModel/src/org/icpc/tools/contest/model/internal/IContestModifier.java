package org.icpc.tools.contest.model.internal;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;

public interface IContestModifier {
	public void notify(IContest contest, IContestObject obj);
}