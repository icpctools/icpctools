package org.icpc.tools.contest.model.internal.account;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.internal.Contest;

public class AccountHelper {
	/**
	 * Create a contest that filters accounting to the given account (primarily the account type).
	 *
	 * @param account an account
	 * @return a contest that is filtered according to what the given account can see.
	 */
	public static Contest createAccountContest(IAccount account) {
		if (account == null || account.getAccountType() == null)
			return new PublicContest();

		switch (account.getAccountType()) {
			case IAccount.ADMIN:
				return new Contest();
			case IAccount.STAFF:
			case IAccount.JUDGE:
				return new StaffContest();
			case IAccount.ANALYST:
				return new AnalystContest(account);
			case IAccount.TEAM:
				return new TeamContest(account);
			case IAccount.BALLOON:
				return new BalloonContest(account);
			case IAccount.SPECTATOR:
			case IAccount.PRES_ADMIN:
				return new SpectatorContest(account);
			default:
				return new PublicContest();
		}
	}
}