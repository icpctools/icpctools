package org.icpc.tools.contest.model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * A contest.
 */
public interface IContest {
	/**
	 * Add a listener to the contest.
	 *
	 * @param listener
	 */
	public void addListener(IContestListener listener);

	/**
	 * Remove a listener from the contest.
	 *
	 * @param listener
	 */
	public void removeListener(IContestListener listener);

	/**
	 * Returns the id of the contest.
	 *
	 * @return the id
	 */
	public String getId();

	/**
	 * Returns the name of the contest.
	 *
	 * @return the name
	 */
	public String getName();

	/**
	 * Returns the formal name of the contest.
	 *
	 * @return the formal name
	 */
	public String getFormalName();

	/**
	 * A positive # indicates a contest start time in seconds from the Unix epoch (Jan 1, 1970). A
	 * negative # indicates that the contest start is paused and gives the negative time of the
	 * pause, in seconds (e.g. -65 means it is paused 1m 5s before the start of the contest). Null
	 * indicates that the start time is not known or set.
	 *
	 * @return the start time
	 */
	public Long getStartTime();

	/**
	 * Returns the time the countdown is paused at, or null if there is no pause. Only one of the
	 * state time and pause time may be non-null at a time.
	 *
	 * @return the time remaining before the contest, in ms
	 */
	public Integer getCountdownPauseTime();

	/**
	 * Returns the duration of the contest, in ms.
	 *
	 * @return the duration
	 */
	public int getDuration();

	/**
	 * Returns the duration of the end of contest freeze, in ms, or -1 if there is no freeze in this
	 * contest.
	 *
	 * @return the freeze duration
	 */
	public int getFreezeDuration();

	/**
	 * Returns the penalty time. -1 means there is no concept of penalty time; 0 indicates there is
	 * no penalty.
	 *
	 * @return the penalty time
	 */
	public int getPenaltyTime();

	/**
	 * Returns the time multiplier, if the contest is in test/playback mode. Otherwise, returns 1.
	 *
	 * @return the time multiplier
	 */
	public double getTimeMultiplier();

	/**
	 * Returns the logo file.
	 *
	 * @return
	 */
	public File getLogo(int width, int height, boolean force);

	/**
	 * Returns the logo image.
	 *
	 * @return
	 */
	public BufferedImage getLogoImage(int width, int height, boolean forceLoad, boolean resizeToFit);

	/**
	 * Returns the banner file.
	 *
	 * @return
	 */
	public File getBanner(int width, int height, boolean force);

	/**
	 * Returns the contest banner image.
	 *
	 * @return
	 */
	public BufferedImage getBannerImage(int width, int height, boolean forceLoad, boolean resizeToFit);

	/**
	 * Return the contest state.
	 *
	 * @return the state
	 */
	public IState getState();

	/**
	 * Returns the total number of events in this contest.
	 *
	 * @return
	 */
	public int getNumObjects();

	/**
	 * Elapsed contest time, in ms. A lower bound - based on the contest time of the most recent
	 * event!
	 *
	 * @return
	 */
	public int getContestTimeOfLastEvent();

	/**
	 * Return the most recent timed contest object.
	 *
	 * @return
	 */
	public IContestObject getLastTimedObject();

	/**
	 * Return the most recent timed contest object's index.
	 *
	 * @return
	 */
	public int getLastTimedObjectEventIndex();

	/**
	 * Returns the countdown status.
	 *
	 * @return the countdown status
	 */
	public ICountdown getCountdown();

	/**
	 * Returns a start status for the contest time. Null indicates the contest start is not set.
	 * Negative numbers indicate a countdown pause, in ms. Positive numbers indicate contest time,
	 * in ms. Long.MAX_VALUE indicates the contest is over.
	 *
	 * @return
	 */
	public Long getStartStatus();

	/**
	 * Returns the pauses.
	 *
	 * @return the pauses
	 */
	public IPause[] getPauses();

	/**
	 * Returns a list of all languages used in the contest.
	 *
	 * @return a list of all languages
	 */
	public ILanguage[] getLanguages();

