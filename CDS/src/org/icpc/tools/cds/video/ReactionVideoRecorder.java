package org.icpc.tools.cds.video;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.CDSConfig;
import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.util.Role;
import org.icpc.tools.cds.video.VideoStream.StreamType;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.FileReference;
import org.icpc.tools.contest.model.internal.FileReferenceList;
import org.icpc.tools.contest.model.internal.Submission;

public class ReactionVideoRecorder {
	private ScheduledExecutorService scheduledExecutor;
	private static ReactionVideoRecorder instance = new ReactionVideoRecorder();
	private static final int DURATION = 30; // 30 seconds
	private static final int MAX_DURATION = 5 * 60; // 5 minutes

	protected class Info {
		int[] stream;
		VideoStreamListener[] listener;
		OutputStream[] out;
		File[] tempFile;
		File[] file;
		ScheduledFuture<?> future;
	}

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
		String dir = cc.getLocation();
		if (dir == null)
			return;

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
		String rootFolder = cc.getLocation();

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

		File file = new File(submissionDir, "reaction.m2ts");
		if (file.exists()) { // recordings already exist, just return the files
			FileReferenceList list = new FileReferenceList();
			File[] files = submissionDir.listFiles(
					(dir, name) -> (name.toLowerCase().startsWith("reaction") && name.toLowerCase().endsWith(".m2ts")));

			if (files != null) {
				for (File f : files) {
					FileReference ref = new FileReference();
					String name = f.getName().substring(0, f.getName().length() - 5);
					ref.href = "contests/" + cc.getId() + "/submissions/" + submissionId + "/" + name;
					ref.mime = "application/m2ts";
					ref.file = f;
					list.add(ref);
				}
			}

			((Submission) submission).setReaction(list);
			return;
		}

		List<Integer> streams = cc.getStreams(teamId, StreamType.WEBCAM);
		if (streams == null || streams.isEmpty())
			return;

		int numStreams = streams.size();
		info.file = new File[numStreams];
		info.stream = new int[numStreams];
		info.listener = new VideoStreamListener[numStreams];
		info.out = new OutputStream[numStreams];
		info.tempFile = new File[numStreams];

		FileReferenceList list = new FileReferenceList();
		for (int i = 0; i < numStreams; i++) {
			if (i > 0)
				file = new File(submissionDir, "reaction-" + (i + 1) + ".m2ts");
			info.file[i] = file;
			info.stream[i] = streams.get(i);

			// create marker file
			info.tempFile[i] = new File(submissionDir, file.getName() + "-temp");
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
					info.out[i] = new BufferedOutputStream(new FileOutputStream(file));
				} catch (Exception e) {
					// could not create file
					Trace.trace(Trace.ERROR, "Error creating output stream", e);
					return;
				}

				info.listener[i] = new VideoStreamListener(info.out[i], true);

				VideoAggregator aggregator = VideoAggregator.getInstance();
				Trace.trace(Trace.INFO, "Recording reaction for " + submissionId + " from " + teamId + " on "
						+ aggregator.getStreamName(info.stream[i]));
				aggregator.addStreamListener(info.stream[i], info.listener[i]);
			}

			FileReference ref = new FileReference();
			String name = file.getName().substring(0, file.getName().length() - 5);
			ref.href = "contests/" + cc.getId() + "/submissions/" + submissionId + "/" + name;
			ref.mime = "application/m2ts";
			ref.file = file;
			list.add(ref);
		}

		((Submission) submission).setReaction(list);

		info.future = scheduleEndOfRecording(info, MAX_DURATION);
	}

	public void stopRecording(ConfiguredContest cc, IJudgement judgement) {
		IContest contest = cc.getContest();

		String subId = judgement.getSubmissionId();
		ISubmission submission = contest.getSubmissionById(subId);
		if (submission == null)
			return;

		final Info info = submissions.get(subId);
		if (info == null || info.out == null)
			return;

		scheduleEndOfRecording(info, DURATION);
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
							Trace.trace(Trace.WARNING, "No video received for submission. Stream: " + info.stream);
							info.file[i].delete();
						}
					}
				}
			}
		}, time, TimeUnit.SECONDS);
	}

	public static void streamReaction(ConfiguredContest cc, ISubmission submission, HttpServletRequest request,
			HttpServletResponse response) throws IOException { // TODO
		String rootFolder = cc.getLocation();
		if (rootFolder == null) {
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			return;
		}

		if (submission == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// security - reject after freeze
		final IContest contest = cc.getContest();
		if (!Role.isBlue(request) && !contest.isBeforeFreeze(submission)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		File submissionDir = new File(rootFolder, "submissions" + File.separator + submission.getId());
		File file = new File(submissionDir, "reaction" + ".m2ts"); // TODO 3 per team
		if (!file.exists()) {
			if (cc.isTesting())
				file = new File(CDSConfig.getFolder(), "test" + File.separator + "reaction.m2ts");

			// otherwise, fail
			if (!file.exists()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		}

		response.setContentType("application/octet");
		response.setHeader("Content-Disposition", "inline; filename=\"reaction" + submission.getId() + ".m2ts\"");

		final File tempFile = new File(submissionDir, file.getName() + "-temp");
		if (!tempFile.exists()) {
			long lastModified = file.lastModified() / 1000 * 1000;
			try {
				long ifModifiedSince = request.getDateHeader("If-Modified-Since");
				if (ifModifiedSince != -1 && ifModifiedSince >= lastModified) {
					response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					return;
				}
			} catch (Exception e) {
				// ignore, send anyway
			}

			response.setContentLength((int) file.length());
			response.setDateHeader("Last-Modified", lastModified);
		}

		ServletOutputStream out = response.getOutputStream();
		BufferedInputStream bin = new BufferedInputStream(new FileInputStream(file));
		byte[] b = new byte[8096];

		try {
			if (!tempFile.exists()) {
				int n = bin.read(b);
				while (n != -1) {
					out.write(b, 0, n);
					n = bin.read(b);
				}
			} else {
				// still recording - stream the file instead
				long start = System.currentTimeMillis();
				boolean recording = true;
				while (recording) {
					int n = bin.read(b);
					while (n != -1) {
						out.write(b, 0, n);
						out.flush();
						n = bin.read(b);
					}

					try {
						Thread.sleep(400);
					} catch (Exception e) {
						// ignore
					}

					if (!tempFile.exists()) {
						// try one last time
						n = bin.read(b);
						while (n != -1) {
							out.write(b, 0, n);
							n = bin.read(b);
						}
						recording = false;
					}

					// give up after 240s
					if (start + 240000L < System.currentTimeMillis())
						recording = false;
				}
			}
		} finally {
			try {
				bin.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}
}