package org.icpc.tools.contest.model.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.IState;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.feed.RelativeTime;
import org.icpc.tools.contest.model.feed.Timestamp;
import org.icpc.tools.contest.model.util.ScoreboardData.SProblem;
import org.icpc.tools.contest.model.util.ScoreboardData.STeam;

public class ScoreboardUtil {
	private static class Row {
		public String row;
		public String problem;
		public String message;

		public Row(String row, String problem, String message) {
			this.row = row;
			this.problem = problem;
			this.message = message;
		}

		@Override
		public String toString() {
			return row + " / " + problem + " / " + message;
		}
	}

	public static class CompareCount {
		private List<Row> messages = new ArrayList<>();
		private int numTeams;
		private int numProblems;
		private int different;

		public boolean isOk() {
			return different == 0;
		}

		protected void add(String row, String message) {
			messages.add(new Row(row, null, message));
		}

		protected void add(String row, String problem, String message) {
			messages.add(new Row(row, problem, message));
		}

		public String printSummary(boolean includeDetails) {
			StringBuilder sb = new StringBuilder();
			if (numTeams + numProblems == 0)
				sb.append("No teams or mismatch in number of teams. ");
			else
				sb.append(numTeams + " teams and " + numProblems + " problems compared. ");

			if (isOk())
				return sb.append("No differences found.").toString();

			if (different == 1)
				sb.append(different + " difference found");
			else
				sb.append(different + " differences found");

			if (!includeDetails)
				return sb.append(".").toString();

			sb.append(":");

			String lastRow = null;
			String lastProblem = null;
			for (Row r : messages) {
				sb.append("\n");
				String row = "Team row " + r.row;
				if ("header".equalsIgnoreCase(r.row) || "state".equalsIgnoreCase(r.row))
					row = r.row;
				if (!row.equals(lastRow)) {
					sb.append("  " + row + "\n");
					lastRow = row;
					lastProblem = null;
				}
				String m = r.message;
				if (r.problem != null) {
					m = "  " + m;
					if (!r.problem.equals(lastProblem)) {
						sb.append("    Problem " + r.problem + "\n");
						lastProblem = r.problem;
					}
				}
				sb.append("    " + m);
			}
			return sb.toString();
		}

		public String printSummaryHTML(boolean includeDetails) {
			StringBuilder sb = new StringBuilder();
			if (numTeams + numProblems == 0)
				sb.append("No teams or mismatch in number of teams.<p/>");
			else
				sb.append(numTeams + " teams and " + numProblems + " problems compared.<p/>");

			if (isOk())
				return sb.append("No differences found.").toString();

			if (different == 1)
				sb.append(different + " difference found");
			else
				sb.append(different + " differences found");

			if (!includeDetails)
				return sb.append(".").toString();

			sb.append(":<p/>\n");

			String lastRow = null;
			String lastProblem = null;
			for (Row r : messages) {
				sb.append("\n");
				String row = "Team row " + r.row;
				if ("header".equalsIgnoreCase(r.row) || "state".equalsIgnoreCase(r.row))
					row = r.row;
				if (!row.equals(lastRow)) {
					sb.append("  " + row + "<p/>\n");
					lastRow = row;
					lastProblem = null;
				}
				String m = r.message;
				if (r.problem != null) {
					m = " - " + m;
					if (!r.problem.equals(lastProblem)) {
						sb.append("Problem " + r.problem);
						lastProblem = r.problem;
					}
				}
				sb.append("&nbsp;&nbsp;&nbsp;" + m + "<p/>");
			}
			return sb.toString();
		}
	}

	private static void match(CompareCount cc, String row, String problem, String field, String a, String b) {
		cc.add(row, problem, "Different " + field + " (" + a + " vs " + b + ")");
	}

	private static void match(CompareCount cc, String row, String field, String a, String b) {
		cc.add(row, "Different " + field + " (" + a + " vs " + b + ")");
	}

	private static void match(CompareCount cc, String row, String field, int a, int b) {
		cc.add(row, "Different " + field + " (" + a + " vs " + b + ")");
	}

	private static void match(CompareCount cc, String row, String problem, String field, int a, int b) {
		cc.add(row, problem, "Different " + field + " (" + a + " vs " + b + ")");
	}

	private static void compareScoreboardProblem(CompareCount cc, String row, SProblem p1, SProblem p2) {
		boolean diff = false;
		String problem = p1.problemId;
		cc.numProblems++;
		if (!p1.problemId.equals(p2.problemId)) {
			match(cc, row, problem, "label", p1.problemId, p2.problemId);
			diff = true;
		}
		if (p1.numJudged != p2.numJudged) {
			match(cc, row, problem, "num_judged", p1.numJudged, p2.numJudged);
			diff = true;
		}
		if (p1.numPending != p2.numPending) {
			match(cc, row, problem, "num_pending", p1.numPending, p2.numPending);
			diff = true;
		}
		if (p1.solved != p2.solved) {
			match(cc, row, problem, "solved", p1.solved + "", p2.solved + "");
			diff = true;
		}
		if (p1.time != p2.time) {
			match(cc, row, problem, "total_time", p1.time, p2.time);
			diff = true;
		}
		if (p1.firstToSolve != p2.firstToSolve) {
			match(cc, row, problem, "first_to_solve", p1.firstToSolve + "", p2.firstToSolve + "");
			diff = true;
		}

		if (diff)
			cc.different++;
	}

