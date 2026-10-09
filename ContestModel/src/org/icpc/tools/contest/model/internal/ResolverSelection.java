package org.icpc.tools.contest.model.internal;

import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IResolverSelection;

public class ResolverSelection extends ContestObject implements IResolverSelection {
	private static final String TEAM_IDS = "team_ids";
	private static final String PROBLEM_ID = "problem_id";

	private String[] teamIds;
	private String problemId;

	@Override
	public ContestType getType() {
		return ContestType.RESOLVER_SELECTION;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public String[] getTeamIds() {
		return teamIds;
	}

	@Override
	public String getProblemId() {
		return problemId;
	}

	public void setSelection(String[] teamIds, String problemId) {
		this.teamIds = teamIds;
		this.problemId = problemId;
	}

	@Override
	public IContestObject clone() {
		ResolverSelection s = new ResolverSelection();
		s.teamIds = teamIds;
		s.problemId = problemId;
		return s;
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (TEAM_IDS.equals(name)) {
			try {
				Object[] ob = (Object[]) value;
				teamIds = new String[ob.length];
				for (int i = 0; i < ob.length; i++)
					teamIds[i] = (String) ob[i];
			} catch (Exception e) {
				// ignore
			}
			return true;
		} else if (PROBLEM_ID.equals(name)) {
			problemId = (String) value;
			return true;
		}
		return super.addImpl(name, value);
	}

	@Override
	protected void getProperties(Properties props) {
		props.addArray(TEAM_IDS, teamIds);
		props.addLiteralString(PROBLEM_ID, problemId);
		super.getProperties(props);
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		for (String teamId : teamIds) {
			if (c.getTeamById(teamId) == null)
				errors.add("Invalid team " + teamId);
		}

		if (problemId != null && c.getProblemById(problemId) == null)
			errors.add("Invalid problem " + problemId);

		if (errors.isEmpty())
			return null;
		return errors;
	}

	@Override
	public String toString() {
		int numTeams = 0;
		if (teamIds != null)
			numTeams = teamIds.length;
		return "ResolverSelection [" + numTeams + ", " + problemId + "]";
	}
}