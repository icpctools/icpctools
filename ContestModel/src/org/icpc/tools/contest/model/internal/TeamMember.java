package org.icpc.tools.contest.model.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.ITeamMember;
import org.icpc.tools.contest.model.feed.JSONEncoder;

public class TeamMember extends ContestObject implements ITeamMember {
	private static final String ICPC_ID = "icpc_id";
	private static final String TEAM_ID = "team_id";
	private static final String FIRST_NAME = "first_name";
	private static final String LAST_NAME = "last_name";
	private static final String SEX = "sex";
	private static final String ROLE = "role";
	private static final String PHOTO = "photo";

	private String icpcId;
	private String firstName;
	private String lastName;
	private String sex;
	private String teamId;
	private String role;
	private FileReferenceList photo;

	@Override
	public ContestType getType() {
		return ContestType.TEAM_MEMBER;
	}

	@Override
	public String getICPCId() {
		return icpcId;
	}

	@Override
	public String getFirstName() {
		return firstName;
	}

	@Override
	public String getLastName() {
		return lastName;
	}

	@Override
	public String getSex() {
		return sex;
	}

	@Override
	public String getTeamId() {
		return teamId;
	}

	@Override
	public String getRole() {
		return role;
	}

	public FileReferenceList getPhoto() {
		return photo;
	}

	@Override
	public File getPhoto(int width, int height, boolean force) {
		return getFile(getBestFileReference(photo, new ImageSizeFit(width, height)), PHOTO, force);
	}

	@Override
	public BufferedImage getPhotoImage(int width, int height, boolean forceLoad, boolean resizeToFit) {
		return getRefImage(PHOTO, photo, width, height, forceLoad, resizeToFit);
	}

	public void setPhoto(FileReferenceList list) {
		photo = list;
	}

	@Override
	public Object resolveFileReference(String url) {
		return FileReferenceList.resolve(url, photo);
	}

	@Override
	protected boolean addImpl(String name2, Object value) throws Exception {
		switch (name2) {
			case ICPC_ID: {
				icpcId = (String) value;
				return true;
			}
			case FIRST_NAME: {
				firstName = (String) value;
				return true;
			}
			case LAST_NAME: {
				lastName = (String) value;
				return true;
			}
			case SEX: {
				sex = (String) value;
				return true;
			}
			case TEAM_ID: {
				teamId = (String) value;
				return true;
			}
			case ROLE: {
				role = (String) value;
				return true;
			}
			case PHOTO: {
				photo = new FileReferenceList(value);
				return true;
			}
		}

		return false;
	}

	@Override
	public IContestObject clone() {
		TeamMember t = new TeamMember();
		t.id = id;
		t.photo = photo;
		t.icpcId = icpcId;
		t.firstName = firstName;
		t.lastName = lastName;
		t.sex = sex;
		t.teamId = teamId;
		t.role = role;
		return t;
	}

	@Override
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		props.put(ICPC_ID, icpcId);
		props.put(FIRST_NAME, firstName);
		props.put(LAST_NAME, lastName);
		props.put(SEX, sex);
		props.put(TEAM_ID, teamId);
		props.put(ROLE, role);
		props.put(PHOTO, photo);
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);
		if (icpcId != null)
			je.encode(ICPC_ID, icpcId);
		je.encode(FIRST_NAME, firstName);
		je.encode(LAST_NAME, lastName);
		je.encode(SEX, sex);
		je.encode(TEAM_ID, teamId);
		je.encode(ROLE, role);
		je.encode(PHOTO, photo, false);
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = new ArrayList<>();

		if (c.getTeamById(teamId) == null)
			errors.add("Invalid team " + teamId);

		if (firstName == null || firstName.isEmpty())
			errors.add("First name missing");

		if (lastName == null || lastName.isEmpty())
			errors.add("Last name missing");

		if (sex == null || sex.isEmpty())
			errors.add("Sex missing");

		if (role == null || role.isEmpty())
			errors.add("Role missing");

		if (errors.isEmpty())
			return null;
		return errors;
	}
}