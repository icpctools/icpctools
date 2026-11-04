package org.icpc.tools.cds.video;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.video.VideoHandler.IRecording;
import org.icpc.tools.cds.video.VideoStream.StreamType;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.FileReference;
import org.icpc.tools.contest.model.internal.FileReferenceList;
import org.icpc.tools.contest.model.internal.Submission;

/**
 * Reaction video recorder. Will record webcam and desktop video from when a submission is received
 * until 30 seconds after the corresponding judgement. If there is no judgement, recording will be
 * capped at 5 minutes.
 *
 * The recording itself is delegated to each stream's VideoHandler, so the format (single file, HLS
 * index and segments, etc.) is up to the handler.
 */
public class ReactionVideoRecorder {
	private ScheduledExecutorService scheduledExecutor;
	private static ReactionVideoRecorder instance = new ReactionVideoRecorder();
	private static final int DURATION = 30; // 30 seconds
	private static final int MAX_DURATION = 5 * 60; // 5 minutes
	private static final String WEBCAM = "webcam";
	private static final String DESKTOP = "desktop";
	private static final String MARKER_SUFFIX = "-temp";
	// recording markers older than this are left over from a crash and can be removed
	private static final long STALE_MARKER_AGE_MS = (MAX_DURATION + 2 * 60) * 1000L;

	protected static class Info {
		List<IRecording> recordings;
		List<File> markers;
	}

	// map from submission id to reaction Info
	private Map<String, Info> submissions = new HashMap<>(100);

	private ReactionVideoRecorder() {
		// do nothing
	}

	public void start(ScheduledExecutorService executor) {
		scheduledExecutor = executor;
		instance = this;

		// markers can be left behind by a crash (here or on another CDS), so check periodically
		executor.scheduleWithFixedDelay(this::cleanUpReactions, 0, MAX_DURATION * 2, TimeUnit.SECONDS);
	}

	/**
	 * Remove stale recording markers from all contests.
	 */
	public void cleanUpReactions() {
		try {
			for (ConfiguredContest cc : CDSConfig.getContests()) {
				String dir = cc.getPath();
				if (dir != null)
					cleanUpMarkers(new File(dir));
			}
		} catch (Exception e) {
			// don't let an exception cancel the scheduled cleanup
			Trace.trace(Trace.ERROR, "Error cleaning up reaction markers", e);
		}
	}

	public static ReactionVideoRecorder getInstance() {
		return instance;
	}

	/**
	 * Remove recording markers (submissions/<id>/*-temp) that are older than the maximum recording
	 * time, i.e. left over from a CDS that crashed or was stopped while recording.
	 */
	protected static void cleanUpMarkers(File contestDir) {
		File[] submissionDirs = new File(contestDir, "submissions").listFiles(File::isDirectory);
		if (submissionDirs == null)
			return;

		long cutoff = System.currentTimeMillis() - STALE_MARKER_AGE_MS;
		for (File submissionDir : submissionDirs) {
			File[] markers = submissionDir.listFiles((d, name) -> name.endsWith(MARKER_SUFFIX));
			if (markers == null)
				continue;

			for (File marker : markers) {
				long modified = marker.lastModified();
				if (modified > 0 && modified < cutoff) {
					if (marker.delete())
						Trace.trace(Trace.INFO, "Removed stale reaction marker: " + marker);
					else
						Trace.trace(Trace.WARNING, "Could not remove stale reaction marker: " + marker);
				}
			}
		}
	}

	public void startRecording(ConfiguredContest cc, ISubmission submission) {
		IContest contest = cc.getContest();
		String rootFolder = cc.getPath();

		String submissionId = submission.getId();
		if (submissions.containsKey(submissionId))
			return;

		Info info = new Info();
		submissions.put(submissionId, info);

		if (scheduledExecutor.isShutdown())
			return;

		String teamId = submission.getTeamId();
		ITeam team = contest.getTeamById(teamId);
		if (team != null && contest.isTeamHidden(team))
			return;

		// check if already recorded in a previous run
		File submissionDir = new File(rootFolder, "submissions" + File.separator + submissionId);
		if (!submissionDir.exists())
			submissionDir.mkdirs();

		File[] files = submissionDir
				.listFiles((dir, name) -> (name.toLowerCase().startsWith("reaction") && name.indexOf(".") >= 0));
		if (files != null && files.length > 0) {
			// recordings already exist, just return (and attachLocal will add them)
			return;
		}

		startRecordingImpl(cc, info, teamId, submission);
	}

