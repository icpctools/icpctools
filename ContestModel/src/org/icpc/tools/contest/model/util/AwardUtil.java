package org.icpc.tools.contest.model.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IAward.AwardType;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Award;
import org.icpc.tools.contest.model.internal.Contest;

public class AwardUtil {
	private static final String[][] GROUPS = new String[][] { { "Asia", "Asian Champions" },
			{ "North America", "North American Champions" }, { "South America", "South American Champions" },
			{ "Europe", "European Champions" }, { "Africa", "African Champions" },
			{ "Africa and the Middle East", "Africa and Middle East Champions" },
			{ "Latin America", "Latin American Champions" }, { "South Pacific", "South Pacific Champions" } };

	protected static String FIRST_PLACE_CITATION = Messages.getString("awardWorldChampion");
	private static final String[] MEDAL_NAMES = new String[] { Messages.getString("awardMedalGold"),
			Messages.getString("awardMedalSilver"), Messages.getString("awardMedalBronze") };

	public static String getAwardTypeNames(List<AwardType> types) {
		List<String> names = new ArrayList<>();
		for (AwardType type : types)
			names.add(type.getName());

		return String.join(", ", names);
	}

	public static String getAwardTypeNames(AwardType[] types) {
		List<String> names = new ArrayList<>();
		for (AwardType type : types)
			names.add(type.getName());

		return String.join(", ", names);
	}

