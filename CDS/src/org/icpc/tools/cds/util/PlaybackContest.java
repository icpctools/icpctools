package org.icpc.tools.cds.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.ConfiguredContest.Test;
import org.icpc.tools.cds.video.ReactionVideoRecorder;
import org.icpc.tools.cds.video.VideoAggregator;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.feed.Timestamp;
import org.icpc.tools.contest.model.internal.Clarification;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.FileReference;
import org.icpc.tools.contest.model.internal.FileReferenceList;
import org.icpc.tools.contest.model.internal.Info;
import org.icpc.tools.contest.model.internal.Judgement;
import org.icpc.tools.contest.model.internal.Organization;
import org.icpc.tools.contest.model.internal.Run;
import org.icpc.tools.contest.model.internal.State;
import org.icpc.tools.contest.model.internal.Submission;
import org.icpc.tools.contest.model.internal.Team;
import org.icpc.tools.contest.model.internal.TeamMember;

public class PlaybackContest extends Contest {
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
			downloadMissingFiles(src, obj, "logo", (o) -> ((Info) o).getLogo());
			downloadMissingFiles(src, obj, "banner", (o) -> ((Info) o).getBanner());
		} else if (obj instanceof Team) {
			downloadMissingFiles(src, obj, "photo", (o) -> ((Team) o).getPhoto());
			downloadMissingFiles(src, obj, "video", (o) -> ((Team) o).getVideo());
			// downloadMissingFiles(contest, src, obj, "backup", (o) -> ((Team) o).getBackup());
		} else if (obj instanceof TeamMember) {
			downloadMissingFiles(src, obj, "photo", (o) -> ((TeamMember) o).getPhoto());
		} else if (obj instanceof Organization) {
			downloadMissingFiles(src, obj, "logo", (o) -> ((Organization) o).getLogo());
		} else if (obj instanceof Submission) {
			downloadMissingFiles(src, obj, "files", (o) -> ((Submission) o).getFiles());
			downloadMissingFiles(src, obj, "reaction", (o) -> ((Submission) o).getReaction());
		}
	}

	interface IRefList {
		public FileReferenceList getList(IContestObject obj);
	}

	private void downloadMissingFiles(RESTContestSource src, IContestObject obj, String name, IRefList test) {
		try {
			FileReferenceList newList = test.getList(obj);
			src.attachLocalResources(obj);
			if (downloadMissingFiles(src, obj, name, newList, test.getList(obj)))
				src.attachLocalResources(obj);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not download missing files", e);
		}
	}

	protected boolean downloadMissingFiles(RESTContestSource src, IContestObject obj, String property,
			FileReferenceList newList, FileReferenceList currentList) {
		if (src == null || newList == null || newList.isEmpty())
			return false;
		FileReferenceList currentList2 = currentList;
		if (currentList == null)
			currentList2 = new FileReferenceList();

		// if we don't have any files and there is at least one, download it
		if (!newList.isEmpty() && currentList2.size() == 0) {
			// download new ref
			for (FileReference newRef : newList) {
				try {
					src.downloadFile(obj, newRef, property);
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error downloading file: " + obj.getType() + ": " + obj.getId(), e);
				}
			}

			return true;
		}

		// more complex matching
		// for now, only download new images that have a different width & height than local images
		for (FileReference newRef : newList) {
			if (newRef.height <= 0 || newRef.width <= 0)
				continue;

			boolean found = false;
			for (FileReference currentRef : currentList2) {
				if (currentRef.height == newRef.height && currentRef.width == newRef.width) {
					found = true;
					continue;
				}
			}

			if (found)
				continue;

			try {
				src.downloadFile(obj, newRef, property);
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error downloading file: " + obj.getType() + ": " + obj.getId(), e);
			}
		}
		return false;
	}

	@Override
	public void add(IContestObject obj) {
		RESTContestSource src = null;
		if (cc != null && cc.getContestSource() instanceof RESTContestSource) {
			src = (RESTContestSource) cc.getContestSource();
		}
		downloadAndSync(src, obj);

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