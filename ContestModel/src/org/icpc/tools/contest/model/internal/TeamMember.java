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
	private static final String DESKTOP = "desktop";
	private static final String WEBCAM = "webcam";
	private static final String AUDIO = "audio";
	private static final String BACKUP = "backup";
	private static final String KEY_LOG = "key_log";
	private static final String TOOL_DATA = "tool_data";

	private String icpcId;
	private String firstName;
	private String lastName;
	private String sex;
	private String teamId;
	private String role;
	private FileReferenceList photo;
	private FileReferenceList desktop;
	private FileReferenceList webcam;
	private FileReferenceList audio;
	private FileReferenceList backup;
	private FileReferenceList keylog;
	private FileReferenceList tooldata;

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
			case DESKTOP: {
				desktop = new FileReferenceList(value);
				return true;
			}
			case WEBCAM: {
				webcam = new FileReferenceList(value);
				return true;
			}
			case AUDIO: {
				audio = new FileReferenceList(value);
				return true;
			}
			case BACKUP: {
				backup = new FileReferenceList(value);
				return true;
			}
			case KEY_LOG: {
				keylog = new FileReferenceList(value);
				return true;
			}
			case TOOL_DATA: {
				tooldata = new FileReferenceList(value);
				return true;
			}
		}

		return false;
	}

	public FileReferenceList getDesktop() {
		return desktop;
	}

	@Override
	public String getDesktopURL() {
		if (desktop == null || desktop.isEmpty())
			return null;

		return desktop.first().href;
	}

	public String[] getDesktopURLs() {
		if (desktop == null || desktop.isEmpty())
			return null;

		return desktop.getHrefs();
	}

	public void setDesktop(FileReferenceList list) {
		desktop = list;
	}

	public FileReferenceList getWebcam() {
		return webcam;
	}

	@Override
	public String getWebcamURL() {
		if (webcam == null || webcam.isEmpty())
			return null;

		return webcam.first().href;
	}

	public String[] getWebcamURLs() {
		if (webcam == null || webcam.isEmpty())
			return null;

		return webcam.getHrefs();
	}

	public void setWebcam(FileReferenceList list) {
		webcam = list;
	}

	public FileReferenceList getAudio() {
		return audio;
	}

	@Override
	public String getAudioURL() {
		if (audio == null || audio.isEmpty())
			return null;

		return audio.first().href;
	}

	public void setAudio(FileReferenceList list) {
		audio = list;
	}

	public FileReferenceList getBackup() {
		return backup;
	}

	@Override
	public File getBackup(boolean force) {
		return getFile(backup.first(), BACKUP, force);
	}

	public void setBackup(FileReferenceList list) {
		backup = list;
	}

	public FileReferenceList getKeyLog() {
		return keylog;
	}

	@Override
	public File getKeyLog(boolean force) {
		return getFile(keylog.first(), KEY_LOG, force);
	}

	public void setKeyLog(FileReferenceList list) {
		keylog = list;
	}

	public FileReferenceList getToolData() {
		return tooldata;
	}

	@Override
	public File getToolData(boolean force) {
		return getFile(tooldata.first(), TOOL_DATA, force);
	}

	public void setToolData(FileReferenceList list) {
		tooldata = list;
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
		props.put(DESKTOP, desktop);
		props.put(WEBCAM, webcam);
		props.put(AUDIO, audio);
		props.put(BACKUP, backup);
		props.put(KEY_LOG, keylog);
		props.put(TOOL_DATA, tooldata);
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
		je.encodeSubs(DESKTOP, desktop, false);
		je.encodeSubs(WEBCAM, webcam, false);
		je.encodeSubs(AUDIO, audio, false);
		je.encode(BACKUP, backup, false);
		je.encode(KEY_LOG, keylog, false);
		je.encode(TOOL_DATA, tooldata, false);
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