package org.icpc.tools.resolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FreezeFilter;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.AwardUtil;
import org.icpc.tools.presentation.contest.internal.SubmissionInfo;
import org.icpc.tools.presentation.contest.internal.scoreboard.AbstractScoreboardPresentation;
import org.icpc.tools.resolver.ResolutionUtil.AwardStep;
import org.icpc.tools.resolver.ResolutionUtil.ContestStateStep;
import org.icpc.tools.resolver.ResolutionUtil.DelayStep;
import org.icpc.tools.resolver.ResolutionUtil.DelayType;
import org.icpc.tools.resolver.ResolutionUtil.PauseStep;
import org.icpc.tools.resolver.ResolutionUtil.PresentationStep;
import org.icpc.tools.resolver.ResolutionUtil.ResolutionStep;
import org.icpc.tools.resolver.ResolutionUtil.ScrollStep;
import org.icpc.tools.resolver.ResolutionUtil.ScrollTeamListStep;
import org.icpc.tools.resolver.ResolutionUtil.SubmissionSelectionStep;
import org.icpc.tools.resolver.ResolutionUtil.SubmissionSelectionStep2;
import org.icpc.tools.resolver.ResolutionUtil.TeamListStep;
import org.icpc.tools.resolver.ResolutionUtil.TeamSelectionStep;
import org.icpc.tools.resolver.ResolutionUtil.ToJudgeStep;

/**
 * The Resolver 'logic'. This is the class that performs the actual resolving of submissions to
 * find the final result. It runs on startup to determine all the individual steps and timing that
 * the resolution will take.
 */
public class ResolverLogic {
	enum State {
		SELECT_TEAM, SELECT_PROBLEM, SOLVED_MOVE, SOLVED_STAY, FAILED, DESELECT, SELECT_SUBMISSION
	}

	interface Timing {
		public void onStep(List<ResolutionStep> steps, State s);
	}

	static class PauseTiming implements Timing {
		@Override
		public void onStep(List<ResolutionStep> steps2, State s) {
			if (s == State.DESELECT || s == State.SELECT_SUBMISSION)
				return;
			steps2.add(new PauseStep());
		}
	}

	static class ScoreboardTiming implements Timing {
		@Override
		public void onStep(List<ResolutionStep> steps2, State s) {
			if (s == State.SELECT_TEAM)
				steps2.add(new DelayStep(DelayType.SELECT_TEAM));
			else if (s == State.SELECT_PROBLEM)
				steps2.add(new DelayStep(DelayType.SELECT_PROBLEM));
			else if (s == State.SELECT_SUBMISSION)
				return;
			else if (s == State.SOLVED_MOVE)
				steps2.add(new DelayStep(DelayType.SOLVED_MOVE));
			else if (s == State.SOLVED_STAY)
				steps2.add(new DelayStep(DelayType.SOLVED_STAY));
			else if (s == State.FAILED)
				steps2.add(new DelayStep(DelayType.FAILED));
			else if (s == State.DESELECT)
				steps2.add(new DelayStep(DelayType.DESELECT));
		}
	}

	static class JudgeQueueTiming implements Timing {
		@Override
		public void onStep(List<ResolutionStep> steps2, State s) {
			if (s == State.SELECT_TEAM)
				return;
			else if (s == State.SELECT_PROBLEM)
				steps2.add(new DelayStep(DelayType.SELECT_PROBLEM));
			else if (s == State.SELECT_SUBMISSION)
				steps2.add(new DelayStep(DelayType.SELECT_SUBMISSION));
			else if (s == State.SOLVED_MOVE)
				steps2.add(new DelayStep(DelayType.DESELECT));
			else if (s == State.SOLVED_STAY)
				steps2.add(new DelayStep(DelayType.DESELECT));
			else if (s == State.FAILED)
				steps2.add(new DelayStep(DelayType.DESELECT));
			else if (s == State.DESELECT)
				return;
		}
	}

	static class PredeterminedStep {
		String teamId;
		String problemLabel;

		public PredeterminedStep(String teamId, String problemLabel) {
			this.teamId = teamId;
			this.problemLabel = problemLabel;
		}

		@Override
		public String toString() {
			return "Step: " + teamId + " " + problemLabel;
		}
	}

	// which row to start single-stepping on
	private int singleStepStartRow;
	private int rowOffset;
	private boolean calculateProjections;

