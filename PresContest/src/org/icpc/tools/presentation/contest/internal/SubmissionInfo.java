package org.icpc.tools.presentation.contest.internal;

import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ITeam;

/**
 * This class encapsulates the concept of a single SubmissionInfo operation; that is, for a given
 * Team and Problem, for each of the pending submission the team has on the problem, what will be
 * the standing of the team if the resolution of the pending submission is Yes; what will be the
 * team's best possible standing if they get a Yes on ALL their pending problems and all other
 * teams get a No; and what will be the team's worst possible standing if they get a No on all
 * their pending problems and all other teams get a Yes on theirs.
 *
 * The class does not actually calculate any of this data; it simple acts as a holder for it. The
 * actual calculations are done in {@link Resolver.projectStandings(SubmissionInfo)}, which accepts
 * a SubmissionInfo object and fills it in with the solved, best-case, and worst-case data.
 */
public class SubmissionInfo {
	private final ITeam team;
	private final int problemIndex;
	private IStanding[] ifSolved;
	private IStanding[] bestCase;
	private IStanding[] worstCase;

	public SubmissionInfo(ITeam team, int problemIndex) {
		this.team = team;
		this.problemIndex = problemIndex;
	}

	public void setPossibleStandings(IStanding[] ifSolved, IStanding[] bestCase, IStanding[] worstCase) {
		this.ifSolved = ifSolved;
		this.bestCase = bestCase;
		this.worstCase = worstCase;
	}

	public ITeam getTeam() {
		return team;
	}

	public int getProblemIndex() {
		return problemIndex;
	}

	public IStanding[] getStandingIfSolved() {
		return ifSolved;
	}

	public IStanding[] getStandingBestCase() {
		return bestCase;
	}

	public IStanding[] getStandingWorstCase() {
		return worstCase;
	}

	@Override
	public String toString() {
		return "SubmissionInfo[" + team.getId() + ", " + problemIndex + "]";
	}
}