package org.icpc.tools.cds.video;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.cds.ConfiguredContest;
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
 */
public class ReactionVideoRecorder {
	private ScheduledExecutorService scheduledExecutor;
	private static ReactionVideoRecorder instance = new ReactionVideoRecorder();
	private static final int DURATION = 30; // 30 seconds
	private static final int MAX_DURATION = 5 * 60; // 5 minutes
	private static final String WEBCAM = "webcam";
	private static final String DESKTOP = "desktop";

	protected class Info {
		int[] stream;
		VideoStreamListener[] listener;
		OutputStream[] out;
		File[] tempFile;
		File[] file;
	}

	// map from submission id to reaction Info
	private Map<String, Info> submissions = new HashMap<>(100);

	private ReactionVideoRecorder() {
		// do nothing
	}

	public void start(ScheduledExecutorService executor) {
		scheduledExecutor = executor;
		cleanUpReactions();
		instance = this;
	}

	public void cleanUpReactions() {
		for (ConfiguredContest cc : CDSConfig.getContests())
			cleanUpReactions(cc);
	}

	public static ReactionVideoRecorder getInstance() {
		return instance;
	}

	private static void cleanUpReactions(ConfiguredContest cc) {
		// check if already recorded in a previous run
		String dir = cc.getPath();
		if (dir == null)
			return;

		// TODO - wrong folder, need to remove or do deep scan on submissions folder
		File reactDir = new File(dir, "video" + File.separator + "reactions");
		if (!reactDir.exists())
			return;

		File[] files = reactDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File f, String s) {
				return f.getName().endsWith(".temp");
			}
		});

		for (File f : files)
			f.delete();
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

		VideoHandler handler = VideoAggregator.handler;
		String extension = handler.getFileExtension();

		File[] files = submissionDir.listFiles(
				(dir, name) -> (name.toLowerCase().startsWith("reaction") && name.toLowerCase().endsWith("." + extension)));
		if (files != null && files.length > 0) {
			// recordings already exist, just return (and attachLocal will add them)
			return;
		}

		startRecordingImpl(cc, info, teamId, submission);
	}

	private void startRecordingImpl(ConfiguredContest cc, Info info, String teamId, ISubmission submission) {
		int numStreams = 0;

		List<Integer> streamsWebcam = cc.getStreams(teamId, StreamType.WEBCAM);
		if (streamsWebcam != null)
			numStreams += streamsWebcam.size();

		List<Integer> streamsDesktop = cc.getStreams(teamId, StreamType.DESKTOP);
		if (streamsDesktop != null)
			numStreams += streamsDesktop.size();

		if (numStreams == 0)
			return;

		info.file = new File[numStreams];
		info.stream = new int[numStreams];
		info.listener = new VideoStreamListener[numStreams];
		info.out = new OutputStream[numStreams];
		info.tempFile = new File[numStreams];

		VideoHandler handler = VideoAggregator.handler;
		String extension = handler.getFileExtension();

		String rootFolder = cc.getPath();
		String submissionId = submission.getId();
		File submissionDir = new File(rootFolder, "submissions" + File.separator + submissionId);

		// setup all the streams we're going to save and the filenames we'll save them to
		int count = 0;
		if (streamsWebcam != null) {
			int numWebcam = streamsWebcam.size();
			for (int i = 0; i < numWebcam; i++) {
				info.stream[count] = streamsWebcam.get(i);
				if (numWebcam > 1)
					info.file[count] = new File(submissionDir, "reaction-" + WEBCAM + (i + 1) + "." + extension);
				else
					info.file[count] = new File(submissionDir, "reaction-" + WEBCAM + "." + extension);
				count++;
			}
		}

		if (streamsDesktop != null) {
			int numDesktop = streamsDesktop.size();
			for (int i = 0; i < numDesktop; i++) {
				info.stream[count] = streamsDesktop.get(i);
				if (numDesktop > 1)
					info.file[count] = new File(submissionDir, "reaction-" + DESKTOP + (i + 1) + "." + extension);
				else
					info.file[count] = new File(submissionDir, "reaction-" + DESKTOP + "." + extension);
				count++;
			}
		}

		FileReferenceList list = new FileReferenceList();
		for (int i = 0; i < numStreams; i++) {
			// create marker file
			info.tempFile[i] = new File(submissionDir, info.file[i].getName() + "-temp");
			boolean secondary = false;
			if (info.tempFile[i].exists()) // another CDS already recording
				secondary = true;

			if (!secondary) {
				try {
					if (!info.tempFile[i].createNewFile()) {
						// warning, couldn't create temp file
						if (info.tempFile[i].exists()) // another CDS already recording
							secondary = true;
					}
				} catch (Exception e) {
					// ignore
					Trace.trace(Trace.ERROR, "Error creating file", e);
				}
			}

			if (!secondary) {
				info.tempFile[i].deleteOnExit();

				try {
					info.out[i] = new BufferedOutputStream(new FileOutputStream(info.file[i]));
				} catch (Exception e) {
					// could not create file
					Trace.trace(Trace.ERROR, "Error creating output stream", e);
					return;
				}

				info.listener[i] = new VideoStreamListener(info.out[i], true);

				VideoStream stream = VideoAggregator.getInstance().getStream(info.stream[i]);
				Trace.trace(Trace.INFO,
						"Recording reaction for " + submissionId + " from " + teamId + " on " + stream.getName());
				try {
					stream.addListener(info.listener[i]);
				} catch (Exception e) {
					Trace.trace(Trace.WARNING, "Could not write header info for reaction " + submissionId);
				}
			}

			VideoStream stream = VideoAggregator.getInstance().getStream(info.stream[0]);
			FileReference ref = new FileReference();
			String name = info.file[i].getName().substring(0, info.file[i].getName().length() - extension.length() - 1);
			ref.href = "contests/" + cc.getId() + "/submissions/" + submissionId + "/" + name;
			ref.mime = stream.getMimeType();
			ref.file = info.file[i];
			list.add(ref);
		}

		((Submission) submission).setReaction(list);

		scheduleEndOfRecording(info, MAX_DURATION);
	}

	public void stopRecording(ConfiguredContest cc, IJudgement judgement) {
		IContest contest = cc.getContest();

		String subId = judgement.getSubmissionId();
		ISubmission submission = contest.getSubmissionById(subId);
		if (submission == null)
			return;

		Info info = submissions.get(subId);
		if (info != null && info.out != null) {
			scheduleEndOfRecording(info, DURATION);
		}
	}

	protected ScheduledFuture<?> scheduleEndOfRecording(Info info, int time) {
		return scheduledExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				if (info.out != null) {
					VideoAggregator aggregator = VideoAggregator.getInstance();
					for (int i = 0; i < info.stream.length; i++) {
						Trace.trace(Trace.INFO, "Reaction recording done: " + aggregator.getStreamName(info.stream[i]));
						aggregator.removeStreamListener(info.stream[i], info.listener[i]);

						try {
							info.out[i].close();
						} catch (Exception e) {
							// ignore
						}
						info.out = null;

						if (info.tempFile[i].exists())
							info.tempFile[i].delete();

						if (info.file[i].exists() && info.file[i].length() == 0) {
							Trace.trace(Trace.WARNING, "No video received for submission: " + info.file[i].toString());
							// TODO - not safe to delete if another CDS has picked it up already
							// info.file[i].delete();
						}
					}
				}
			}
		}, time, TimeUnit.SECONDS);
	}
}