	// map from all the teamIds getting an award to the list of awards they're getting
	private Map<String, List<IAward>> awards = new HashMap<>();
	private List<TeamListStep> teamLists = new ArrayList<>();

	private List<ResolutionStep> steps = new ArrayList<>();

	private Contest contest;
	private Contest finalContest;

	private List<PredeterminedStep> predeterminedSteps = new ArrayList<>();

	public ResolverLogic(Contest contest, int singleStepStartRow, int rowOffset, boolean calculateProjections,
			List<PredeterminedStep> predeterminedSteps) {
		finalContest = contest;
		this.singleStepStartRow = singleStepStartRow;
		this.rowOffset = rowOffset;
		this.calculateProjections = calculateProjections;
		this.predeterminedSteps = predeterminedSteps;

		// defaults to -2 (don't start single step automatically) if no medals exist
		if (this.singleStepStartRow < 0 && finalContest.isDoneUpdating()) {
			IAward[] awards2 = finalContest.getAwards();
			for (IAward a : awards2) {
				if (a.getAwardType() == IAward.MEDAL) {
					for (String id : a.getTeamIds()) {
						ITeam team = finalContest.getTeamById(id);
						if (team != null) {
							int row = finalContest.getOrderOf(team);
							this.singleStepStartRow = Math.max(row, this.singleStepStartRow);
						}
					}
				}
			}
			int lastBronze = AwardUtil.getLastBronze(finalContest);
			this.singleStepStartRow = Math.max(lastBronze - 1, this.singleStepStartRow);
		}
	}

	/**
	 * Configure the resolver and run against the given contest.
	 *
	 * @param time the starting time (in seconds) to resolve from
	 * @return the full list of resolution steps
	 */
	public List<ResolutionStep> resolveFrom(boolean bill) {
		Trace.trace(Trace.INFO, "Resolving...");

		// cache all the awards by team id
		IAward[] contestAwards = finalContest.getAwards();
		AwardUtil.sortAwards(contest, contestAwards);
		for (IAward award : contestAwards) {
			String[] teamIds = award.getTeamIds();
			for (String teamId : teamIds) {
				List<IAward> aw = awards.get(teamId);
				if (aw == null) {
					aw = new ArrayList<>();
					awards.put(teamId, aw);
				}
				aw.add(award);
			}
		}

		// find team lists
		if (bill) {
			List<ITeam> teamGroup = new ArrayList<>();
			ITeam[] teams = finalContest.getOrderedTeams();
			String rank = null;
			int solved = 0;
			for (int i = teams.length - 1; i >= 0; i--) {
				ITeam team = teams[i];
				IStanding standing = finalContest.getStanding(team);
				if (rank != null && !standing.getRank().equals(rank)) {
					boolean showList = true;
					if (teamGroup.size() == 1) {
						ITeam team2 = teamGroup.get(0);
						List<IAward> aw = awards.get(team2.getId());
						if (aw != null) {
							for (IAward a : aw) {
								if (a.getAwardType() == IAward.MEDAL)
									showList = false;
							}
						}
					}
					if (showList) {
						String title = null;
						String subTitle = null;
						if ("H".equals(rank))
							title = Messages.getString("teamListHM");
						else {
							title = Messages.getString("teamListTitle").replace("{0}", getPlace(rank));
							subTitle = Messages.getString("teamListSubtitle").replace("{0}", solved + "");
						}

						int size = teamGroup.size();
						ITeam[] teamList = new ITeam[size];
						Map<String, AbstractScoreboardPresentation.SelectType> selections = new HashMap<>();
						for (int j = 0; j < size; j++) {
							teamList[j] = teamGroup.get(size - j - 1);

							// figure out awards
							String teamId = teamList[j].getId();
							List<IAward> teamAwards = awards.get(teamId);
							if (teamAwards != null && !teamAwards.isEmpty()) {
								IOrganization org = finalContest.getOrganizationById(teamList[j].getOrganizationId());
								if (org == null)
									Trace.trace(Trace.INFO, "Team list award for: " + teamId);
								else
									Trace.trace(Trace.INFO, "Team list award for: " + teamId + " " + org.getFormalName());

								// are we going to show this on a separate page?
								boolean show = false;
								for (IAward award : teamAwards) {
									if (award.showAward())
										show = true;
								}

								boolean hasFTS = false;
								if (!teamAwards.isEmpty()) {
									for (IAward a : teamAwards) {
										if (a.getAwardType() == IAward.FIRST_TO_SOLVE)
											hasFTS = true;
									}
								}
								if (hasFTS) {
									if (show)
										selections.put(teamId, AbstractScoreboardPresentation.SelectType.FTS_HIGHLIGHT);
									else
										selections.put(teamId, AbstractScoreboardPresentation.SelectType.FTS);
								} else
									selections.put(teamId, AbstractScoreboardPresentation.SelectType.HIGHLIGHT);
							}
						}

						teamLists.add(new TeamListStep(title, subTitle, teamList, selections));
					}
					teamGroup = new ArrayList<>();
				}
				rank = standing.getRank();
				solved = standing.getNumSolved();
				teamGroup.add(team);
			}
		}

		// revert to start of the freeze
		contest = finalContest.clone(new FreezeFilter(finalContest));

		// clean up unjudged and non-penalty submissions in the last hour that don't matter and will
		// mess with resolving
		cleanOutlierSubmissions();

		// set the initial state
		steps.add(new PauseStep());
		steps.add(new ContestStateStep(contest));
		steps.add(new TeamSelectionStep());
		steps.add(new SubmissionSelectionStep(null));
		steps.add(new ToJudgeStep(null));

		Trace.trace(Trace.USER, "Resolving " + countUnjudgedSubmissions(contest) + " pending submissions out of the "
				+ contest.getNumSubmissions() + " total submissions in the contest... ");

		// first click - show the scoreboard
		steps.add(new ScrollStep(0));
		steps.add(new PresentationStep(PresentationStep.Presentations.SCOREBOARD));
		steps.add(new PauseStep());

		// second click - scroll down to bottom (so last team is at bottom of screen),
		// turn off the scoreboard presentation Legend, then do nothing else
		steps.add(new ScrollStep(getScrollToCurrentRow(contest.getNumTeams() - 1, 0)));
		steps.add(new PauseStep());

		// third click - select the team or switch to judge queue

		// fourth click and beyond: start resolving!
		resolveEverything(bill);
		ResolutionUtil.numberThePauses(steps);

		Trace.trace(Trace.USER, "Done resolving");

		Trace.trace(Trace.INFO, "Resolution steps:");
		for (ResolutionStep step : steps)
			Trace.trace(Trace.INFO, "  " + step);

		return steps;
	}

