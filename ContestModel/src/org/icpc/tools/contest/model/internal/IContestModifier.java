package org.icpc.tools.contest.model.internal;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;

public interface IContestModifier {
	public boolean notify(IContest contest, IContestObject obj);
}