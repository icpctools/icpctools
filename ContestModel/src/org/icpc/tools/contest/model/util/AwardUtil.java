package org.icpc.tools.contest.model.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IAward.AwardType;
import org.icpc.tools.contest.model.IAward.DisplayMode;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Award;
import org.icpc.tools.contest.model.internal.Contest;

public class AwardUtil {
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

				if (contest.getFreezeDuration() != null) {
					long freezeMin = (contest.getDuration() - contest.getFreezeDuration()) / 60000;
					if (ContestUtil.getTimeInMin(s.getContestTime()) < freezeMin)
						beforeFreeze = true;
					else
						afterFreeze = true;
				} else
					beforeFreeze = true;

				DisplayMode mode = DisplayMode.PAUSE;
				if ((beforeFreeze && showBeforeFreeze) || (afterFreeze && showAfterFreeze))
					mode = null;

				String citation = Messages.getString("awardFTSOne").replace("{0}", p.getLabel());
				contest.add(
						new Award(IAward.FIRST_TO_SOLVE, s.getProblemId(), new String[] { team.getId() }, citation, mode));
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
			else
				a.setCitation(a.getCitation().replace("{0}", group.getName()));
		}
	}

	public static void createGroupAwards(Contest contest, int numPerGroup) {
		Award group = new Award(IAward.GROUP, "*", null, (String) null);
		group.setParameter("numPerGroup", numPerGroup + "");
		createGroupAwards(contest, group);
	}

	public static void createGroupAwards(Contest contest, IAward template) {
		int numPerGroup = 1;
		try {
			if (template.getParameters() != null && template.getParameters().containsKey("numPerGroup"))
				numPerGroup = Integer.parseInt(template.getParameters().get("numPerGroup"));

			if (numPerGroup < 1)
				throw new IllegalArgumentException("Cannot assign group awards to less than one team");
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Could not parse group parameter numPerGroup: " + template.getParameters().get("numPerGroup"));
		}

		DisplayMode mode = template.getDisplayMode();

		if (template.getId().equals("group-winner-*")) {
			// to all groups
			for (IGroup group : contest.getGroups()) {
				if (group.isHidden())
					continue;

				Award groupAward = new Award(IAward.GROUP, group.getId(), null, template.getCitation(), mode);
				// groupAward.setCount("1");
				contest.add(groupAward);
				assignGroup(contest, groupAward, numPerGroup);
			}
		} else {
			assignGroup(contest, (Award) template, numPerGroup);
		}
	}

	public static void createSolutionAwards(Contest contest) {
		Award solved = new Award(IAward.SOLVED, "*", null, (String) null);
		createSolutionAwards(contest, solved);
	}

	public static void createSolutionAwards(Contest contest, IAward template) {
		// skip any team that already has an honourable mention
		List<String> teamIdsHM = new ArrayList<>();
		IAward[] awards = contest.getAwards();
		for (IAward a : awards) {
			if (a.getAwardType() == IAward.HONORS && a.getId().contains("mention")) {
				for (String s : a.getTeamIds())
					teamIdsHM.add(s);
			}
		}

		List<String> teamIdsNumMedalsSolved = new ArrayList<>();

		if (template.getParameters() != null && template.getParameters().containsKey("mode")
				&& template.getParameters().get("mode").equals("less-than-medals")) {
			int lowestMedalNumSolved = Integer.MAX_VALUE;
			for (IAward a : awards) {
				if (a.getAwardType() == IAward.MEDAL) {
					for (String tid : a.getTeamIds()) {
						IStanding s = contest.getStanding(contest.getTeamById(tid));
						if (s != null && s.getNumSolved() > 0) {
							lowestMedalNumSolved = Math.min(lowestMedalNumSolved, s.getNumSolved());
						}
					}
				}
			}

			for (ITeam t : contest.getTeams()) {
				IStanding s = contest.getStanding(t);

				if (teamIdsNumMedalsSolved.contains(t.getId())) {
					continue;
				}

				if (s.getNumSolved() >= lowestMedalNumSolved) {
					teamIdsNumMedalsSolved.add(t.getId());
				}
			}
		}

		// create buckets
		Map<Integer, List<ITeam>> solutions = new HashMap<>();
		for (ITeam t : contest.getTeams()) {
			IStanding s = contest.getStanding(t);

			if (teamIdsHM.contains(t.getId()))
				continue;

			if (teamIdsNumMedalsSolved.contains(t.getId()))
				continue;

			if (s != null && s.getNumSolved() > 0) {
				int ns = s.getNumSolved();
				List<ITeam> teams = solutions.get(ns);
				if (teams == null) {
					teams = new ArrayList<ITeam>();
					solutions.put(ns, teams);
				}
				teams.add(t);
			}
		}

		String citation = template.getCitation();
		if (citation == null || citation.trim().length() < 1)
			citation = Messages.getString("awardSolvedMultiple");

		DisplayMode mode = null;
		if (template.hasDisplayMode())
			mode = template.getDisplayMode();
		else
			mode = DisplayMode.LIST;

		// create awards for each bucket
		for (Integer ns : solutions.keySet()) {
			List<ITeam> teams = solutions.get(ns);
			String[] teamIds = new String[teams.size()];
			for (int i = 0; i < teams.size(); i++) {
				teamIds[i] = teams.get(i).getId();
			}

			Award solutionAward = new Award(IAward.SOLVED, "solved-" + ns, teamIds, citation.replace("{0}", ns + ""),
					mode);
			solutionAward.setParameter("numSolved", ns + "");
			contest.add(solutionAward);
		}
	}

	private static void assignFirstToSolve(Contest contest, Award a) {
		String problemId = a.getId().substring(15);
		a.setTeamIds(new String[0]);
		boolean showBeforeFreeze = false;
		boolean showAfterFreeze = true;
		// assign award to one problem
		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission s : submissions) {
			if (problemId.equals(s.getProblemId()) && contest.isSolved(s) && contest.isFirstToSolve(s)) {
				ITeam team = contest.getTeamById(s.getTeamId());
				IProblem p = contest.getProblemById(s.getProblemId());

				boolean beforeFreeze = false;
				boolean afterFreeze = false;

				if (contest.getFreezeDuration() != null) {
					long freezeMin = (contest.getDuration() - contest.getFreezeDuration()) / 60000;
					if (ContestUtil.getTimeInMin(s.getContestTime()) < freezeMin)
						beforeFreeze = true;
					else
						afterFreeze = true;
				} else
					beforeFreeze = true;

				DisplayMode mode = DisplayMode.PAUSE;
				if ((beforeFreeze && showBeforeFreeze) || (afterFreeze && showAfterFreeze))
					mode = null;

				a.setTeamIds(new String[] { team.getId() });
				if (a.getCitation() == null)
					a.setCitation(Messages.getString("awardFTSOne").replace("{0}", p.getLabel()));
				if (a.getDisplayMode() == null) {
					a.setDisplayMode(mode);
				}
			}
		}
	}

	public static void createFirstToSolveAwards(Contest contest, IAward template) {
		if (template.getId().equals("first-to-solve-*")) {
			for (IProblem problem : contest.getProblems()) {
				Award ftsAward = new Award(IAward.FIRST_TO_SOLVE, problem.getId(), null, (String) null);
				if (template.getDisplayMode() != null) {
					ftsAward.setDisplayMode(template.getDisplayMode());
				}
				contest.add(ftsAward);
				assignFirstToSolve(contest, ftsAward);
			}
		} else {
			assignFirstToSolve(contest, (Award) template);
		}
	}

	public static void createGroupHighlightAwards(Contest contest, String groupId2, int numToHighlight, String citation,
			DisplayMode show) {
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
		contest.add(new Award(IAward.WINNER, team.getId(), citation));
	}

	public static void createWinnerAward(Contest contest, IAward template) {
		ITeam[] teams = contest.getOrderedTeams();
		if (teams.length == 0)
			return;

		String citation = template.getCitation();
		if (citation == null)
			citation = Messages.getString("awardChampion");

		contest.add(new Award(IAward.WINNER, teams[0].getId(), citation, template.getDisplayMode()));
	}

	public static void createRankAwards(Contest contest, int num) {
		Award rank = new Award(IAward.RANK, "*", null);
		rank.setParameter("numTeams", num + "");
		createRankAwards(contest, rank);
	}

	public static void createRankAwards(Contest contest, IAward template) {
		int numTeams = 0;
		try {
			if (template.getParameters() != null && template.getParameters().containsKey("numTeams"))
				numTeams = Integer.parseInt(template.getParameters().get("numTeams"));
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Could not parse rank parameter: " + template.getParameters().get("numTeams"));
		}

		if (numTeams < 1)
			return;

		DisplayMode mode = template.getDisplayMode();

		ITeam[] teams = contest.getOrderedTeams();
		if (teams.length == 0)
			return;
		int n = Math.min(numTeams, teams.length);

		for (int i = 0; i < n; i++) {
			ITeam team = teams[i];
			String[] teamIds = new String[] { team.getId() };
			IStanding standing = contest.getStanding(team);
			try {
				int rank = Integer.parseInt(standing.getRank());
				contest.add(new Award(IAward.RANK, (i + 1) + "", teamIds,
						Messages.getString("awardPlace").replace("{0}", getPlaceString(rank)), mode));
			} catch (Exception e) {
				contest.add(new Award(IAward.RANK, (i + 1) + "", teamIds,
						Messages.getString("awardPlace").replace("{0}", standing.getRank()), mode));
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
			contest.add(new Award(IAward.MEDAL, "gold", teamIds, Messages.getString("awardMedalGold")));
		}

		// silver medals
		if (numSilver2 > 0) {
			String[] teamIds = new String[numSilver2];
			int count = 0;
			while (nextAwardNum < numGold2 + numSilver2 && nextAwardNum < teams.length)
				teamIds[count++] = teams[nextAwardNum++].getId();
			contest.add(new Award(IAward.MEDAL, "silver", teamIds, Messages.getString("awardMedalSilver")));
		}

		// bronze medals
		if (numBronze2 > 0) {
			String[] teamIds = new String[numBronze2];
			int count = 0;
			while (nextAwardNum < numGold2 + numSilver2 + numBronze2 && nextAwardNum < teams.length)
				teamIds[count++] = teams[nextAwardNum++].getId();
			contest.add(new Award(IAward.MEDAL, "bronze", teamIds, Messages.getString("awardMedalBronze")));
		}
	}

	private static int assignMedal(IAward award, int firstTeamIndex, ITeam[] teams, String citation) {
		if (award == null)
			return firstTeamIndex;

		int numTeams = 1;
		try {
			if (award.getParameters() != null && award.getParameters().containsKey("numTeams"))
				numTeams = Integer.parseInt(award.getParameters().get("numTeams"));

			if (numTeams < 1)
				throw new IllegalArgumentException("Cannot assign medals to less than one team");
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Could not parse medal parameter: " + award.getParameters().get("numTeams"));
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
		((Award) award).clearParameter("numTeams");

		return firstTeamIndex + numTeams;
	}

	public static void createMedalAwards(Contest contest, List<IAward> goldList, List<IAward> silverList,
			List<IAward> bronzeList) {
		ITeam[] teams = contest.getOrderedTeams();
		if (teams.length == 0)
			return;

		int maxCount = Math.max(Math.max(goldList.size(), silverList.size()), bronzeList.size());
		for (int i = 0; i < maxCount; i++) {
			IAward gold = null;
			IAward silver = null;
			IAward bronze = null;
			if (i < goldList.size()) {
				gold = goldList.get(i);
			}
			if (i < silverList.size()) {
				silver = silverList.get(i);
			}
			if (i < bronzeList.size()) {
				bronze = bronzeList.get(i);
			}
			int nextTeam = 0;
			nextTeam = assignMedal(gold, nextTeam, teams, Messages.getString("awardMedalGold"));
			nextTeam = assignMedal(silver, nextTeam, teams, Messages.getString("awardMedalSilver"));
			assignMedal(bronze, nextTeam, teams, Messages.getString("awardMedalBronze"));
			// Add them in reverse order, so we can display them in the correct order in the resolver
			contest.add(bronze);
			contest.add(silver);
			contest.add(gold);
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
		Award rank = new Award(IAward.TOP, "*", null);
		rank.setParameter("percent", percent + "");
		createTopAwards(contest, rank);
	}

	public static void createTopAwards(Contest contest, IAward template) {
		int percent = 0;
		try {
			if (template.getParameters() != null && template.getParameters().containsKey("percent"))
				percent = Integer.parseInt(template.getParameters().get("percent"));
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Could not parse top parameter: " + template.getParameters().get("percent"));
		}
		if (percent < 1 || percent > 100)
			throw new IllegalArgumentException(percent + " is not a valid top percentage");

		DisplayMode mode = template.getDisplayMode();
		if (!template.hasDisplayMode())
			mode = IAward.DisplayMode.LIST;

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

		contest.add(new Award(IAward.TOP, template.getId().substring(4), teamIds, citation, mode));
	}

	public static void createHonorsAwards(Contest contest, IAward template) throws IllegalArgumentException {
		int solvedTop = -1;
		int solvedBottom = -1;
		int percentileTop = -1;
		int percentileBottom = -1;
		DisplayMode mode = template.getDisplayMode();
		if (!template.hasDisplayMode())
			mode = IAward.DisplayMode.LIST;

		// find teams we're going to base this on
		ITeam[] teams = contest.getOrderedTeams();
		if (teams.length == 0)
			return;

		try {
			if (template.getParameters() != null) {
				if (template.getParameters().containsKey("solvedTop"))
					solvedTop = Integer.parseInt(template.getParameters().get("solvedTop"));
				if (template.getParameters().containsKey("solvedBottom"))
					solvedBottom = Integer.parseInt(template.getParameters().get("solvedBottom"));
				if (template.getParameters().containsKey("percentileTop"))
					percentileTop = Integer.parseInt(template.getParameters().get("percentileTop"));
				if (template.getParameters().containsKey("percentileBottom"))
					percentileBottom = Integer.parseInt(template.getParameters().get("percentileBottom"));
			}

			if (percentileTop < 0 && solvedTop < 0)
				throw new IllegalArgumentException("Honor solved parameter invalid");
			if (percentileBottom < 0 && solvedBottom < 0)
				throw new IllegalArgumentException("Honor solved parameter invalid");
			if (percentileBottom >= 0 && percentileTop >= 0 && percentileBottom < percentileTop)
				throw new IllegalArgumentException("Honor solved parameter invalid");
			if (percentileTop > 99 || percentileBottom > 100)
				throw new IllegalArgumentException("Honor solved parameter invalid");
			if (solvedBottom >= 0 && solvedTop >= 0 && solvedBottom < solvedTop)
				throw new IllegalArgumentException("Honor solved parameter invalid");
			if (solvedBottom > teams.length || solvedTop > teams.length)
				throw new IllegalArgumentException("Honor solved parameter invalid");

		} catch (Exception e) {
			throw new IllegalArgumentException("Could not parse honor parameters: " + template.getParameters());
		}

		IAward[] awards = contest.getAwards();

		// Determine how many problems the lowest medalist solved. We need this for the
		// solvedTop/solvedBottom case.
		int lowestMedalNumSolved = Integer.MAX_VALUE;
		Set<String> medalTeams = new HashSet<>();
		for (IAward a : awards) {
			if (a.getAwardType() == IAward.MEDAL) {
				medalTeams.addAll(Arrays.asList(a.getTeamIds()));
				for (String tid : a.getTeamIds()) {
					IStanding s = contest.getStanding(contest.getTeamById(tid));
					if (s != null && s.getNumSolved() > 0) {
						lowestMedalNumSolved = Math.min(lowestMedalNumSolved, s.getNumSolved());
					}
				}
			}
		}
		int numMedalists = medalTeams.size();

		// find teams we're going to base this on

		// Find the top team
		int n = teams.length;
		int t = 0;
		if (percentileTop > 0) {
			t = (int) Math.floor(percentileTop * n / 100.0);
			IStanding standing = contest.getStanding(teams[t]);
			int numSolved = standing.getNumSolved();

			while (t < n && contest.getStanding(teams[t]).getNumSolved() == numSolved) {
				t++;
			}
		} else if (solvedTop > 0) {
			while (t < n && contest.getStanding(teams[t]).getNumSolved() > lowestMedalNumSolved - solvedTop) {
				t++;
			}
		} else {
			// solvedTop == 0, start just below the medalists.
			t = numMedalists;
		}

		// find the bottom team
		int b = n;
		if (percentileBottom >= 0 && percentileBottom < 100) {
			b = (int) Math.round(percentileBottom * n / 100.0);
			IStanding standing = contest.getStanding(teams[b]);
			int numSolved = standing.getNumSolved();
			while (b < n && contest.getStanding(teams[b]).getNumSolved() == numSolved) {
				b++;
			}
		} else if (solvedBottom >= 0) {
			b = 0;
			while (b < n && contest.getStanding(teams[b]).getNumSolved() > lowestMedalNumSolved - solvedBottom) {
				b++;
			}
		}

		if (t >= b)
			return;

		String[] teamIds = new String[b - t];
		for (int i = 0; i < b - t; i++) {
			teamIds[i] = teams[t + i].getId();
		}

		String citation = template.getCitation();
		if (citation == null || citation.trim().isEmpty()) {
			if (template.getId().equals("highest-honors"))
				citation = Messages.getString("awardHighestHonors");
			else if (template.getId().equals("high-honors"))
				citation = Messages.getString("awardHighHonors");
			else if (template.getId().equals("honors"))
				citation = Messages.getString("awardHonors2");
			else if (percentileTop > 1 && percentileBottom == 100)
				citation = Messages.getString("awardHonorableMention");
			else
				// TODO: this seems wrong. Entry 'awardHonors' is "{0}% Honors"
				// The "{0}" should be replaced by something!
				citation = Messages.getString("awardHonors");
		}

		Award award = new Award(IAward.HONORS, template.getId(), teamIds, citation, mode);
		// Overwrite the ID to the actual ID, since we know what we are doing
		award.add("id", template.getId());
		IStanding standing = contest.getStanding(teams[t]);

		// If the top of our list is just below the medalists or in the medalists, show it before the
		// medalists
		if (t <= numMedalists) {
			award.setParameter("before", numMedalists + "");
		} else {
			award.setParameter("numSolved", standing.getNumSolved() + "");
		}
		contest.add(award);
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

	private static void assignExpectedToAdvance(Contest contest, Award a, int numTeams) {
		// initialize to no teams
		a.setTeamIds(new String[0]);

		int count = 0;
		List<String> teamIds = new ArrayList<>();
		List<String> organizations = new ArrayList<>();
		for (ITeam team : contest.getOrderedTeams()) {
			if (contest.getStanding(team).getNumSolved() == 0 || count == numTeams)
				break;

			String organization = team.getOrganizationId();
			if (!organizations.contains(organization)) {
				if (count < numTeams) {
					teamIds.add(team.getId());
					if (organization != null)
						organizations.add(organization);
					count++;
				}
			}
		}

		if (!teamIds.isEmpty()) {
			a.setTeamIds(teamIds.toArray(new String[0]));
			if (a.getCitation() == null)
				a.setCitation(IAward.EXPECTED_TO_ADVANCE.getName());
		}
	}

	public static void createExpectedToAdvanceAwards(Contest contest, int numTeams) {
		Award expectedToAdvance = new Award(IAward.EXPECTED_TO_ADVANCE, "", null, (String) null);
		expectedToAdvance.setParameter("numTeams", numTeams + "");
		createExpectedToAdvanceAwards(contest, expectedToAdvance);
	}

	public static void createExpectedToAdvanceAwards(Contest contest, IAward template) {
		int numTeams = 3;
		try {
			if (template.getParameters() != null && template.getParameters().containsKey("numTeams"))
				numTeams = Integer.parseInt(template.getParameters().get("numTeams"));

			if (numTeams < 1)
				throw new IllegalArgumentException("Cannot assign expected to advance awards to less than one team");
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Could not parse numTeams parameter: " + template.getParameters().get("numTeams"));
		}

		assignExpectedToAdvance(contest, (Award) template, numTeams);
		contest.add(template);
	}

	public static void createAllGroupWinnersAward(Contest contest, IAward template) throws IllegalArgumentException {
		DisplayMode mode = template.getDisplayMode();
		if (!template.hasDisplayMode())
			mode = DisplayMode.PHOTOS;

		IAward[] awards = contest.getAwards();

		// Determine the position of the lowest medalist, which we need to display this award
		// corrrectly
		// Also keep track of all teams that have a group award
		Set<String> groupWinners = new HashSet<>();
		Set<String> medalWinners = new HashSet<>();
		for (IAward a : awards) {
			if (a.getAwardType() == IAward.MEDAL) {
				medalWinners.addAll(Arrays.asList(a.getTeamIds()));
			} else if (a.getAwardType() == IAward.GROUP) {
				groupWinners.addAll(Arrays.asList(a.getTeamIds()));
			}
		}
		int numMedalists = medalWinners.size();

		String citation = template.getCitation();
		if (citation == null || citation.trim().isEmpty()) {
			citation = Messages.getString("awardGroupWinners");
		}

		// Remove medal winners since they will be announced separately
		groupWinners.removeAll(medalWinners);

		String[] teamIds = groupWinners.toArray(new String[0]);

		Award award = new Award(IAward.ALL_GROUP_WINNERS, template.getId(), teamIds, citation, mode);

		award.setParameter("before", numMedalists + "");
		award.setParameter("showGroupName", "true");
		award.setParameter("highlight", "false");
		award.setParameter("showScoreboardBefore", "false");
		contest.add(award);
	}

	public static void createDefaultAwards(Contest contest) {
		Award gold = new Award(IAward.MEDAL, "gold", null, (String) null);
		gold.setParameter("numTeams", "4");
		Award silver = new Award(IAward.MEDAL, "silver", null, (String) null);
		silver.setParameter("numTeams", "4");
		Award bronze = new Award(IAward.MEDAL, "bronze", null, (String) null);
		bronze.setParameter("numTeams", "4");
		createMedalAwards(contest, Collections.singletonList(gold), Collections.singletonList(silver),
				Collections.singletonList(bronze));

		Award group = new Award(IAward.GROUP, "*", null, (String) null);
		group.setParameter("numPerGroup", "1");
		createGroupAwards(contest, group);

		Award fts = new Award(IAward.FIRST_TO_SOLVE, "*", null, (String) null);
		createFirstToSolveAwards(contest, fts);
	}

	public static void applyAwards(Contest contest, IAward[] awardTemplate) {
		List<IAward> gold = new ArrayList<>();
		List<IAward> silver = new ArrayList<>();
		List<IAward> bronze = new ArrayList<>();
		List<IAward> honors = new ArrayList<>();

		List<IAward> solvedAwards = new ArrayList<>();

		IAward groupWinnersAward = null;

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
			} else if (award.getAwardType() == IAward.EXPECTED_TO_ADVANCE) {
				createExpectedToAdvanceAwards(contest, award);
			} else if (award.getAwardType() == IAward.HONORS) {
				honors.add(award);
			} else if (award.getAwardType() == IAward.SOLVED) {
				solvedAwards.add(award);
			} else if (award.getAwardType() == IAward.MEDAL) {
				if (award.getId().contains("gold"))
					gold.add(award);
				else if (award.getId().contains("silver"))
					silver.add(award);
				else if (award.getId().contains("bronze"))
					bronze.add(award);
			} else if (award.getAwardType() == IAward.ALL_GROUP_WINNERS)
				groupWinnersAward = award;
		}

		if (!gold.isEmpty() || !silver.isEmpty() || !bronze.isEmpty()) {
			createMedalAwards(contest, gold, silver, bronze);
		}

		// We need to do this after the medals are created, because we need to know the number of
		// solved problems for the lowest medalist
		if (!honors.isEmpty()) {
			for (IAward award : honors) {
				createHonorsAwards(contest, award);
			}
		}

		// We need to do this after both the medals are created and the individual group winners are
		// known
		if (groupWinnersAward != null) {
			createAllGroupWinnersAward(contest, groupWinnersAward);
		}

		if (!solvedAwards.isEmpty()) {
			for (IAward award : solvedAwards) {
				createSolutionAwards(contest, award);
			}
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
					if (IAward.KNOWN_TYPES[indi].equals(ai.getAwardType()))
						break;

				int indj;
				for (indj = 0; indj < IAward.KNOWN_TYPES.length; indj++)
					if (IAward.KNOWN_TYPES[indj].equals(aj.getAwardType()))
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
					.replace("{1}", standing.getNumSolved() + "").replace("{2}", ContestUtil.getTime(standing.getTime()));
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
		if (ind == 1)
			return Messages.getString("awardChampions").replace("{0}", groupName);
		return Messages.getString("awardChampions2").replace("{0}", groupName).replace("{1}", getPlaceString(ind));
	}
}
