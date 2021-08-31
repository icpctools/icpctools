package org.icpc.tools.contest.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IClarification;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Scoreboard;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.DiskContestSource;
import org.icpc.tools.contest.model.feed.HTTPSSecurity;
import org.icpc.tools.contest.model.feed.NDJSONFeedWriter;
import org.icpc.tools.contest.model.feed.XMLFeedWriter;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.AwardUtil;
import org.icpc.tools.contest.model.util.ContestComparator;

/**
 * Event feed utility. Provides summary info or compares two event feeds.
 */
public class EventFeedUtil {

	protected static void showHelp() {
		System.out.println("Usage: [options]");
		System.out.println();
		System.out.println("  Options:");
		System.out.println("     --summary [event-feed.json]");
		System.out.println("         Shows information the given feed");
		System.out.println("     --validate [event-feed.json]");
		System.out.println("         Validate consistency of the given feed");
		System.out.println("     --compareSummary [event-feed1.json] [event-feed2.json]");
		System.out.println("         Performs a quick comparison of the two feeds");
		System.out.println("     --compare [event-feed1.json] [event-feed2.json]");
		System.out.println("         Perform a deep comparison of the two feeds");
		System.out.println("     --download [url] [user] [password]");
		System.out.println("         Download an event feed from the given url");
		System.out.println("     --testFeed [url] [user] [password]");
		System.out.println("         Test for feed heartbeat times");
		System.out.println("     --team [event-feed.json] [teamId]");
		System.out.println("         Produce a report on the activity of the given team");
		System.out.println("     --scoreboard [event-feed.json] [scoreboard.json]");
		System.out.println("         Generate a JSON scoreboard from an event feed");
		System.out.println("     --removeInactive [event-feed.json]");
		System.out.println("         Remove all teams that had no activity from the given event feed");
		System.out.println("     --convert [eventFeed.xml/json]");
		System.out.println("         Convert between event feed formats");
		System.out.println("     --help");
		System.out.println("         Displays this information");
		System.out.println("     --version");
		System.out.println("         Displays version information");
	}

	private static void expectArgs(List<String> argList, String o, String... s) {
		int num = s.length + 1;
		if (argList.size() < num) {
			Trace.trace(Trace.ERROR, "Missing arguments. " + o + " expects ");
			for (String ss : s)
				Trace.trace(Trace.ERROR, "[" + ss + "] ");

			System.exit(2);
		}
	}

	public static void main(String[] args) {
		Trace.init("ICPC Event Feed Utility", "eventFeedUtil", args);

		if (args == null || args.length == 0) {
			showHelp();
			return;
		}

		List<String> argList = new ArrayList<>();
		for (int i = 0; i < args.length; i++)
			argList.add(args[i]);

		switch (argList.get(0).toLowerCase()) {
			case "--help": {
				showHelp();
				break;
			}
			case "--summary": {
				expectArgs(argList, "--summary", "event-feed.json");
				summary(new File(argList.get(1)));
				break;
			}
			case "--comparesummary": {
				expectArgs(argList, "--compareSummary", "event-feed1.json", "event-feed2.json");
				compare(new File(argList.get(1)), new File(argList.get(2)), true);
				break;
			}
			case "--compare": {
				expectArgs(argList, "--compare", "event-feed1.json", "event-feed2.json");
				compare(new File(argList.get(1)), new File(argList.get(2)), false);
				break;
			}
			case "--download": {
				expectArgs(argList, "--download", "http://event-feed.json", "user", "password");
				download(argList.get(1), argList.get(2), argList.get(3));
				break;
			}
			case "--testfeed": {
				expectArgs(argList, "--testFeed", "http://event-feed.json", "user", "password");
				testFeedTiming(argList.get(1), argList.get(2), argList.get(3));
				break;
			}
			case "--validate": {
				expectArgs(argList, "--validate", "event-feed.json");
				validate(new File(argList.get(1)));
				break;
			}
			case "--team": {
				expectArgs(argList, "--team", "event-feed.json", "teamLabel");
				teamReport(new File(argList.get(1)), argList.get(2));
				break;
			}
			case "--scoreboard": {
				expectArgs(argList, "--scoreboard", "event-feed.json", "scoreboard.json");
				generateScoreboard(new File(argList.get(1)), new File(argList.get(2)));
				break;
			}
			case "--removeinactive": {
				expectArgs(argList, "--removeInactive", "event-feed.json");
				removeInactiveTeams(new File(argList.get(1)));
				break;
			}
			case "--convert": {
				expectArgs(argList, "--convert", "event-feed.json");
				convert(new File(argList.get(1)));
				break;
			}
			default: {
				Trace.trace(Trace.ERROR, "Invalid option(s): ");
				for (String a : args)
					Trace.trace(Trace.ERROR, a + " ");

				Trace.trace(Trace.ERROR, "");
				showHelp();
				return;
			}
		}
	}

