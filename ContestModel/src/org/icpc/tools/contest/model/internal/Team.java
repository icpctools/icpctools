package org.icpc.tools.contest.model.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class Team extends ContestObject implements ITeam {
	private static final String NAME = "name";
	private static final String DISPLAY_NAME = "display_name";
	private static final String GROUP_IDS = "group_ids";
	private static final String ORGANIZATION_ID = "organization_id";
	private static final String ICPC_ID = "icpc_id";
	private static final String X = "x";
	private static final String Y = "y";
	private static final String ROTATION = "rotation";
	private static final String LOCATION = "location";
	private static final String PHOTO = "photo";
	private static final String VIDEO = "video";
	private static final String BACKUP = "backup";
	private static final String DESKTOP = "desktop";
	private static final String WEBCAM = "webcam";
	private static final String AUDIO = "audio";
	private static final String KEY_LOG = "key_log";
	private static final String TOOL_DATA = "tool_data";

	private String name;
	private String displayName;
	private String[] groupIds;
	private String organizationId;
	private String icpcId;
	private double x = Double.NaN;
	private double y = Double.NaN;
	private double rotation = Double.NaN;
	private FileReferenceList photo;
	private FileReferenceList video;
	private FileReferenceList desktop;
	private FileReferenceList webcam;
	private FileReferenceList audio;
	private FileReferenceList backup;
	private FileReferenceList keylog;
	private FileReferenceList tooldata;

	@Override
	public ContestType getType() {
		return ContestType.TEAM;
	}

	@Override
	public String[] getGroupIds() {
		return groupIds;
	}

	@Override
	public String getOrganizationId() {
		return organizationId;
	}

	@Override
	public String getICPCId() {
		return icpcId;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String name) {
		displayName = name;
	}

	@Override
	public String getActualDisplayName() {
		if (displayName != null)
			return displayName;
		return name;
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	@Override
	public double getRotation() {
		return rotation;
	}

	public FileReferenceList getPhoto() {
		return photo;
	}

	@Override
	public File getPhoto(int width, int height, boolean force) {
		return getFile(getBestFileReference(photo, new ImageSizeFit(width, height)), PHOTO, force);
	}

	public void setPhoto(FileReferenceList list) {
		photo = list;
	}

	@Override
	public BufferedImage getPhotoImage(int width, int height, boolean forceLoad, boolean resizeToFit) {
		return getRefImage(PHOTO, photo, width, height, forceLoad, resizeToFit);
	}

	public FileReferenceList getVideo() {
		return video;
	}

	@Override
	public File getVideo(boolean force) {
		return getFile(getBestFileReference(video, null), VIDEO, force);
	}

	public FileReferenceList getBackup() {
		return backup;
	}

	@Override
	public File getBackup(boolean force) {
		return getFile(getBestFileReference(backup, null), BACKUP, force);
	}

	public void setBackup(FileReferenceList list) {
		backup = list;
	}

	public FileReferenceList getKeyLog() {
		return keylog;
	}

	@Override
	public File getKeyLog(boolean force) {
		return getFile(getBestFileReference(keylog, null), KEY_LOG, force);
	}

	public void setKeyLog(FileReferenceList list) {
		keylog = list;
	}

	public FileReferenceList getToolData() {
		return tooldata;
	}

	@Override
	public File getToolData(boolean force) {
		return getFile(getBestFileReference(tooldata, null), TOOL_DATA, force);
	}

	public void setToolData(FileReferenceList list) {
		tooldata = list;
	}

	public void setVideo(FileReferenceList list) {
		video = list;
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

	@Override
	public Object resolveFileReference(String url) {
		return FileReferenceList.resolve(url, photo, video, backup, desktop, webcam, audio, keylog, tooldata);
	}

	@Override
	protected boolean addImpl(String name2, Object value) throws Exception {
		switch (name2) {
			case GROUP_IDS: {
				try {
					Object[] ob = (Object[]) value;
					groupIds = new String[ob.length];
					for (int i = 0; i < ob.length; i++)
						groupIds[i] = (String) ob[i];
				} catch (Exception e) {
					// ignore
				}
				return true;
			}
			case "group_id": { /// deprecated - temporary support for Nov 2017 feeds
				groupIds = new String[] { (String) value };
				return true;
			}
			case ORGANIZATION_ID: {
				organizationId = (String) value;
				return true;
			}
			case ICPC_ID: {
				icpcId = (String) value;
				return true;
			}
			case NAME: {
				name = (String) value;
				return true;
			}
			case DISPLAY_NAME: {
				displayName = (String) value;
				return true;
			}
			case X: { // deprecated - use child location object
				x = parseDouble(value);
				return true;
			}
			case Y: { // deprecated - use child location object
				y = parseDouble(value);
				return true;
			}
			case ROTATION: { // deprecated - use child location object
				rotation = parseDouble(value);
				return true;
			}
			case LOCATION: {
				JsonObject obj = JSONParser.getOrReadObject(value);
				x = obj.getDouble(X);
				y = obj.getDouble(Y);
				rotation = obj.getDouble(ROTATION);
				return true;
			}
			case PHOTO: {
				photo = new FileReferenceList(value);
				return true;
			}
			case VIDEO: {
				video = new FileReferenceList(value);
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
		}

		return false;
	}

	@Override
	public IContestObject clone() {
		Team t = new Team();
		t.id = id;
		t.photo = photo;
		t.video = video;
		t.backup = backup;
		t.keylog = keylog;
		t.tooldata = tooldata;
		t.desktop = desktop;
		t.webcam = webcam;
		t.audio = audio;
		t.name = name;
		t.displayName = displayName;
		t.groupIds = groupIds;
		t.organizationId = organizationId;
		t.icpcId = icpcId;
		t.x = x;
		t.y = y;
		t.rotation = rotation;
		return t;
	}

	@Override
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		props.put(NAME, name);
		if (displayName != null)
			props.put(DISPLAY_NAME, displayName);
		props.put(ICPC_ID, icpcId);
		if (groupIds != null)
			props.put(GROUP_IDS, "[\"" + String.join("\",\"", groupIds) + "\"]");
		props.put(ORGANIZATION_ID, organizationId);
		props.put(PHOTO, photo);
		props.put(VIDEO, video);
		props.put(BACKUP, backup);
		props.put(KEY_LOG, keylog);
		props.put(TOOL_DATA, tooldata);
		props.put(DESKTOP, desktop);
		props.put(WEBCAM, webcam);
		props.put(AUDIO, audio);
		if (!Double.isNaN(x) || !Double.isNaN(y) || !Double.isNaN(rotation)) {
			List<String> attrs = new ArrayList<>(3);
			if (!Double.isNaN(x))
				attrs.add("\"" + X + "\":" + round(x));
			if (!Double.isNaN(y))
				attrs.add("\"" + Y + "\":" + round(y));
			if (!Double.isNaN(rotation))
				attrs.add("\"" + ROTATION + "\":" + round(rotation));
			props.put(LOCATION, "{" + String.join(",", attrs) + "}");
		}
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);

		if (name != null)
			je.encode(NAME, name);
		if (displayName != null)
			je.encode(DISPLAY_NAME, displayName);
		if (icpcId != null)
			je.encode(ICPC_ID, icpcId);
		if (groupIds != null)
			je.encodePrimitive(GROUP_IDS, "[\"" + String.join("\",\"", groupIds) + "\"]");
		if (organizationId != null)
			je.encode(ORGANIZATION_ID, organizationId);
		je.encode(PHOTO, photo, false);
		je.encode(VIDEO, video, false);
		je.encode(BACKUP, backup, false);
		je.encode(KEY_LOG, keylog, false);
		je.encode(TOOL_DATA, tooldata, false);
		je.encodeSubs(DESKTOP, desktop, false);
		je.encodeSubs(WEBCAM, webcam, false);
		je.encodeSubs(AUDIO, audio, false);

		if (!Double.isNaN(x) || !Double.isNaN(y) || !Double.isNaN(rotation)) {
			List<String> attrs = new ArrayList<>(3);
			if (!Double.isNaN(x))
				attrs.add("\"" + X + "\":" + round(x));
			if (!Double.isNaN(y))
				attrs.add("\"" + Y + "\":" + round(y));
			if (!Double.isNaN(rotation))
				attrs.add("\"" + ROTATION + "\":" + round(rotation));
			je.encodePrimitive(LOCATION, "{" + String.join(",", attrs) + "}");
		}
	}

	private static double round(double d) {
		return Math.round(d * 100.0) / 100.0;
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (name == null || name.isEmpty())
			errors.add("Name missing");

		if (organizationId != null && c.getOrganizationById(organizationId) == null)
			errors.add("Invalid organization " + organizationId);

		if (groupIds != null) {
			for (String groupId : groupIds)
				if (c.getGroupById(groupId) == null)
					errors.add("Invalid group " + groupId);
		}

		if (errors.isEmpty())
			return null;
		return errors;
	}
}