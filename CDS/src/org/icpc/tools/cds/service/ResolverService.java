package org.icpc.tools.cds.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import javax.servlet.http.HttpServletResponse;

import org.icpc.tools.cds.ConfiguredContest;
import org.icpc.tools.cds.presentations.PresentationServer;
import org.icpc.tools.cds.presentations.PresentationServer.IPropertyListener;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.feed.JSONWriter;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.resolver.ResolutionControl;
import org.icpc.tools.contest.model.resolver.ResolutionControl.IResolutionListener;
import org.icpc.tools.contest.model.resolver.ResolutionUtil;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ContestStateStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ResolutionStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.SubmissionSelectionStep;
import org.icpc.tools.contest.model.resolver.ResolverLogic;

public class ResolverService {
	private static final String DATA_RESOLVER_CLICKS = "org.icpc.tools.presentation.contest.resolver.clicks";
	private static final String DATA_RESOLVER_SETTINGS = "org.icpc.tools.presentation.contest.resolver.settings";
	private static final String DATA_RESOLVER_SPEED = "org.icpc.tools.presentation.contest.resolver.speed";

	protected static List<ResolutionStep> steps;
	protected static ResolutionControl control;
	protected static ScheduledExecutorService executor;
	protected static boolean localControl;

	protected static void doGet(HttpServletResponse response, ConfiguredContest cc) throws IOException {
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setContentType("application/json");

		JsonObject obj = new JsonObject();
		if (control != null) {
			obj.put("pause", control.getCurrentPause());
			obj.put("total_pauses", ResolutionUtil.getTotalPauses(steps));
			obj.put("total_time", ResolutionUtil.getTotalTime(steps));
			obj.put("stepping", control.isStepping());
		} else {
			obj.put("pause", -1);
			obj.put("total_pauses", -1);
			obj.put("total_time", -1);
			obj.put("stepping", false);
		}

		JSONWriter pw = new JSONWriter(response.getWriter());
		pw.writeObject(obj);
	}

	protected static void doPut(HttpServletResponse response, String command, ConfiguredContest cc) throws IOException {
		IContest contest = cc.getContest();
		if (contest.getState().isRunning()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Contest still running");
			return;
		}

		Trace.trace(Trace.USER, "Resolver command: " + command);
		try {
			Contest c = (Contest) contest;
			if ("init".equals(command)) {
				if (steps != null)
					return;

				if (executor == null)
					executor = ExecutorListener.getExecutor();

				// The resolver skips over non-solution/non-penalty judgements (e.g. compile error)
				// since these typically 'disappear' from scoreboards during the regular contest.
				// Resolve these first to avoid confusing the presenter
				int count = 0;
				for (IJudgement j : contest.getJudgements()) {
					IJudgementType jt = contest.getJudgementTypeById(j.getJudgementTypeId());
					if (!jt.isSolved() && !jt.isPenalty()) {
						cc.exposeContestObject(j);
						count++;
					}
				}
				Trace.trace(Trace.USER, "Auto-resolved " + count + " judgements");

				ResolverLogic resolver = new ResolverLogic(c, 12, -1, false, null);
				steps = resolver.resolveFrom(false);
				control = new ResolutionControl(steps);
				control.addListener(new IResolutionListener() {
					protected List<IJudgement> judgements;

					@Override
					public void atStep(ResolutionStep step) {
						if (step instanceof SubmissionSelectionStep) {
							SubmissionSelectionStep step2 = (SubmissionSelectionStep) step;
							if (step2.subInfo == null)
								return;

							String teamId = step2.subInfo.getTeam().getId();
							int pInd = step2.subInfo.getProblemIndex();
							String pId = contest.getProblems()[pInd].getId();
							ISubmission[] subs = contest.getSubmissions();
							judgements = new ArrayList<>();
							for (ISubmission s : subs) {
								if (s.getTeamId().equals(teamId) && s.getProblemId().equals(pId)) {
									if (s.getContestTime() < contest.getDuration()) {
										IJudgement[] j = contest.getJudgementsBySubmissionId(s.getId());
										judgements.addAll(Arrays.asList(j));
									}
								}
							}
						} else if (step instanceof ContestStateStep) {
							if (judgements == null)
								return;

							for (IJudgement j : judgements)
								cc.exposeContestObject(j);

							judgements = null;
						}
					}

					@Override
					public void atPause(int pause) {
						Trace.trace(Trace.INFO, "Resolver at pause " + pause);
					}

					@Override
					public void toPause(int pause, boolean includeDelays) {
						// send out changes coming from the local/CDS control
						if (!localControl)
							return;

						localControl = false;

						Trace.trace(Trace.INFO, "Resolver going to pause " + pause);

						PresentationServer ps = PresentationServer.getInstance();
						int[] cl = ps.getAllClientUIDs();
						if (!includeDelays)
							ps.setProperty(cl, DATA_RESOLVER_CLICKS, (pause + 1000) + "");
						else
							ps.setProperty(cl, DATA_RESOLVER_CLICKS, pause + "");
					}
				});

				PresentationServer.getInstance().addListener(new IPropertyListener() {
					@Override
					public void propertyUpdated(int[] clients, String key, String value) {
						Trace.trace(Trace.INFO, "New property: " + key + ": " + value);
						if (DATA_RESOLVER_CLICKS.equals(key)) {
							int pause = Integer.parseInt(value);
							boolean includeDelays = true;
							if (pause > 999) {
								pause -= 1000;
								includeDelays = false;
							} else if (Math.abs(pause - control.getCurrentPause()) > 1)
								includeDelays = false;

							final int pause2 = pause;
							boolean includeDelays2 = includeDelays;
							executor.submit(new Runnable() {
								@Override
								public void run() {
									control.moveToPause(pause2, includeDelays2);
								}
							});
						} else if (DATA_RESOLVER_SPEED.equals(key)) {
							double sf = Double.parseDouble(value);
							control.setSpeedFactor(sf);
						} else if (DATA_RESOLVER_SETTINGS.equals(key)) {
							String[] clientArgs = value.split(",");
							control.setSpeedFactor(Double.parseDouble(clientArgs[0]));
						}
					}
				});
				return;
			}

			if (control == null || control.isStepping())
				return;

			localControl = true;
			executor.submit(new Runnable() {
				@Override
				public void run() {
					if ("fast-forward".equals(command))
						control.forward(false);
					else if ("forward".equals(command))
						control.forward(true);
					else if ("rewind".equals(command))
						control.rewind(true);
					else if ("fast-rewind".equals(command))
						control.rewind(false);
					else if ("reset".equals(command))
						control.reset();
				}
			});
		} catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
			Trace.trace(Trace.ERROR, "Error durng finalization", e);
		}
	}
}