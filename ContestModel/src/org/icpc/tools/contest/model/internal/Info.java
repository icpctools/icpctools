package org.icpc.tools.contest.model.internal;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContest.ScoreboardType;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IInfo;
import org.icpc.tools.contest.model.feed.RelativeTime;
import org.icpc.tools.contest.model.feed.Timestamp;

public class Info extends ContestObject implements IInfo {
	private static final boolean isDraftSpec = "draft".equals(System.getProperty("ICPC_CONTEST_API"));

	private static final String NAME = "name";
	private static final String FORMAL_NAME = "formal_name";
	private static final String START_TIME = "start_time";
	private static final String SCOREBOARD_THAW_TIME = "scoreboard_thaw_time";
	private static final String DURATION = "duration";
	private static final String SCOREBOARD_FREEZE_DURATION = "scoreboard_freeze_duration";
	private static final String PENALTY_TIME = "penalty_time";
	private static final String LOGO = "logo";
	private static final String LOGO_BACKGROUNDMODE = "prlogo";
	private static final String BANNER = "banner";
	private static final String BANNER_BACKGROUNDMODE = "prbanner";
	private static final String TIME_MULTIPLIER = "time_multiplier";
	private static final String COUNTDOWN_PAUSE_TIME = "countdown_pause_time";
	private static final String LOCATION = "location";
	private static final String SCOREBOARD_TYPE = "scoreboard_type";
	private static final String RGB = "rgb";

	private String name;
	private String formalName;
	private Long startTime;
	private Long pauseTime;
	private Long thawTime;
	private boolean supportsPauseTime;
	private long duration;
	private Long freezeDuration;
	private Long penalty;
	private ScoreboardType scoreboardType;
	private double timeMultiplier = Double.NaN;
	private Location location;
	private FileReferenceList logo;
	private FileReferenceList logoLightMode;
	private FileReferenceList banner;
	private FileReferenceList bannerLightMode;
	private String rgb;
	private Color colorVal;

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

	public String getRGB() {
		return rgb;
	}

