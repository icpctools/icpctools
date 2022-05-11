package org.icpc.tools.contest.model.feed;

import java.io.PrintWriter;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener.Delta;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.Deletion;

public class NDJSONFeedWriter {
	protected PrintWriter pw;
	protected JSONEncoder je;
	protected boolean isOldFeed = "true".equals(System.getProperty("ICPC_OLD_FEED"));

	public NDJSONFeedWriter(PrintWriter pw) {
		this.pw = pw;
		je = new JSONEncoder(pw);
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

	public void writeEvent(IContestObject obj, String token, Delta d) {
		je.open();

		String type = IContestObject.getTypeName(obj.getType());
		je.encode("type", type);

		if (isOldFeed) {
			if (token != null)
				je.encode("id", token);

			if (d == Delta.DELETE) {
				je.encode("op", "delete");
				je.openChild("data");
				je.encode("id", obj.getId());
				je.close();
			} else {
				if (d == Delta.UPDATE)
					je.encode("op", "update");
				else
					je.encode("op", "create");

				je.openChild("data");
				((ContestObject) obj).writeBody(je);
				je.close();
			}
		} else {
			if (!IContestObject.isSingleton(obj.getType()))
				je.encode("id", obj.getId());

			if (obj instanceof Deletion)
				je.encode("data", (String) null);
			else {
				je.openChild("data");
				((ContestObject) obj).writeBody(je);
				je.close();
			}

			if (token != null)
				je.encode("token", token);
		}

		je.close();
		je.reset();
		pw.write("\n");
	}

	public void writeContest(Contest contest) {
		String prefix = getContestPrefix(contest);
		final int[] index = new int[] { 0 };
		contest.addListenerFromStart((contest2, obj, d) -> {
			writeEvent(obj, prefix + index[0]++, d);
		});

		pw.close();
	}

	public void writeHeartbeat() {
		pw.println();
	}
}