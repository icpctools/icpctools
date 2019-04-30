package org.icpc.tools.cds;

import java.io.PrintWriter;

import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;

// http://feedvalidator.org/
public class RSSWriter {
	protected PrintWriter pw = null;
	private IContest contest = null;

	public RSSWriter(PrintWriter pw, IContest contest) {
		this.pw = pw;
		this.contest = contest;
	}

	public void writePrelude() {
		pw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "<rss version=\"2.0\">\n" + "<channel>\n"
				+ "  <title>" + escape(contest.getName()) + "</title>\n" + "  <link>http://cds/rss</link>\n"
				+ "  <description>RSS feed of successfully solved problems</description>\n");
	}

	public void write(ISubmission r) {
		if (!contest.isSolved(r))
			return;

		ITeam team = contest.getTeamById(r.getTeamId());
		int pInd = contest.getProblemIndex(r.getProblemId());
		IProblem p = contest.getProblems()[pInd];

		String title = team.getName() + " solved problem " + p.getLabel() + "!";
		String description = team.getName() + " solved problem " + p.getLabel() + " (" + p.getName() + ") in "
				+ ContestUtil.getTimeInMin(r.getContestTime()) + " minutes";

		IResult result = contest.getResult(team, pInd);
		int attempts = result.getNumSubmissions();
		if (attempts < 2)
			description += " on the first attempt!";
		else
			description += " after " + attempts + " attempts";

		pw.append("<item>\n" + "  <title>" + escape(title) + "</title>\n" + "  <link>http://cds/rss</link>\n"
				+ "  <description>" + escape(description) + "</description>\n" + "  <guid>http://cds/rss/run" + r.getId()
				+ "</guid>\n" + "</item>\n");
	}

	public void writePostlude() {
		pw.append("</channel>\n</rss>");
	}

	private static String escape(String in) {
		if (in == null)
			return "";
		boolean found = false;
		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			if (c < 31 || c > 126 || "<>\"'\\&".indexOf(c) >= 0) {
				found = true;
			}
		}
		if (!found)
			return in;

		StringBuilder out = new StringBuilder();
		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			if (c < 31 || c > 126 || "<>\"'\\&".indexOf(c) >= 0) {
				out.append("&#" + (int) c + ";");
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}
}