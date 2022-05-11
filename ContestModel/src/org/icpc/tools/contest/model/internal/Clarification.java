package org.icpc.tools.contest.model.internal;

import java.util.List;

import org.icpc.tools.contest.model.IClarification;
import org.icpc.tools.contest.model.IContest;

public class Clarification extends TimedEvent implements IClarification {
	private static final String REPLY_TO_ID = "reply_to_id";
	private static final String FROM_TEAM_ID = "from_team_id";
	private static final String TO_TEAM_ID = "to_team_id";
	private static final String PROBLEM_ID = "problem_id";
	private static final String TEXT = "text";

	private String replyToId;
	private String fromTeamId;
	private String toTeamId;
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
	public String getToTeamId() {
		return toTeamId;
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
	protected boolean addImpl(String name, Object value) throws Exception {
		if (REPLY_TO_ID.equals(name)) {
			replyToId = (String) value;
			return true;
		} else if (FROM_TEAM_ID.equals(name)) {
			fromTeamId = (String) value;
			return true;
		} else if (TO_TEAM_ID.equals(name)) {
			toTeamId = (String) value;
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
		props.addLiteralString(TO_TEAM_ID, toTeamId);
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

		if (toTeamId != null && c.getTeamById(toTeamId) == null)
			errors.add("Clarification to unknown team " + toTeamId);

		if (fromTeamId != null && toTeamId != null)
			errors.add("Clarification between teams");

		if (errors.isEmpty())
			return null;
		return errors;
	}
}