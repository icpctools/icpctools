package org.icpc.tools.contest.model.internal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IInfo;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.feed.RelativeTime;
import org.icpc.tools.contest.model.feed.Timestamp;

public class Info extends ContestObject implements IInfo {
	private static final String NAME = "name";
	private static final String FORMAL_NAME = "formal_name";
	private static final String START_TIME = "start_time";
	private static final String DURATION = "duration";
	private static final String SCOREBOARD_FREEZE_DURATION = "scoreboard_freeze_duration";
	private static final String PENALTY_TIME = "penalty_time";
	private static final String LOGO = "logo";
	private static final String BANNER = "banner";
	private static final String TIME_MULTIPLIER = "time_multiplier";
	private static final String COUNTDOWN_PAUSE_TIME = "countdown_pause_time";
	private static final String LOCATION = "location";
	private static final String LATITUDE = "latitude";
	private static final String LONGITUDE = "longitude";

	private String name;
	private String formalName;
	private Long startTime;
	private Integer pauseTime;
	private boolean supportsPauseTime;
	private int duration;
	private int freezeDuration;
	private int penalty;
	private double timeMultiplier = Double.NaN;
	private double latitude = Double.MIN_VALUE;
	private double longitude = Double.MIN_VALUE;
	private FileReferenceList banner;
	private FileReferenceList logo;

	@Override
	public ContestType getType() {
		return ContestType.CONTEST;
	}

	@Override
	public void setId(String id) {
		super.setId(id);
	}

	public String getName() {
		return name;
	}

	public String getFormalName() {
		return formalName;
	}