	/**
	 * Returns the language with the given id.
	 *
	 * @param id an identifier
	 * @return a language, or <code>null</code> if the id was invalid or doesn't exist
	 */
	public ILanguage getLanguageById(String id);

	/**
	 * Returns a list of all judgement types used in the contest.
	 *
	 * @return a list of all judgement types
	 */
	public IJudgementType[] getJudgementTypes();

	/**
	 * Returns the judgement type with the given id.
	 *
	 * @param id an identifier
	 * @return a judgement type, or <code>null</code> if the id was invalid or doesn't exist
	 */
	public IJudgementType getJudgementTypeById(String id);

	/**
	 * Return the judgement type for a given submission.
	 *
	 * @param submission
	 * @return
	 */
	public IJudgementType getJudgementType(ISubmission submission);

	/**
	 * Returns a list of all groups in the contest.
	 *
	 * @return a list of all groups
	 */
	public IGroup[] getGroups();

	/**
	 * Return the group with the given id, or null if it doesn't exist.
	 *
	 * @param groupId
	 * @return a group
	 */
	public IGroup getGroupById(String groupId);

	/**
	 * Return the groups with the given ids, or null if it doesn't exist.
	 *
	 * @param groupIds
	 * @return an array of groups
	 */
	public IGroup[] getGroupsByIds(String[] groupIds);

	/**
	 * Returns the total number of problems.
	 *
	 * @return the total number of problems
	 */
	public int getNumProblems();

	/**
	 * Returns a list of all problems in the contest.
	 *
	 * @return a list of all problems
	 */
	public IProblem[] getProblems();

	/**
	 * Return the problem with the given id, or null if it doesn't exist.
	 *
	 * @param problemId
	 * @return a problem
	 */
	public IProblem getProblemById(String problemId);

	/**
	 * Return the index of the problem in the array of problems based on the problem id, or -1 if
	 * the problem does not exist.
	 *
	 * @param problemId
	 * @return an array index
	 */
	public int getProblemIndex(String problemId);

	/**
	 * Returns the total number of organizations.
	 *
	 * @return the total number of organizations
	 */
	public int getNumOrganizations();

	/**
	 * Returns a list of all organizations competing in the contest.
	 *
	 * @return a list of all organizations
	 */
	public IOrganization[] getOrganizations();

	/**
	 * Returns the organization with the given id.
	 *
	 * @param id an identifier
	 * @return an organization, or <code>null</code> if the id was invalid
	 */
	public IOrganization getOrganizationById(String id);

	/**
	 * Returns the total number of teams.
	 *
	 * @return the total number of teams
	 */
	public int getNumTeams();

	/**
	 * Returns a list of all teams competing in the contest.
	 *
	 * @return a list of all teams
	 */
	public ITeam[] getTeams();

	/**
	 * Returns the team with the given id.
	 *
	 * @param id an identifier
	 * @return a team, or <code>null</code> if the id was invalid
	 */
	public ITeam getTeamById(String id);

	/**
	 * Returns true if the team belongs to one or more hidden groups.
	 *
	 * @param team
	 * @return
	 */
	public boolean isTeamHidden(ITeam team);

	/**
	 * Returns the current standing for the given team.
	 *
	 * @param team a team
	 * @return a standing, or <code>null</code> if the team was invalid
	 */
	public IStanding getStanding(ITeam team);

	/**
	 * Returns a list of teams, ordered by their current rank.
	 *
	 * @return an ordered list of teams
	 */
	public ITeam[] getOrderedTeams();

	/**
	 * Returns the order (index in list of ordered teams) of the given team.
	 *
	 * @param team
	 * @return an index, or -1 if the team was invalid or hidden
	 */
	public int getOrderOf(ITeam team);

	/**
	 * Returns the total number of team members.
	 *
	 * @return the total number of team members
	 */
	public int getNumTeamMembers();