	private static void compareScoreboardTeam(CompareCount cc, String row, STeam t1, STeam t2) {
		boolean diff = false;
		cc.numTeams++;
		if (!t1.rank.equals(t2.rank)) {
			match(cc, row, "rank", t1.rank, t2.rank);
			diff = true;
		}
		if (!t1.teamId.equals(t2.teamId)) {
			match(cc, row, "team", t1.teamId, t2.teamId);
			diff = true;
		}
		if (t1.numSolved != t2.numSolved) {
			match(cc, row, "num_solved", t1.numSolved, t2.numSolved);
			diff = true;
		}
		if (t1.totalTime != t2.totalTime) {
			match(cc, row, "total_time", t1.totalTime, t2.totalTime);
			diff = true;
		}

		if (t1.problems == null || t2.problems == null) {
			match(cc, row, "length", "size", "0?", "0?");
			cc.different++;
		} else if (t1.problems.length != t2.problems.length) {
			match(cc, row, "length", "size", t1.problems.length, t2.problems.length);
			cc.different++;
		} else {
			for (int j = 0; j < t1.problems.length; j++) {
				SProblem prob = t2.problems[j];
				for (int k = 0; k < t1.problems.length; k++) {
					if (t1.problems[j].problemId != null && t1.problems[j].problemId.equals(t2.problems[k].problemId))
						prob = t2.problems[k];
				}
				compareScoreboardProblem(cc, row, t1.problems[j], prob);
			}
		}

		if (diff)
			cc.different++;
	}

	private static void removeEmptyRows(ScoreboardData data) {
		for (STeam team : data.teams) {
			boolean foundEmptyRows = false;
			for (SProblem problem : team.problems) {
				if (problem.numJudged == 0 && problem.numPending == 0)
					foundEmptyRows = true;
			}
			if (foundEmptyRows) {
				List<SProblem> list = new ArrayList<>();
				for (SProblem problem : team.problems) {
					if (problem.numJudged != 0 || problem.numPending != 0)
						list.add(problem);
				}
				team.problems = list.toArray(new SProblem[0]);
			}
		}
	}

	public static CompareCount compare(ScoreboardData a, ScoreboardData b) {
		CompareCount cc = new CompareCount();

		removeEmptyRows(a);
		removeEmptyRows(b);

		// compare headers
		if ((a.eventId == null && b.eventId != null) || (a.eventId != null && !a.eventId.equals(b.eventId))) {
			match(cc, "Header", "event_id", a.eventId, b.eventId);
			cc.different++;
		}
		if ((a.time == null && b.time != null) || (a.time != null && !a.time.equals(b.time))) {
			match(cc, "Header", "time", Timestamp.format(a.time), Timestamp.format(b.time));
			cc.different++;
		}
		if ((a.contestTime == null && b.contestTime != null)
				|| (a.contestTime != null && !a.contestTime.equals(b.contestTime))) {
			match(cc, "Header", "contest_time", RelativeTime.format(a.contestTime), RelativeTime.format(b.contestTime));
			cc.different++;
		}
		if ((a.state == null && b.state != null) || (a.state != null && b.state == null)) {
			match(cc, "Header", "state", a.state + "", b.state + "");
			cc.different++;
		} else if (a.state != null && b.state != null)
			compareState(cc, a.state, b.state);

		if (a.teams.length != b.teams.length) {
			match(cc, "length", "teams", a.teams.length, b.teams.length);
			cc.different++;
		} else {
			for (int i = 0; i < a.teams.length; i++) {
				compareScoreboardTeam(cc, (i + 1) + "", a.teams[i], getTeam(cc, b, i, a.teams[i].teamId));
			}
		}

		return cc;
	}