	public static void createFirstToSolveAwards(Contest contest, boolean showBeforeFreeze, boolean showAfterFreeze) {
		if (contest.getNumTeams() == 0)
			return;

		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission s : submissions) {
			if (contest.isSolved(s) && contest.isFirstToSolve(s)) {
				ITeam team = contest.getTeamById(s.getTeamId());
				IProblem p = contest.getProblemById(s.getProblemId());

				boolean beforeFreeze = false;
				boolean afterFreeze = false;

				if (contest.getFreezeDuration() > 0) {
					int freezeMin = (contest.getDuration() - contest.getFreezeDuration()) / 60000;
					if (ContestUtil.getTimeInMin(s.getContestTime()) < freezeMin)
						beforeFreeze = true;
					else
						afterFreeze = true;
				} else
					beforeFreeze = true;

				boolean show = false;
				if ((beforeFreeze && showBeforeFreeze) || (afterFreeze && showAfterFreeze))
					show = true;

				String citation = Messages.getString("awardFTS").replace("{0}", p.getLabel());
				contest.add(
						new Award(IAward.FIRST_TO_SOLVE, s.getProblemId(), new String[] { team.getId() }, citation, show));
			}
		}
	}

	public static void createGroupAwards(Contest contest, int numPerGroup) {
		for (IGroup group : contest.getGroups()) {
			if (group.isHidden())
				continue;

			int count = 0;
			String lastRank = null;
			List<String> teamIds = new ArrayList<>();
			for (ITeam team : contest.getOrderedTeams()) {
				String[] groupIds = team.getGroupIds();
				boolean teamInGroup = false;
				for (String groupId : groupIds)
					if (groupId.equals(group.getId()))
						teamInGroup = true;

				if (teamInGroup) {
					String rank = contest.getStanding(team).getRank();
					if (count <= numPerGroup && rank.equals(lastRank)) {
						teamIds.add(team.getId());
					} else if (count < numPerGroup) {
						teamIds.add(team.getId());
						count++;
					}
					lastRank = rank;
				}
			}

			if (!teamIds.isEmpty())
				contest.add(new Award(IAward.GROUP, group.getId(), teamIds.toArray(new String[teamIds.size()]),
						getGroupCitation(contest, group.getName(), 1), true));
		}
	}

	public static void createGroupHighlightAwards(Contest contest, String groupId2, int numToHighlight, String citation,
			boolean show) {
		IGroup group = contest.getGroupById(groupId2);
		if (group == null)
			return;

		List<String> teamIds = new ArrayList<>();

		for (ITeam team : contest.getOrderedTeams()) {
			String[] groupIds = team.getGroupIds();
			if (groupIds != null) {
				for (String groupId : groupIds) {
					if (groupId.equals(groupId2)) {
						if (teamIds.size() < numToHighlight)
							teamIds.add(team.getId());
					}
				}
			}
		}

		contest.add(
				new Award(IAward.GROUP_HIGHLIGHT, groupId2, teamIds.toArray(new String[teamIds.size()]), citation, show));
	}

	public static void createFirstPlaceAward(Contest contest, String citation) {
		// construct an award for the champion (winner)
		ITeam[] teams = contest.getOrderedTeams();
		if (teams.length == 0)
			return;

		ITeam team = teams[0];
		contest.add(new Award(IAward.WINNER, team.getId(), citation, true));
	}

	public static void createRankAwards(Contest contest, int num) {
		if (num < 1)
			return;

		ITeam[] teams = contest.getOrderedTeams();
		if (teams.length == 0)
			return;
		int n = Math.min(num, teams.length);

		for (int i = 0; i < n; i++) {
			ITeam team = teams[i];
			IStanding standing = contest.getStanding(team);
			try {
				int rank = Integer.parseInt(standing.getRank());
				contest.add(new Award(IAward.RANK, i + 1, team.getId(),
						Messages.getString("awardPlace").replace("{0}", getPlaceString(rank)), true));
			} catch (Exception e) {
				contest.add(new Award(IAward.RANK, i + 1, team.getId(),
						Messages.getString("awardPlace").replace("{0}", standing.getRank()), true));
			}
		}
	}

	public static String getPlaceString(int i) {
		String[] sufixes = new String[] { Messages.getString("0"), Messages.getString("1"), Messages.getString("2"),
				Messages.getString("3"), Messages.getString("4"), Messages.getString("5"), Messages.getString("6"),
				Messages.getString("7"), Messages.getString("8"), Messages.getString("9") };
		switch (i % 100) {
			case 11:
			case 12:
			case 13:
				return i + Messages.getString("11to13");
			default:
				return i + sufixes[i % 10];
		}
	}

	public static void createMedalAwards(Contest contest, int numGold, int numSilver, int numBronze) {
		ITeam[] teams = contest.getOrderedTeams();
		if (teams.length == 0)
			return;

		int numGold2 = numGold;
		int numSilver2 = numSilver;
		int numBronze2 = numBronze;

		if (numGold2 > teams.length)
			numGold2 = teams.length;
		if (numGold2 + numSilver2 > teams.length)
			numSilver2 = teams.length - numGold2;
		if (numGold2 + numSilver2 + numBronze2 > teams.length)
			numBronze2 = teams.length - numGold2 - numSilver2;

		int nextAwardNum = 0;
		if (numGold2 > 0) {
			String[] teamIds = new String[numGold2];
			int count = 0;
			while (nextAwardNum < numGold2 && nextAwardNum < teams.length)
				teamIds[count++] = teams[nextAwardNum++].getId();
			contest.add(new Award(IAward.MEDAL, "gold", teamIds, MEDAL_NAMES[0], true));
		}

		// silver medals
		if (numSilver2 > 0) {
			String[] teamIds = new String[numSilver2];
			int count = 0;
			while (nextAwardNum < numGold2 + numSilver2 && nextAwardNum < teams.length)
				teamIds[count++] = teams[nextAwardNum++].getId();
			contest.add(new Award(IAward.MEDAL, "silver", teamIds, MEDAL_NAMES[1], true));
		}

		// bronze medals
		if (numBronze2 > 0) {
			String[] teamIds = new String[numBronze2];
			int count = 0;
			while (nextAwardNum < numGold2 + numSilver2 + numBronze2 && nextAwardNum < teams.length)
				teamIds[count++] = teams[nextAwardNum++].getId();
			contest.add(new Award(IAward.MEDAL, "bronze", teamIds, MEDAL_NAMES[2], true));
		}
	}

	public static int getLastBronze(IContest contest) {
		int lastBronze = Math.min(12, contest.getNumTeams());
		IAward[] awards = contest.getAwards();
		if (awards != null) {
			for (IAward a : awards)
				if (a.getAwardType() == IAward.MEDAL && a.getId().contains("bronze")) {
					lastBronze = 0;
					String[] teamIds = a.getTeamIds();
					for (String tId : teamIds) {
						lastBronze = Math.max(lastBronze, contest.getOrderOf(contest.getTeamById(tId)) + 1);
					}
				}
		}
		return lastBronze;
	}

	public static int[] getMedalCounts(IContest contest) {
		int[] num = new int[3];
		IAward[] awards = contest.getAwards();
		if (awards != null) {
			for (IAward a : awards)
				if (a.getAwardType() == IAward.MEDAL) {
					if (a.getId().contains("gold"))
						num[0] = a.getTeamIds().length;
					else if (a.getId().contains("silver"))
						num[1] = a.getTeamIds().length;
					else if (a.getId().contains("bronze"))
						num[2] = a.getTeamIds().length;
				}
		}
		return num;
	}

	protected static void createSolutionAwards(Contest contest) {
		ITeam[] teams = contest.getOrderedTeams();
		if (teams.length == 0)
			return;

		int lastBronze = getLastBronze(contest);

		for (int i = 0; i < lastBronze; i++) {
			ITeam team = teams[i];
			IStanding s = contest.getStanding(team);
			if (s.getNumSolved() == 1)
				contest.add(new Award(IAward.SOLUTION, i + 1, team.getId(), Messages.getString("awardSolvedOne"), false));
			else if (s.getNumSolved() > 1)
				contest.add(new Award(IAward.SOLUTION, i + 1, team.getId(),
						Messages.getString("awardSolvedMultiple").replace("{0}", s.getNumSolved() + ""), false));
		}
	}

	public static void createDefaultAwards(Contest contest) {
		createWorldFinalsAwards(contest);
	}

	public static void createWorldFinalsAwards(Contest contest) {
		createWorldFinalsAwards(contest, 0);
	}

	public static void createWorldFinalsAwards(Contest contest, int b) {
		createFirstPlaceAward(contest, FIRST_PLACE_CITATION);

		int numTeams = contest.getNumTeams();
		int gold = Math.min(4, numTeams);
		int silver = Math.min(4, Math.max(numTeams - 4, 0));
		int bronze = Math.min(4 + b, Math.max(numTeams - 8, 0));
		createMedalAwards(contest, gold, silver, bronze);

		createSolutionAwards(contest);

		createFirstToSolveAwards(contest, false, true);

		createGroupAwards(contest, 1);
	}

	public static void sortAwards(IContest contest, IAward[] awards) {
		int size = awards.length;
		for (int i = 0; i < size; i++) {
			for (int j = i + 1; j < size; j++) {
				IAward ai = awards[i];
				IAward aj = awards[j];

				int indi;
				for (indi = 0; indi < IAward.KNOWN_TYPES.length; indi++)
					if (IAward.KNOWN_TYPES[indi] == ai.getAwardType())
						break;

				int indj;
				for (indj = 0; indj < IAward.KNOWN_TYPES.length; indj++)
					if (IAward.KNOWN_TYPES[indj] == ai.getAwardType())
						break;

				if (indi > indj) {
					awards[i] = aj;
					awards[j] = ai;
				}
			}
		}
	}

	public static void printAwards(IContest contest) {
		Trace.trace(Trace.USER, "--- Awards ---");
		Trace.trace(Trace.USER, "");

		IAward[] awards = contest.getAwards();
		AwardUtil.sortAwards(contest, awards);
		Map<String, List<IAward>> map = new HashMap<>();
		for (IAward award : awards) {
			String[] teamIds = award.getTeamIds();
			for (String teamId : teamIds) {
				List<IAward> aw = map.get(teamId);
				if (aw == null) {
					aw = new ArrayList<>();
					map.put(teamId, aw);
				}
				aw.add(award);
			}
		}

		ITeam[] teams = contest.getOrderedTeams();
		int size = teams.length;
		for (int i = size - 1; i >= 0; i--) {
			List<IAward> teamAwards = map.get(teams[i].getId());
			if (teamAwards != null) {
				Trace.trace(Trace.USER, getFullCitation(contest, teams[i].getId(), teamAwards));
				Trace.trace(Trace.USER, "");
			}
		}
	}

	private static String getFullCitation(IContest contest, String teamId, List<IAward> awards) {
		StringBuilder sb = new StringBuilder();

		boolean hasMedal = false;
		for (IAward award : awards) {
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(award.getCitation());
			if (award.getAwardType() == IAward.MEDAL)
				hasMedal = true;
		}

		ITeam team = contest.getTeamById(teamId);
		IStanding standing = contest.getStanding(team);
		if (hasMedal) {
			String s = Messages.getString("awardSolving").replace("{0}", sb.toString())
					.replace("{1}", standing.getNumSolved() + "").replace("{2}", standing.getTime() + "");
			sb = new StringBuilder(s);
		}

		sb.append("\n\t");
		sb.append(standing.getRank() + ": " + team.getName());

		return sb.toString();
	}

	/**
	 *
	 * @param contest
	 * @param group
	 * @param ind is 1st, 2nd, 3rd place in group award
	 * @return
	 */
	private static String getGroupCitation(IContest contest, String groupName, int ind) {
		for (String[] s : GROUPS) {
			if (s[0].equals(groupName)) {
				if (ind == 1)
					return s[1];
				return getPlaceString(ind) + " " + s[1];
			}
		}
		if (ind == 1)
			return Messages.getString("awardChampions").replace("{0}", groupName);
		return getPlaceString(ind) + " " + groupName
				+ Messages.getString("awardChampions2").replace("{0}", groupName).replace("{1}", getPlaceString(ind));
	}
}