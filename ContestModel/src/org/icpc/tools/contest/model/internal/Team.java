package org.icpc.tools.contest.model.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class Team extends ContestObject implements ITeam {
	private static final String NAME = "name";
	private static final String DISPLAY_NAME = "display_name";
	private static final String LABEL = "label";
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
	private static final String HIDDEN = "hidden";

	private String name;
	private String displayName;
	private String label;
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
	private boolean isHidden;

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
	public String getLabel() {
		return label;
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

	public void setLocation(double x, double y, double rotation) {
		this.x = x;
		this.y = y;
		this.rotation = rotation;
	}

	@Override
	public double getRotation() {
		return rotation;
	}

	@Override
	public FileReferenceList getPhoto() {
		return photo;
	}

	@Override
	public File getPhoto(int width, int height, boolean force) {
		return getFile(getBestFileReference(photo, new ImageSizeFit(width, height)), PHOTO, force);
	}

	@Override
	public File[] getPhotos(boolean force) {
		return getFiles(photo, PHOTO, force);
	}

	public void setPhoto(FileReferenceList list) {
		photo = list;
	}

	@Override
	public BufferedImage getPhotoImage(int width, int height, boolean forceLoad, boolean resizeToFit) {
		return getRefImage(PHOTO, photo, width, height, forceLoad, resizeToFit);
	}

	@Override
	public FileReferenceList getVideo() {
		return video;
	}

	@Override
	public File getVideo(boolean force) {
		return getFile(video.first(), VIDEO, force);
	}

	@Override
	public File[] getVideos(boolean force) {
		return getFiles(video, VIDEO, force);
	}

	@Override
	public FileReferenceList getBackup() {
		return backup;
	}

	@Override
	public File getBackup(boolean force) {
		return getFile(backup.first(), BACKUP, force);
	}

	@Override
	public File[] getBackups(boolean force) {
		return getFiles(backup, BACKUP, force);
	}

	public void setBackup(FileReferenceList list) {
		backup = list;
	}

	@Override
	public FileReferenceList getKeyLog() {
		return keylog;
	}

	@Override
	public File getKeyLog(boolean force) {
		return getFile(keylog.first(), KEY_LOG, force);
	}

	@Override
	public File[] getKeylogs(boolean force) {
		return getFiles(keylog, KEY_LOG, force);
	}

	public void setKeyLog(FileReferenceList list) {
		keylog = list;
	}

	@Override
	public FileReferenceList getToolData() {
		return tooldata;
	}

	@Override
	public File getToolData(boolean force) {
		return getFile(tooldata.first(), TOOL_DATA, force);
	}

	@Override
	public File[] getToolDatas(boolean force) {
		return getFiles(tooldata, TOOL_DATA, force);
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

	@Override
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

	@Override
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

	@Override
	public String[] getAudioURLs() {
		if (audio == null || audio.isEmpty())
			return null;

		return audio.getHrefs();
	}

	public void setAudio(FileReferenceList list) {
		audio = list;
	}

	@Override
	public boolean isHidden() {
		return isHidden;
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
				if (label == null)
					label = id;
				return true;
			}
			case DISPLAY_NAME: {
				displayName = (String) value;
				return true;
			}
			case LABEL: {
				label = (String) value;
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
				photo = parseFileReference(value);
				return true;
			}
			case VIDEO: {
				video = parseFileReference(value);
				return true;
			}
			case BACKUP: {
				backup = parseFileReference(value);
				return true;
			}
			case KEY_LOG: {
				keylog = parseFileReference(value);
				return true;
			}
			case TOOL_DATA: {
				tooldata = parseFileReference(value);
				return true;
			}
			case DESKTOP: {
				desktop = parseFileReference(value);
				return true;
			}
			case WEBCAM: {
				webcam = parseFileReference(value);
				return true;
			}
			case AUDIO: {
				audio = parseFileReference(value);
				return true;
			}
			case HIDDEN: {
				isHidden = parseBoolean(value);
				return true;
			}
			default:
				return false;
		}
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
		t.label = label;
		t.groupIds = groupIds;
		t.organizationId = organizationId;
		t.icpcId = icpcId;
		t.x = x;
		t.y = y;
		t.rotation = rotation;
		t.isHidden = isHidden;
		return t;
	}

	@Override
	protected void getProperties(Properties props) {
		props.addLiteralString(ID, id);
		props.addString(LABEL, label);
		props.addString(NAME, name);
		props.addString(DISPLAY_NAME, displayName);
		props.addLiteralString(ICPC_ID, icpcId);
		props.addArray(GROUP_IDS, groupIds);
		props.addLiteralString(ORGANIZATION_ID, organizationId);
		props.addFileRef(PHOTO, photo);
		props.addFileRef(VIDEO, video);
		props.addFileRef(BACKUP, backup);
		props.addFileRef(KEY_LOG, keylog);
		props.addFileRef(TOOL_DATA, tooldata);
		props.addFileRefSubs(DESKTOP, desktop);
		props.addFileRefSubs(WEBCAM, webcam);
		props.addFileRefSubs(AUDIO, audio);
		if (!Double.isNaN(x) || !Double.isNaN(y) || !Double.isNaN(rotation)) {
			List<String> attrs = new ArrayList<>(3);
			if (!Double.isNaN(x))
				attrs.add("\"" + X + "\":" + round(x));
			if (!Double.isNaN(y))
				attrs.add("\"" + Y + "\":" + round(y));
			if (!Double.isNaN(rotation))
				attrs.add("\"" + ROTATION + "\":" + round(rotation));
			props.add(LOCATION, "{" + String.join(",", attrs) + "}");
		}
		if (isHidden)
			props.add(HIDDEN, "true");
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