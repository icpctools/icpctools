package org.icpc.tools.cds.service;

import java.io.PrintWriter;

import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Scoreboard;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.RelativeTime;
import org.icpc.tools.contest.model.feed.Timestamp;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.Judgement;
import org.icpc.tools.contest.model.internal.State;
import org.icpc.tools.contest.model.internal.Submission;

public class ProjectionScoreboardService {
	public static void writeScoreboard(PrintWriter pw, Contest contest2) {
		// figure out which judgement type is the solved one
		IJudgementType solvedJT = null;

		for (IJudgementType type : contest2.getJudgementTypes()) {
			if (type.isSolved())
				solvedJT = type;
		}

		Contest contest = Scoreboard.getScoreboard(contest2, contest2.getNumObjects() - 28000);

		ITeam[] teams = contest.getOrderedTeams();
		int numProblems = contest.getNumProblems();
		int contestTimeMs = contest.getContestTimeOfLastEvent();
		// contestTimeMs -= 12000000;

		pw.write("{");
		IContestObject obj = contest.getLastTimedObject();
		int index = contest.getNumObjects();
		if (obj == null) {
			// do nothing
		} else {
			pw.write(" \"event_id\":\"cds" + index + "\",\n");
			pw.write("  \"time\":\"" + Timestamp.format(ContestObject.getTime(obj)) + "\",\n");
			pw.write("  \"contest_time\":\"" + RelativeTime.format(ContestObject.getContestTime(obj)) + "\",\n");
		}
		pw.write("  \"state\": ");

		JSONEncoder je = new JSONEncoder(pw);
		State state = (State) contest.getState();
		state.writeBody(je);

		pw.write(",\n");

		pw.write("  \"rows\": [\n");
		boolean firstTeam = true;
		for (ITeam team : teams) {
			IStanding s = contest.getStanding(team);

			if (!firstTeam)
				pw.write(",\n");
			else
				firstTeam = false;

			// [{"rank":1,"team_id":"42","score":{"num_solved":3,"total_time":340},
			pw.write("{\"rank\":" + s.getRank() + ",");
			pw.write("\"team_id\":\"" + team.getId() + "\",");
			pw.write("\"score\":{");
			pw.write("\"num_solved\":" + s.getNumSolved() + ",");
			pw.write("\"total_time\":" + s.getTime() + "},\n");

			// "problems":[
			// {"problem_id":"A","num_judged":3,"num_pending":1,"solved":false},
			// {"problem_id":"B","num_judged":1,"num_pending":0,"solved":true,"time":20,"first_to_solve":true}
			// ]},
			pw.write("  \"problems\":[");
			boolean first = true;
			for (int pn = 0; pn < numProblems; pn++) {
				IResult r = contest.getResult(team, pn);
				if (r.getStatus() != Status.UNATTEMPTED) {
					if (first)
						first = false;
					else
						pw.write(",");

					IProblem p = contest.getProblems()[pn];
					pw.write("\n   {\"problem_id\":\"" + escape(p.getId()) + "\",");
					pw.write("\"num_judged\":" + r.getNumJudged() + ",");
					pw.write("\"num_pending\":" + r.getNumPending() + ",");
					pw.write("\"solved\":");
					if (r.getStatus() == Status.SOLVED) {
						pw.write("true,");
						pw.write("\"time\":" + ContestUtil.getTime(r.getContestTime()));
						if (r.isFirstToSolve())
							pw.write(",\"first_to_solve\":true");
					} else {
						pw.write("false,");
						pw.write("\"potential\":[");
						projection(pw, contest, team, p, solvedJT, contestTimeMs);
						pw.write("]");
					}
					pw.write("}");
				}
			}
			if (!first)
				pw.write("\n  ");
			pw.write("]}");
		}
		pw.write("\n]}");
		pw.flush();
	}

	private static void projection(PrintWriter pw, Contest contest, ITeam team, IProblem p, IJudgementType solvedJT,
			int contestTimeMs2) {
		int contestTimeMs = contestTimeMs2;
		if (contestTimeMs >= contest.getDuration())
			return;

		Contest clone = contest.clone(false);
		Submission submission = solveAProblem(clone, team, p, contestTimeMs, solvedJT);

		boolean first = true;
		int count = 0;

		while (contestTimeMs < contest.getDuration() && count < 4) {
			if (!first)
				pw.write(",");
			first = false;
			pw.write("{");

			IStanding ns = clone.getStanding(team);
			pw.write("\"rank\":" + ns.getRank());
			pw.write(",\"total_time\":" + ns.getTime());
			int sf = getSafetyFactor(clone, team, ns, contestTimeMs, contestTimeMs2);
			if (sf >= 0)
				pw.write(",\"within_time\":" + sf);

			pw.write("}");

			if (sf < 0)
				return;

			contestTimeMs += (sf + 1) * 1000 * 60;
			if (contestTimeMs > contest.getDuration())
				return;
			submission.add("contest_time", RelativeTime.format(contestTimeMs));
			clone.finalizeResults();
			count++;
		}
	}

	private static String escape(String s) {
		if (s == null)
			return "";
		boolean found = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ("\"\\".indexOf(c) >= 0)
				found = true;
		}
		if (!found)
			return s;

		StringBuilder out = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ("\"\\".indexOf(c) >= 0)
				out.append("\\");

			out.append(c);
		}
		return out.toString();
	}

	private static int getSafetyFactor(Contest contest, ITeam team, IStanding s, int contestTimeMs, int contestTimeMs2) {
		int i = contest.getOrderOf(team);
		int[] order = contest.getOrder();
		if (i == order.length - 1)
			return -1;

		IStanding nextS = contest.getStanding(order[i + 1]);
		if (nextS.getNumSolved() == s.getNumSolved())
			return Math.min((contestTimeMs - contestTimeMs2) + (nextS.getTime() - s.getTime()),
					(contest.getDuration() - contestTimeMs2) / 60000);

		return (contest.getDuration() - contestTimeMs2) / 60000;
	}

	private static Submission solveAProblem(Contest contest, ITeam team, IProblem problem, int contestTimeMs,
			IJudgementType solvedJT) {
		String id = "projection";
		Submission submission = new Submission();
		submission.add("id", id);
		submission.add("team_id", team.getId());
		submission.add("problem_id", problem.getId());
		submission.add("contest_time", RelativeTime.format(contestTimeMs));
		contest.add(submission);

		contest.add(new Judgement(submission.getId(), submission, solvedJT.getId()));
		return submission;
	}
}