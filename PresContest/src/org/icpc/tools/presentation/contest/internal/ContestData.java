package org.icpc.tools.presentation.contest.internal;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.internal.BufferedContest;
import org.icpc.tools.contest.model.internal.Contest;

public class ContestData {
	private static IContest contest;

	public static IContest getContest() {
		if (contest != null)
			return contest;

		synchronized (ContestData.class) {
			if (contest == null)
				init();
		}
		return contest;
	}

	private static void init() {
		ContestSource source = ContestSource.getInstance();
		BufferedContest bufferedContest = new BufferedContest();
		source.setInitialContest(bufferedContest);
		Contest tempContest = source.getContest();
		source.waitForContestLoad();
		int count = 0;
		while (count < 6 && bufferedContest.isBuffering()) {
			count++;
			try {
				Thread.sleep(200);
			} catch (Exception e) {
				// ignore
			}
		}
		contest = tempContest;
	}
}