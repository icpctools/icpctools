package org.icpc.tools.contest.model.feed;

import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Locale;

import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IClarification;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IDelete;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.ILanguage;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IRun;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.Info;

public class XMLFeedWriter {
	private static final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);

	public static class Finalized extends ContestObject {
		public int[] num;

		@Override
		public ContestType getType() {
			return null;
		}
	}

	static {
		nf.setGroupingUsed(false);
		nf.setMinimumFractionDigits(3);
		nf.setMaximumFractionDigits(3);
	}

	private PrintWriter pw;
	private IContest contest;

	public XMLFeedWriter(PrintWriter pw, IContest contest) {
		this.pw = pw;
		this.contest = contest;
		pw.write("<contest>\n");
	}

	public void close() {
		pw.write("</contest>");
		pw.close();
	}

	private static String formatDuration(int duration2) {
		int duration = duration2 / 1000;
		if (duration < 0 || duration == 0)
			return "00:00:00";

		StringBuilder sb = new StringBuilder();
		int hours = (int) Math.floor(duration / 3600);
		if (hours < 9)
			sb.append('0');
		sb.append(hours);
		sb.append(':');

		int mins = (int) Math.floor(duration / 60) % 60;
		if (mins < 9)
			sb.append('0');
		sb.append(mins);
		sb.append(':');

		int secs = duration % 60;
		if (secs < 9)
			sb.append('0');
		sb.append(secs);
		return sb.toString();
	}

	public void write(IContestObject obj) {
		if (obj == null || obj instanceof IDelete) // XML event feed does not support deletes
			return;

		if (obj instanceof Info) {
			Info info = (Info) obj;
			writeStart(XMLFeedParser.INFO);
			write("contest-id", info.getId());
			write("title", info.getActualFormalName());
			write("short-title", info.getName());
			write("length", formatDuration(info.getDuration()));
			write("scoreboard-freeze-length", formatDuration(info.getFreezeDuration()));
			if (info.getPenaltyTime() >= 0)
				write("penalty", info.getPenaltyTime() + "");

			Boolean started = Boolean.FALSE;
			Long startTime = info.getStartTime();
			if (startTime != null && startTime > 0 && startTime.longValue() < System.currentTimeMillis())
				started = Boolean.TRUE;
			write("started", started.toString());
			if (startTime == null || startTime < 0)
				write("starttime", "undefined");
			else
				write("starttime", nf.format(startTime / 1000.0));
			writeEnd(XMLFeedParser.INFO);

			/*if (info.isFinal()) {
				writeStart(XMLContestParser.FINALIZED);
				write("contest-id", info.getId());
				write("title", info.getFormalName());
				writeEnd(XMLContestParser.FINALIZED);
			}*/
		} else if (obj instanceof ITeam) {
			ITeam team = (ITeam) obj;
			writeStart(XMLFeedParser.TEAM);
			IOrganization org = contest.getOrganizationById(team.getOrganizationId());
			IGroup[] groups = contest.getGroupsByIds(team.getGroupIds());
			write("id", team.getId());
			if (org != null) {
				write("name", team.getName());
				if (org.getCountry() != null)
					write("nationality", org.getCountry());
				write("university", org.getActualFormalName());
				write("university-short-name", org.getName());
			}
			if (groups != null && groups.length > 0 && groups[0] != null)
				write("region", groups[0].getName());
			if (team.getICPCId() != null)
				write("external-id", team.getICPCId());
			writeEnd(XMLFeedParser.TEAM);
		} else if (obj instanceof IGroup) {
			IGroup group = (IGroup) obj;
			writeStart(XMLFeedParser.REGION);
			if (group.getICPCId() != null)
				write("external-id", group.getICPCId());
			write("name", group.getName());
			writeEnd(XMLFeedParser.REGION);
		} else if (obj instanceof ILanguage) {
			ILanguage language = (ILanguage) obj;
			writeStart(XMLFeedParser.LANGUAGE);
			write("id", language.getId());
			write("name", language.getName());
			writeEnd(XMLFeedParser.LANGUAGE);
		} else if (obj instanceof IJudgementType) {
			IJudgementType jt = (IJudgementType) obj;
			writeStart(XMLFeedParser.JUDGEMENT);
			write("acronym", jt.getId());
			write("name", jt.getName());
			writeEnd(XMLFeedParser.JUDGEMENT);
		} else if (obj instanceof IProblem) {
			IProblem problem = (IProblem) obj;
			writeStart(XMLFeedParser.PROBLEM);
			write("id", (problem.getOrdinal() + 1) + "");
			write("label", problem.getLabel());
			write("name", problem.getName());
			if (problem.getColor() != null)
				write("color", problem.getColor());
			if (problem.getRGB() != null)
				write("rgb", problem.getRGB());
			writeEnd(XMLFeedParser.PROBLEM);
		} else if (obj instanceof ISubmission || obj instanceof IJudgement) {
			ISubmission sub = null;
			IJudgement sj = null;
			if (obj instanceof ISubmission) {
				sub = (ISubmission) obj;
			} else {
				sj = (IJudgement) obj;
				sub = contest.getSubmissionById(sj.getSubmissionId());
				if (sub == null || sj.getJudgementTypeId() == null) // don't output event when
																						// judgement starts or if missing
																						// submission
					return;
			}

			writeStart(XMLFeedParser.RUN);
			write("id", sub.getId());
			ILanguage language = contest.getLanguageById(sub.getLanguageId());
			if (language != null)
				write("language", language.getName());
			IProblem problem = contest.getProblemById(sub.getProblemId());
			if (problem != null)
				write("problem", (problem.getOrdinal() + 1) + "");
			else
				write("problem", sub.getProblemId());
			write("team", sub.getTeamId());
			String timestamp = nf.format(sub.getTime() / 1000.0);
			if (sj != null && sj.getJudgementTypeId() != null) {
				IJudgementType jt = contest.getJudgementTypeById(sj.getJudgementTypeId());
				write("judged", "true");
				if (jt != null) {
					write("result", jt.getId());
					write("solved", jt.isSolved() + "");
					write("penalty", jt.isPenalty() + "");
				}
				if (sj.getEndTime() != null)
					timestamp = nf.format(sj.getEndTime() / 1000.0);
			} else
				write("judged", "false");
			write("time", nf.format(sub.getContestTime() / 1000.0));
			write("timestamp", timestamp);
			writeEnd(XMLFeedParser.RUN);
		} else if (obj instanceof IRun) {
			IRun run = (IRun) obj;
			writeStart(XMLFeedParser.TESTCASE);
			write("i", run.getOrdinal() + "");
			IJudgement j = contest.getJudgementById(run.getJudgementId());
			if (j != null) {
				ISubmission s = contest.getSubmissionById(j.getSubmissionId());
				if (s != null) {
					IProblem p = contest.getProblemById(s.getProblemId());
					if (p != null)
						write("n", p.getTestDataCount() + "");
					write("run-id", s.getId());
				}
			}
			IJudgementType jt = contest.getJudgementTypeById(run.getJudgementTypeId());
			if (jt != null) {
				write("judged", "true");
				write("solved", Boolean.toString(jt.isSolved()));
			} else
				write("judged", "false");
			write("judgement", run.getJudgementTypeId());
			write("time", nf.format(run.getContestTime() / 1000.0));
			write("timestamp", nf.format(run.getTime() / 1000.0));
			writeEnd(XMLFeedParser.TESTCASE);
		} else if (obj instanceof IClarification) {
			IClarification clar = (IClarification) obj;
			writeStart(XMLFeedParser.CLAR);
			boolean toAll = clar.getFromTeamId() == null && clar.getToTeamId() == null;
			if (clar.getReplyToId() != null) {
				IClarification origClar = contest.getClarificationById(clar.getReplyToId());
				write("id", origClar.getId());
				write("problem", origClar.getProblemId());
				write("team", origClar.getFromTeamId());
				write("question", origClar.getText());
				write("answer", clar.getText());
				write("answered", "true");
				write("to-all", Boolean.toString(toAll));
			} else {
				write("id", clar.getId());
				write("problem", clar.getProblemId());
				write("team", clar.getFromTeamId());
				write("question", clar.getText());
				write("answered", "false");
				write("to-all", "false");
			}

			write("time", nf.format(clar.getContestTime() / 1000.0));
			write("timestamp", nf.format(clar.getTime() / 1000.0));
			writeEnd(XMLFeedParser.CLAR);
		} else if (obj instanceof IAward) {
			IAward a = (IAward) obj;
			writeStart(XMLFeedParser.AWARD);
			write("id", a.getId());
			write("citation", a.getCitation());
			if (a.showAward())
				write("show", "true");
			else
				write("show", "false");
			if (a.getTeamIds() != null)
				for (String t : a.getTeamIds())
					write("teamId", t);
			writeEnd(XMLFeedParser.AWARD);
		} else if (obj instanceof Finalized) {
			Finalized f = (Finalized) obj;
			writeStart(XMLFeedParser.FINALIZED);
			write("last-gold", f.num[0] + "");
			write("last-silver", f.num[0] + f.num[1] + "");
			write("last-bronze", f.num[0] + f.num[1] + f.num[2] + "");
			writeEnd(XMLFeedParser.FINALIZED);
		}
	}

	private void write(String name, String value) {
		pw.write("  <" + name + ">");
		pw.write(escape(value));
		pw.write("</" + name + ">\n");
	}

	private void writeStart(String type) {
		pw.write("<" + type + ">\n");
	}

	private void writeEnd(String type) {
		pw.write("</" + type + ">\n");
	}

	private static String escape(String in) {
		if (in == null)
			return "";

		boolean found = false;
		StringBuilder out = new StringBuilder();
		int length = in.length();
		for (int offset = 0; offset < length;) {
			int codepoint = in.codePointAt(offset);
			// if (codepoint < 31 || codepoint > 126 || "<>\"'\\&".indexOf(codepoint) >= 0) {
			if (codepoint < 31 || "<>\"'\\&".indexOf(codepoint) >= 0) {
				out.append("&#" + codepoint + ";");
				found = true;
			} else
				out.append(Character.toChars(codepoint));

			offset += Character.charCount(codepoint);
		}

		if (!found)
			return in;

		return out.toString();
	}

	public void writeContest() {
		for (IContestObject obj : ((Contest) contest).getObjects())
			write(obj);
		close();
	}
}