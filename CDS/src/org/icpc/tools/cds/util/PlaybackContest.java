package org.icpc.tools.cds.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.ConfiguredContest.Test;
import org.icpc.tools.cds.ConfiguredContest.View;
import org.icpc.tools.cds.video.ReactionVideoRecorder;
import org.icpc.tools.cds.video.VideoAggregator;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.feed.Timestamp;
import org.icpc.tools.contest.model.internal.Award;
import org.icpc.tools.contest.model.internal.Clarification;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.FileReference;
import org.icpc.tools.contest.model.internal.FileReferenceList;
import org.icpc.tools.contest.model.internal.Group;
import org.icpc.tools.contest.model.internal.Info;
import org.icpc.tools.contest.model.internal.Judgement;
import org.icpc.tools.contest.model.internal.Organization;
import org.icpc.tools.contest.model.internal.Problem;
import org.icpc.tools.contest.model.internal.Run;
import org.icpc.tools.contest.model.internal.State;
import org.icpc.tools.contest.model.internal.Submission;
import org.icpc.tools.contest.model.internal.Team;
import org.icpc.tools.contest.model.internal.TeamMember;

public class PlaybackContest extends Contest {
	private static final String LOGO = "logo";
	private static final String PHOTO = "photo";
	private static final String VIDEO = "video";
	private static final String BANNER = "banner";
	private static final String FILES = "files";
	private static final String REACTION = "reaction";
	private static final String COUNTRY_FLAG = "country_flag";

	protected ConfiguredContest cc;
	protected String contestId;
	protected boolean recordReactions;
	protected boolean isTesting;
	protected double timeMultiplier = Double.NaN;
	protected Long startTime;
	private List<IContestObject> defaults = new ArrayList<>();
	protected boolean configurationLoaded;