	protected String getPlace(String place) {
		try {
			int n = Integer.parseInt(place);
			return AwardUtil.getPlaceString(n);
		} catch (Exception e) {
			return place;
		}
	}

	protected void cleanOutlierSubmissions() {
		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission submission : submissions) {
			if (!contest.isJudged(submission)) {
				ITeam team = contest.getTeamById(submission.getTeamId());
				if (team == null)
					continue;

				int probIndex = contest.getProblemIndex(submission.getProblemId());
				IResult result = contest.getResult(team, probIndex);

				if (result.getStatus() != Status.SUBMITTED) {
					contest.updateSubmissionTo(submission, finalContest);
				}
			}
		}

		// remove non-penalty judgements
		submissions = finalContest.getSubmissions();
		for (ISubmission submission : submissions) {
			IJudgement[] sjs = finalContest.getJudgementsBySubmissionId(submission.getId());
			if (sjs != null) {
				for (IJudgement sj : sjs) {
					IJudgementType jt = finalContest.getJudgementTypeById(sj.getJudgementTypeId());
					if (jt != null && !jt.isSolved() && !jt.isPenalty() && !contest.isBeforeFreeze(submission)) {
						contest.add(sj);
					}
				}
			}
		}
	}

	private static int countUnjudgedSubmissions(Contest contest) {
		int count = 0;
		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission submission : submissions) {
			if (!contest.isJudged(submission))
				count++;
		}
		return count;
	}

	/**
	 * Updates the status of the submission referenced by the given SubmissionInfo object to match
	 * the status of that submission in the "finalContest" -- that is, "resolves" the submission.
	 *
	 * @param submissionInfo - an object describing the submission (team and problem) to be resolved
	 * @return true if the submission was "pending" and was ultimately Solved; false otherwise
	 */
	private boolean resolveSubmissionImpl(SubmissionInfo submissionInfo, Timing timing) {
		Trace.trace(Trace.INFO, "Resolving: " + submissionInfo);

		ITeam team = submissionInfo.getTeam();
		int problemIndex = submissionInfo.getProblemIndex();
		IResult r = contest.getResult(team, problemIndex);

		// check if the specified submission is "pending"
		if (r.getStatus() != Status.SUBMITTED) {
			Trace.trace(Trace.ERROR, "Trying to resolve a judged submission");
			return false;
		}

		// yes, it's a pending submission; look through all the submissions in the contest to find it
		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission submission : submissions) {
			if (!contest.isJudged(submission) && submission.getTeamId().equals(team.getId())
					&& submission.getProblemId().equals(contest.getProblems()[problemIndex].getId())) {
				// we found it; update its status to match the correct (final contest) status
				contest = contest.clone(false);
				contest.updateSubmissionTo(submission, finalContest);

				steps.add(new ContestStateStep(contest));
				steps.add(new SubmissionSelectionStep2(submission.getId()));
				timing.onStep(steps, State.SELECT_SUBMISSION);

				// there may be more submissions from this team for this problem so no break
			}
		}

		// get the newly-computed result for the current team on the specified problem
		IResult newResult = contest.getResult(team, problemIndex);

		// get the equivalent information out of the final contest
		IResult finalResult = finalContest.getResult(team, problemIndex);

		// double check that the two match
		if (newResult.getStatus() != finalResult.getStatus())
			Trace.trace(Trace.ERROR, "Results don't match! " + newResult + " -> " + finalResult);

		// return true if the pending submission was a YES; false otherwise
		return newResult.getStatus() == Status.SOLVED;
	}

	private void resolveEverything(boolean bill) {
		int currentRow = contest.getNumTeams() - 1;
		// boolean singleStep = false;

		Timing timing = null;
		if (bill) {
			steps.add(new PresentationStep(PresentationStep.Presentations.JUDGE));
			timing = new JudgeQueueTiming();
		} else {
			ITeam[] teams = contest.getOrderedTeams();
			steps.add(new TeamSelectionStep(teams[teams.length - 1]));
			timing = new ScoreboardTiming();
		}
		steps.add(new PauseStep());

		ToJudgeStep judgeStep = new ToJudgeStep(null);
		if (bill) {
			steps.add(judgeStep);
			steps.add(new PauseStep());
		}
		List<String> judgeRuns = new ArrayList<>();

		SubmissionInfo runInfo = getNextResolve();
		projectStandings(runInfo);

		while (currentRow >= 0) {
			Trace.trace(Trace.INFO, "Resolving row: " + currentRow);

			if (currentRow <= singleStepStartRow) {
				// singleStep = true;
				timing = new PauseTiming();
			}

			// scroll to row
			steps.add(new ScrollStep(getScrollToCurrentRow(currentRow, singleStepStartRow)));

			// highlight team
			boolean doneWithRow = false;
			while (!doneWithRow) {
				ITeam[] teams = contest.getOrderedTeams();
				ITeam team = teams[currentRow];
				steps.add(new TeamSelectionStep(team));
				timing.onStep(steps, State.SELECT_TEAM);

				if (runInfo == null || !runInfo.getTeam().equals(team)) {
					// nothing to resolve here
					if (timing instanceof ScoreboardTiming)
						timing.onStep(steps, State.SELECT_PROBLEM);
					timing.onStep(steps, State.DESELECT);
					doneWithRow = true;
				} else {
					// resolve runs until the team is done or it moves up
					while (runInfo != null && runInfo.getTeam().equals(team) && contest.getOrderOf(team) == currentRow) {
						// select run
						steps.add(new SubmissionSelectionStep(runInfo));
						boolean first = true;
						for (ISubmission r : contest.getSubmissions()) {
							if (!contest.isJudged(r) && r.getTeamId().equals(runInfo.getTeam().getId())
									&& r.getProblemId().equals(contest.getProblems()[runInfo.getProblemIndex()].getId())) {
								if (first) {
									steps.add(new SubmissionSelectionStep2(r.getId()));
									first = false;
								}
								judgeRuns.add(r.getId());
							}
						}
						timing.onStep(steps, State.SELECT_PROBLEM);

						// resolve
						resolveOneSubmission(runInfo, timing);

						// deselect
						steps.add(new SubmissionSelectionStep(null));
						timing.onStep(steps, State.DESELECT);

						// we're done with that pending submission; find next pending submission
						runInfo = getNextResolve();
					}
					// row is fully resolved if it hasn't moved and doesn't have the next pending run
					doneWithRow = contest.getOrderOf(team) == currentRow
							&& (runInfo == null || !runInfo.getTeam().equals(team));
				}

				boolean backToScoreboard = false;
				if (!teamLists.isEmpty()) {
					TeamListStep step = teamLists.get(0);
					if (step != null && allTeamsResolved(step, currentRow)
							&& differenceInScore(step, teamLists.size() == 1)) {
						Trace.trace(Trace.INFO, "Team list at row: " + currentRow);
						teamLists.remove(step);

						steps.add(new TeamSelectionStep(step.teams));
						steps.add(new PauseStep());
						steps.add(new ScrollTeamListStep(true));
						steps.add(step);
						steps.add(new PresentationStep(PresentationStep.Presentations.TEAM_LIST));
						steps.add(new PauseStep());
						steps.add(new ScrollTeamListStep(false));
						steps.add(new PauseStep());
						if (judgeStep != null) {
							int size = judgeRuns.size();
							String[] ii = new String[size];
							for (int i = 0; i < size; i++)
								ii[i] = judgeRuns.get(i);
							judgeStep.submissionIds = ii; // judgeRuns.toArray(new
																	// Integer[judgeRuns.size()]);
							judgeStep = null;
							judgeRuns.clear();
						}

						// check for awards we've missed
						if (timing instanceof JudgeQueueTiming) {
							ITeam[] teams2 = contest.getOrderedTeams();
							for (int r = teams2.length - 1; r >= currentRow; r--) {
								ITeam missedTeam = teams2[r];
								String missedTeamId = missedTeam.getId();
								List<IAward> teamAwards = awards.get(missedTeamId);
								if (teamAwards != null && !teamAwards.isEmpty()) {
									IOrganization org = contest.getOrganizationById(missedTeam.getOrganizationId());
									if (org == null)
										Trace.trace(Trace.INFO, "Catch up award at row: " + r + " " + missedTeamId);
									else
										Trace.trace(Trace.INFO,
												"Catch up award at row: " + r + " " + missedTeamId + " " + org.getFormalName());

									// are we going to show this on a separate page?
									boolean show = false;
									for (IAward award : teamAwards) {
										if (award.showAward())
											show = true;
									}

									if (show) {
										steps.add(new PresentationStep(PresentationStep.Presentations.TEAM_AWARD));
										steps.add(new AwardStep(missedTeamId, teamAwards));
										steps.add(new PauseStep());
									}
									awards.remove(missedTeam.getId());
								}
							}
						}

						timing = new ScoreboardTiming();
						backToScoreboard = true;
					}
				}
				// }

				if (doneWithRow) {
					// we've finished resolving a team, check if they have any awards
					List<IAward> teamAwards = awards.get(team.getId());
					if (!(timing instanceof JudgeQueueTiming) && teamAwards != null && !teamAwards.isEmpty()) {
						IOrganization org = contest.getOrganizationById(team.getOrganizationId());
						if (org != null)
							Trace.trace(Trace.INFO,
									"Award at row: " + currentRow + " " + team.getId() + " " + org.getFormalName());
						else
							Trace.trace(Trace.INFO, "Award at row: " + currentRow + " " + team.getId());

						if (timing instanceof PauseTiming && !(steps.get(steps.size() - 1) instanceof PauseStep))
							steps.add(new PauseStep());

						// are we going to show this on a separate page?
						boolean show = false;
						for (IAward award : teamAwards) {
							if (award.showAward())
								show = true;
						}

						boolean hasFTS = false;
						if (!teamAwards.isEmpty()) {
							for (IAward a : teamAwards) {
								if (a.getAwardType() == IAward.FIRST_TO_SOLVE)
									hasFTS = true;
							}
						}
						if (hasFTS) {
							if (show) {
								steps.add(new TeamSelectionStep(team, AbstractScoreboardPresentation.SelectType.FTS_HIGHLIGHT));
							} else {
								// it's an FTS-only award
								steps.add(new TeamSelectionStep(team, AbstractScoreboardPresentation.SelectType.FTS));
							}
						} else {
							// the award is for something other than FTS
							steps.add(new TeamSelectionStep(team, AbstractScoreboardPresentation.SelectType.HIGHLIGHT));
						}
						steps.add(new PauseStep());

						if (show) {
							// we have successfully resolved up to and including the next award row, so
							// show
							// it
							steps.add(new PresentationStep(PresentationStep.Presentations.TEAM_AWARD));
							steps.add(new AwardStep(team.getId(), teamAwards));
							steps.add(new PauseStep());

							// go back to the scoreboard presentation
							backToScoreboard = true;
						}
					}
				}
				if (backToScoreboard) {
					steps.add(new PresentationStep(PresentationStep.Presentations.SCOREBOARD));
					steps.add(new PauseStep());
				}
			}

			// move up one row
			currentRow--;
		}
		steps.add(new TeamSelectionStep());
		steps.add(new PauseStep());
	}

	protected boolean allTeamsResolved(TeamListStep step, int row) {
		if (step == null)
			return false;

		// check if all teams are resolved
		int numProblems = contest.getNumProblems();
		for (ITeam team : step.teams) {
			if (row > contest.getOrderOf(team))
				return false;
			for (int i = 0; i < numProblems; i++) {
				IResult r = contest.getResult(team, i);
				if (r.getStatus() == Status.SUBMITTED)
					return false;
			}
		}
		return true;
	}

	protected boolean differenceInScore(TeamListStep step, boolean lastList) {
		if (step == null)
			return false;

		// find the top row of this group
		int row = contest.getNumTeams() + 10;
		for (ITeam team : step.teams)
			row = Math.min(row, contest.getOrderOf(team));

		// check if the team above has solved a different number of problems
		ITeam team = step.teams[0];
		int numSolved = contest.getStanding(team).getNumSolved();
		ITeam[] teams = contest.getOrderedTeams();
		ITeam next = teams[row - 1];
		if (numSolved != contest.getStanding(next).getNumSolved())
			return true;

		// if it doesn't, there's only an exception if this is the last one...
		if (!lastList)
			return false;

		// ... and the final score won't be different
		teams = finalContest.getOrderedTeams();
		next = teams[row - 1];
		return numSolved == finalContest.getStanding(next).getNumSolved();
	}

	/**
	 * Returns the row number which should be put at the TOP of the screen in order to have the
	 * "currentRow" at the desired position on the screen. "Desired position" is normally at the
	 * BOTTOM; in this case the returned "scroll-to row" is above the "currentRow" by a constant
	 * based on the number of teams which should appear on one screen. A special case where "desired
	 * position" is NOT at the bottom is when the command-line option "--offsetMedalDisplay" has
	 * been specified; in this case the intent is to position the "currentRow" some number of rows
	 * upward (offsetting it above the heads of Bronze Medalists on-stage).
	 *
	 * @return the row number which should be at the top of the screen
	 */
	private int getScrollToCurrentRow(int currentRow, int lastMedalRow) {
		// TODOint offset = scoreboardPresentation.getTeamsPerScreen() - 2;
		int offset = 12 - 2;
		if (currentRow < lastMedalRow)
			offset -= rowOffset;

		return Math.max(0, currentRow - offset);
	}

	/**
	 * Generates an analysis of what happens to the team identified in the given SubmissionInfo
	 * object if they solve the problem specified in the SubmissionInfo object, including what the
	 * team's best possible standing would be (if they get a 'yes' on all their pending submissions
	 * and every other team gets "no's" on all theirs (i.e., best case for the team), and what
	 * happens if they get all "no's" and all other teams get "yes's" (i.e. worst case standing for
	 * the team). The analysis results are stored into arrays in the specified SubmissionInfo
	 * object.
	 *
	 * @param resolve The SubmissionInfo object identifying the Team and Problem to be resolved
	 */
	private void projectStandings(final SubmissionInfo resolve) {
		if (!calculateProjections)
			return;

		projectStandingsImpl(contest.clone(false), resolve);
	}

	private static void projectStandingsImpl(Contest contest, SubmissionInfo resolve) {
		IStanding currentStanding = contest.getStanding(resolve.getTeam());
		// what if they solve one?
		String teamId = resolve.getTeam().getId();
		ISubmission[] submissions = contest.getSubmissions();
		int numPendingRuns = 0;
		boolean otherProblemsPending = false;
		for (ISubmission submission : submissions) {
			if (submission.getTeamId().equals(teamId) && !contest.isJudged(submission)) {
				if (submission.getProblemId().equals(contest.getProblems()[resolve.getProblemIndex()].getId()))
					numPendingRuns++;
				else
					otherProblemsPending = true;
			}
		}

		IStanding[] standingIfSolved = new IStanding[numPendingRuns];
		IStanding[] standingBest = null;
		if (otherProblemsPending)
			standingBest = new IStanding[numPendingRuns];
		IStanding[] standingWorst = new IStanding[numPendingRuns];

		for (int i = 0; i < numPendingRuns; i++) {
			Contest contestIfSolved = contest.clone(false);
			submissions = contestIfSolved.getSubmissions();

			int j = 0;
			for (ISubmission submission : submissions) {
				if (submission.getTeamId().equals(teamId)
						&& submission.getProblemId().equals(contestIfSolved.getProblems()[resolve.getProblemIndex()].getId())
						&& !contest.isJudged(submission)) {
					if (j == i)
						contestIfSolved.setSubmissionIsSolved(submission, true);
					else
						contestIfSolved.setSubmissionIsSolved(submission, false);
					j++;
				}
			}

			standingIfSolved[i] = contestIfSolved.getStanding(contestIfSolved.getTeamById(teamId));

			// best possible case
			if (otherProblemsPending) {
				Contest contestBest = contestIfSolved.clone(false);
				submissions = contestBest.getSubmissions();
				for (ISubmission submission : submissions) {
					if (!contest.isJudged(submission)) {
						if (submission.getTeamId().equals(teamId))
							contestBest.setSubmissionIsSolved(submission, true);
					}
				}

				standingBest[i] = contestBest.getStanding(contestBest.getTeamById(teamId));
			}

			// what if they solve this problem but nothing else, and every other team solves all?
			Contest contestWorst = contestIfSolved.clone(false);
			submissions = contestWorst.getSubmissions();
			for (ISubmission submission : submissions) {
				if (!contest.isJudged(submission)) {
					if (submission.getTeamId().equals(teamId))
						contestWorst.setSubmissionIsSolved(submission, false);
					else
						contestWorst.setSubmissionIsSolved(submission, true);
				}
			}

			IStanding standing = contestWorst.getStanding(contestWorst.getTeamById(teamId));
			if (!standing.getRank().equals(currentStanding.getRank()))
				standingWorst[i] = standing;
		}

		resolve.setPossibleStandings(standingIfSolved, standingBest, standingWorst);
	}

	/**
	 * This method resolves (determines the effect of the result of) the pending submission
	 * specified by the received "submissionInfo" object.
	 *
	 * @param submissionInfo - a SubmissionInfo object indicating the pending submission to be
	 *           resolved
	 */
	private void resolveOneSubmission(SubmissionInfo submissionInfo, Timing timing) {
		ITeam team = submissionInfo.getTeam();

		// resolve one submission
		int oldRow = contest.getOrderOf(team);
		if (resolveSubmissionImpl(submissionInfo, timing)) {
			// submission was solved, check if the new position in the contest is different
			int newRow = contest.getOrderOf(team);
			if (newRow != oldRow) {
				// yes; they've moved up
				Trace.trace(Trace.INFO, "   Solved. Team moved from row " + oldRow + " to row " + newRow);
				timing.onStep(steps, State.SOLVED_MOVE);
				return;
			}

			// no; the team has not changed positions
			Trace.trace(Trace.INFO, "   Solved, no change in standings");
			timing.onStep(steps, State.SOLVED_STAY);
			return;
		}

		// no; the problem was not solved
		Trace.trace(Trace.INFO, "   Not solved, no change");
		timing.onStep(steps, State.FAILED);
	}

	private SubmissionInfo getNextResolve() {
		ITeam[] teams = contest.getOrderedTeams();
		int numProblems = contest.getNumProblems();
		for (int i = teams.length - 1; i >= 0; i--) {
			ITeam team = teams[i];

			// check for predetermined steps first
			for (PredeterminedStep ps : predeterminedSteps) {
				if (ps.teamId.equals(team.getId())) {
					int pInd = contest.getProblemIndexByLabel(ps.problemLabel);
					IResult r1 = contest.getResult(team, pInd);
					if (r1.getStatus() == Status.SUBMITTED) {
						return new SubmissionInfo(team, pInd);
					}
				}
			}

			// otherwise, default to pick left
			for (int j = 0; j < numProblems; j++) {
				IResult r1 = contest.getResult(team, j);
				if (r1.getStatus() == Status.SUBMITTED) {
					return new SubmissionInfo(team, j);
				}
			}
		}
		return null;
	}
}