	public Color getColorVal() {
		if (colorVal != null)
			return colorVal;

		if (rgb == null || !(rgb.length() == 3 || rgb.length() == 4 || rgb.length() == 6 || rgb.length() == 7)) {
			colorVal = Color.BLACK;
			return colorVal;
		}

		try {
			String rgbv = rgb;
			if (rgbv.length() == 3 || rgbv.length() == 4) {
				if (rgbv.length() == 4)
					rgbv = rgbv.substring(1);
				int r = Integer.parseInt(rgbv.substring(0, 1) + rgbv.substring(0, 1), 16);
				int g = Integer.parseInt(rgbv.substring(1, 2) + rgbv.substring(1, 2), 16);
				int b = Integer.parseInt(rgbv.substring(2, 3) + rgbv.substring(2, 3), 16);
				colorVal = new Color(r, g, b);
				return colorVal;
			}
			if (rgbv.length() == 7)
				rgbv = rgbv.substring(1);
			int r = Integer.parseInt(rgbv.substring(0, 2), 16);
			int g = Integer.parseInt(rgbv.substring(2, 4), 16);
			int b = Integer.parseInt(rgbv.substring(4, 6), 16);
			colorVal = new Color(r, g, b);
			return colorVal;
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Invalid color value for problem " + id + " (" + rgb + ")");
			colorVal = Color.BLACK;
			return colorVal;
		}
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartStatus(Long start) {
		if (start == null) {
			pauseTime = null;
			startTime = null;
		} else if (start.longValue() < 0) {
			pauseTime = -start.longValue();
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

	public Long getCountdownPauseTime() {
		return pauseTime;
	}

	public long getDuration() {
		return duration;
	}

	public Long getFreezeDuration() {
		return freezeDuration;
	}

	public Long getThawTime() {
		return thawTime;
	}

	public Long getPenaltyTime() {
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

	public void setCountdownPauseTime(Long time) {
		supportsPauseTime = true;
		pauseTime = time;
	}

	public double getLatitude() {
		if (location == null)
			return Double.NaN;
		return location.latitude;
	}

	public double getLongitude() {
		if (location == null)
			return Double.NaN;
		return location.longitude;
	}

	public void setLocation(Location loc) {
		location = loc;
	}

	public ScoreboardType getScoreboardType() {
		return scoreboardType == null ? ScoreboardType.PASS_FAIL : scoreboardType;
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

	public void setLogoLightMode(FileReferenceList list) {
		logoLightMode = list;
	}

	public FileReferenceList getLogoLightMode() {
		return logoLightMode;
	}

	public FileReferenceList getLogoLightMode(String mode) {
		return filterListLightMode(logoLightMode, mode);
	}

	public File getLogoLightMode(int width, int height, boolean force, String mode) {
		return getFile(getBestFileReference(filterListLightMode(logoLightMode, mode), new ImageSizeFit(width, height)), LOGO, force);
	}

	public BufferedImage getLogoLightModeImage(int width, int height, boolean forceLoad, boolean resizeToFit, String mode) {
		return getRefImage(LOGO, filterListLightMode(logoLightMode, mode), width, height, forceLoad, resizeToFit);
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

	public void setBannerLightMode(FileReferenceList list) {
		bannerLightMode = list;
	}

	public FileReferenceList getBannerLightMode() {
		return bannerLightMode;
	}

	public FileReferenceList getBannerLightMode(String mode) {
		return filterListLightMode(bannerLightMode, mode);
	}

	public File getBannerLightMode(int width, int height, boolean force, String mode) {
		return getFile(getBestFileReference(filterListLightMode(bannerLightMode, mode), new ImageSizeFit(width, height)), BANNER_BACKGROUNDMODE, force);
	}

	public BufferedImage getBannerLightModeImage(int width, int height, boolean forceLoad, boolean resizeToFit, String mode) {
		return getRefImage(BANNER_BACKGROUNDMODE, filterListLightMode(bannerLightMode, mode), width, height, forceLoad, resizeToFit);
	}

	@Override
	public File resolveFileReference(String url) {
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
		} else if (name2.equals(RGB)) {
			rgb = (String) value;
			colorVal = null;
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
		} else if (name2.equals(SCOREBOARD_THAW_TIME)) {
			thawTime = parseTimestamp(value);
			return true;
		} else if (name2.equals(PENALTY_TIME)) {
			try {
				// parse as integer
				penalty = parseInt(value) * (60 * 1000L);
			} catch (Exception e) {
				try {
					// parse as REL_TIME
					penalty = parseRelativeTime(value);
					return true;
				} catch (Exception ex) {
					return false;
				}
			}
			return true;
		} else if (name2.equals(TIME_MULTIPLIER)) {
			timeMultiplier = parseDouble(value);
			return true;
		} else if (name2.equals(LOCATION)) {
			location = parseLocation(value);
			return true;
		} else if (name2.equals(SCOREBOARD_TYPE)) {
			if ("pass-fail".equals(value))
				scoreboardType = ScoreboardType.PASS_FAIL;
			else if ("score".equals(value))
				scoreboardType = ScoreboardType.SCORE;
			else
				return false;
			return true;
		} else if (name2.equals(LOGO)) {
			logo = parseFileReference(value);
			return true;
		} else if (name2.equals(BANNER)) {
			banner = parseFileReference(value);
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
		i.rgb = rgb;
		i.startTime = startTime;
		i.duration = duration;
		i.freezeDuration = freezeDuration;
		i.thawTime = thawTime;
		i.penalty = penalty;
		i.location = location;
		i.scoreboardType = scoreboardType;
		return i;
	}

	protected static String toStr(int n) {
		if (n < 10)
			return "0" + n;
		return n + "";
	}

	@Override
	protected void getProperties(Properties props) {
		props.addLiteralString(ID, id);
		props.addString(NAME, name);
		props.addString(FORMAL_NAME, formalName);

		if (startTime != null)
			props.addLiteralString(START_TIME, Timestamp.format(startTime.longValue()));
		if (pauseTime != null)
			props.addLiteralString(COUNTDOWN_PAUSE_TIME, RelativeTime.format(pauseTime));

		props.addLiteralString(DURATION, RelativeTime.format(duration));
		if (freezeDuration != null)
			props.addLiteralString(SCOREBOARD_FREEZE_DURATION, RelativeTime.format(freezeDuration));

		if (scoreboardType != null) {
			if (ScoreboardType.PASS_FAIL.equals(scoreboardType))
				props.addLiteralString(SCOREBOARD_TYPE, "pass-fail");
			else if (ScoreboardType.SCORE.equals(scoreboardType))
				props.addLiteralString(SCOREBOARD_TYPE, "score");
		}
		if (thawTime != null)
			props.addLiteralString(SCOREBOARD_THAW_TIME, Timestamp.format(thawTime.longValue()));

		if (penalty != null) {
			if (isDraftSpec)
				props.addLiteralString(PENALTY_TIME, RelativeTime.format(penalty));
			else
				props.addInt(PENALTY_TIME, (int) (penalty.longValue() / (60 * 1000L)));
		}

		if (!Double.isNaN(timeMultiplier))
			props.addDouble(TIME_MULTIPLIER, timeMultiplier);

		if (location != null)
			props.add(LOCATION, location.getJSON());

		props.addLiteralString(RGB, rgb);

		props.addFileRef(LOGO, logo);
		props.addFileRef(LOGO_BACKGROUNDMODE, logoLightMode);
		props.addFileRef(BANNER, banner);
		props.addFileRef(BANNER_BACKGROUNDMODE, bannerLightMode);
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
		int hash = (int) duration + (int) timeMultiplier;
		if (freezeDuration != null)
			hash += freezeDuration;
		if (penalty != null)
			hash += penalty;
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
