package org.icpc.tools.resolver.awards;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.Scoreboard;
import org.icpc.tools.contest.model.feed.EventFeedContestSource;
import org.icpc.tools.contest.model.feed.NDJSONFeedWriter;
import org.icpc.tools.contest.model.feed.XMLFeedWriter;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.ArgumentParser;
import org.icpc.tools.contest.model.util.ArgumentParser.OptionParser;
import org.icpc.tools.contest.model.util.AwardUtil;

public class Awards {
	protected static Contest loadEventFeed(File file, IContestListener listener) {
		if (!file.exists()) {
			Trace.trace(Trace.ERROR, "Event feed could not be found: " + file.getAbsolutePath());
			System.exit(2);
		}

		Trace.trace(Trace.INFO, "Loading event feed: " + file.getAbsolutePath());
		try {
			return EventFeedContestSource.loadContest(file, listener);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not parse event feed: " + file);
			System.exit(2);
			return null;
		}
	}

	/**
	 * Check that the contest is valid and has no unjugded submissions.
	 */
	private static void validateContest(Contest contest) {
		if (contest.validate() != null)
			Trace.trace(Trace.WARNING, "Contest is not valid. Run \"eventFeed --validate\" for details");

		// check for unjudged submissions
		ISubmission[] submissions = contest.getSubmissions();
		List<ISubmission> unjudgedSubmissions = new ArrayList<>();
		for (ISubmission s : submissions) {
			if (!contest.isJudged(s))
				unjudgedSubmissions.add(s);
		}

		if (!unjudgedSubmissions.isEmpty()) {
			int size = unjudgedSubmissions.size();
			Trace.trace(Trace.ERROR, "Contest event feed has " + size + " unjudged submissions, cannot run");

			int count = 0;
			StringBuilder sb = new StringBuilder("Submission ids: ");
			for (ISubmission r : unjudgedSubmissions) {
				if (count > 10) {
					sb.append(", ...");
					break;
				} else if (count > 0)
					sb.append(", ");
				sb.append(r.getId());
				count++;
			}
			Trace.trace(Trace.ERROR, sb.toString());
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		Trace.init("ICPC Award Utility", "awards", args);

		if (args == null || args.length == 0) {
			org.icpc.tools.resolver.awards.AwardUI.main(args);
			return;
		}

		if (args.length == 1) {
			showHelp();
			System.exit(0);
			return;
		}

		String ef = args[0];
		Trace.trace(Trace.USER, "Reading event feed: " + ef);
		Contest contest = loadEventFeed(new File(ef), null);
		validateContest(contest);
		boolean[] changed = new boolean[1];
		ArgumentParser.parse(args, new OptionParser() {
			@Override
			public boolean setOption(String option, List<Object> options) throws IllegalArgumentException {
				switch (option) {
					case "--firstplacecitation": {
						ArgumentParser.expectOptions(option, options, "text:string");
						Trace.trace(Trace.USER, "Assigning first place awards.");
						AwardUtil.createFirstPlaceAward(contest, (String) options.get(0));
						changed[0] = true;
						break;
					}
					case "--rank": {
						ArgumentParser.expectOptions(option, options, "num:int");
						int num = (int) options.get(0);
						if (num < 1)
							throw new IllegalArgumentException("Invalid number of rank awards");

						Trace.trace(Trace.USER, "Assigning rank awards.");
						AwardUtil.createRankAwards(contest, num);
						changed[0] = true;
						break;
					}
					case "--group": {
						ArgumentParser.expectOptions(option, options, "num:int");
						int num = (int) options.get(0);
						if (num < 1 || num > 200)
							throw new IllegalArgumentException("Invalid number of group awards");

						Trace.trace(Trace.USER, "Assigning group awards.");
						AwardUtil.createGroupAwards(contest, num);
						changed[0] = true;
						break;
					}
					case "--medals": {
						ArgumentParser.expectOptions(option, options, "numGold:int", "numSilver:int", "numBronze:int");
						Trace.trace(Trace.USER, "Assigning medal awards.");
						AwardUtil.createMedalAwards(contest, (int) options.get(0), (int) options.get(1),
								(int) options.get(2));
						changed[0] = true;
						break;
					}
					case "--fts": {
						ArgumentParser.expectOptions(option, options, "beforeFreeze:boolean", "afterFreeze:boolean");
						boolean showBeforeFreeze = (boolean) options.get(0);
						boolean showAfterFreeze = (boolean) options.get(1);
						Trace.trace(Trace.USER, "Assigning first to solve awards.");
						AwardUtil.createFirstToSolveAwards(contest, showBeforeFreeze, showAfterFreeze);
						changed[0] = true;
						break;
					}
					case "--list": {
						ArgumentParser.expectNoOptions(option, options);
						IAward[] awards = contest.getAwards();
						if (awards == null || awards.length == 0) {
							Trace.trace(Trace.USER, "No awards found in feed, showing default awards");
							contest.finalizeResults();
							AwardUtil.createDefaultAwards(contest);
							awards = contest.getAwards();
						} else
							Trace.trace(Trace.USER, "Awards found in feed");

						// create the official scoreboard
						contest.officialResults();

						AwardUtil.printAwards(contest);
						break;
					}
					case "--scoreboard": {
						ArgumentParser.expectOptions(option, options, "scoreboard.json:string");
						generateScoreboard((String) options.get(0), contest);
						break;
					}
					default: {
						return false;
					}
				}
				return true;
			}

			@Override
			public void showHelp() {
				Awards.showHelp();
			}
		});

		if (changed[0]) {
			try {
				if (ef.endsWith(".xml"))
					ef = ef.substring(0, ef.length() - 4);
				else if (ef.endsWith(".json"))
					ef = ef.substring(0, ef.length() - 5);
				ef = ef + "-awards.json";
				File f = new File(ef);
				if (!f.exists() || promptToOverwrite(f)) {
					save(f, contest);
					Trace.trace(Trace.USER, "Event feed saved to: " + ef);
				} else
					Trace.trace(Trace.USER, "Save cancelled");
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error saving event feed", e);
			}
		}
	}

	protected static void showHelp() {
		System.out.println("Usage: awards.bat/sh [options]");
		System.out.println("   or: awards.bat/sh contestURL user password [options]");
		System.out.println("   or: awards.bat/sh contestPath [options]");
		System.out.println();
		System.out.println("  Options:");
		System.out.println("     --medals lastGold lastSilver lastBronze");
		System.out.println("         Assigns medal awards, overriding medal results in <finalize> element");
		System.out.println("     --rank numTeams");
		System.out.println("         Assigns rank awards to the given number of teams, e.g. \"1st place\"");
		System.out.println("     --firstPlaceCitation text");
		System.out.println("         Assigns first place award with the given citation, e.g. \"World Champion\"");
		System.out.println("     --fts beforeFreeze afterFreeze");
		System.out.println("         Assigns first to solve awards, displaying for teams whose award");
		System.out.println("         is before or after scoreboard freeze");
		System.out.println("     --group");
		System.out.println("         Assigns group awards");
		System.out.println("     --list");
		System.out.println("         List current awards");
		System.out.println("     --help");
		System.out.println("         Shows this message");
		System.out.println("     --version");
		System.out.println("         Displays version information");
	}

	protected static boolean promptToOverwrite(File f) throws IOException {
		Trace.trace(Trace.USER, f.getAbsolutePath() + " already exists.");
		Trace.trace(Trace.USER, "Do you want to overwrite it? Enter Y/y to accept or any other key to cancel.");
		char c = (char) System.in.read();
		return 'Y' == c || 'y' == c;
	}

	protected static void save(File file, Contest contest) throws IOException {
		PrintWriter pw = new PrintWriter(file);
		if (file.getName().endsWith(".xml")) {
			XMLFeedWriter writer = new XMLFeedWriter(pw, contest);
			writer.writeContest();
		} else {
			NDJSONFeedWriter writer = new NDJSONFeedWriter(pw);
			writer.writeContest(contest);
		}
	}

	protected static void generateScoreboard(String file, Contest contest) {
		try {
			File f = new File(file);
			if (f.exists() && !promptToOverwrite(f))
				return;

			PrintWriter pw = new PrintWriter(new FileWriter(f));
			Scoreboard.writeScoreboard(pw, contest);
			pw.close();
			Trace.trace(Trace.USER, "Scoreboard saved to " + f.getAbsolutePath());
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not generate scoreboard", e);
		}
	}
}