	protected static void compare(File f1, File f2, boolean summaryOnly) {
		Contest c1 = loadEventFeed(f1);
		Contest c2 = loadEventFeed(f2);

		outputValidationWarning(c1);
		outputValidationWarning(c2);

		Trace.trace(Trace.USER, "");

		boolean ok = false;
		try {
			ok = ContestComparator.compareContests(c1, c2).print(summaryOnly);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error comparing event feeds", e);
			System.exit(2);
		}

		if (!ok) {
			Trace.trace(Trace.ERROR, "The event feeds are not identical.");
			System.exit(1);
		} else {
			Trace.trace(Trace.USER, "The event feeds match!");
		}
	}

	protected static Contest loadEventFeed(File file) {
		return loadEventFeed(file, null);
	}

	protected static Contest loadEventFeed(File file, IContestListener listener) {
		if (!file.exists()) {
			Trace.trace(Trace.ERROR, "Event feed could not be found: " + file.getAbsolutePath());
			System.exit(2);
		}

		Trace.trace(Trace.USER, "Loading event feed: " + file.getAbsolutePath());

		try {
			Contest c = DiskContestSource.loadContest(file, listener);
			ContestSource.getInstance().waitForContest(10000);
			return c;
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not parse event feed: " + file);
			System.exit(2);
			return null;
		}
	}

	private static String getGroupLabel(IContest contest, ITeam team) {
		IGroup[] groups = contest.getGroupsByIds(team.getGroupIds());
		if (groups == null || groups.length == 0)
			return "";

		String groupName = "";
		boolean first = true;
		for (IGroup g : groups) {
			if (!first)
				groupName += ", ";
			groupName += g.getName();
			first = false;
		}
		return groupName;
	}

	protected static void summary(File file) {
		Contest contest = loadEventFeed(file);
		outputValidationWarning(contest);

		// IInfo info = contest.getInfo();
		Trace.trace(Trace.USER, "");
		Trace.trace(Trace.USER, "Name: " + contest.getName());
		Trace.trace(Trace.USER, "Start time: " + ContestUtil.formatStartTime(contest.getStartTime()));
		Trace.trace(Trace.USER, "Duration: " + ContestUtil.formatDuration(contest.getDuration()));
		Trace.trace(Trace.USER, "Freeze duration: " + ContestUtil.formatDuration(contest.getFreezeDuration()));
		Trace.trace(Trace.USER, "");

		Trace.trace(Trace.USER, "Problems: " + contest.getNumProblems());
		Trace.trace(Trace.USER, "Organizations: " + contest.getNumOrganizations());
		Trace.trace(Trace.USER, "Teams: " + contest.getNumTeams());
		Trace.trace(Trace.USER, "Submissions: " + contest.getNumSubmissions());

		ITeam[] teams = contest.getOrderedTeams();
		int num = Math.min(10, teams.length);
		if (num > 1) {
			Trace.trace(Trace.USER, "");
			Trace.trace(Trace.USER, "Top teams:");

			for (int i = 0; i < num; i++) {
				ITeam team = teams[i];
				IStanding standing = contest.getStanding(team);
				Trace.trace(Trace.USER,
						"  " + standing.getRank() + " " + standing.getNumSolved() + " " + standing.getTime());
				Trace.trace(Trace.USER, "    " + team.getId() + ": " + team.getActualDisplayName() + " ("
						+ getGroupLabel(contest, team) + ")");
			}
		}

		int freeze = contest.getDuration() - contest.getFreezeDuration();
		int unjudgedSubmissions = 0;
		int judgedAfterFreeze = 0;
		for (ISubmission submission : contest.getSubmissions()) {
			if (!contest.isJudged(submission))
				unjudgedSubmissions++;
			if (submission.getContestTime() / 1000 > freeze && contest.isJudged(submission))
				judgedAfterFreeze++;
		}

		Trace.trace(Trace.USER, "");
		if (unjudgedSubmissions > 0)
			Trace.trace(Trace.USER, "Contest has " + unjudgedSubmissions + " unjudged submissions");

		if (judgedAfterFreeze > 0)
			Trace.trace(Trace.USER, "Contest has " + judgedAfterFreeze + " judged submissions after the freeze");
		else
			Trace.trace(Trace.USER, "Contest has no judged submissions after the freeze");

		Trace.trace(Trace.USER, "");
		if (!contest.isDoneUpdating()) {
			Trace.trace(Trace.USER, "Not done updating!");
		} else if (contest.getAwards() != null) {
			Trace.trace(Trace.USER, "Medals - last bronze: " + AwardUtil.getLastBronze(contest));
		}
	}

