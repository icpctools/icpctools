package org.icpc.tools.contest.model.internal;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Locale;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContest.ScoreboardType;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ITeam;

public class Ranking {
	private static Collator collator;

	private Ranking() {
		// do not call
	}

	static {
		// Java ignores spaces by default in collation rules, so we need to add a rule for it
		RuleBasedCollator englishCollator = (RuleBasedCollator) Collator.getInstance(Locale.US);
		String rules = englishCollator.getRules();
		try {
			collator = new RuleBasedCollator(rules.replaceAll("<'_'", "<' '<'_'"));
		} catch (ParseException e) {
			collator = englishCollator;
		}
	}

	protected static void rankIt(IContest contest, ITeam[] teams, IStanding[] standings, int[] order) {
		sort(contest, teams, standings, order);
		rank(contest, standings, teams, order);
	}

	/**
	 * Sort the teams based on their current standing.
	 */
	private static void sort(IContest contest, ITeam[] teams, IStanding[] standings, int[] order) {
		int numTeams = order.length;

		boolean passFail = contest.getScoreboardType() == ScoreboardType.PASS_FAIL;
		for (int i = 0; i < numTeams - 1; i++) {
			for (int j = i + 1; j < numTeams; j++) {
				boolean swap = false;
				IStanding si = standings[order[i]];
				IStanding sj = standings[order[j]];
				if (passFail) {
					if (si.getNumSolved() < sj.getNumSolved())
						swap = true;
					else if (si.getNumSolved() == sj.getNumSolved()) {
						if (si.getTime() > sj.getTime())
							swap = true;
						else if (si.getTime() == sj.getTime()) {
							if (si.getLastSolutionTime() > sj.getLastSolutionTime())
								swap = true;
							else if (si.getLastSolutionTime() == sj.getLastSolutionTime()) {
								String tin = teams[order[i]].getActualDisplayName();
								String tjn = teams[order[j]].getActualDisplayName();
								if (tin != null && tjn != null && collator.compare(tin, tjn) > 0)
									swap = true;
							}
						}
					}
				} else {
					if (si.getScore() < sj.getScore())
						swap = true;
					else if (si.getScore() == sj.getScore()) {
						if ((sj.getLastSolutionTime() >= 0 && si.getLastSolutionTime() < 0)
								|| (sj.getLastSolutionTime() >= 0 && si.getLastSolutionTime() > sj.getLastSolutionTime())) {
							swap = true;
						} else if (si.getLastSolutionTime() == sj.getLastSolutionTime()) {
							String tin = teams[order[i]].getActualDisplayName();
							String tjn = teams[order[j]].getActualDisplayName();
							if (tin != null && tjn != null && collator.compare(tin, tjn) > 0)
								swap = true;
						}
					}
					// Future: some contests also use penalty as a tiebreaker, but there's no way
					// to know this from the Contest API (yet)
				}

				if (swap)
					swapOrder(order, i, j);
			}
		}
	}

	/**
	 * Rank all teams using ICPC rules.
	 */
	private static void rank(IContest contest, IStanding[] standings, ITeam[] teams, int[] order) {
		int numTeams = order.length;
		if (numTeams == 0)
			return;

		if (contest.getScoreboardType() == ScoreboardType.SCORE) {
			IStanding lastStanding = null;
			for (int i = 0; i < numTeams; i++) {
				Standing standing = (Standing) standings[order[i]];
				if (lastStanding != null && standing.getScore() == lastStanding.getScore()
						&& standing.getLastSolutionTime() == lastStanding.getLastSolutionTime()) {
					standing.setRank(lastStanding.getRank());
				} else {
					standing.setRank(i + 1);
					lastStanding = standing;
				}
			}

			return;
		}

		int n = 0;
		IStanding standingN = standings[order[n]];
		while (n < numTeams) {
			int next = n + 1;
			IStanding standingNext = null;
			if (next < numTeams)
				standingNext = standings[order[next]];

			// find a group
			// round last solution time to minutes for ICPC rules?
			while (next < numTeams && ((standingN.getNumSolved() == standingNext.getNumSolved()
					&& standingN.getTime() == standingNext.getTime()
					&& standingN.getLastSolutionTime() == standingNext.getLastSolutionTime()))) {
				next++;
				if (next < numTeams)
					standingNext = standings[order[next]];
			}

			for (int i = n; i < next; i++) {
				Standing standingI = (Standing) standings[order[i]];
				standingI.setRank(n + 1);
			}

			n = next;

			if (n < numTeams)
				standingN = standings[order[n]];
		}
	}

	/**
	 * Swap two array elements.
	 *
	 * @param ob an int array
	 * @param i an index
	 * @param j an index
	 */
	private static void swapOrder(int[] order, int i, int j) {
		int t = order[i];
		order[i] = order[j];
		order[j] = t;
	}
}
