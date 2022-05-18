package org.icpc.tools.contest.model.internal;

import java.util.List;

import org.icpc.tools.contest.model.ICommentary;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.feed.JSONParser;

// {"type": "commentary", "data": {"id": "191", "time": "2020-02-20T16:26:51.028-05", "message":
// "#t6(Tex WA", "team_ids": ["6"], "problem_ids": ["directingrainfall"], "contest_time":
// "0:26:51.028"}}

public class Commentary extends TimedEvent implements ICommentary {
	private static final String MESSAGE = "message";
	private static final String TEAM_IDS = "team_ids";
	private static final String PROBLEM_IDS = "problem_ids";

	private String message;
	private String[] teamIds;
	private String[] problemIds;

	@Override
	public ContestType getType() {
		return ContestType.COMMENTARY;
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (MESSAGE.equals(name)) {
			message = (String) value;
			return true;
		} else if (TEAM_IDS.equals(name)) {
			if (value != null) {
				Object[] ob = JSONParser.getOrReadArray(value);
				teamIds = new String[ob.length];
				for (int i = 0; i < ob.length; i++)
					teamIds[i] = (String) ob[i];
			} else {
				teamIds = null;
			}
			return true;
		} else if (PROBLEM_IDS.equals(name)) {
			if (value != null) {
				Object[] ob = JSONParser.getOrReadArray(value);
				problemIds = new String[ob.length];
				for (int i = 0; i < ob.length; i++)
					problemIds[i] = (String) ob[i];
			} else {
				problemIds = null;
			}
			return true;
		}

		return super.addImpl(name, value);
	}

	@Override
	public String[] getProblemIds() {
		return problemIds;
	}

	@Override
	public String[] getTeamIds() {
		return teamIds;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	protected void getProperties(Properties props) {
		props.addLiteralString(ID, id);
		props.addString(MESSAGE, message);
		props.addArray(TEAM_IDS, teamIds);
		props.addArray(PROBLEM_IDS, problemIds);
		super.getProperties(props);
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (message == null || message.isEmpty())
			errors.add("Missing message");

		if (teamIds != null && teamIds.length > 0) {
			for (String tId : teamIds) {
				if (c.getTeamById(tId) == null)
					errors.add("Invalid team " + tId);
			}
		}

		if (problemIds != null && problemIds.length > 0) {
			for (String pId : problemIds) {
				if (c.getProblemById(pId) == null)
					errors.add("Invalid problem " + pId);
			}
		}

		if (errors.isEmpty())
			return null;
		return errors;
	}
}