package org.icpc.tools.contest.model.internal;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Arrays;
import java.util.Comparator;
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
		Integer[] sortedOrder = new Integer[order.length];
		for (int i = 0; i < order.length; i++)
			sortedOrder[i] = order[i];

		boolean passFail = contest.getScoreboardType() == ScoreboardType.PASS_FAIL;
		Comparator<String> nameComparator = Comparator.nullsLast(collator);
		Arrays.sort(sortedOrder, (i, j) -> {
			IStanding si = standings[i];
			IStanding sj = standings[j];
			int comparison;
			if (passFail) {
				comparison = Integer.compare(sj.getNumSolved(), si.getNumSolved());
				if (comparison != 0)
					return comparison;
				comparison = Long.compare(si.getTime(), sj.getTime());
				if (comparison != 0)
					return comparison;
			} else {
				comparison = si.getScore() == sj.getScore() ? 0 : Double.compare(sj.getScore(), si.getScore());
				if (comparison != 0)
					return comparison;
				// Teams with a scoring submission come before teams with no submission time.
				comparison = Boolean.compare(si.getLastSolutionTime() < 0, sj.getLastSolutionTime() < 0);
				if (comparison != 0)
					return comparison;
				// Future: some contests also use penalty as a tiebreaker, but there's no way
				// to know this from the Contest API (yet)
			}

			comparison = Long.compare(si.getLastSolutionTime(), sj.getLastSolutionTime());
			if (comparison != 0)
				return comparison;
			return nameComparator.compare(teams[i].getActualDisplayName(), teams[j].getActualDisplayName());
		});

		for (int i = 0; i < order.length; i++)
			order[i] = sortedOrder[i];
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

}
