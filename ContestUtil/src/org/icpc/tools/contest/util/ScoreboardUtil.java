package org.icpc.tools.contest.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.util.ScoreboardData;

public class ScoreboardUtil {
	private static boolean summaryOnly = false;

	protected static ScoreboardData read(String s) throws Exception {
		InputStream is = null;
		try {
			is = new FileInputStream(new File(s));
			return org.icpc.tools.contest.model.util.ScoreboardUtil.read(is);
		} catch (Exception e) {
			try {
				if (is != null)
					is.close();
			} catch (Exception ex2) {
				//
			}
			throw e;
		}
	}

	public static void showHelp() {
		System.out.println("Usage: scoreboardUtil [scoreboard1.json] [scoreboard2.json]");
		System.out.println();
		System.out.println("   Performs a logical comparison of two scoreboards");
	}

	public static void main(String[] args) {
		Trace.init("ICPC Scoreboard Utility", "scoreboardUtil", args);

		if (args == null || args.length != 2) {
			showHelp();
			return;
		}

		try {
			Trace.trace(Trace.USER, "Reading " + args[0]);
			ScoreboardData s1 = read(args[0]);
			Trace.trace(Trace.USER, "Reading " + args[1]);
			ScoreboardData s2 = read(args[1]);
			Trace.trace(Trace.USER, "Comparing...");
			Trace.trace(Trace.USER, "");
			Trace.trace(Trace.USER, org.icpc.tools.contest.model.util.ScoreboardUtil.compare(s1, s2, summaryOnly));
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error comparing scoreboards", e);
		}
	}
}