	protected static void download(String url, String user, String password) {
		String url2 = url;
		if (!url2.endsWith("/"))
			url2 += "/";
		url2 += "event-feed";
		try {
			HttpURLConnection conn = HTTPSSecurity.createConnection(new URL(url2), user, password);
			conn.setReadTimeout(15 * 1000); // 15s timeout
			BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("events.json"));
			byte[] buf = new byte[8092];
			int n = in.read(buf);
			while (n > 0) {
				out.write(buf, 0, n);
				n = in.read(buf);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			Trace.trace(Trace.ERROR, "I/O error downloading", e);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error downloading", e);
		}
	}

	protected static void testFeedTiming(String url, String user, String password) {
		String url2 = url;
		if (!url2.endsWith("/"))
			url2 += "/";
		url2 += "event-feed";

		NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
		nf.setMinimumFractionDigits(2);
		nf.setMaximumFractionDigits(2);

		long last = 0;
		try {
			HttpURLConnection conn = HTTPSSecurity.createConnection(new URL(url2), user, password);
			conn.setReadTimeout(180 * 1000); // 3 min timeout
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			last = System.currentTimeMillis();
			long lastSysOut = System.currentTimeMillis();
			String s = br.readLine();
			while (s != null) {
				long time = System.currentTimeMillis();
				String type = "Event";
				if (s.isEmpty()) { // heartbeat
					type = "Heartbeat";
				}
				if (time - last > 10000) {
					Trace.trace(Trace.USER, type + " after " + nf.format((time - last) / 1000.0) + "s");
					lastSysOut = time;
				} else {
					if (time - lastSysOut > 60000) {
						System.out.println(".");
						lastSysOut = time;
					}
				}
				last = time;

				s = br.readLine();
			}
			Trace.trace(Trace.USER, "Feed complete");
		} catch (IOException e) {
			if (last > 0 && System.currentTimeMillis() - last > 175000) {
				Trace.trace(Trace.USER, "Fail: read timeout after 180s!");
				return;
			}
			Trace.trace(Trace.ERROR, "I/O error downloading", e);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error downloading", e);
		}
	}

	protected static void teamReport(File file, final String teamId) {
		if (teamId == null)
			return;

		Contest contest = loadEventFeed(file, null);
		ITeam team = contest.getTeamById(teamId);
		if (team == null) {
			Trace.trace(Trace.USER, "Team not found");
			System.exit(0);
		}
		Trace.trace(Trace.USER, "");
		Trace.trace(Trace.USER, "Team: " + team.getActualDisplayName());
		IStanding standing = contest.getStanding(team);
		Trace.trace(Trace.USER, "Rank: " + standing.getRank());
		Trace.trace(Trace.USER, "Solved: " + standing.getNumSolved());
		Trace.trace(Trace.USER, "Total time: " + standing.getTime());
		Trace.trace(Trace.USER, "");

		Trace.trace(Trace.USER, "Problem Summary:");
		for (int i = 0; i < contest.getNumProblems(); i++) {
			IProblem p = contest.getProblems()[i];
			IResult r = contest.getResult(team, i);
			String s = "   " + p.getLabel() + ": " + r.getStatus();
			if (r.getStatus() != Status.UNATTEMPTED) {
				s += " (submissions: " + r.getNumSubmissions();
				if (r.getStatus() == Status.SOLVED) {
					s += ", time: " + ContestUtil.getTimeInMin(r.getContestTime());
					if (r.getPenaltyTime() != 0)
						s += ", penalty: " + r.getPenaltyTime() + "";
					s += ")";
				}
			}
			Trace.trace(Trace.USER, s);
		}

		Trace.trace(Trace.USER, "");
		Trace.trace(Trace.USER, "Submissions:");
		for (ISubmission s : contest.getSubmissions()) {
			if (s.getTeamId().equals(team.getId())) {
				IProblem p = contest.getProblemById(s.getProblemId());
				String st = "   " + s.getId() + ": problem: " + p.getId() + ", time: "
						+ ContestUtil.getTime(s.getContestTime());

				IJudgement[] sjs = contest.getJudgementsBySubmissionId(s.getId());
				if (sjs != null) {
					for (IJudgement sj : sjs) {
						IJudgementType jt = contest.getJudgementTypeById(sj.getJudgementTypeId());
						if (jt == null)
							st += " (judgement: null)";
						else
							st += " (judgement: " + jt.getId() + ", solved: " + jt.isSolved() + ", penalty: " + jt.isPenalty()
									+ ")";
					}
				} else
					st += " (no judgement)";
				Trace.trace(Trace.USER, st);
			}
		}

	}

