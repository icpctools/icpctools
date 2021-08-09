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
	void addListener(IContestListener listener);

	/**
	 * Remove a listener from the contest.
	 *
	 * @param listener
	 */
	void removeListener(IContestListener listener);

	/**
	 * Returns the id of the contest.
	 *
	 * @return the id
	 */
	String getId();

	/**
	 * Returns the name of the contest.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the formal name of the contest.
	 *
	 * @return the formal name
	 */
	String getFormalName();

	/**
	 * Returns the formal name of the contest, or fall back to the name.
	 *
	 * @return the formal name
	 */
	String getActualFormalName();

	/**
	 * The contest start time in ms from the Unix epoch (Jan 1, 1970). Null indicates that the start
	 * time is not known or set.
	 *
	 * @return the start time, in ms since the epoch
	 */
	Long getStartTime();

	/**
	 * Returns the time the countdown is paused at, or null if there is no pause. Only one of the
	 * start time and pause time may be non-null at a time.
	 *
	 * @return the time remaining before the contest, in ms
	 */
	Integer getCountdownPauseTime();

	/**
	 * Returns the duration of the contest, in ms.
	 *
	 * @return the duration
	 */
	int getDuration();

	/**
	 * Returns the duration of the end of contest freeze, in ms, or -1 if there is no freeze in this
	 * contest.
	 *
	 * @return the freeze duration
	 */
	int getFreezeDuration();

	/**
	 * Returns the penalty time. -1 means there is no concept of penalty time; 0 indicates there is
	 * no penalty.
	 *
	 * @return the penalty time
	 */
	int getPenaltyTime();

	/**
	 * Returns the time multiplier, if the contest is in test/playback mode. Otherwise, returns 1.
	 *
	 * @return the time multiplier
	 */
	double getTimeMultiplier();

	/**
	 * The latitude of the contest location.
	 *
	 * @return the latitude
	 */
	double getLatitude();

	/**
	 * The longitude of the contest location.
	 *
	 * @return the longitude
	 */
	double getLongitude();

	/**
	 * Returns the logo file.
	 *
	 * @return
	 */
	File getLogo(int width, int height, boolean force);

	/**
	 * Returns the logo image.
	 *
	 * @return
	 */
	BufferedImage getLogoImage(int width, int height, boolean forceLoad, boolean resizeToFit);

	/**
	 * Returns the banner file.
	 *
	 * @return
	 */
	File getBanner(int width, int height, boolean force);

	/**
	 * Returns the contest banner image.
	 *
	 * @return
	 */
	BufferedImage getBannerImage(int width, int height, boolean forceLoad, boolean resizeToFit);

	/**
	 * Return the contest state.
	 *
	 * @return the state
	 */
	IState getState();

	/**
	 * Returns the total number of events in this contest.
	 *
	 * @return
	 */
	int getNumObjects();

	/**
	 * Elapsed contest time, in ms. A lower bound - based on the contest time of the most recent
	 * event!
	 *
	 * @return
	 */
	int getContestTimeOfLastEvent();

	/**
	 * Return the most recent timed contest object.
	 *
	 * @return
	 */
	IContestObject getLastTimedObject();

	/**
	 * Return the most recent timed contest object's index.
	 *
	 * @return
	 */
	int getLastTimedObjectEventIndex();

	/**
	 * Returns the countdown status.
	 *
	 * @return the countdown status
	 */
	IStartStatus[] getStartStatuses();

	/**
	 * Returns the start-status with the given id.
	 *
	 * @param id an identifier
	 * @return a start-status, or <code>null</code> if the id was invalid or doesn't exist
	 */
	IStartStatus getStartStatusById(String id);

	/**
	 * Returns a start status for the contest time. Null indicates the contest start is not set.
	 * Negative numbers indicate a countdown pause, in ms. Positive numbers indicate contest time,
	 * in ms. Long.MAX_VALUE indicates the contest is over.
	 *
	 * @return
	 */
	Long getStartStatus();

	/**
	 * Returns the pauses.
	 *
	 * @return the pauses
	 */
	IPause[] getPauses();

	/**
	 * Returns a list of all languages used in the contest.
	 *
	 * @return a list of all languages
	 */
	ILanguage[] getLanguages();

	/**
	 * Returns the language with the given id.
	 *
	 * @param id an identifier
	 * @return a language, or <code>null</code> if the id was invalid or doesn't exist
	 */
	ILanguage getLanguageById(String id);

	/**
	 * Returns a list of all judgement types used in the contest.
	 *
	 * @return a list of all judgement types
	 */
	IJudgementType[] getJudgementTypes();

	/**
	 * Returns the judgement type with the given id.
	 *
	 * @param id an identifier
	 * @return a judgement type, or <code>null</code> if the id was invalid or doesn't exist
	 */
	IJudgementType getJudgementTypeById(String id);

	/**
	 * Return the judgement type for a given submission.
	 *
	 * @param submission
	 * @return
	 */
	IJudgementType getJudgementType(ISubmission submission);

	/**
	 * Returns a list of all groups in the contest.
	 *
	 * @return a list of all groups
	 */
	IGroup[] getGroups();

	/**
	 * Return the group with the given id, or null if it doesn't exist.
	 *
	 * @param groupId
	 * @return a group
	 */
	IGroup getGroupById(String groupId);

	/**
	 * Return the groups with the given ids, or null if it doesn't exist.
	 *
	 * @param groupIds
	 * @return an array of groups
	 */
	IGroup[] getGroupsByIds(String[] groupIds);

	/**
	 * Returns the total number of problems.
	 *
	 * @return the total number of problems
	 */
	int getNumProblems();

	/**
	 * Returns a list of all problems in the contest.
	 *
	 * @return a list of all problems
	 */
	IProblem[] getProblems();

	/**
	 * Return the problem with the given id, or null if it doesn't exist.
	 *
	 * @param problemId
	 * @return a problem
	 */
	IProblem getProblemById(String problemId);

	/**
	 * Return the index of the problem in the array of problems based on the problem id, or -1 if
	 * the problem does not exist.
	 *
	 * @param problemId
	 * @return an array index
	 */
	int getProblemIndex(String problemId);

	/**
	 * Returns the total number of organizations.
	 *
	 * @return the total number of organizations
	 */
	int getNumOrganizations();

	/**
	 * Returns a list of all organizations competing in the contest.
	 *
	 * @return a list of all organizations
	 */
	IOrganization[] getOrganizations();

	/**
	 * Returns the organization with the given id.
	 *
	 * @param id an identifier
	 * @return an organization, or <code>null</code> if the id was invalid
	 */
	IOrganization getOrganizationById(String id);

	/**
	 * Returns the total number of teams.
	 *
	 * @return the total number of teams
	 */
	int getNumTeams();

	/**
	 * Returns a list of all teams competing in the contest.
	 *
	 * @return a list of all teams
	 */
	ITeam[] getTeams();

	/**
	 * Returns the team with the given id.
	 *
	 * @param id an identifier
	 * @return a team, or <code>null</code> if the id was invalid
	 */
	ITeam getTeamById(String id);

	/**
	 * Returns true if the team belongs to one or more hidden groups.
	 *
	 * @param team
	 * @return
	 */
	boolean isTeamHidden(ITeam team);

	/**
	 * Returns the current standing for the given team.
	 *
	 * @param team a team
	 * @return a standing, or <code>null</code> if the team was invalid
	 */
	IStanding getStanding(ITeam team);

	/**
	 * Returns a list of teams, ordered by their current rank.
	 *
	 * @return an ordered list of teams
	 */
	ITeam[] getOrderedTeams();

	/**
	 * Returns the order (index in list of ordered teams) of the given team.
	 *
	 * @param team
	 * @return an index, or -1 if the team was invalid or hidden
	 */
	int getOrderOf(ITeam team);

	/**
	 * Returns the total number of team members.
	 *
	 * @return the total number of team members
	 */
	int getNumTeamMembers();

	/**
	 * Returns a list of all team members competing in the contest.
	 *
	 * @return a list of all team members
	 */
	ITeamMember[] getTeamMembers();

	/**
	 * Returns the team member with the given id.
	 *
	 * @param id an identifier
	 * @return a team member, or <code>null</code> if the id was invalid
	 */
	ITeamMember getTeamMemberById(String teamId);

	/**
	 * Returns a list of all team members in the given team id.
	 *
	 * @return a list of all team members in the given team id
	 */
	ITeamMember[] getTeamMembersByTeamId(String teamId);

	/**
	 * Returns the total number of submissions.
	 *
	 * @return the total number of submissions
	 */
	int getNumSubmissions();

	/**
	 * Returns the submission with the given id.
	 *
	 * @param id an identifier
	 * @return a submission, or <code>null</code> if the id was invalid
	 */
	ISubmission getSubmissionById(String id);

	/**
	 * Returns a list of all submissions in the contest.
	 *
	 * @return a list of all submissions
	 */
	ISubmission[] getSubmissions();

	/**
	 * Returns the total number of submission judgements.
	 *
	 * @return the total number of submission judgements
	 */
	int getNumJudgements();

	/**
	 * Returns the submission judgement with the given id.
	 *
	 * @param id an identifier
	 * @return a submission judgement, or <code>null</code> if the id was invalid
	 */
	IJudgement getJudgementById(String id);

	/**
	 * Returns the submission judgements with the given submission id.
	 *
	 * @param id an identifier
	 * @return submission judgements, or <code>null</code> if the id was invalid
	 */
	IJudgement[] getJudgementsBySubmissionId(String id);

	/**
	 * Returns a list of all submission judgements in the contest.
	 *
	 * @return a list of all submission judgements
	 */
	IJudgement[] getJudgements();

	/**
	 * Returns the total number of clarifications.
	 *
	 * @return the total number of clarifications
	 */
	int getNumClarifications();

	/**
	 * Returns a list of all clarifications in the contest.
	 *
	 * @return a list of all clarifications
	 */
	IClarification[] getClarifications();

	/**
	 * Return the clarification with the given id, or null if it doesn't exist.
	 *
	 * @param clarId
	 * @return a clarification
	 */
	IClarification getClarificationById(String id);

	/**
	 * Returns a list of all commentary in the contest.
	 *
	 * @return a list of all commentary
	 */
	ICommentary[] getCommentary();

	/**
	 * Return the commentary with the given id, or null if it doesn't exist.
	 *
	 * @param clarId
	 * @return a commentary
	 */
	ICommentary getCommentaryById(String id);

	/**
	 * Returns the total number of runs.
	 *
	 * @return the total number of runs
	 */
	int getNumRuns();

	/**
	 * Returns a list of all runs in the contest.
	 *
	 * @return a list of all runs
	 */
	IRun[] getRuns();

	/**
	 * Return the run with the given id, or null if it doesn't exist.
	 *
	 * @param runId
	 * @return a run
	 */
	IRun getRunById(String runId);

	/**
	 * Return the runs for a given judgement id.
	 *
	 * @param id
	 * @return
	 */
	IRun[] getRunsByJudgementId(String id);

	Status getStatus(ISubmission submission);

	boolean isSolved(ISubmission submission);

	boolean isJudged(ISubmission submission);

	boolean isFirstToSolve(ISubmission submission);

	/**
	 * Return the result of a given problem for this team.
	 *
	 * @param teamIndex
	 * @param problemIndex
	 * @return
	 */
	IResult getResult(int teamIndex, int problemIndex);

	/**
	 * Return the result of a given problem for this team.
	 *
	 * @param team
	 * @param problemIndex
	 * @return
	 */
	IResult getResult(ITeam team, int problemIndex);

	/**
	 * Return a summary of the results for a given problem.
	 *
	 * @param problemIndex
	 * @return
	 */
	IProblemSummary getProblemSummary(int problemIndex);

	/**
	 * Returns true if the submission was before the contest freeze.
	 *
	 * @return
	 */
	boolean isBeforeFreeze(ISubmission s);

	/**
	 * Return the awards given in this contest.
	 *
	 * @return the awards
	 */
	IAward[] getAwards();

	/**
	 * Returns the award with the given id.
	 *
	 * @param id an identifier
	 * @return an award, or <code>null</code> if the id was invalid
	 */
	IAward getAwardById(String id);

	/**
	 * Return the extended map info for this contest.
	 *
	 * @return the map info
	 */
	IMapInfo getMapInfo();

	/**
	 * Validate all contest objects that can be validated.
	 *
	 * @return a list of validation issues
	 */
	List<String> validate();
}