	public PlaybackContest(ConfiguredContest cc) {
		contestId = cc.getId();
		recordReactions = cc.isRecordingReactions();
		this.cc = cc;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public void setTestMode() {
		Test test = cc.getTest();
		timeMultiplier = test.getMultiplier();
		if (test.getStartTime() >= 0) {
			startTime = test.getStartTime();
			Trace.trace(Trace.USER, "Contest " + contestId + " starting at " + Timestamp.format(startTime));
		} else if (test.getCountdown() >= 0) {
			startTime = System.currentTimeMillis() + test.getCountdown() * 1000L;
			Trace.trace(Trace.USER, "Contest " + contestId + " starting in " + test.getCountdown() + " seconds");
		} else { // default to 30s countdown
			startTime = System.currentTimeMillis() + 30L * 1000L;
			Trace.trace(Trace.USER, "Contest " + contestId + " starting in 30s");
		}

		isTesting = true;
	}

	/**
	 * Delay if we haven't reached the given contest time in ms based on the contest start time and
	 * the time multiplier.
	 *
	 * @param info
	 * @param time
	 */
	protected void waitForContestTime(int time) {
		if (startTime == null || startTime < 0)
			return;

		double contestTime = System.currentTimeMillis() - startTime;
		long dt = (long) (time / timeMultiplier - contestTime);

		// no point waiting for less that 5ms
		if (dt < 5)
			return;

		// let people know if we'll be waiting for a while
		if (dt > 5000)
			Trace.trace(Trace.USER, "Sleeping for " + (int) (dt / 100) / 10f + " seconds.");

		try {
			LockSupport.parkNanos(dt * 1000000);
		} catch (Exception e) {
			// ignore
		}
	}

	private void fixInfoStartTime(Info info) {
		if (info.getStartTime() == null)
			return;

		info.setStartStatus(startTime);
	}

	private void fixStateTimes(State state) {
		if (startTime == null || startTime < 0)
			return;

		if (state.getStarted() != null)
			state.setStarted(startTime);
		if (state.getEnded() != null)
			state.setEnded(startTime + getDuration());
		if (state.getFrozen() != null)
			state.setFrozen(startTime + getDuration() - getFreezeDuration());
		if (state.getThawed() != null)
			state.setThawed(startTime + getDuration());
		if (state.getFinalized() != null)
			state.setFinalized(startTime + getDuration());
		if (state.getEndOfUpdates() != null)
			state.setEndOfUpdates(startTime + getDuration());
	}

	protected void downloadAndSync(RESTContestSource src, IContestObject obj) {
		if (src == null)
			return;

		if (obj instanceof Info) {
			Info i = (Info) obj;
			downloadMissingFiles(src, obj, LOGO, i.getLogo());
			downloadMissingFiles(src, obj, BANNER, i.getBanner());
			src.attachLocalResources(i);
		} else if (obj instanceof Team) {
			Team t = (Team) obj;
			downloadMissingFiles(src, obj, PHOTO, t.getPhoto());
			downloadMissingFiles(src, obj, VIDEO, t.getVideo());
			// later: backup, key_log or tool data
			src.attachLocalResources(t);
		} else if (obj instanceof TeamMember) {
			TeamMember tm = (TeamMember) obj;
			downloadMissingFiles(src, obj, PHOTO, tm.getPhoto());
			src.attachLocalResources(tm);
		} else if (obj instanceof Organization) {
			Organization o = (Organization) obj;
			downloadMissingFiles(src, obj, LOGO, o.getLogo());
			downloadMissingFiles(src, obj, COUNTRY_FLAG, o.getCountryFlag());
			src.attachLocalResources(o);
		} else if (obj instanceof Submission) {
			Submission s = (Submission) obj;
			downloadMissingFiles(src, obj, FILES, s.getFiles());
			downloadMissingFiles(src, obj, REACTION, s.getReaction());
			src.attachLocalResources(s);
		}
	}

	protected boolean downloadMissingFiles(RESTContestSource src, IContestObject obj, String property,
			FileReferenceList sourceFiles) {
		if (src == null || sourceFiles == null || sourceFiles.isEmpty())
			return false;
		FileReferenceList localFiles = src.getFilesWithPattern(obj, property);

		// if we don't have any files and there is at least one at the source, download everything
		if (localFiles == null || localFiles.size() == 0) {
			for (FileReference sourceFile : sourceFiles) {
				try {
					src.downloadFile(obj, sourceFile, property);
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error downloading file: " + obj.getType() + ": " + obj.getId(), e);
				}
			}

			return true;
		}

		// more complex matching
		// for now, only download new images that have a different width & height or mime type than
		// local images
		for (FileReference sourceFile : sourceFiles) {
			if (sourceFile.height <= 0 || sourceFile.width <= 0)
				continue;

			boolean found = false;
			for (FileReference currentRef : localFiles) {
				if (currentRef.height == sourceFile.height && currentRef.width == sourceFile.width
						&& (currentRef.mime == null || currentRef.mime.equals(sourceFile.mime))) {
					found = true;
					continue;
				}
			}

			try {
				if (!found)
					src.downloadFile(obj, sourceFile, property);
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error downloading file: " + obj.getType() + ": " + obj.getId(), e);
			}
		}
		return false;
	}

	@Override
	public void add(IContestObject obj) {
		if (cc.getView() != null) {
			View view = cc.getView();
			if (obj instanceof Group) {
				Group g = (Group) obj;
				if (!view.matchesGroup(g.getId()))
					return;
			}
			if (obj instanceof Problem) {
				String[] problems = cc.getView().getProblems();
				if (problems != null) {
					Problem p = (Problem) obj;
					if (!view.matchesProblem(p.getLabel()))
						return;
				}
			}
			if (obj instanceof Team) {
				Team team = (Team) obj;
				if (team.getGroupIds() == null)
					return;

				boolean found = false;
				for (String gId : team.getGroupIds()) {
					if (view.matchesGroup(gId)) {
						found = true;
						break;
					}
				}
				if (!found)
					return;
			}
			if (obj instanceof Submission) {
				Submission sub = (Submission) obj;
				if (getTeamById(sub.getTeamId()) == null)
					return;
				if (getProblemById(sub.getProblemId()) == null)
					return;
			}
			if (obj instanceof Judgement) {
				Judgement jud = (Judgement) obj;
				if (getSubmissionById(jud.getSubmissionId()) == null)
					return;
			}
			if (obj instanceof Run) {
				Run run = (Run) obj;
				if (getJudgementById(run.getJudgementTypeId()) == null)
					return;
			}
			if (obj instanceof TeamMember) {
				TeamMember member = (TeamMember) obj;
				ITeam team = getTeamById(member.getTeamId());
				if (team == null)
					return;
			}
			if (obj instanceof Clarification) {
				Clarification clar = (Clarification) obj;
				if (clar.getFromTeamId() != null && getTeamById(clar.getFromTeamId()) == null)
					return;
				if (clar.getToTeamId() != null && getTeamById(clar.getToTeamId()) == null)
					return;
				if (clar.getProblemId() != null && getProblemById(clar.getProblemId()) == null)
					return;
			}
			if (obj instanceof Award) {
				Award a = (Award) obj;
				if (a.getAwardType() == IAward.FIRST_TO_SOLVE) {
					boolean found = false;
					for (IProblem p : getProblems()) {
						if (a.getId().endsWith("-" + p.getId()))
							found = true;
					}
					if (!found)
						return;
				}

				String[] teamIds = a.getTeamIds();
				if (teamIds != null) {
					int count = 0;
					for (String teamId : teamIds) {
						ITeam team = getTeamById(teamId);
						if (team != null)
							count++;
					}
					if (count == 0)
						return;

					if (count != teamIds.length) {
						List<String> ids = new ArrayList<String>(count);
						for (String teamId : teamIds) {
							ITeam team = getTeamById(teamId);
							if (team != null)
								ids.add(teamId);
						}
						a.setTeamIds(ids.toArray(new String[count]));
					}
				}
			}
		}

		if (cc.getContestSource() instanceof RESTContestSource)
			downloadAndSync((RESTContestSource) cc.getContestSource(), obj);

		// set team video references
		if (obj instanceof Team) {
			Team team = (Team) obj;
			String teamId = team.getId();
			if (cc.isDesktopEnabled(teamId)) {
				FileReference desktopRef = new FileReference();
				desktopRef.href = "http://<host>/video/desktop/" + team.getId();
				desktopRef.mime = "application/m2ts";
				// desktopRef.data = "desktop";
				team.setDesktop(new FileReferenceList(desktopRef));
			} else
				team.setDesktop(null);
			if (cc.isWebcamEnabled(teamId)) {
				FileReference webcamRef = new FileReference();
				webcamRef.href = "http://<host>/video/webcam/" + team.getId();
				webcamRef.mime = "application/m2ts";
				// webcamRef.data = "webcam";
				team.setWebcam(new FileReferenceList(webcamRef));
			} else
				team.setWebcam(null);
			if (cc.isAudioEnabled(teamId)) {
				FileReference audioRef = new FileReference();
				audioRef.href = "http://<host>/video/audio/" + team.getId();
				audioRef.mime = "application/m2ts";
				// webcamRef.data = "audio";
				team.setAudio(new FileReferenceList(audioRef));
			} else
				team.setAudio(null);
		}

		if (obj instanceof Submission) {
			Submission sub = (Submission) obj;

			if (recordReactions && !isTesting)
				ReactionVideoRecorder.getInstance().startRecording(cc, sub);
		}

		if (obj instanceof Judgement) {
			Judgement j = (Judgement) obj;
			if (recordReactions && !isTesting && j.getJudgementTypeId() != null)
				ReactionVideoRecorder.getInstance().stopRecording(cc, j);
		}

		if (obj instanceof State) {
			State state = (State) obj;
			if (state.isFrozen() && state.isRunning())
				VideoAggregator.getInstance().dropUntrustedListeners();
		}

		ContestType type = obj.getType();
		boolean configType = true;
		if (type == ContestType.PROBLEM || type == ContestType.GROUP || type == ContestType.LANGUAGE
				|| type == ContestType.JUDGEMENT_TYPE || type == ContestType.TEAM || type == ContestType.TEAM_MEMBER
				|| type == ContestType.ORGANIZATION) {
			applyDefaults(obj);
			configType = true;
		}

		if (!configurationLoaded) {
			if (type != ContestType.CONTEST && !configType)
				configurationLoaded = true;
			else if (type != ContestType.CONTEST)
				defaults.add(obj);
		}

		// if the CCS says the contest is stopped but doesn't support pause time, fix it
		if (obj instanceof Info) {
			Info info = (Info) obj;
			Info currentInfo = getInfo();
			if (info.getStartTime() == null && info.getCountdownPauseTime() == null && !info.supportsCountdownPauseTime()
					&& currentInfo != null)
				info.setCountdownPauseTime(currentInfo.getCountdownPauseTime());
		}

		if (!isTesting) {
			super.add(obj);
			return;
		}

		if (obj instanceof Info) {
			Info info = (Info) obj;
			fixInfoStartTime(info);
			if (!Double.isNaN(timeMultiplier))
				info.setTimeMultiplier(timeMultiplier);
		}

		if (obj instanceof State) {
			State state = (State) obj;
			fixStateTimes(state);

			// if we're testing and contest hasn't started yet, just loop for 5s in case something
			// changes
			if (getState().getStarted() == null) {
				double dt = 5000;
				if (startTime != null && startTime > 0)
					dt = startTime - System.currentTimeMillis();
				while (dt > 2500) {
					// wait for 2s
					try {
						LockSupport.parkNanos(2000 * 1000000);
					} catch (Exception e) {
						// ignore
					}

					fixStateTimes(state);
					dt = 5000;
					if (startTime != null && startTime > 0)
						dt = startTime - System.currentTimeMillis();
				}
			}

			if (state.isRunning())
				waitForContestTime(0);
			if (state.isFrozen())
				waitForContestTime(getDuration() - getFreezeDuration());
			if ((!state.isRunning() && state.isFrozen()) || state.isFinal())
				waitForContestTime(getDuration());
		}

		if (obj instanceof Submission) {
			Submission s = (Submission) obj;
			waitForContestTime(s.getContestTime());
			s.add("time", Timestamp.now());
		}

		if (obj instanceof Judgement) {
			Judgement j = (Judgement) obj;
			if (j.getEndContestTime() != null) {
				waitForContestTime(j.getEndContestTime());
				j.add("end_time", Timestamp.now());
			} else {
				waitForContestTime(j.getStartContestTime());
				j.add("start_time", Timestamp.now());
			}
		}

		if (obj instanceof Run) {
			Run r = (Run) obj;
			waitForContestTime(r.getContestTime());
			r.add("time", Timestamp.now());
		}

		if (obj instanceof Clarification) {
			Clarification c = (Clarification) obj;
			waitForContestTime(c.getContestTime());
			c.add("time", Timestamp.now());
		}

		super.add(obj);
	}

	public void setConfigurationLoaded() {
		configurationLoaded = true;
	}

	private void applyDefaults(IContestObject obj) {
		for (IContestObject de : defaults) {
			if (de.equals(obj)) {
				Map<String, Object> props = de.getProperties();
				Map<String, Object> existingProps = obj.getProperties();
				for (String key : props.keySet()) {
					boolean found = false;
					for (String key2 : existingProps.keySet()) {
						if (key2.equals(key))
							found = true;
					}
					if (!found) {
						((ContestObject) obj).add(key, props.get(key));
						// ((ContestObject) obj).cloneProperty(de, p);
					}
				}
			}
		}
	}
}