	private static void compareState(CompareCount cc, IState a, IState b) {
		if ((a.getStarted() == null && b.getStarted() != null)
				|| (a.getStarted() != null && !a.getStarted().equals(b.getStarted()))) {
			match(cc, "State", "started", Timestamp.format(a.getStarted()), Timestamp.format(b.getStarted()));
			cc.different++;
		}
		if ((a.getFrozen() == null && b.getFrozen() != null)
				|| (a.getFrozen() != null && !a.getFrozen().equals(b.getFrozen()))) {
			match(cc, "State", "frozen", Timestamp.format(a.getFrozen()), Timestamp.format(b.getFrozen()));
			cc.different++;
		}
		if ((a.getThawed() == null && b.getThawed() != null)
				|| (a.getThawed() != null && !a.getThawed().equals(b.getThawed()))) {
			match(cc, "State", "thawed", Timestamp.format(a.getThawed()), Timestamp.format(b.getThawed()));
			cc.different++;
		}
		if ((a.getEnded() == null && b.getEnded() != null)
				|| (a.getEnded() != null && !a.getEnded().equals(b.getEnded()))) {
			match(cc, "State", "ended", Timestamp.format(a.getEnded()), Timestamp.format(b.getEnded()));
			cc.different++;
		}
		if ((a.getFinalized() == null && b.getFinalized() != null)
				|| (a.getFinalized() != null && !a.getFinalized().equals(b.getFinalized()))) {
			match(cc, "State", "finalized", Timestamp.format(a.getFinalized()), Timestamp.format(b.getFinalized()));
			cc.different++;
		}
		if ((a.getEndOfUpdates() == null && b.getEndOfUpdates() != null)
				|| (a.getEndOfUpdates() != null && !a.getEndOfUpdates().equals(b.getEndOfUpdates()))) {
			match(cc, "State", "end_of_updates", Timestamp.format(a.getEndOfUpdates()),
					Timestamp.format(b.getEndOfUpdates()));
			cc.different++;
		}
	}

	public static String compare(ScoreboardData s1, ScoreboardData s2, boolean summaryOnly) {
		CompareCount cc = compare(s1, s2);
		return cc.printSummary(!summaryOnly);
	}

	public static String compareHTML(ScoreboardData a, ScoreboardData b) {
		CompareCount cc = compare(a, b);
		return cc.printSummaryHTML(true);
	}

	protected static STeam getTeam(CompareCount cc, ScoreboardData s, int row, String teamId) {
		STeam t = s.teams[row];
		if (t.teamId.equals(teamId))
			return t;

		for (int i = 0; i < s.teams.length; i++) {
			if (s.teams[i].teamId.equals(teamId)) {
				match(cc, row + "", "position for team " + teamId, "row " + row, "row " + i);
				cc.different++;
				return s.teams[i];
			}
		}

		return t;
	}

	public static ScoreboardData read(InputStream is) throws IOException {
		try {
			ScoreboardData sc = new ScoreboardData();
			// is.mark(1000);
			JSONParser parser = new JSONParser(is);
			JsonObject obj = null;
			Object[] teams = null;
			try {
				obj = parser.readObject();

				teams = obj.getArray("rows");
				sc.eventId = obj.getString("event_id");
				sc.time = Timestamp.parse(obj.getString("time"));
				sc.contestTime = RelativeTime.parse(obj.getString("contest_time"));
				JsonObject stateObj = obj.getJsonObject("state");
				ScoreboardData.SState state = new ScoreboardData.SState();
				state.started = Timestamp.parse(stateObj.getString("started"));
				state.ended = Timestamp.parse(stateObj.getString("ended"));
				state.frozen = Timestamp.parse(stateObj.getString("frozen"));
				state.thawed = Timestamp.parse(stateObj.getString("thawed"));
				state.finalized = Timestamp.parse(stateObj.getString("finalized"));
				state.endOfUpdates = Timestamp.parse(stateObj.getString("end_of_updates"));
				sc.state = state;
			} catch (IllegalArgumentException e) {
				teams = parser.readArray();
			}

			List<STeam> teamList = new ArrayList<>();
			for (Object t : teams) {
				STeam st = new STeam();
				JsonObject team = (JsonObject) t;
				st.rank = team.getString("rank");
				st.teamId = team.getString("team_id");
				if (st.teamId == null)
					st.teamId = team.getString("team");
				JsonObject score = team.getJsonObject("score");
				st.numSolved = score.getInt("num_solved");
				st.totalTime = score.getInt("total_time");
				teamList.add(st);

				List<SProblem> problemList = new ArrayList<>();
				Object[] problems = (Object[]) team.get("problems");
				for (Object prob : problems) {
					JsonObject problem = (JsonObject) prob;
					SProblem p = new SProblem();
					p.problemId = problem.getString("problem_id");
					if (p.problemId == null)
						p.problemId = problem.getString("label");
					p.numJudged = problem.getInt("num_judged");
					p.numPending = problem.getInt("num_pending");
					p.solved = problem.getBoolean("solved");
					if (p.solved) {
						p.time = problem.getInt("time");
						if (problem.containsKey("first_to_solve"))
							p.firstToSolve = problem.getBoolean("first_to_solve");
					}
					problemList.add(p);
				}
				st.problems = problemList.toArray(new SProblem[0]);
			}

			sc.teams = teamList.toArray(new STeam[0]);
			return sc;
		} catch (ParseException e) {
			throw new IOException("Could not parse: " + e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
}