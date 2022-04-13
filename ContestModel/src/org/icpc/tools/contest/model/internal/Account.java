package org.icpc.tools.contest.model.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.feed.JSONEncoder;

public class Account extends ContestObject implements IAccount {
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String TYPE = "type";
	private static final String IP = "ip";
	private static final String TEAM_ID = "team_id";
	private static final String PERSON_ID = "person_id";

	private String username;
	private String password;
	private String type;
	private String ip;
	private String teamId;
	private String personId;

	@Override
	public ContestType getType() {
		return ContestType.ACCOUNT;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getAccountType() {
		return type;
	}

	@Override
	public String getIp() {
		return ip;
	}

	@Override
	public String getTeamId() {
		return teamId;
	}

	@Override
	public String getPersonId() {
		return personId;
	}

	@Override
	protected boolean addImpl(String name2, Object value) throws Exception {
		switch (name2) {
			case USERNAME: {
				username = (String) value;
				return true;
			}
			case PASSWORD: {
				password = (String) value;
				return true;
			}
			case TYPE: {
				type = (String) value;
				return true;
			}
			case IP: {
				ip = (String) value;
				return true;
			}
			case TEAM_ID: {
				teamId = (String) value;
				return true;
			}
			case PERSON_ID: {
				personId = (String) value;
				return true;
			}
		}

		return false;
	}

	@Override
	public IContestObject clone() {
		Account a = new Account();
		a.id = id;
		a.username = username;
		a.password = password;
		a.type = type;
		a.ip = ip;
		a.teamId = teamId;
		a.personId = personId;
		return a;
	}

	@Override
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		if (username != null)
			props.put(USERNAME, username);
		if (password != null)
			props.put(PASSWORD, password);
		if (type != null)
			props.put(TYPE, type);
		if (ip != null)
			props.put(IP, ip);
		if (teamId != null)
			props.put(TEAM_ID, teamId);
		if (personId != null)
			props.put(PERSON_ID, personId);
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);
		if (username != null)
			je.encode(USERNAME, username);
		if (password != null)
			je.encode(PASSWORD, password);
		if (type != null)
			je.encode(TYPE, type);
		if (ip != null)
			je.encode(IP, ip);
		if (teamId != null)
			je.encode(TEAM_ID, teamId);
		if (personId != null)
			je.encode(PERSON_ID, personId);
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = new ArrayList<>();

		if (getUsername() == null || getUsername().isEmpty())
			errors.add("Username missing");

		if (teamId != null && c.getTeamById(teamId) == null)
			errors.add("Invalid team " + teamId);

		if (personId != null && c.getPersonById(personId) == null)
			errors.add("Invalid person " + personId);

		if (errors.isEmpty())
			return null;
		return errors;
	}
}