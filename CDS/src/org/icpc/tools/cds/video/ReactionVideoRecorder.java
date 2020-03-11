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
		int stream;
		VideoStreamListener listener;
		OutputStream out;
		File tempFile;
		File file;
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

		final VideoMapper va = VideoMapper.WEBCAM;

		String teamId = submission.getTeamId();
		ITeam team = contest.getTeamById(teamId);
		if (team != null && contest.isTeamHidden(team))
			return;

		// check if already recorded in a previous run
		File reactDir = new File(rootFolder, "submissions" + File.separator + submissionId);
		if (!reactDir.exists())
			reactDir.mkdirs();

		File file = new File(reactDir, "reaction.m2ts");
		if (file.exists()) {
			FileReference ref = new FileReference();
			ref.href = "contests/" + cc.getId() + "/submissions/" + submissionId + "/reaction";
			ref.mime = "application/m2ts";
			ref.file = file;
			// webcamRef.data = "webcam";
			((Submission) submission).setReaction(new FileReferenceList(ref));
			return;
		}
		info.file = file;

		// create marker file
		Trace.trace(Trace.INFO, "Recording reaction for " + submissionId);
		info.tempFile = new File(reactDir, file.getName() + "-temp");
		boolean secondary = false;
		if (info.tempFile.exists()) // another CDS already recording
			secondary = true;

		if (!secondary) {
			try {
				if (!info.tempFile.createNewFile()) {
					// warning, couldn't create temp file
					if (info.tempFile.exists()) // another CDS already recording
						secondary = true;
				}
			} catch (Exception e) {
				// ignore
				Trace.trace(Trace.ERROR, "Error creating file", e);
			}
		}

		if (!secondary) {
			info.tempFile.deleteOnExit();

			try {
				info.out = new BufferedOutputStream(new FileOutputStream(file));
			} catch (Exception e) {
				// could not create file
				Trace.trace(Trace.ERROR, "Error creating output stream", e);
				return;
			}

			info.listener = new VideoStreamListener(info.out, true);
			try {
				info.stream = va.getVideoStream(teamId);
			} catch (Exception e) {
				// invalid team
				return;
			}

			VideoAggregator aggregator = VideoAggregator.getInstance();
			aggregator.addStreamListener(info.stream, info.listener);
		}

		FileReference ref = new FileReference();
		ref.href = "contests/" + cc.getId() + "/submissions/" + submissionId + "/reaction";
		ref.mime = "application/m2ts";
		ref.file = file;
		// webcamRef.data = "webcam";
		((Submission) submission).setReaction(new FileReferenceList(ref));

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
					aggregator.removeStreamListener(info.stream, info.listener);

					try {
						info.out.close();
					} catch (Exception e) {
						// ignore
					}
					info.out = null;

					if (info.tempFile.exists())
						info.tempFile.delete();

					if (info.file.exists() && info.file.length() == 0) {
						Trace.trace(Trace.WARNING, "No video received for submission. Stream: " + info.stream);
						info.file.delete();
					}
				}
			}
		}, time, TimeUnit.SECONDS);
	}

	public static void streamReaction(ConfiguredContest cc, ISubmission submission, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
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

		File reactDir = new File(rootFolder, "video" + File.separator + "reactions");
		File file = new File(reactDir, "reaction" + submission.getId() + ".m2ts");
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

		final File tempFile = new File(reactDir, file.getName() + "-temp");
		if (!tempFile.exists()) {
			long lastModified = file.lastModified() / 1000 * 1000;
			try {
				long ifModifiedSince = request.getDateHeader("If-Modified-Since");
				if (ifModifiedSince != -1 && ifModifiedSince >= lastModified) {
					response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
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