	private void startRecordingImpl(ConfiguredContest cc, Info info, String teamId, ISubmission submission) {
		String submissionId = submission.getId();
		File submissionDir = new File(cc.getPath(), "submissions" + File.separator + submissionId);

		// figure out which streams we're going to save and the base names we'll save them to
		List<Integer> streams = new ArrayList<>();
		List<String> baseNames = new ArrayList<>();
		addStreams(cc.getStreams(teamId, StreamType.WEBCAM), WEBCAM, streams, baseNames);
		addStreams(cc.getStreams(teamId, StreamType.DESKTOP), DESKTOP, streams, baseNames);

		if (streams.isEmpty())
			return;

		VideoAggregator va = VideoAggregator.getInstance();
		FileReferenceList list = new FileReferenceList();
		List<IRecording> recordings = new ArrayList<>();
		List<File> markers = new ArrayList<>();
		for (int i = 0; i < streams.size(); i++) {
			VideoStream stream = va.getStream(streams.get(i));
			VideoHandler handler = stream.getHandler();
			String baseName = baseNames.get(i);
			File file = handler.getRecordingFile(submissionDir, baseName);

			// path of the recording relative to the submission folder, e.g. "reaction.webcam.ts" or
			// "reaction.webcam/index.m3u8"
			String relPath = submissionDir.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
			String topLevel = relPath.contains("/") ? relPath.substring(0, relPath.indexOf('/')) : relPath;

			// create marker file beside the top-level file or folder. if it already exists, another
			// CDS is already recording
			File marker = new File(submissionDir, topLevel + MARKER_SUFFIX);
			boolean secondary = false;
			try {
				secondary = !marker.createNewFile();
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error creating marker file", e);
				secondary = marker.exists();
			}

			if (!secondary) {
				marker.deleteOnExit();
				markers.add(marker);

				Trace.trace(Trace.INFO,
						"Recording reaction for " + submissionId + " from " + teamId + " on " + stream.getName());
				try {
					recordings.add(handler.startRecording(stream, submissionDir, baseName));
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Could not record reaction " + submissionId + " on " + stream.getName(), e);
					marker.delete();
					markers.remove(marker);
					continue;
				}
			}

			FileReference ref = new FileReference();
			// single files are referenced without extension, folder-based recordings by their path
			String name = relPath.contains("/") ? relPath : baseName;
			ref.href = "contests/" + cc.getId() + "/submissions/" + submissionId + "/" + name;
			ref.mime = stream.getMimeType();
			ref.file = file;
			list.add(ref);
		}

		synchronized (info) {
			info.recordings = recordings;
			info.markers = markers;
		}

		((Submission) submission).setReaction(list);

		scheduleEndOfRecording(info, MAX_DURATION);
	}

	private static void addStreams(List<Integer> list, String type, List<Integer> streams, List<String> baseNames) {
		if (list == null)
			return;

		int num = list.size();
		for (int i = 0; i < num; i++) {
			streams.add(list.get(i));
			if (num > 1)
				baseNames.add("reaction." + type + (i + 1));
			else
				baseNames.add("reaction." + type);
		}
	}

	public void stopRecording(ConfiguredContest cc, IJudgement judgement) {
		IContest contest = cc.getContest();

		String subId = judgement.getSubmissionId();
		ISubmission submission = contest.getSubmissionById(subId);
		if (submission == null)
			return;

		Info info = submissions.get(subId);
		if (info != null && info.recordings != null) {
			scheduleEndOfRecording(info, DURATION);
		}
	}

	protected ScheduledFuture<?> scheduleEndOfRecording(Info info, int time) {
		return scheduledExecutor.schedule(() -> endRecording(info), time, TimeUnit.SECONDS);
	}

	private static void endRecording(Info info) {
		List<IRecording> recordings;
		List<File> markers;
		synchronized (info) {
			recordings = info.recordings;
			markers = info.markers;
			info.recordings = null;
			info.markers = null;
		}
		if (recordings == null)
			return;

		for (IRecording rec : recordings) {
			try {
				rec.stop();
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error stopping reaction recording", e);
			}
			if (rec.isEmpty())
				Trace.trace(Trace.WARNING, "No video received for reaction recording: " + rec);
			// TODO - not safe to delete empty recordings if another CDS has picked it up already
		}

		for (File marker : markers) {
			if (marker.exists())
				marker.delete();
		}
		Trace.trace(Trace.INFO, "Reaction recording done");
	}
}
