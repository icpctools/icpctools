package org.icpc.tools.contest.model.resolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FreezeFilter;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IAward.DisplayMode;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IResolveInfo;
import org.icpc.tools.contest.model.IResolveInfo.PredeterminedStep;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.contest.model.TypeFilter;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ResolverPresentation;
import org.icpc.tools.contest.model.internal.ResolverSelection;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.DelayStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.DelayType;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.PauseStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ResolutionStep;
import org.icpc.tools.contest.model.util.AwardUtil;

/**
 * The Resolver 'logic'. This is the class that performs the actual resolving of submissions to
 * find the final result. It runs on startup to determine all the individual steps and timing that
 * the resolution will take.
 */
public class ResolverLogic {
	public static final String AFTER = "after";
	private static final String AFTER_ID = "afterId";
	private static final String SHOW_SCOREBOARD = "showScoreboard";

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

	// which row to start single-stepping on
	private int singleStepStartRow = -1;
	private boolean calculateProjections;

	// map from all the teamIds getting an award to the list of awards they're getting
	private Map<String, List<IAward>> awards = new HashMap<>();
	private List<IAward> teamLists = new ArrayList<>();
	private Map<String, IAward> afterAwards = new HashMap<>();

	private List<ResolutionStep> steps = new ArrayList<>();

	private Contest frozenContest;
	private Contest contest;
	private Contest finalContest;

	private List<PredeterminedStep> predeterminedSteps = new ArrayList<>();

	public ResolverLogic(Contest contest, boolean calculateProjections) {
		finalContest = filter(contest);
		this.calculateProjections = calculateProjections;

		IResolveInfo resolveInfo = finalContest.getResolveInfo();
		if (resolveInfo != null) {
			if (resolveInfo.getSingleStepRow() >= 0)
				this.singleStepStartRow = resolveInfo.getSingleStepRow();
			this.predeterminedSteps = resolveInfo.getPredeterminedSteps();
		}

		if (predeterminedSteps == null)
			this.predeterminedSteps = new ArrayList<>(0);

		// defaults to -2 (don't start single step automatically) if no medals exist
		if (this.singleStepStartRow < 0 && finalContest.getAwards().length > 0) {
			IAward[] awards2 = finalContest.getAwards();
			for (IAward a : awards2) {
				if (a.getAwardType() == IAward.MEDAL) {
					if (a.getTeamIds() != null) {
						for (String id : a.getTeamIds()) {
							ITeam team = finalContest.getTeamById(id);
							if (team != null) {
								int row = finalContest.getOrderOf(team);
								this.singleStepStartRow = Math.max(row, this.singleStepStartRow);
							}
						}
					}
				}
			}
			int lastBronze = AwardUtil.getLastBronze(finalContest);
			this.singleStepStartRow = Math.max(lastBronze - 1, this.singleStepStartRow);
		}
	}

	public Contest getFrozenContest() {
		return frozenContest;
	}

