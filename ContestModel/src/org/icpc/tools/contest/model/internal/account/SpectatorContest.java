package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;

/**
 * Filter that adds things spectators can see compared to public:
 * <ul>
 * <li>Commentary</li>
 * </ul>
 */
public class SpectatorContest extends PublicContest {
	public SpectatorContest(IAccount account) {
		super();
		username = account.getUsername();
	}

	@Override
	public void add(IContestObject obj) {
		if (obj.getType() == ContestType.COMMENTARY) {
			addIt(obj);
			return;
		}

		super.add(obj);
	}
}