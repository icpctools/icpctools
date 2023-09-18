package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;

/**
 * Helper interface that marks filtered contests and provides an extra method to limit file
 * attachments (streams and downloads).
 */
public interface IFilteredContest extends IContest {

	public boolean allowFileReference(IContestObject obj, String attribute);
}