	/**
	 * Configure the resolver and run against the given contest.
	 *
	 * @param time the starting time (in seconds) to resolve from
	 * @return the full list of resolution steps
	 */
	public List<ResolutionStep> resolveFrom(boolean startWithJudgeQueue) {
		Trace.trace(Trace.INFO, "Resolving...");
		long time = System.currentTimeMillis();

		// cache all the awards by team id
		IAward[] contestAwards = finalContest.getAwards();
		AwardUtil.sortAwards(contest, contestAwards);
		for (IAward award : contestAwards) {
			String[] teamIds = award.getTeamIds();
			if (teamIds != null) {
				if (award.getParameters() != null && award.getParameters().containsKey(AFTER_ID)) {
					String afterId = award.getParameters().get(AFTER_ID);
					IAward aw = afterAwards.get(afterId);
					if (aw != null) {
						Trace.trace(Trace.ERROR, afterId + " is duplicated, chain awards instead");
						System.exit(0);
					}
					afterAwards.put(afterId, award);
				} else if (award.getDisplayMode() == DisplayMode.IGNORE || teamIds.length == 0) {
					// requested to ignore, or no/empty award
				} else if (award.getDisplayMode() != DisplayMode.LIST && award.getDisplayMode() != DisplayMode.PHOTOS) {
					for (String teamId : teamIds) {
						List<IAward> aw = awards.get(teamId);
						if (aw == null) {
							aw = new ArrayList<>();
							awards.put(teamId, aw);
						}
						aw.add(award);
					}
				} else {
					teamLists.add(award);
				}
			}
		}
		Trace.trace(Trace.USER, contestAwards.length + " awards assigned");

		// revert to start of the freeze
		contest = finalContest.clone(new FreezeFilter(finalContest));
		frozenContest = contest;

		// clean up unjudged and non-penalty submissions in the last hour that don't matter and will
		// mess with resolving
		cleanOutlierSubmissions();

		// set the initial state
		steps.add(new ResolverPresentation(ResolverPresentation.Presentations.SPLASH));
		steps.add(new PauseStep());
		steps.add(new ResolverSelection());

		Trace.trace(Trace.USER, "Resolving " + countUnjudgedSubmissions(contest) + " pending submissions out of the "
				+ contest.getNumSubmissions() + " total submissions in the contest... ");

		// take our 'moving copy' to make changes without touching the initial contest
		contest = contest.clone(false);

		// first click - show the scoreboard
		steps.add(new ResolverPresentation(ResolverPresentation.Presentations.SCOREBOARD).withScroll(true));
		steps.add(new PauseStep());

		// second click - scroll down to bottom (so last team is at bottom of screen)
		steps.add(new ResolverPresentation(ResolverPresentation.Presentations.SCOREBOARD).withScroll(false));
		steps.add(new PauseStep());

		// third click - select the team or switch to judge queue

		// fourth click and beyond: start resolving!
		resolveEverything(startWithJudgeQueue);

		ResolutionUtil.numberThePauses(steps);

		Trace.trace(Trace.USER, "Done resolving in " + (System.currentTimeMillis() - time) + " ms");

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

	/**
	 * Some submissions (like compile errors) have no impact on the resolving process but show up as
	 * odd or unexpected behaviour in the UI because they typically 'automatically disappear' from
	 * scoreboards during the regular contest.
	 *
	 * This releases these judgements before the resolving process begins to avoid confusion.
	 **/
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
					IJudgement[] sjs = finalContest.getJudgementsBySubmissionId(submission.getId());
					if (sjs != null) {
						for (IJudgement sj : sjs)
							steps.add(sj);
					}
				}
			}
		}

		// remove non-penalty judgements
		// TODO - probably shouldn't!!
		submissions = finalContest.getSubmissions();
		for (ISubmission submission : submissions) {
			IJudgement[] sjs = finalContest.getJudgementsBySubmissionId(submission.getId());
			if (sjs != null) {
				for (IJudgement sj : sjs) {
					IJudgementType jt = finalContest.getJudgementTypeById(sj.getJudgementTypeId());
					if (jt != null && !jt.isSolved() && !jt.isPenalty() && !contest.isBeforeFreeze(submission)) {
						steps.add(sj);
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
	private boolean resolveSubmissionImpl(SubmissionInfo submissionInfo, ResolverSelection selStep, Timing timing) {
		Trace.trace(Trace.INFO, "Resolving: " + submissionInfo);

		ITeam team = submissionInfo.getTeam();
		String problemId = submissionInfo.getProblemId();
		IResult r = contest.getResult(team, problemId);

		// check if the specified submission is "pending"
		if (r.getStatus() != Status.SUBMITTED) {
			Trace.trace(Trace.ERROR, "Trying to resolve a judged submission");
			return false;
		}

		// yes, it's a pending submission; look through all the submissions in the contest to find it
		ISubmission[] submissions = contest.getSubmissions();
		for (ISubmission submission : submissions) {
			if (!contest.isJudged(submission) && submission.getTeamId().equals(team.getId())
					&& submission.getProblemId().equals(problemId)) {
				// we found it; update its status to match the correct (final contest) status
				IJudgement[] sjs = finalContest.getJudgementsBySubmissionId(submission.getId());
				if (sjs != null) {
					for (IJudgement sj : sjs) {
						contest.add(sj);
						steps.add(sj);
					}
				}

				steps.add(selStep.withSubmissionId(submission.getId()));
				timing.onStep(steps, State.SELECT_SUBMISSION);

				// there may be more submissions from this team for this problem so no break

				IResult newResult = contest.getResult(team, problemId);
				if (newResult.getScore() > r.getScore()) {
					break;
				}
			}
		}

		// get the newly-computed result for the current team on the specified problem
		IResult newResult = contest.getResult(team, problemId);

		// get the equivalent information out of the final contest
		IResult finalResult = finalContest.getResult(team, problemId);

		// double check that the two match
		if (newResult.getStatus() != finalResult.getStatus())
			Trace.trace(Trace.ERROR, "Results don't match! " + newResult + " -> " + finalResult);

		// return true if the pending submission was a YES; false otherwise
		return newResult.getStatus() == Status.SOLVED;
	}

	private void resolveEverything(boolean startWithJudgeQueue) {
		ResolverSelection selStep = new ResolverSelection();

		Timing timing = null;
		if (startWithJudgeQueue && !teamLists.isEmpty()) {
			steps.add(new ResolverPresentation(ResolverPresentation.Presentations.JUDGE));
			timing = new JudgeQueueTiming();
		} else {
			ITeam[] teams = contest.getOrderedTeams();
			steps.add(selStep.withTeam(teams[teams.length - 1]));
			timing = new ScoreboardTiming();
		}
		steps.add(new PauseStep());

		List<String> judgeRuns = null;
		ResolutionStep placeholder = (ResolutionStep) new Object();
		if (startWithJudgeQueue && !teamLists.isEmpty()) {
			// add a placeholder so we can add the submissions we're going to judge later
			steps.add(placeholder);
			steps.add(new PauseStep());
			judgeRuns = new ArrayList<>();
		}

		SubmissionInfo runInfo = getNextResolve();
		// Project standings for the first run, if we have any. We don't have any for an empty
		// scoreboard
		if (runInfo != null)
			projectStandings(runInfo);

		int currentRow = contest.getOrderedTeams().length - 1;
		while (currentRow >= 0) {
			Trace.trace(Trace.INFO, "Resolving row: " + currentRow);

			if (currentRow <= singleStepStartRow) {
				// singleStep = true;
				timing = new PauseTiming();
			}

			// highlight team
			boolean doneWithRow = false;
			while (!doneWithRow) {
				ITeam[] teams = contest.getOrderedTeams();
				ITeam team = teams[currentRow];
				steps.add(selStep.withTeam(team));
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
						steps.add(selStep.withProblem(runInfo));
						boolean first = true;
						for (ISubmission r : contest.getSubmissions()) {
							if (!contest.isJudged(r) && r.getTeamId().equals(runInfo.getTeam().getId())
									&& r.getProblemId().equals(runInfo.getProblemId())) {
								if (first) {
									steps.add(selStep.withSubmissionId(r.getId()));
									first = false;
								}
								if (judgeRuns != null) {
									judgeRuns.add(r.getId());
								}
							}
						}
						timing.onStep(steps, State.SELECT_PROBLEM);

						// resolve
						resolveOneSubmission(runInfo, selStep, timing);

						// deselect
						steps.add(selStep.withProblem(null));
						timing.onStep(steps, State.DESELECT);

						// we're done with that pending submission; find next pending submission
						runInfo = getNextResolve();
						// Also project standings for this run, if we have any
						if (runInfo != null)
							projectStandings(runInfo);
					}
					// row is fully resolved if it hasn't moved and doesn't have the next pending run
					doneWithRow = contest.getOrderOf(team) == currentRow
							&& (runInfo == null || !runInfo.getTeam().equals(team));
				}

				boolean backToScoreboard = false;
				IAward award = getListAward(currentRow, false);
				if (award != null) {
					Trace.trace(Trace.INFO, "Team list at row: " + currentRow + " " + award);
					teamLists.remove(award);

					if (ResolutionUtil.shouldHighlight(award))
						steps.add(selStep.withTeams(award.getTeamIds()));
					steps.add(new PauseStep());

					if (award.getDisplayMode() == DisplayMode.PHOTOS) {
						steps.add(new ResolverPresentation(ResolverPresentation.Presentations.TEAM_PHOTOS)
								.withAwardId(award.getId()));
					} else {
						steps.add(new ResolverPresentation(ResolverPresentation.Presentations.TEAM_LIST)
								.withAwardId(award.getId()).withScroll(true));
						steps.add(new PauseStep());
						steps.add(new ResolverPresentation(ResolverPresentation.Presentations.TEAM_LIST).withScroll(false));
					}
					steps.add(new PauseStep());
					checkAfterAwards(award.getId());
					if (judgeRuns != null) {
						String[] subIds = judgeRuns.toArray(new String[0]);
						int ind = steps.indexOf(placeholder);
						steps.set(ind,
								new ResolverPresentation(ResolverPresentation.Presentations.JUDGE).withSubmissionIds(subIds));
						judgeRuns = null;
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
											"Catch up award at row: " + r + " " + missedTeamId + " " + org.getActualFormalName());

								// are we going to show this on a separate page?
								boolean show = false;
								for (IAward award2 : teamAwards) {
									if (award2.getDisplayMode() == DisplayMode.DETAIL)
										show = true;
								}

								if (show) {
									for (IAward a : teamAwards) {
										steps.add(a);
									}
									steps.add(new ResolverPresentation(ResolverPresentation.Presentations.TEAM_AWARD)
											.withTeamId(missedTeamId));
									steps.add(new PauseStep());
								}
								awards.remove(missedTeam.getId());
							}
						}
						checkAfterAwards("judge-queue");
					}

					timing = new ScoreboardTiming();
					if (currentRow <= singleStepStartRow) {
						// singleStep = true;
						timing = new PauseTiming();
					}
					// backToScoreboard = true;

					steps.add(new ResolverPresentation(ResolverPresentation.Presentations.SCOREBOARD));
					steps.add(new PauseStep());
				}

				if (doneWithRow) {
					// we've finished resolving a team, check if they have any awards
					List<IAward> teamAwards = awards.get(team.getId());
					if (!(timing instanceof JudgeQueueTiming) && teamAwards != null && !teamAwards.isEmpty()) {
						IOrganization org = contest.getOrganizationById(team.getOrganizationId());
						if (org != null)
							Trace.trace(Trace.INFO,
									"Award at row: " + currentRow + " " + team.getId() + " " + org.getActualFormalName());
						else
							Trace.trace(Trace.INFO, "Award at row: " + currentRow + " " + team.getId());

						if (timing instanceof PauseTiming && !(steps.get(steps.size() - 1) instanceof PauseStep))
							steps.add(new PauseStep());

						// are we going to show this on a separate page?
						boolean show = false;
						for (IAward award2 : teamAwards) {
							if (award2.getDisplayMode() == DisplayMode.DETAIL)
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
								steps.add(selStep.withTeam(team, SelectType.FTS_HIGHLIGHT));
							} else {
								// it's an FTS-only award
								steps.add(selStep.withTeam(team, SelectType.FTS));
							}
						} else {
							// the award is for something other than FTS
							steps.add(selStep.withTeam(team, SelectType.HIGHLIGHT));// TODO
						}
						steps.add(new PauseStep());

						if (show) {
							// we have successfully resolved up to and including the next award row, so
							// show it
							for (IAward a : teamAwards) {
								steps.add(a);
							}
							steps.add(new ResolverPresentation(ResolverPresentation.Presentations.TEAM_AWARD)
									.withTeamId((team.getId())));
							steps.add(new PauseStep());

							for (IAward award2 : teamAwards) {
								checkAfterAwards(award2.getId());
							}

							// go back to the scoreboard presentation
							backToScoreboard = true;
						}
					}

					if (checkAfterAwards(currentRow + ""))
						steps.add(new ResolverPresentation(ResolverPresentation.Presentations.SCOREBOARD));

					award = getListAward(currentRow, true);
					while (award != null) {
						Trace.trace(Trace.INFO, "Team list at row after: " + currentRow + " " + award);
						teamLists.remove(award);

						if (ResolutionUtil.showScoreboardBefore(award) && backToScoreboard)
							steps.add(new ResolverPresentation(ResolverPresentation.Presentations.SCOREBOARD));

						if (ResolutionUtil.shouldHighlight(award))
							steps.add(selStep.withTeams(award.getTeamIds()));
						// Note: if this ever is false for an award that doesn't happen AFTER another
						// list award, we'll
						// need to change the logic here to pause before showing the list award
						if (ResolutionUtil.showScoreboardBefore(award))
							steps.add(new PauseStep());

						if (award.getDisplayMode() == DisplayMode.PHOTOS) {
							steps.add(new ResolverPresentation(ResolverPresentation.Presentations.TEAM_PHOTOS)
									.withAwardId(award.getId()));
						} else {
							steps.add(new ResolverPresentation(ResolverPresentation.Presentations.TEAM_LIST)
									.withAwardId(award.getId()).withScroll(true));
							steps.add(new PauseStep());
							steps.add(
									new ResolverPresentation(ResolverPresentation.Presentations.TEAM_LIST).withScroll(false));
						}
						steps.add(new PauseStep());
						checkAfterAwards(award.getId());

						backToScoreboard = true;

						award = getListAward(currentRow, true);
					}
				}
				if (backToScoreboard) {
					steps.add(new ResolverPresentation(ResolverPresentation.Presentations.SCOREBOARD));
					steps.add(new PauseStep());
				}
			}

			// move up one row
			currentRow--;
		}
		steps.add(new ResolverSelection());
		steps.add(new PauseStep());
	}

	protected IAward getListAward(int currentRow, boolean after) {
		for (IAward step : teamLists) {
			if (isListReadyToDisplay(step, currentRow, after)) {
				return step;
			}
		}
		return null;
	}

	protected boolean checkAfterAwards(String id) {
		IAward award = afterAwards.get(id);
		Trace.trace(Trace.INFO, "Checking after award: " + id + " " + (award != null));
		if (award != null) {
			if ("true".equals(award.getParameters().get(SHOW_SCOREBOARD))) {
				steps.add(new ResolverPresentation(ResolverPresentation.Presentations.SCOREBOARD));
				steps.add(new PauseStep());
			}
			String[] teamIds = award.getTeamIds();

			if (award.getDisplayMode() == DisplayMode.MESSAGE) {
				steps.add(new ResolverPresentation(ResolverPresentation.Presentations.MESSAGE)
						.withMessage(award.getCitation()));
			} else if (award.getDisplayMode() != DisplayMode.LIST && award.getDisplayMode() != DisplayMode.PHOTOS) {
				steps.add(award);
				steps.add(new ResolverPresentation(ResolverPresentation.Presentations.TEAM_AWARD).withTeamId(teamIds[0]));
			} else {
				steps.add(award);
				if (award.getDisplayMode() == DisplayMode.PHOTOS) {
					steps.add(new ResolverPresentation(ResolverPresentation.Presentations.TEAM_PHOTOS)
							.withAwardId(award.getId()));
				} else {
					steps.add(new ResolverPresentation(ResolverPresentation.Presentations.TEAM_LIST)
							.withAwardId(award.getId()).withScroll(true));
					steps.add(new PauseStep());
					steps.add(new ResolverPresentation(ResolverPresentation.Presentations.TEAM_LIST).withScroll(false));
				}
			}
			steps.add(new PauseStep());
			if (!id.equals(award.getId())) {
				checkAfterAwards(award.getId());
			}
			afterAwards.remove(id);
			return true;
		}
		return false;
	}

	protected boolean isListReadyToDisplay(IAward award, int row, boolean after) {
		if (award == null)
			return false;

		String[] teamIds = award.getTeamIds();
		if (award.getParameters() != null && award.getParameters().containsKey("before")) {
			if (!after)
				return false;
			int targetRow = -1;
			try {
				targetRow = Integer.parseInt(award.getParameters().get("before"));
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Could not parse before award parameter " + award.getParameters().get("before"));
				return true;
			}

			if (row <= targetRow)
				return true;
		}

		if (after != ResolutionUtil.after(award))
			return false;

		// we've done all the teams
		int numTeaams = teamIds.length;
		for (int i = 0; i < numTeaams; i++) {
			ITeam team = finalContest.getTeamById(teamIds[i]);
			if (award.getParameters() != null && award.getParameters().containsKey("numSolved")) {
				if (finalContest.getOrderOf(team) < row)
					return false;
			}
		}

		// check if all teams are resolved
		int numProblems = contest.getNumProblems();
		for (String teamId : award.getTeamIds()) {
			ITeam team = contest.getTeamById(teamId);
			if (row > contest.getOrderOf(team))
				return false;
			for (int i = 0; i < numProblems; i++) {
				IResult r = contest.getResult(team, i);
				if (r.getStatus() == Status.SUBMITTED)
					return false;
			}
		}

		// for solution and honor awards, wait until every team higher on the scoreboard has solved
		// more
		// problems than the given number
		if ((award.getAwardType() == IAward.SOLVED || award.getAwardType() == IAward.HONORS)
				&& award.getParameters() != null && award.getParameters().containsKey("numSolved")) {
			int numSolved = -1;
			try {
				numSolved = Integer.parseInt(award.getParameters().get("numSolved"));
			} catch (Exception e) {
				Trace.trace(Trace.ERROR,
						"Could not parse solved award parameter " + award.getParameters().get("numSolved"));
			}

			if (numSolved > 0) {
				ITeam[] teams = contest.getOrderedTeams();
				for (int i = 0; i < row; i++) {
					IStanding s = contest.getStanding(teams[i]);
					if (s.getNumSolved() <= numSolved)
						return false;
				}
				if (!after) {
					IStanding s = contest.getStanding(teams[row]);
					if (s.getNumSolved() <= numSolved)
						return false;
				}
			}
		}

		return true;
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
				if (submission.getProblemId().equals(resolve.getProblemId()))
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
				if (submission.getTeamId().equals(teamId) && submission.getProblemId().equals(resolve.getProblemId())
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
			if (standing.getRank() != currentStanding.getRank())
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
	private void resolveOneSubmission(SubmissionInfo submissionInfo, ResolverSelection selStep, Timing timing) {
		ITeam team = submissionInfo.getTeam();

		// resolve one submission
		int oldRow = contest.getOrderOf(team);
		if (resolveSubmissionImpl(submissionInfo, selStep, timing)) {
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
		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;
		for (int i = teams.length - 1; i >= 0; i--) {
			ITeam team = teams[i];

			// check for predetermined steps first
			for (PredeterminedStep ps : predeterminedSteps) {
				if (ps.teamLabel.equals(team.getLabel())) {
					int pInd = contest.getProblemIndexByLabel(ps.problemLabel);
					if (pInd >= 0) {
						IResult r1 = contest.getResult(team, pInd);
						if (r1.getStatus() == Status.SUBMITTED) {
							return new SubmissionInfo(team, problems[pInd].getId());
						}
					}
				}
			}

			// otherwise, default to pick left
			for (int j = 0; j < numProblems; j++) {
				IResult r1 = contest.getResult(team, j);
				if (r1.getStatus() == Status.SUBMITTED) {
					return new SubmissionInfo(team, problems[j].getId());
				}
			}
		}
		return null;
	}

	public static Contest filter(Contest contest) {
		Contest c = removeUnnecessaryTypes(contest);
		c.removeHiddenTeams();
		c.removeSubmissionsOutsideOfContestTime();
		return c;
	}

	private static Contest removeUnnecessaryTypes(Contest contest) {
		List<ContestType> types = new ArrayList<>();
		types.add(ContestType.CONTEST);
		types.add(ContestType.STATE);
		types.add(ContestType.TEAM);
		types.add(ContestType.PERSON);
		types.add(ContestType.ORGANIZATION);
		types.add(ContestType.GROUP);
		types.add(ContestType.PROBLEM);
		types.add(ContestType.SUBMISSION);
		types.add(ContestType.JUDGEMENT);
		types.add(ContestType.JUDGEMENT_TYPE);
		types.add(ContestType.LANGUAGE);
		types.add(ContestType.AWARD);
		types.add(ContestType.RESOLVE_INFO);
		TypeFilter filter = new TypeFilter(types);
		return contest.clone(false, filter);
	}
}
