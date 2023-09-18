package org.icpc.tools.contest.model.internal;

import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IAccount;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;

public class Account extends ContestObject implements IAccount {
	private static final String NAME = "name";
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
	private String name;

	@Override
	public ContestType getType() {
		return ContestType.ACCOUNT;
	}

	@Override
	public String getName() {
		return name;
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
			case NAME: {
				name = (String) value;
				return true;
			}
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
		a.name = name;
		a.username = username;
		a.password = password;
		a.type = type;
		a.ip = ip;
		a.teamId = teamId;
		a.personId = personId;
		return a;
	}

	@Override
	protected void getProperties(Properties props) {
		props.addLiteralString(ID, id);
		props.addString(NAME, name);
		props.addString(USERNAME, username);
		props.addString(PASSWORD, password);
		props.addLiteralString(TYPE, type);
		props.addLiteralString(IP, ip);
		props.addLiteralString(TEAM_ID, teamId);
		props.addLiteralString(PERSON_ID, personId);
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