	protected static void outputValidationWarning(Contest contest) {
		if (contest.validate() != null)
			Trace.trace(Trace.WARNING, "Event feed is not valid. Run \"eventFeed --validate\" for details");
	}

	protected static void validate(File file) {
		Contest c = loadEventFeed(file);

		boolean valid = true;

		List<String> list2 = c.validate();

		if (list2 != null) {
			for (String s : list2)
				Trace.trace(Trace.ERROR, s);

			valid = false;
		}

		if (valid)
			Trace.trace(Trace.USER, "Event feed was successfully validated, no problems found");
		else {
			Trace.trace(Trace.ERROR, "Event feed is not valid");
			System.exit(1);
		}
	}

	protected static void convert(File file) {
		Contest contest = loadEventFeed(file);

		try {
			String fileName = file.getName().substring(0, file.getName().lastIndexOf("."));
			if (file.getName().endsWith("xml"))
				fileName += ".json";
			else
				fileName += ".xml";
			File toFile = new File(file.getParentFile(), fileName);
			if (toFile.exists())
				if (!promptToOverwrite(toFile))
					System.exit(0);

			PrintWriter pw = new PrintWriter(toFile);
			if (toFile.getName().endsWith("xml")) {
				XMLFeedWriter writer = new XMLFeedWriter(pw, contest);
				writer.writeContest();
			} else {
				NDJSONFeedWriter writer = new NDJSONFeedWriter(pw);
				writer.writeContest(contest);
			}
			Trace.trace(Trace.USER, "Feed saved to " + toFile);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error converting", e);
		}
	}

	protected static boolean promptToOverwrite(File f) throws IOException {
		Trace.trace(Trace.USER, f.getAbsolutePath() + " already exists.");
		Trace.trace(Trace.USER, "Do you want to overwrite it? Enter Y/y to accept or any other key to cancel.");
		char c = (char) System.in.read();
		return 'Y' == c || 'y' == c;
	}

	protected static void removeInactiveTeams(File file) {
		Contest contest = loadEventFeed(file);

		outputValidationWarning(contest);

		ITeam[] teams = contest.getTeams();
		List<String> teamIds = new ArrayList<>();
		for (ITeam t : teams)
			teamIds.add(t.getId());

		for (ISubmission s : contest.getSubmissions()) {
			String teamId = s.getTeamId();
			if (teamIds.contains(teamId))
				teamIds.remove(teamId);
		}

		for (IClarification clar : contest.getClarifications()) {
			String teamId = clar.getFromTeamId();
			if (teamIds.contains(teamId))
				teamIds.remove(teamId);
		}

		if (teamIds.isEmpty()) {
			Trace.trace(Trace.USER, "All teams had activity during the contest.");
			return;
		}

		Trace.trace(Trace.USER, "There were " + teamIds.size() + " team(s) with no activity during the contest:");
		for (String teamId : teamIds) {
			ITeam team = contest.getTeamById(teamId);
			Trace.trace(Trace.USER, "   " + teamId + ": " + team.getActualDisplayName());
		}

		Trace.trace(Trace.USER, "Do you want to remove these teams? Enter Y/y to accept or any other key to cancel.");
		try {
			char c = (char) System.in.read();
			if ('Y' == c || 'y' == c) {
				for (String teamId : teamIds) {
					ITeam team = contest.getTeamById(teamId);
					Trace.trace(Trace.USER, "Removing: ");
					Trace.trace(Trace.USER, "   " + teamId + ": " + team.getActualDisplayName());
					contest.removeFromHistory(team);
				}

				File backupFile = new File(file.getParentFile(), file.getName() + ".bk");
				try {
					Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Could not backup file to: " + backupFile.getName());
					System.exit(1);
				}
				Trace.trace(Trace.USER, "Original backed up to: " + backupFile.getName());
				PrintWriter pw = new PrintWriter(file);
				if (file.getName().endsWith(".xml")) {
					XMLFeedWriter writer = new XMLFeedWriter(pw, contest);
					writer.writeContest();
				} else {
					NDJSONFeedWriter writer = new NDJSONFeedWriter(pw);
					writer.writeContest(contest);
				}
				Trace.trace(Trace.USER, teamIds.size() + " team(s) removed.");
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error removing teams", e);
		}
	}

	protected static void generateScoreboard(File file, File scoreFile) {
		Contest contest = loadEventFeed(file);

		try {
			if (scoreFile.exists() && !promptToOverwrite(scoreFile))
				return;

			PrintWriter pw = new PrintWriter(new FileWriter(scoreFile));
			Scoreboard.writeScoreboard(pw, contest);
			pw.close();
			Trace.trace(Trace.USER, "Scoreboard saved to " + scoreFile.getAbsolutePath());
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not generate scoreboard", e);
		}
	}
}