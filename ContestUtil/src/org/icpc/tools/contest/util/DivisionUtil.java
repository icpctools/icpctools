package org.icpc.tools.contest.util;

import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.RESTContestSource;

public class DivisionUtil {
	protected static String div1;
	protected static String div2;
	protected static List<String> submissions = new ArrayList<>();
	protected static List<String> teamDiv1 = new ArrayList<>();
	protected static List<String> teamDiv2 = new ArrayList<>();

	public static void main(String[] args) {
		Trace.init("ICPC Division Util", "divisionUtil", args);

		if (args == null || args.length != 5) {
			Trace.trace(Trace.ERROR, "Usage: [url] [user] [password] [div1problems] [div2problems]");
			return;
		}

		try {
			String url = args[0];
			String user = args[1];
			String password = args[2];

			RESTContestSource source = new RESTContestSource(url, user, password);
			source.outputValidation();

			div1 = args[3];
			div2 = args[4];
			monitor(source);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error launching", e);
		}
	}

	protected static void monitor(RESTContestSource source) {
		Trace.trace(Trace.USER, "Connecting...");

		source.loadContest(((contest2, obj, d) -> {
			if (obj instanceof IProblem) {
				validateProblem((IProblem) obj);
			} else if (obj instanceof ISubmission) {
				validateSubmission(contest2, (ISubmission) obj);
			}
		}));
	}

	protected static void validateProblem(IProblem p) {
		String pId = p.getId();
		boolean isDiv1 = div1.contains(pId);
		boolean isDiv2 = div2.contains(pId);
		if (!isDiv1 && !isDiv2) {
			Trace.trace(Trace.ERROR, "printlProblem " + pId + " is not in either division!");
			return;
		}
		if (isDiv1 && isDiv2) {
			Trace.trace(Trace.ERROR, "Problem " + pId + " is in both divisions!");
			return;
		}
	}

	protected static void validateSubmission(IContest contest, ISubmission submission) {
		String submissionId = submission.getId();
		if (submissions.contains(submissionId))
			return;

		submissions.add(submissionId);

		IProblem p = contest.getProblemById(submission.getProblemId());
		ITeam team = contest.getTeamById(submission.getTeamId());
		if (team == null) {
			Trace.trace(Trace.ERROR, "Could not find team: " + submission.getTeamId());
			return;
		}
		String teamId = team.getId();

		String pId = p.getId();
		boolean isDiv1 = div1.contains(pId);
		boolean isDiv2 = div2.contains(pId);

		boolean isTeamDiv1 = teamDiv1.contains(teamId);
		boolean isTeamDiv2 = teamDiv2.contains(teamId);

		if (!isTeamDiv1 && !isTeamDiv2) {
			if (isDiv1) {
				isTeamDiv1 = true;
				teamDiv1.add(teamId);
			} else if (isDiv2) {
				isTeamDiv2 = true;
				teamDiv2.add(teamId);
			}
		}
		if ((!isDiv1 && isTeamDiv1) || (isDiv2 && !isTeamDiv2)) {
			Trace.trace(Trace.ERROR, "Team submitted out of division: " + teamId + " - " + team.getName());
			Trace.trace(Trace.ERROR, "   Submission: " + submission.getId() + " " + contest.getStatus(submission).name());
			Trace.trace(Trace.ERROR, "   Problem: " + pId + " - " + p.getName());
		}
	}
}