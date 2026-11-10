package org.icpc.tools.contest.model.internal;

import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IResolverSelection;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ResolutionStep;
import org.icpc.tools.contest.model.resolver.SelectType;
import org.icpc.tools.contest.model.resolver.SubmissionInfo;

/**
 * A selection in the resolver. withX() methods change the existing object (clearing any old data,
 * e.g. if you select a team then the previous problem and submission id are reset) and then return
 * a clone(), so you can do:
 * <ol>
 * <li>steps.add(step.withProblem(p));</li>
 * <li>steps.add(step.withSubmissionId(subId));</li>
 * </ol>
 * and not worry about creating new objects each time.
 */
public class ResolverSelection extends ContestObject implements IResolverSelection, ResolutionStep {
	private static final String TEAM_IDS = "team_ids";
	private static final String SELECT_TYPE = "select_type";
	private static final String PROBLEM_ID = "problem_id";
	private static final String SUBMISSION_ID = "submission_id";

	private String[] teamIds;
	private String selectType;
	private String problemId;
	public String submissionId;

	// internal property - never serialized
	public SubmissionInfo subInfo;

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

	public SelectType getSelectType() {
		if (selectType == null)
			return null;

		try {
			return SelectType.valueOf(selectType);
		} catch (Exception e) {
			return null;
		}
	}

	public void setSelection(String[] teamIds, String problemId) {
		this.teamIds = teamIds;
		this.problemId = problemId;
	}

	@Override
	public IContestObject clone() {
		ResolverSelection s = new ResolverSelection();
		s.teamIds = teamIds;
		s.selectType = selectType;
		s.problemId = problemId;
		s.subInfo = subInfo;
		s.submissionId = submissionId;
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
		} else if (SELECT_TYPE.equals(name)) {
			selectType = (String) value;
			return true;
		} else if (PROBLEM_ID.equals(name)) {
			problemId = (String) value;
			return true;
		} else if (SUBMISSION_ID.equals(name)) {
			submissionId = (String) value;
			return true;
		}
		return super.addImpl(name, value);
	}

	@Override
	protected void getProperties(Properties props) {
		props.addArray(TEAM_IDS, teamIds);
		if (selectType != null && !"NORMAL".equals(selectType)) {
			props.addLiteralString(SELECT_TYPE, selectType.toLowerCase());
		}
		props.addLiteralString(PROBLEM_ID, problemId);
		props.addLiteralString(SUBMISSION_ID, submissionId);
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

	public ResolverSelection withTeam(ITeam team) {
		return withTeam(team, SelectType.NORMAL);
	}

	public ResolverSelection withTeam(ITeam team, SelectType newType) {
		teamIds = new String[] { team.getId() };
		selectType = newType.toString();
		problemId = null;
		subInfo = null;
		submissionId = null;
		return (ResolverSelection) clone();
	}

	public ResolverSelection withTeams(String[] newTeamIds) {
		teamIds = newTeamIds;
		selectType = SelectType.TEAM_LIST.toString();
		problemId = null;
		subInfo = null;
		submissionId = null;
		return (ResolverSelection) clone();
	}

	public ResolverSelection withProblem(SubmissionInfo newSubInfo) {
		if (newSubInfo != null)
			problemId = newSubInfo.getProblemId();
		else
			problemId = null;
		subInfo = newSubInfo;
		submissionId = null;
		return (ResolverSelection) clone();
	}

	public ResolverSelection withSubmissionId(String newSubId) {
		submissionId = newSubId;
		return (ResolverSelection) clone();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ResolverSelection [");
		if (teamIds == null || teamIds.length == 0) {
			sb.append("no teams");
		} else if (teamIds.length == 1) {
			sb.append("team " + teamIds[0]);
		} else {
			sb.append(teamIds.length + " teams");
		}

		if (selectType != null && !selectType.equals("NORMAL")) {
			sb.append(", select " + selectType);
		}
		if (problemId != null) {
			sb.append(", problem " + problemId);
		}
		if (submissionId != null) {
			sb.append(", submission " + submissionId);
		}
		sb.append("]");
		return sb.toString();
	}
}