	/**
	 * Returns a list of all team members competing in the contest.
	 *
	 * @return a list of all team members
	 */
	public ITeamMember[] getTeamMembers();

	/**
	 * Returns a list of all team members in the given team id.
	 *
	 * @return a list of all team members in the given team id
	 */
	public ITeamMember[] getTeamMembersByTeamId(String teamId);

	/**
	 * Returns the total number of submissions.
	 *
	 * @return the total number of submissions
	 */
	public int getNumSubmissions();

	/**
	 * Returns the submission with the given id.
	 *
	 * @param id an identifier
	 * @return a submission, or <code>null</code> if the id was invalid
	 */
	public ISubmission getSubmissionById(String id);

	/**
	 * Returns a list of all submissions in the contest.
	 *
	 * @return a list of all submissions
	 */
	public ISubmission[] getSubmissions();

	/**
	 * Returns the total number of submission judgements.
	 *
	 * @return the total number of submission judgements
	 */
	public int getNumJudgements();

	/**
	 * Returns the submission judgement with the given id.
	 *
	 * @param id an identifier
	 * @return a submission judgement, or <code>null</code> if the id was invalid
	 */
	public IJudgement getJudgementById(String id);

	/**
	 * Returns the submission judgements with the given submission id.
	 *
	 * @param id an identifier
	 * @return submission judgements, or <code>null</code> if the id was invalid
	 */
	public IJudgement[] getJudgementsBySubmissionId(String id);

	/**
	 * Returns a list of all submission judgements in the contest.
	 *
	 * @return a list of all submission judgements
	 */
	public IJudgement[] getJudgements();

	/**
	 * Returns the total number of clarifications.
	 *
	 * @return the total number of clarifications
	 */
	public int getNumClarifications();

	/**
	 * Returns a list of all clarifications in the contest.
	 *
	 * @return a list of all clarifications
	 */
	public IClarification[] getClarifications();

	/**
	 * Return the clarification with the given id, or null if it doesn't exist.
	 *
	 * @param clarId
	 * @return a clarification
	 */
	public IClarification getClarificationById(String clarId);

	/**
	 * Returns the total number of runs.
	 *
	 * @return the total number of runs
	 */
	public int getNumRuns();

	/**
	 * Returns a list of all runs in the contest.
	 *
	 * @return a list of all runs
	 */
	public IRun[] getRuns();

	/**
	 * Return the run with the given id, or null if it doesn't exist.
	 *
	 * @param runId
	 * @return a run
	 */
	public IRun getRunById(String runId);

	/**
	 * Return the runs for a given judgement id.
	 *
	 * @param id
	 * @return
	 */
	public IRun[] getRunsByJudgementId(String id);

	public Status getStatus(ISubmission submission);

	public boolean isSolved(ISubmission submission);

	public boolean isJudged(ISubmission submission);

	public boolean isFirstToSolve(ISubmission submission);

	/**
	 * Return the result of a given problem for this team.
	 *
	 * @param teamIndex
	 * @param problemIndex
	 * @return
	 */
	public IResult getResult(int teamIndex, int problemIndex);

	/**
	 * Return the result of a given problem for this team.
	 *
	 * @param team
	 * @param problemIndex
	 * @return
	 */
	public IResult getResult(ITeam team, int problemIndex);

	/**
	 * Return a summary of the results for a given problem.
	 *
	 * @param problemIndex
	 * @return
	 */
	public IProblemSummary getProblemSummary(int problemIndex);

	/**
	 * Returns true if the submission was before the contest freeze.
	 *
	 * @return
	 */
	public boolean isBeforeFreeze(ISubmission s);

	/**
	 * Return the awards given in this contest.
	 *
	 * @return the awards
	 */
	public IAward[] getAwards();

	/**
	 * Returns the award with the given id.
	 *
	 * @param id an identifier
	 * @return an award, or <code>null</code> if the id was invalid
	 */
	public IAward getAwardById(String id);

	/**
	 * Validate all contest objects that can be validated.
	 *
	 * @return a list of validation issues
	 */
	public List<String> validate();
}