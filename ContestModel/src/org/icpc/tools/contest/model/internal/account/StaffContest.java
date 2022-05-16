package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.internal.Account;
import org.icpc.tools.contest.model.internal.Contest;

/**
 * The only thing staff doesn't see are account passwords.
 */
public class StaffContest extends Contest {
	public StaffContest() {
		// do nothing
	}

	@Override
	public void add(IContestObject obj) {
		if (obj instanceof IDelete) {
			super.add(obj);
			return;
		}

		IContestObject.ContestType cType = obj.getType();

		if (cType == IContestObject.ContestType.ACCOUNT) {
			IAccount account = (IAccount) obj;
			if (account.getPassword() != null) {
				account = (IAccount) ((Account) account).clone();
				((Account) account).add("password", null);
			}
			super.add(account);
		} else
			super.add(obj);
	}
}