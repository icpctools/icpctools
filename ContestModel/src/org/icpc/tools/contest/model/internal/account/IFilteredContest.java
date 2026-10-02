package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;

/**
 * Helper interface that marks filtered contests and provides an extra method to limit file
 * attachments (streams and downloads).
 */
public interface IFilteredContest extends IContest {

	/**
	 * Return true if a property is allowed/accessible on this specific object: "Can I see this
	 * property on this object now"?
	 *
	 * @param obj
	 * @param property
	 * @return
	 */
	public boolean allowProperty(IContestObject obj, String property);

	/**
	 * Return true if a property is allowed/accessible on the type at some point in time: "Can I
	 * ever see this property on some object of this type?"
	 *
	 * @param type
	 * @param property
	 * @return
	 */
	public boolean canAccessProperty(IContestObject.ContestType type, String property);
}