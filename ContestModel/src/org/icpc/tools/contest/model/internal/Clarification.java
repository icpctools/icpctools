package org.icpc.tools.contest.model.internal;

import java.util.List;

import org.icpc.tools.contest.model.IClarification;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.feed.JSONParser;

public class Clarification extends TimedEvent implements IClarification {
	private static final boolean isDraftSpec = "draft".equals(System.getProperty("ICPC_CONTEST_API"));

	private static final String REPLY_TO_ID = "reply_to_id";
	private static final String FROM_TEAM_ID = "from_team_id";
	private static final String TO_TEAM_ID = "to_team_id";
	private static final String TO_TEAM_IDS = "to_team_ids";
	private static final String TO_GROUP_IDS = "to_group_ids";
	private static final String PROBLEM_ID = "problem_id";
	private static final String TEXT = "text";

	private String replyToId;
	private String fromTeamId;
	private String[] toTeamIds;
	private String[] toGroupIds;
	private String problemId;
	private String text;

	@Override
	public ContestType getType() {
		return ContestType.CLARIFICATION;
	}

	@Override
	public String getReplyToId() {
		return replyToId;
	}

	@Override
	public String getFromTeamId() {
		return fromTeamId;
	}

	@Override
	public String[] getToTeamIds() {
		return toTeamIds;
	}

	@Override
	public String[] getToGroupIds() {
		return toGroupIds;
	}

	@Override
	public String getProblemId() {
		return problemId;
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public boolean isBroadcast() {
		return fromTeamId == null
				&& ((toTeamIds == null || toTeamIds.length == 0) && (toGroupIds == null || toGroupIds.length == 0));
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (REPLY_TO_ID.equals(name)) {
			replyToId = (String) value;
			return true;
		} else if (FROM_TEAM_ID.equals(name)) {
			fromTeamId = (String) value;
			return true;
		} else if (TO_TEAM_ID.equals(name)) {
			if (value == null || "null".equals(value))
				toTeamIds = null;
			else
				toTeamIds = new String[] { (String) value };
			return true;
		} else if (TO_TEAM_IDS.equals(name)) {
			if (value == null || "null".equals(value))
				toTeamIds = null;
			else {
				Object[] ob = JSONParser.getOrReadArray(value);
				toTeamIds = new String[ob.length];
				for (int i = 0; i < ob.length; i++)
					toTeamIds[i] = (String) ob[i];
			}
			return true;
		} else if (TO_GROUP_IDS.equals(name)) {
			if (value == null || "null".equals(value))
				toGroupIds = null;
			else {
				Object[] ob = JSONParser.getOrReadArray(value);
				toGroupIds = new String[ob.length];
				for (int i = 0; i < ob.length; i++)
					toGroupIds[i] = (String) ob[i];
			}
			return true;
		} else if (PROBLEM_ID.equals(name)) {
			problemId = (String) value;
			return true;
		} else if (TEXT.equals(name)) {
			text = (String) value;
			return true;
		}

		return super.addImpl(name, value);
	}

	@Override
	protected void getProperties(Properties props) {
		props.addLiteralString(ID, id);
		props.addLiteralString(REPLY_TO_ID, replyToId);
		props.addLiteralString(FROM_TEAM_ID, fromTeamId);
		if (!isDraftSpec) {
			if (toTeamIds != null && toTeamIds.length > 0) {
				props.addLiteralString(TO_TEAM_ID, toTeamIds[0]);
			}
		} else {
			props.addArray(TO_TEAM_IDS, toTeamIds);
			props.addArray(TO_GROUP_IDS, toGroupIds);
		}
		props.addLiteralString(PROBLEM_ID, problemId);
		props.addString(TEXT, text);
		super.getProperties(props);
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (text == null || text.isEmpty())
			errors.add("Missing text");

		if (problemId != null && c.getProblemById(problemId) == null)
			errors.add("Clarification about unknown problem " + problemId);

		if (fromTeamId != null && c.getTeamById(fromTeamId) == null)
			errors.add("Clarification from unknown team " + fromTeamId);

		if (toTeamIds != null) {
			for (String teamId : toTeamIds) {
				if (c.getTeamById(teamId) == null) {
					errors.add("Clarification to unknown team " + teamId);
				}
			}
		}

		if (toGroupIds != null) {
			for (String groupId : toGroupIds) {
				if (c.getGroupById(groupId) == null) {
					errors.add("Clarification to unknown group " + groupId);
				}
			}
		}

		if (fromTeamId != null && (toTeamIds != null || toGroupIds != null))
			errors.add("Clarification between teams");

		if (errors.isEmpty())
			return null;
		return errors;
	}
}
