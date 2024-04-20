package org.icpc.tools.contest.model.feed;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.internal.Contest;

public class ContestWriter {
	public static void write(IContest contest, File folder, boolean force) {
		if (!folder.exists())
			folder.mkdirs();

		IContestObject.ContestType[] types = IContestObject.ContestType.values();

		for (int i = 0; i < types.length; i++) {
			IContestObject.ContestType type = types[i];

			String name = IContestObject.getTypeName(type);
			IContestObject[] objs = ((Contest) contest).getObjects(type);
			if (objs != null && (force || objs.length > 0)) {
				// do not write empty singletons
				if (!force && IContestObject.isSingleton(type) && objs[0].getProperties().isEmpty())
					continue;
				try {
					File f = new File(folder, name + ".json");
					BufferedWriter bw = new BufferedWriter(new FileWriter(f));
					PrintWriter pw = new PrintWriter(bw);
					JSONArrayWriter jw = new JSONArrayWriter(pw);
					if (IContestObject.isSingleton(type)) {
						jw.write(objs[0]);
					} else {
						jw.writePrelude();

						boolean first = true;
						for (IContestObject co : objs) {
							if (!first)
								jw.writeSeparator();
							else
								first = false;

							jw.write(co);
						}

						jw.writePostlude();
					}
					pw.close();
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Could not write contest: " + e.getMessage(), e);
				}
			}
		}
	}
}