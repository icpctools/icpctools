package org.icpc.tools.contest.model.feed;

import java.io.PrintWriter;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener.Delta;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;

public class NDJSONFeedWriter {
	protected PrintWriter pw;
	protected JSONEncoder je;
	protected String prefix = "icpc";

	public NDJSONFeedWriter(PrintWriter pw) {
		this.pw = pw;
		je = new JSONEncoder(pw);
	}

	public NDJSONFeedWriter(PrintWriter pw, IContest contest) {
		this.pw = pw;
		je = new JSONEncoder(pw);
		prefix = getContestPrefix(contest);
	}

	public static String getContestPrefix(IContest contest) {
		// A = 65, a = 97
		int i = contest.hashCode() % 52;
		StringBuilder sb = new StringBuilder("cd");
		if (i < 26)
			sb.appendCodePoint(97 + i);
		else
			sb.appendCodePoint(65 + i - 26);
		return sb.toString();
	}

	public String getContentType() {
		return IContentType.JSON;
	}

	public void writeEvent(IContestObject obj, int index, Delta d) {
		writeEvent(obj, prefix + index, d);
	}

	public void writeEvent(IContestObject obj, String id, Delta d) {
		je.open();

		String type = IContestObject.getTypeName(obj.getType());
		je.encode("type", type);
		je.encode("id", id);

		if (d == Delta.DELETE) {
			je.encode("op", "delete");
			je.encode3("data");
			je.open();
			je.encode("id", obj.getId());
			je.close();
		} else {
			if (d == Delta.UPDATE)
				je.encode("op", "update");
			else
				je.encode("op", "create");

			je.encode3("data");
			((ContestObject) obj).write(je);
		}

		je.close();
		pw.write("\n");
	}

	public void write(IContestObject obj) {
		((ContestObject) obj).write(je);
	}

	public void writeContest(Contest contest) {
		final int[] index = new int[] { 1 };
		contest.addListenerFromStart((contest2, obj, d) -> {
			writeEvent(obj, index[0]++, d);
		});

		pw.close();
	}

	public void writeHeartbeat() {
		pw.println();
	}
}