	public String getActualFormalName() {
		if (formalName == null)
			return name;
		return formalName;
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartStatus(Long start) {
		if (start == null) {
			pauseTime = null;
			startTime = null;
		} else if (start.longValue() < 0) {
			pauseTime = -start.intValue();
			supportsPauseTime = true;
			startTime = null;
		} else {
			pauseTime = null;
			startTime = start;
		}
	}

	public boolean supportsCountdownPauseTime() {
		return supportsPauseTime;
	}

	public Integer getCountdownPauseTime() {
		return pauseTime;
	}

	public int getDuration() {
		return duration;
	}

	public int getFreezeDuration() {
		return freezeDuration;
	}

	public int getPenaltyTime() {
		return penalty;
	}

	public double getTimeMultiplier() {
		if (Double.isNaN(timeMultiplier))
			return 1.0;
		return timeMultiplier;
	}

	public void setTimeMultiplier(double multiplier) {
		timeMultiplier = multiplier;
	}

	public void setCountdownPauseTime(Integer time) {
		supportsPauseTime = true;
		pauseTime = time;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLogo(FileReferenceList list) {
		logo = list;
	}

	public FileReferenceList getLogo() {
		return logo;
	}

	public File getLogo(int width, int height, boolean force) {
		return getFile(getBestFileReference(logo, new ImageSizeFit(width, height)), LOGO, force);
	}

	public BufferedImage getLogoImage(int width, int height, boolean forceLoad, boolean resizeToFit) {
		return getRefImage(LOGO, logo, width, height, forceLoad, resizeToFit);
	}

	public void setBanner(FileReferenceList list) {
		banner = list;
	}

	public FileReferenceList getBanner() {
		return banner;
	}

	public File getBanner(int width, int height, boolean force) {
		return getFile(getBestFileReference(banner, new ImageSizeFit(width, height)), BANNER, force);
	}

	public BufferedImage getBannerImage(int width, int height, boolean forceLoad, boolean resizeToFit) {
		return getRefImage(BANNER, banner, width, height, forceLoad, resizeToFit);
	}

	@Override
	public Object resolveFileReference(String url) {
		return FileReferenceList.resolve(url, logo, banner);
	}

	@Override
	protected boolean addImpl(String name2, Object value) throws Exception {
		if (name2.equals(NAME)) {
			name = (String) value;
			return true;
		} else if (name2.equals(FORMAL_NAME)) {
			formalName = (String) value;
			return true;
		} else if (name2.equals(START_TIME)) {
			startTime = parseTimestamp(value);
			return true;
		} else if (name2.equals(COUNTDOWN_PAUSE_TIME)) {
			supportsPauseTime = true;
			pauseTime = parseRelativeTime(value);
			return true;
		} else if (name2.equals(DURATION)) {
			duration = parseRelativeTime(value);
			return true;
		} else if (name2.equals(SCOREBOARD_FREEZE_DURATION)) {
			freezeDuration = parseRelativeTime(value);
			return true;
		} else if (name2.equals(PENALTY_TIME)) {
			penalty = parseInt(value);
			return true;
		} else if (name2.equals(TIME_MULTIPLIER)) {
			timeMultiplier = parseDouble(value);
			return true;
		} else if (name2.equals(LOCATION)) {
			JsonObject obj = JSONParser.getOrReadObject(value);
			latitude = obj.getDouble(LATITUDE);
			longitude = obj.getDouble(LONGITUDE);
			return true;
		} else if (name2.equals(LOGO)) {
			logo = new FileReferenceList(value);
			return true;
		} else if (name2.equals(BANNER)) {
			banner = new FileReferenceList(value);
			return true;
		}

		return false;
	}

	@Override
	public IContestObject clone() {
		Info i = new Info();
		i.id = id;
		i.logo = logo;
		i.banner = banner;
		i.name = name;
		i.formalName = formalName;
		i.startTime = startTime;
		i.duration = duration;
		i.freezeDuration = freezeDuration;
		i.penalty = penalty;
		i.latitude = latitude;
		i.longitude = longitude;
		return i;
	}

	protected static String toStr(int n) {
		if (n < 10)
			return "0" + n;
		return n + "";
	}

	private static double round(double d) {
		return Math.round(d * 10000.0) / 10000.0;
	}

	@Override
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		props.put(NAME, name);
		if (formalName != null)
			props.put(FORMAL_NAME, formalName);

		if (startTime != null)
			props.put(START_TIME, Timestamp.format(startTime.longValue()));
		if (pauseTime != null)
			props.put(COUNTDOWN_PAUSE_TIME, RelativeTime.format(pauseTime));

		props.put(DURATION, RelativeTime.format(duration));
		if (freezeDuration > -1)
			props.put(SCOREBOARD_FREEZE_DURATION, RelativeTime.format(freezeDuration));
		if (penalty >= 0)
			props.put(PENALTY_TIME, penalty);

		if (!Double.isNaN(timeMultiplier))
			props.put(TIME_MULTIPLIER, timeMultiplier);

		if (latitude != Double.MIN_VALUE || longitude != Double.MIN_VALUE) {
			List<String> attrs = new ArrayList<>(2);
			if (latitude != Double.MIN_VALUE)
				attrs.add("\"" + LATITUDE + "\":" + round(latitude));
			if (longitude != Double.MIN_VALUE)
				attrs.add("\"" + LONGITUDE + "\":" + round(longitude));
			props.put(LOCATION, "{" + String.join(",", attrs) + "}");
		}

		props.put(LOGO, logo);
		props.put(BANNER, banner);
	}

	@Override
	public void writeBody(JSONEncoder je) {
		je.encode(ID, id);
		je.encode(NAME, name);
		je.encode(FORMAL_NAME, formalName);

		if (startTime == null)
			je.encode(START_TIME);
		else
			je.encodeString(START_TIME, Timestamp.format(startTime.longValue()));

		if (pauseTime != null)
			je.encode(COUNTDOWN_PAUSE_TIME, RelativeTime.format(pauseTime));

		je.encodeString(DURATION, RelativeTime.format(duration));
		if (freezeDuration > -1)
			je.encodeString(SCOREBOARD_FREEZE_DURATION, RelativeTime.format(freezeDuration));
		if (penalty >= 0)
			je.encode(PENALTY_TIME, penalty);

		if (!Double.isNaN(timeMultiplier))
			je.encode(TIME_MULTIPLIER, timeMultiplier);

		if (latitude != Double.MIN_VALUE || longitude != Double.MIN_VALUE) {
			List<String> attrs = new ArrayList<>(2);
			if (latitude != Double.MIN_VALUE)
				attrs.add("\"" + LATITUDE + "\":" + round(latitude));
			if (longitude != Double.MIN_VALUE)
				attrs.add("\"" + LONGITUDE + "\":" + round(longitude));
			je.encodePrimitive(LOCATION, "{" + String.join(",", attrs) + "}");
		}

		je.encode(LOGO, logo, false);
		je.encode(BANNER, banner, false);
	}

	@Override
	public List<String> validate(IContest c) {
		List<String> errors = super.validate(c);

		if (name == null || name.isEmpty())
			errors.add("Name missing");

		if (duration <= 0)
			errors.add("Invalid duration");

		if (errors.isEmpty())
			return null;
		return errors;
	}

	public int deepHash() {
		int hash = duration + freezeDuration + penalty + (int) timeMultiplier;
		if (id != null)
			hash += id.hashCode();
		if (name != null)
			hash += name.hashCode();
		if (startTime != null)
			hash += startTime.hashCode();
		if (pauseTime != null)
			hash += pauseTime.hashCode();
		return hash;
	}
}