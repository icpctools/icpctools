package org.icpc.tools.contest.model;

import java.io.PrintWriter;

import org.icpc.tools.contest.model.IContest.ScoreboardType;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.RelativeTime;
import org.icpc.tools.contest.model.feed.Timestamp;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.State;

public class Scoreboard {
	/**
	 * Get the contest immediately after the given event id.
	 */
	public static Contest getScoreboard(Contest contest, int eventIndex) {
		// if the index wasn't specified or is past the current event, output the current scoreboard
		if (eventIndex <= 0 || eventIndex >= contest.getNumObjects())
			return contest;

		return contest.clone(true, new IContestObjectFilter() {
			private int count = 1;

			@Override
			public IContestObject filter(IContestObject obj) {
				if (count >= eventIndex)
					return null;
				count++;
				return obj;
			}
		});
	}

	private static double round(double d) {
		return Math.round(d * 100000.0) / 100000.0;
	}

	/**
	 * Outputs the current/latest scoreboard.
	 */
	public static void writeScoreboard(PrintWriter pw, IContest contest) {
		ITeam[] teams = contest.getOrderedTeams();
		int numProblems = contest.getNumProblems();

		pw.write("{");
		IContestObject obj = contest.getLastTimedObject();
		int index = contest.getNumObjects();
		pw.write(" \"event_id\":\"cds" + index + "\",\n");
		if (obj == null) {
			pw.write("  \"time\":\"" + Timestamp.format(System.currentTimeMillis()) + "\",\n");
			pw.write("  \"contest_time\":\"" + RelativeTime.format(0) + "\",\n");
		} else {
			pw.write("  \"time\":\"" + Timestamp.format(ContestObject.getTime(obj)) + "\",\n");
			pw.write("  \"contest_time\":\"" + RelativeTime.format(ContestObject.getContestTime(obj)) + "\",\n");
		}

		ScoreboardType scoreboardType = contest.getScoreboardType();
		if (scoreboardType != null)
			pw.write("  \"scoreboard_type\":\"" + scoreboardType.name().toLowerCase() + "\",\n");

		if (scoreboardType == null)
			scoreboardType = ScoreboardType.PASS_FAIL;

		pw.write("  \"state\": ");

		JSONEncoder je = new JSONEncoder(pw);
		je.open();
		State state = (State) contest.getState();
		state.writeBody(je);
		je.close();

		pw.write(",\n");

		pw.write("  \"rows\": [\n");
		boolean firstTeam = true;
		for (ITeam team : teams) {
			IStanding s = contest.getStanding(team);

			if (!firstTeam)
				pw.write(",\n");
			else
				firstTeam = false;

			pw.write("{\"rank\":" + s.getRank() + ",");
			pw.write("\"team_id\":\"" + team.getId() + "\",");
			pw.write("\"score\":{");
			if (ScoreboardType.PASS_FAIL.equals(scoreboardType)) {
				pw.write("\"num_solved\":" + s.getNumSolved() + ",");
				pw.write("\"total_time\":" + s.getTime() + "},\n");
			} else if (ScoreboardType.SCORE.equals(scoreboardType)) {
				pw.write("\"score\":" + round(s.getScore()));
				if (s.getLastSolutionTime() >= 0)
					pw.write(",\"time\":" + s.getLastSolutionTime() + "},\n");
				else
					pw.write("},\n");
			}
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
					if (r.getStatus() == Status.SOLVED) {
						pw.write("\"solved\":true,");
						pw.write("\"time\":" + ContestUtil.getTime(r.getContestTime()));
						if (ScoreboardType.PASS_FAIL.equals(scoreboardType)) {
							if (r.isFirstToSolve())
								pw.write(",\"first_to_solve\":true");
						} else if (ScoreboardType.SCORE.equals(scoreboardType))
							pw.write(",\"score\":" + round(r.getScore()));
					} else
						pw.write("\"solved\":false");
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
}