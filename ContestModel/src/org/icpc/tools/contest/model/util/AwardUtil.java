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
			{ "Latin America", "Latin American Champions" } };

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

	private static void assignGroup(Contest contest, Award a, int numPerGroup) {
		// assign award to one group
		String awardGroupId = a.getId().substring(13);
		a.setTeamIds(new String[0]);
		// int numPerGroup = Integer.parseInt(a.getCount());

		int count = 0;
		String lastRank = null;
		List<String> teamIds = new ArrayList<>();
		for (ITeam team : contest.getOrderedTeams()) {
			if (contest.getStanding(team).getNumSolved() == 0)
				break;

			String[] groupIds = team.getGroupIds();
			boolean teamInGroup = false;
			for (String groupId : groupIds)
				if (groupId.equals(awardGroupId)) {
					teamInGroup = true;
					break;
				}

			if (teamInGroup) {
				String rank = contest.getStanding(team).getRank();
				if (count <= numPerGroup && rank.equals(lastRank)) {
					teamIds.add(team.getId());
				} else if (count < numPerGroup) {
					teamIds.add(team.getId());
					lastRank = rank;
					count++;
				}
			}
		}

		if (!teamIds.isEmpty()) {
			IGroup group = contest.getGroupById(awardGroupId);
			a.setTeamIds(teamIds.toArray(new String[0]));
			if (a.getCitation() == null)
				a.setCitation(getGroupCitation(contest, group.getName(), 1));
		}
	}

	public static void createGroupAwards(Contest contest, int numPerGroup) {
		Award group = new Award(IAward.GROUP, "*", null, null, true);
		group.setParameter(numPerGroup + "");
		createGroupAwards(contest, group);
	}

	public static void createGroupAwards(Contest contest, IAward template) {
		int numPerGroup = 1;
		try {
			if (template.getParameter() != null)
				numPerGroup = Integer.parseInt(template.getParameter());

			if (numPerGroup < 1)
				throw new IllegalArgumentException("Cannot assign group awards to less than one team");
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not parse group parameter: " + template.getParameter());
		}

		if (template.getId().equals("group-winner-*")) {
			// to all groups
			for (IGroup group : contest.getGroups()) {
				if (group.isHidden())
					continue;

				Award groupAward = new Award(IAward.GROUP, group.getId(), null, null, true);
				// groupAward.setCount("1");
				contest.add(groupAward);
				assignGroup(contest, groupAward, numPerGroup);
			}
		} else {
			assignGroup(contest, (Award) template, numPerGroup);
		}
	}

	private static void assignFirstToSolve(Contest contest, Award a) {
		String problemId = a.getId().substring(15);
		a.setTeamIds(new String[0]);
		boolean showBeforeFreeze = true;
		boolean showAfterFreeze = true;
		// assign award to one problem
		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission s : submissions) {
			if (problemId.equals(s.getProblemId()) && contest.isSolved(s) && contest.isFirstToSolve(s)) {
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

				a.setTeamIds(new String[] { team.getId() });
				if (a.getCitation() == null)
					a.setCitation(Messages.getString("awardFTS").replace("{0}", p.getLabel()));
				a.setShowAward(show);
			}
		}
	}

	public static void createFirstToSolveAwards(Contest contest, IAward template) {
		if (template.getId().equals("first-to-solve-*")) {
			for (IProblem problem : contest.getProblems()) {
				Award ftsAward = new Award(IAward.FIRST_TO_SOLVE, problem.getId(), null, null, true);
				contest.add(ftsAward);
				assignFirstToSolve(contest, ftsAward);
			}
		} else {
			assignFirstToSolve(contest, (Award) template);
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

		contest.add(new Award(IAward.GROUP_HIGHLIGHT, groupId2, teamIds.toArray(new String[0]), citation, show));
	}

	public static void createFirstPlaceAward(Contest contest, String citation) {
		// construct an award for the champion (winner)
		ITeam[] teams = contest.getOrderedTeams();
		if (teams.length == 0)
			return;

		ITeam team = teams[0];
		contest.add(new Award(IAward.WINNER, team.getId(), citation, true));
	}

	public static void createWinnerAward(Contest contest, IAward awardTemplate) {
		ITeam[] teams = contest.getOrderedTeams();
		if (teams.length == 0)
			return;

		String citation = awardTemplate.getCitation();
		if (citation == null)
			citation = Messages.getString("awardWorldChampion");
		contest.add(new Award(IAward.WINNER, teams[0].getId(), citation, true));
	}

	public static void createRankAwards(Contest contest, int num) {
		Award rank = new Award(IAward.RANK, "*", null, true);
		rank.setParameter(num + "");
		createRankAwards(contest, rank);
	}

	public static void createRankAwards(Contest contest, IAward template) {
		int numTeams = 0;
		try {
			numTeams = Integer.parseInt(template.getParameter());
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not parse rank parameter: " + template.getParameter());
		}

		if (numTeams < 1)
			return;

		ITeam[] teams = contest.getOrderedTeams();
		if (teams.length == 0)
			return;
		int n = Math.min(numTeams, teams.length);

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
			contest.add(new Award(IAward.MEDAL, "gold", teamIds, Messages.getString("awardMedalGold"), true));
		}

		// silver medals
		if (numSilver2 > 0) {
			String[] teamIds = new String[numSilver2];
			int count = 0;
			while (nextAwardNum < numGold2 + numSilver2 && nextAwardNum < teams.length)
				teamIds[count++] = teams[nextAwardNum++].getId();
			contest.add(new Award(IAward.MEDAL, "silver", teamIds, Messages.getString("awardMedalSilver"), true));
		}

		// bronze medals
		if (numBronze2 > 0) {
			String[] teamIds = new String[numBronze2];
			int count = 0;
			while (nextAwardNum < numGold2 + numSilver2 + numBronze2 && nextAwardNum < teams.length)
				teamIds[count++] = teams[nextAwardNum++].getId();
			contest.add(new Award(IAward.MEDAL, "bronze", teamIds, Messages.getString("awardMedalBronze"), true));
		}
	}

	private static int assignMedal(IAward award, int firstTeamIndex, ITeam[] teams, String citation) {
		if (award == null)
			return firstTeamIndex;

		int numTeams = 1;
		try {
			if (award.getParameter() != null)
				numTeams = Integer.parseInt(award.getParameter());

			if (numTeams < 1)
				throw new IllegalArgumentException("Cannot assign medals to less than one team");
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not parse medal parameter: " + award.getParameter());
		}

		numTeams = Math.min(numTeams, teams.length - firstTeamIndex);
		if (numTeams == 0)
			return firstTeamIndex;

		int count = 0;
		String[] teamIds = new String[numTeams];
		while (count < numTeams) {
			teamIds[count] = teams[firstTeamIndex + count].getId();
			count++;
		}
		((Award) award).setTeamIds(teamIds);
		if (award.getCitation() == null)
			((Award) award).setCitation(citation);

		return firstTeamIndex + numTeams;
	}

	public static void createMedalAwards(Contest contest, IAward gold, IAward silver, IAward bronze) {
		ITeam[] teams = contest.getOrderedTeams();
		if (teams.length == 0)
			return;

		int nextTeam = 0;
		nextTeam = assignMedal(gold, nextTeam, teams, Messages.getString("awardMedalGold"));
		contest.add(gold);
		nextTeam = assignMedal(silver, nextTeam, teams, Messages.getString("awardMedalSilver"));
		contest.add(silver);
		assignMedal(bronze, nextTeam, teams, Messages.getString("awardMedalBronze"));
		contest.add(bronze);
	}

	public static int getLastBronze(IContest contest) {
		int lastBronze = Math.min(12, contest.getNumTeams());
		IAward[] awards = contest.getAwards();
		if (awards != null) {
			for (IAward a : awards)
				if (a.getAwardType() == IAward.MEDAL && a.getId().contains("bronze")) {
					lastBronze = 0;
					String[] teamIds = a.getTeamIds();
					if (teamIds != null) {
						for (String tId : teamIds) {
							lastBronze = Math.max(lastBronze, contest.getOrderOf(contest.getTeamById(tId)) + 1);
						}
					}
				}
		}
		return lastBronze;
	}

	public static void createTopAwards(Contest contest, int percent) {
		Award rank = new Award(IAward.TOP, "*", null, true);
		rank.setParameter(percent + "");
		createTopAwards(contest, rank);
	}

	public static void createTopAwards(Contest contest, IAward template) {
		int percent = 0;
		try {
			percent = Integer.parseInt(template.getParameter());
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not parse top parameter: " + template.getParameter());
		}
		if (percent < 1 || percent > 100)
			throw new IllegalArgumentException(percent + " is not a valid top percentage");

		ITeam[] teams = contest.getOrderedTeams();
		if (teams.length == 0)
			return;

		int n = teams.length;
		if (percent < 100)
			n = percent * teams.length / 100;

		String[] teamIds = new String[n];
		for (int i = 0; i < n; i++)
			teamIds[i] = teams[i].getId();

		String citation = template.getCitation();
		if (citation == null || citation.trim().length() < 1)
			citation = Messages.getString("awardTop");
		citation = citation.replace("{0}", percent + "");

		contest.add(new Award(IAward.TOP, template.getId().substring(4), teamIds, citation, true));
	}

	public static void createHonorsAwards(Contest contest, int percentile) {
		Award rank = new Award(IAward.HONORS, "*", null, true);
		rank.setParameter(percentile + "");
		createTopAwards(contest, rank);
	}

	public static void createHonorsAwards(Contest contest, IAward template) throws IllegalArgumentException {
		int percentileTop = 0;
		int percentileBottom = 100;
		try {
			String param = template.getParameter();
			int ind = param.indexOf("-");
			percentileTop = Integer.parseInt(param.substring(0, ind));
			percentileBottom = Integer.parseInt(param.substring(ind + 1, param.length()));

			if (percentileTop < 0 || percentileTop > 99)
				throw new IllegalArgumentException("Honor parameter invalid");
			if (percentileBottom < 1 || percentileBottom > 100)
				throw new IllegalArgumentException("Honor parameter invalid");
			if (percentileBottom < percentileTop)
				throw new IllegalArgumentException("Honor parameter invalid");
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not parse honor parameter: " + template.getParameter());
		}

		// find teams we're going to base this on
		ITeam[] teams = contest.getOrderedTeams();
		if (teams.length == 0)
			return;

		int n = teams.length;
		int t = 0;
		if (percentileTop > 0) {
			t = (int) Math.round(percentileTop * n / 100.0);
			IStanding standing = contest.getStanding(teams[t]);
			int numSolved = standing.getNumSolved();

			while (t < n && contest.getStanding(teams[t]).getNumSolved() == numSolved) {
				t++;
			}
		}

		// find the bottom team
		int b = n;
		if (percentileBottom < 100) {
			b = (int) Math.round(percentileBottom * n / 100.0);
			IStanding standing = contest.getStanding(teams[b]);
			int numSolved = standing.getNumSolved();
			while (b < n && contest.getStanding(teams[b]).getNumSolved() == numSolved) {
				b++;
			}
		}

		String[] teamIds = new String[b - t];
		for (int i = 0; i < b - t; i++) {
			teamIds[i] = teams[t + i].getId();
		}

		String citation = template.getCitation();
		if (citation == null || citation.trim().length() < 1) {
			if (percentileTop > 1 && percentileBottom == 100)
				citation = Messages.getString("awardHonorableMention");
			else
				citation = Messages.getString("awardHonors");
		}

		contest.add(new Award(IAward.HONORS, template.getId().substring(7), teamIds, citation, true));
	}

	public static int[] getMedalCounts(IContest contest) {
		int[] num = new int[3];
		IAward[] awards = contest.getAwards();
		if (awards != null) {
			for (IAward a : awards)
				if (a.getAwardType() == IAward.MEDAL) {
					if (a.getTeamIds() != null) {
						if (a.getId().contains("gold"))
							num[0] = a.getTeamIds().length;
						else if (a.getId().contains("silver"))
							num[1] = a.getTeamIds().length;
						else if (a.getId().contains("bronze"))
							num[2] = a.getTeamIds().length;
					}
				}
		}
		return num;
	}

	public static void createDefaultAwards(Contest contest) {
		createWorldFinalsAwards(contest);
	}

	public static void createWorldFinalsAwards(Contest contest) {
		createWorldFinalsAwards(contest, 0);
	}

	public static void createWorldFinalsAwards(Contest contest, int b) {
		Award gold = new Award(IAward.MEDAL, "gold", null, null, true);
		gold.setParameter("4");
		Award silver = new Award(IAward.MEDAL, "silver", null, null, true);
		silver.setParameter("4");
		Award bronze = new Award(IAward.MEDAL, "bronze", null, null, true);
		bronze.setParameter((4 + b) + "");
		createMedalAwards(contest, gold, silver, bronze);

		Award group = new Award(IAward.GROUP, "*", null, null, true);
		group.setParameter("1");
		createGroupAwards(contest, group);

		Award fts = new Award(IAward.FIRST_TO_SOLVE, "*", null, null, true);
		createFirstToSolveAwards(contest, fts);
	}

	public static void applyAwards(Contest contest, IAward[] awardTemplate) {
		IAward gold = null;
		IAward silver = null;
		IAward bronze = null;

		for (IAward award : awardTemplate) {
			if (award.getAwardType() == IAward.WINNER) {
				createWinnerAward(contest, award);
			} else if (award.getAwardType() == IAward.GROUP) {
				createGroupAwards(contest, award);
			} else if (award.getAwardType() == IAward.RANK) {
				createRankAwards(contest, award);
			} else if (award.getAwardType() == IAward.FIRST_TO_SOLVE) {
				createFirstToSolveAwards(contest, award);
			} else if (award.getAwardType() == IAward.TOP) {
				createTopAwards(contest, award);
			} else if (award.getAwardType() == IAward.HONORS) {
				createHonorsAwards(contest, award);
			} else if (award.getAwardType() == IAward.MEDAL) {
				if (award.getId().contains("gold"))
					gold = award;
				else if (award.getId().contains("silver"))
					silver = award;
				else if (award.getId().contains("bronze"))
					bronze = award;
			}
		}

		if (gold != null || silver != null || bronze != null)
			createMedalAwards(contest, gold, silver, bronze);
	}

	public static void applyAward(Contest contest, IAward awardTemplate) {
		if (awardTemplate.getAwardType() == IAward.GROUP) {
			createGroupAwards(contest, awardTemplate);
		} else if (awardTemplate.getAwardType() == IAward.FIRST_TO_SOLVE) {
			createFirstToSolveAwards(contest, awardTemplate);
		} else if (awardTemplate.getAwardType() == IAward.MEDAL) {
			// createFirstToSolveAwards(contest, awardTemplate);
		} else if (awardTemplate.getAwardType() == IAward.WINNER) {
			createWinnerAward(contest, awardTemplate);
		} else if (awardTemplate.getAwardType() == IAward.TOP) {
			createTopAwards(contest, awardTemplate);
		} else if (awardTemplate.getAwardType() == IAward.HONORS) {
			createHonorsAwards(contest, awardTemplate);
		}
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
			if (teamIds != null) {
				for (String teamId : teamIds) {
					List<IAward> aw = map.get(teamId);
					if (aw == null) {
						aw = new ArrayList<>();
						map.put(teamId, aw);
					}
					aw.add(award);
				}
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
		sb.append(standing.getRank() + ": " + team.getActualDisplayName());

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