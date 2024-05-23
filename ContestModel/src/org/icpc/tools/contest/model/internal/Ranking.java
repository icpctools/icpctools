package org.icpc.tools.contest.model.internal;

import java.text.Collator;
import java.util.Locale;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContest.ScoreboardType;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ITeam;

public class Ranking {
	private static final Collator collator = Collator.getInstance(Locale.US);

	public enum Scoring {
		LIVE, // scoring applied during a contest, no groups and full details shown
		UNOFFICIAL, // unofficial results, with grouping by score and honorable mention applied
		OFFICIAL // official results, sorted alphabetically
	}

	private Ranking() {
		// do not call
	}

	protected static void rankIt(IContest contest, ITeam[] teams, IStanding[] standings, int[] order) {
		rankIt(contest, teams, standings, Scoring.LIVE, order, 12);
	}

	protected static void rankIt(IContest contest, ITeam[] teams, IStanding[] standings, Scoring scoring, int[] order,
			int lastBronze) {
		sort(contest, teams, standings, order);
		if (scoring == Scoring.OFFICIAL)
			sortOfficial(contest, standings, teams, order, scoring, lastBronze);
		rank(contest, standings, teams, order, scoring, lastBronze);
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
						if (si.getLastSolutionTime() > sj.getLastSolutionTime())
							swap = true;
						else if (si.getLastSolutionTime() == sj.getLastSolutionTime()) {
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
	private static void rank(IContest contest, IStanding[] standings, ITeam[] teams, int[] order, Scoring scoring,
			int lastBronze) {
		int numTeams = order.length;
		if (numTeams == 0)
			return;

		if (contest.getScoreboardType() == ScoreboardType.SCORE) {
			IStanding lastStanding = null;
			for (int i = 0; i < numTeams; i++) {
				Standing standing = (Standing) standings[order[i]];
				if (lastStanding != null && standing.getScore() == lastStanding.getScore() && standing.getLastSolutionTime() == lastStanding.getLastSolutionTime()) {
					standing.setRank(lastStanding.getRank());
				} else {
					standing.setRank((i + 1) + "");
					lastStanding = standing;
				}
			}

			return;
		}

		int median = median(standings, teams, order);

		int n = 0;
		IStanding standingN = standings[order[n]];
		while (n < numTeams) {
			int next = n + 1;
			IStanding standingNext = null;
			if (next < numTeams)
				standingNext = standings[order[next]];

			// find a group
			while (next < numTeams && ((standingN.getNumSolved() == standingNext.getNumSolved()
					&& standingN.getTime() == standingNext.getTime()
					&& standingN.getLastSolutionTime() == standingNext.getLastSolutionTime())
					|| (scoring != Scoring.LIVE && next > lastBronze
							&& standingN.getNumSolved() == standingNext.getNumSolved())
					|| (scoring != Scoring.LIVE && standingNext.getNumSolved() < median
							&& standingN.getNumSolved() < median))) {
				next++;
				if (next < numTeams)
					standingNext = standings[order[next]];
			}

			for (int i = n; i < next; i++) {
				Standing standingI = (Standing) standings[order[i]];

				if (scoring != Scoring.LIVE && standingI.getNumSolved() < median) {
					standingI.setRank("H");
					standingI.setNumSolved(-1);
				} else
					standingI.setRank((n + 1) + "");

				if (scoring != Scoring.LIVE && i > lastBronze - 1
						&& standingI.getNumSolved() < standings[order[lastBronze - 1]].getNumSolved())
					standingI.setPenalty(-1);
			}

			n = next;

			if (n < numTeams)
				standingN = standings[order[n]];
		}
	}

	private static void sortOfficial(IContest contest, IStanding[] standings, ITeam[] teams, int[] order,
			Scoring scoring, int lastBronze) {
		int numTeams = order.length;
		int median = median(standings, teams, order);

		if (numTeams == 0)
			return;

		int n = 0;
		IStanding standingN = standings[order[n]];
		while (n < numTeams) {
			int next = n + 1;
			IStanding standingNext = null;
			if (next < numTeams)
				standingNext = standings[order[next]];

			// find a group
			while (next < numTeams && ((standingN.getNumSolved() == standingNext.getNumSolved()
					&& standingN.getTime() == standingNext.getTime()
					&& standingN.getLastSolutionTime() == standingNext.getLastSolutionTime())
					|| (scoring != Scoring.LIVE && next > lastBronze
							&& standingN.getNumSolved() == standingNext.getNumSolved())
					|| (scoring != Scoring.LIVE && standingNext.getNumSolved() < median
							&& standingN.getNumSolved() < median))) {
				next++;
				if (next < numTeams)
					standingNext = standings[order[next]];
			}

			// sort alpha-numerically within group
			for (int i = n; i < next - 1; i++) {
				for (int j = i + 1; j < next; j++) {
					IOrganization org1 = contest.getOrganizationById(teams[order[i]].getOrganizationId());
					IOrganization org2 = contest.getOrganizationById(teams[order[j]].getOrganizationId());
					if (org1 != null && org2 != null
							&& collator.compare(org1.getActualFormalName(), org2.getActualFormalName()) > 0) {
						swapOrder(order, i, j);
					}
				}
			}

			n = next;

			if (n < numTeams)
				standingN = standings[order[n]];
		}
	}

	/**
	 * Returns the median score assuming the teams are already sorted.
	 *
	 * @return
	 */
	private static int median(IStanding[] standings, ITeam[] teams, int[] order) {
		int numTeams = order.length;
		if (numTeams == 0)
			return 0;

		int teamIndex;
		if (numTeams % 2 == 0)
			teamIndex = order[numTeams / 2 - 1];
		else
			teamIndex = order[numTeams / 2];
		return standings[teamIndex].getNumSolved();
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
