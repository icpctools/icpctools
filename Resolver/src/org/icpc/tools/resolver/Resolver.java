package org.icpc.tools.resolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.icpc.tools.client.core.IPropertyListener;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IContestObject.ContestType;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.Scoreboard;
import org.icpc.tools.contest.model.TimeFilter;
import org.icpc.tools.contest.model.TypeFilter;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.ArgumentParser;
import org.icpc.tools.contest.model.util.ArgumentParser.OptionParser;
import org.icpc.tools.contest.model.util.AwardUtil;
import org.icpc.tools.presentation.contest.internal.PresentationClient;
import org.icpc.tools.presentation.contest.internal.TeamUtil;
import org.icpc.tools.presentation.contest.internal.TeamUtil.Style;
import org.icpc.tools.resolver.ResolutionUtil.ResolutionStep;
import org.icpc.tools.resolver.ResolverLogic.PredeterminedStep;
import org.icpc.tools.resolver.ResolverUI.ClickListener;
import org.icpc.tools.resolver.ResolverUI.Screen;

/**
 * A Resolver contains two types of Presentations: a "ScoreboardPresentation", which is a grid
 * showing teams and the problems they've solved; and a "TeamAwardPresentation", which is a
 * separate display showing a picture of the team (if available) along with a list of the award(s)
 * they've earned. The Resolver displays the ProblemsPresentation by default while it goes through
 * the "resolve" process, then switches to an appropriate TeamAwardPresentation each time a team
 * arrives at the point in the process where they should receive their award. A Resolver also
 * optionally (and by default) contains a third "SplashPresentation" which is displayed on startup.
 *
 * @author Tim deBoer
 * @author with documentation and minor improvements added by John Clevenger
 */
public class Resolver {
	private static final int DEFAULT_ROW_OFFSET_WHEN_ENABLED = 4;

	private static final String DATA_RESOLVER_CLICKS = "org.icpc.tools.presentation.contest.resolver.clicks";
	private static final String DATA_RESOLVER_SETTINGS = "org.icpc.tools.presentation.contest.resolver.settings";
	private static final String DATA_RESOLVER_SCROLL = "org.icpc.tools.presentation.contest.resolver.scroll";
	private static final String DATA_RESOLVER_SPEED = "org.icpc.tools.presentation.contest.resolver.speed";

	// contest/resolving variables
	private Contest finalContest;
	private int singleStepStartRow = Integer.MIN_VALUE;
	private int rowOffset;

	// UI variables
	private ResolverUI ui;
	private double speedFactor = 1;
	private int clicks;
	private boolean isPresenter;
	private Screen screen = null;
	private String displayStr;
	private boolean show_info;
	private boolean bill;
	private boolean test;
	private Style style;
	private String[] groups;

	// client/server variables
	private PresentationClient client;
	private int[] clients = new int[0];

	private List<PredeterminedStep> predeterminedSteps = new ArrayList<>();

	protected static void showHelp() {
		System.out.println("Usage: resolver.bat/sh contestURL user password [options]");
		System.out.println("   or: resolver.bat/sh contestPath [options]");
		System.out.println();
		System.out.println("  General options:");
		System.out.println("     --info");
		System.out.println("         Show additional info to presenter client");
		System.out.println("     --speed speedFactor");
		System.out.println("         Resolution delay multiplier. e.g. 0.5 will be twice");
		System.out.println("         as fast, 2 will be twice as slow");
		System.out.println("     --singleStep startRow");
		System.out.println("         Require a click for each step starting at a specific");
		System.out.println("         row, or for entire contest if no row specified");
		System.out.println("     --rowDisplayOffset numRows");
		System.out.println("         Move the display up the screen by some number of");
		System.out.println("         rows (default 4)");
		System.out.println("     --display #");
		System.out.println("         Uses the specified display");
		System.out.println("         1 = primary display, 2 = secondary display, etc.");
		System.out.println("     --style style");
		System.out.println("         Change the display of team names. Must be one of:");
		System.out.println("         team_name, org_name, org_formal_name, or team_and_org_name");
		System.out.println("     --groups");
		System.out.println("         Resolve only the teams in the specified list of comma separated group ids.");
		System.out.println("         If multiple groups are given, each is resolved separately");
		System.out.println("     --pause #");
		System.out.println("         Start at the given pause #. Useful for testing/preview");
		System.out.println("     --bill");
		System.out.println("         Temporary option to test features for ICPC 2017/2018");
		System.out.println("     --test");
		System.out.println("         Test on an unfinished contest. Ignores (removes) all unjudged runs");
		System.out.println("     --help");
		System.out.println("         Shows this message");
		System.out.println("     --version");
		System.out.println("         Displays version information");
		System.out.println();
		System.out.println("  Client options:");
		System.out.println("     --presenter");
		System.out.println("         connect to a CDS and control it");
		System.out.println("     --client");
		System.out.println("         connect to a CDS in slave (view-only) mode");
		System.out.println("     --side");
		System.out.println("         same as --client, but displays logos suitable for");
		System.out.println("         a lower resolution/side display");
		System.out.println("     --team");
		System.out.println("         same as --client, but displays minimal content, e.g.");
		System.out.println("         to display on all team machines");

		System.out.println();
		System.out.println("  Keyboard shortcuts:");
		System.out.println("     Ctrl-Q - Quit");
		System.out.println("     r      - Rewind");
		System.out.println("     0      - Restart (jump to beginning)");
		System.out.println("     2      - Fast forward (jump one step without delays)");
		System.out.println("     1      - Fast rewind (jump one step back without delays)");
		System.out.println("     +/up   - Speed up (reduce resolution delay)");
		System.out.println("     -/down - Slow down (increase resolution delay)");
		System.out.println("     j      - Reset resolution speed");
		System.out.println("     p      - Pause/unpause scrolling");
		System.out.println("     i      - Toggle additional info");
	}

	public static void main(String[] args) {
		// create the Resolver object
		final Resolver r = new Resolver();
		ContestSource[] contestSource = ArgumentParser.parseMulti(args, new OptionParser() {
			@Override
			public boolean setOption(String option, List<Object> options) throws IllegalArgumentException {
				return r.processOption(option, options);
			}

			@Override
			public void showHelp() {
				Resolver.showHelp();
			}
		});

		String log = "resolver";
		List<String> argList = Arrays.asList(args);
		if (argList.contains("--client"))
			log += "-client";
		else if (argList.contains("--team"))
			log += "-team";
		Trace.init("ICPC Resolver", log, args);
		System.setProperty("apple.awt.application.name", "Resolver");

		for (ContestSource cs : contestSource)
			cs.outputValidation();

		List<ResolutionStep> steps = new ArrayList<ResolutionUtil.ResolutionStep>();
		for (ContestSource cs : contestSource) {
			r.loadFromSource(cs);
			r.loadSteps(cs);
			r.init(steps);
		}

		ResolutionUtil.numberThePauses(steps);
		Trace.trace(Trace.INFO, "Resolution steps:");
		for (ResolutionStep step : steps)
			Trace.trace(Trace.INFO, "  " + step);

		try {
			r.connectToCDS(contestSource[0]);
		} catch (NumberFormatException e) {
			Trace.trace(Trace.ERROR, "Could not connect to CDS");
			System.exit(2);
		}

		r.launch(steps);
	}

	private void connectToCDS(ContestSource source) {
		if (!isPresenter && screen == null)
			return;

		if (!(source instanceof RESTContestSource)) {
			Trace.trace(Trace.ERROR, "Source argument must be a CDS");
			System.exit(1);
		}

		RESTContestSource cdsSource = (RESTContestSource) source;
		try {
			String role = "blue";
			if (isPresenter)
				role = "presAdmin";

			client = new PresentationClient(cdsSource.getUser(), role, cdsSource, "resolver") {
				@Override
				protected void clientsChanged(Client[] cl) {
					Trace.trace(Trace.INFO, "Client list changed: " + cl.length);
					int size = cl.length;
					int[] c = new int[size];
					for (int i = 0; i < size; i++)
						c[i] = cl[i].uid;
					clients = c;

					// re-send the settings to all the clients
					sendSettings();

					// click count too
					sendClicks();
				}
			};
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Trouble connecting to CDS: " + e.getMessage());
			System.exit(1);
		}

		try {
			client.addListener(new IPropertyListener() {
				@Override
				public void propertyUpdated(String key, String value) {
					Trace.trace(Trace.INFO, "New property: " + key + ": " + value);
					if (DATA_RESOLVER_CLICKS.equals(key)) {
						clicks = Integer.parseInt(value);
						if (ui != null)
							ui.moveTo(clicks);
					} else if (DATA_RESOLVER_SCROLL.equals(key)) {
						boolean b = Boolean.parseBoolean(value);
						if (ui != null)
							ui.setScrollPause(b);
					} else if (DATA_RESOLVER_SPEED.equals(key)) {
						double sf = Double.parseDouble(value);
						if (ui != null)
							ui.setSpeedFactor(sf);
					} else if (DATA_RESOLVER_SETTINGS.equals(key)) {
						String[] clientArgs = value.split(",");
						ui.setSpeedFactor(Double.parseDouble(clientArgs[0]));
						singleStepStartRow = Integer.parseInt(clientArgs[1]);
						rowOffset = Integer.parseInt(clientArgs[2]);
					}
				}
			});

			client.connect();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Client error", e);
		}
	} // end connectToServer

	private boolean processOption(String option, List<Object> options) {
		if ("--fast".equalsIgnoreCase(option) || "--speed".equalsIgnoreCase(option)) {
			// --fast varies the speed at which the resolving process should run (useful for
			// previewing results).
			// This option allows for an float parameter following --fast that is used as the
			// speed multiplier; SpeedFactor values greater than zero but less than one
			// INCREASE
			// the resolution speed; values greater than one DECREASE the speed; values <= zero are
			// ignored.
			ArgumentParser.expectOptions(option, options, "speed:float");
			float fastVal = (float) options.get(0);
			if (fastVal <= 0) {
				// illegal value; ignore and use default
				Trace.trace(Trace.ERROR, "Illegal --fast value ignored (must be positive)");
			} else
				speedFactor = fastVal;
		} else if ("--singlestep".equalsIgnoreCase(option)) {
			// --singleStep option: indicate row where single-stepping should start
			ArgumentParser.expectOptions(option, options, "startRow:int");

			// Default to single-stepping from the very beginning (bottom)
			singleStepStartRow = Integer.MAX_VALUE;

			// check if arguments specify a specific row on which to start single-stepping
			// get the row on which single-stepping should start; subtract 1 for zero-base
			singleStepStartRow = (int) options.get(0) - 1;
			if (singleStepStartRow <= 0)
				Trace.trace(Trace.ERROR, "Illegal --singleStep value ignored");
		} else if ("--info".equalsIgnoreCase(option)) {
			ArgumentParser.expectNoOptions(option, options);
			// --info option: display extra commentary information visible only on the Presenter
			show_info = true;
		} else if ("--pause".equalsIgnoreCase(option)) {
			ArgumentParser.expectOptions(option, options, "#:int");
			clicks = (int) options.get(0);
		} else if ("--client".equalsIgnoreCase(option) || "--presenter".equalsIgnoreCase(option)
				|| "--team".equalsIgnoreCase(option) || "--side".equalsIgnoreCase(option)
				|| "--org".equalsIgnoreCase(option)) {
			ArgumentParser.expectNoOptions(option, options);
			if ("--presenter".equalsIgnoreCase(option))
				isPresenter = true;
			else if ("--team".equalsIgnoreCase(option))
				screen = Screen.TEAM;
			else if ("--org".equalsIgnoreCase(option))
				screen = Screen.ORG;
			else if ("--side".equalsIgnoreCase(option))
				screen = Screen.SIDE;
			else
				screen = Screen.MAIN;
		} else if ("--display".equalsIgnoreCase(option)) {
			// --display allows to specify which display to use in full-screen exclusive mode.
			// The value is used as follows:
			// 1 --> run presentation full-screen on the primary display (the default)
			// 2 --> run presentation full-screen on display 2
			// 3 --> run presentation full-screen on display 3
			// 1a --> run presentation in the top-left corner of the display (for testing)
			// 1b --> run presentation in the top-right corner of the display (for testing)
			// 2a --> run presentation in the top-left corner of display 2 ...
			ArgumentParser.expectOptions(option, options, "screen:string");
			displayStr = (String) options.get(0);
		} else if ("--style".equalsIgnoreCase(option)) {
			ArgumentParser.expectOptions(option, options, "style:string");
			style = TeamUtil.getStyleByString((String) options.get(0));
		} else if ("--groups".equalsIgnoreCase(option)) {
			ArgumentParser.expectOptions(option, options, "group-id:string", "*");
			groups = options.toArray(new String[0]);
		} else if ("--rowDisplayOffset".equalsIgnoreCase(option)) {
			// causes rows to be moved up the screen so they are not blocked by people on
			// stage

			ArgumentParser.expectOptions(option, options, "rows:int");
			// get the number of rows to move row displays up
			int numRows = (int) options.get(0);
			if (numRows <= 0) {
				// illegal value; ignore and use default
				Trace.trace(Trace.ERROR, "Illegal --rowDisplayOffset value ignored (must be positive)");
				rowOffset = DEFAULT_ROW_OFFSET_WHEN_ENABLED;
			} else
				rowOffset = numRows;
		} else if ("--bill".equalsIgnoreCase(option)) {
			ArgumentParser.expectNoOptions(option, options);
			bill = true;
		} else if ("--test".equalsIgnoreCase(option)) {
			ArgumentParser.expectNoOptions(option, options);
			test = true;
		} else {
			// the argument read from the command line was unrecognized...
			return false;
		}

		return true;
	}

	private Resolver() {
		// do not create
	}

	protected static Contest filter(Contest contest) {
		Contest c = removeUnnecessaryTypes(contest);
		c.removeHiddenTeams();
		c.removeSubmissionsOutsideOfContestTime();
		return c;
	}

	private void loadFromSource(ContestSource source) {
		Trace.trace(Trace.INFO, "Loading from " + source);

		int showHour = -1;
		// we're a stand-alone resolver; load the event feed and create a Contest object
		try {
			source.outputValidation();
			finalContest = source.getContest();
			if (test)
				source.waitForContest(10000);
			else if (!source.waitForContest(20000))
				Trace.trace(Trace.ERROR, "Could not load complete contest");

			if (test) {
				if (finalContest.isDoneUpdating()) {
					Trace.trace(Trace.ERROR, "Test mode cannot be used on contests that are done updating.");
					System.exit(1);
				}
				int num = finalContest.removeUnjudgedSubmissions();
				Trace.trace(Trace.WARNING, "Test mode active, " + num + " unjudged submissions discarded.");
			}

			if (!test && !finalContest.isDoneUpdating()) {
				Trace.trace(Trace.ERROR, "Contest not done updating. Use --test if running against an incomplete contest");
				System.exit(1);
			}

			finalContest = filter(finalContest);
			validateContest(finalContest);

			if (isPresenter) {
				try {
					File logFolder = new File("logs");
					File scoreFile = new File(logFolder, "scoreboard.json");
					PrintWriter pw = new PrintWriter(new FileWriter(scoreFile));
					Scoreboard.writeScoreboard(pw, finalContest);
					pw.close();
					Trace.trace(Trace.USER, "Scoreboard written to " + scoreFile.getAbsolutePath());
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Could not write scoreboard");
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Couldn't load event feed", e);
			System.exit(1);
		}

		if (showHour > 0)
			finalContest = finalContest.clone(false, new TimeFilter(finalContest, showHour * 3600000));
	}

	private void loadSteps(ContestSource source) {
		Trace.trace(Trace.INFO, "Checking for predetermined steps");

		try {
			File f = source.getFile("resolver.tsv");
			if (f == null || !f.exists()) {
				Trace.trace(Trace.INFO, "No predetermined steps found");
				return;
			}
			Trace.trace(Trace.INFO, "Predetermined steps found in " + f);

			BufferedReader br = new BufferedReader(new FileReader(f));
			try {
				// read header
				br.readLine();

				String s = br.readLine();
				while (s != null) {
					String[] st = s.split("\\t");
					if (st != null && st.length > 0)
						predeterminedSteps.add(new PredeterminedStep(st[0], st[1]));

					s = br.readLine();
				}
			} finally {
				try {
					br.close();
				} catch (Exception e) {
					// ignore
				}
			}
			Trace.trace(Trace.INFO, "Loaded " + predeterminedSteps.size() + " predetermined steps");
		} catch (Exception e) {
			// Trace.trace(Trace.ERROR, "Could not load predetermined steps", e);
		}
	}

	private void init(List<ResolutionStep> steps) {
		Trace.trace(Trace.INFO, "Initializing resolver...");

		if (groups != null) {
			IAward[] fullAwards = finalContest.getAwards();
			boolean hasAwards = fullAwards != null && fullAwards.length > 0;
			for (int i = 0; i < groups.length; i++) {
				Trace.trace(Trace.INFO, "Resolving for group " + groups[i]);
				Contest cc = finalContest.clone(true);
				// set the current group to be visible and all others hidden
				Pattern pattern = Pattern.compile(groups[i].trim());
				for (IGroup g : cc.getGroups()) {
					if (pattern.matcher(g.getId()).matches())
						cc.setGroupIsHidden(g, false);
					else if (!g.isHidden())
						cc.setGroupIsHidden(g, true);
				}

				cc.removeHiddenTeams();

				if (!hasAwards) {
					Trace.trace(Trace.USER, "Generating awards");
					AwardUtil.createDefaultAwards(cc);
				}

				// create the official scoreboard
				cc.officialResults();

				ResolverLogic logic = new ResolverLogic(cc, singleStepStartRow, rowOffset, show_info, predeterminedSteps);

				long time = System.currentTimeMillis();
				List<ResolutionStep> subSteps = logic.resolveFrom(bill);
				steps.addAll(subSteps);
				outputStats(steps, time);
			}
		} else {
			IAward[] contestAwards = finalContest.getAwards();
			if (contestAwards == null || contestAwards.length == 0) {
				Trace.trace(Trace.USER, "Generating awards");
				AwardUtil.createDefaultAwards(finalContest);
			}

			// create the official scoreboard
			finalContest.officialResults();

			ResolverLogic logic = new ResolverLogic(finalContest, singleStepStartRow, rowOffset, show_info,
					predeterminedSteps);

			int showHour = -1;
			if (showHour > 0) {
				// IInfo info = finalContest.getInfo();
				// ((Info)info).setFreezeDuration();
				// contestTime = (showHour - 1) * 3600;
			}

			long time = System.currentTimeMillis();
			List<ResolutionStep> subSteps = logic.resolveFrom(bill);
			steps.addAll(subSteps);
			outputStats(steps, time);
		}

		ResolutionUtil.numberThePauses(steps);

		Trace.trace(Trace.INFO, "Resolution steps:");
		for (ResolutionStep step : steps)
			Trace.trace(Trace.INFO, "  " + step);
	}

	protected void launch(List<ResolutionStep> steps) {
		ui = new ResolverUI(steps, show_info, displayStr, isPresenter || client == null, screen, new ClickListener() {
			@Override
			public void clicked(int num) {
				clicks = num;
				sendClicks();
			}

			@Override
			public void scroll(boolean pause) {
				sendScroll(pause);
			}

			@Override
			public void speedFactor(double d) {
				sendSpeedFactor(d);
			}
		}, style);

		ui.setSpeedFactor(speedFactor);
		ui.display();
		ui.moveTo(clicks);

		if (client != null && clicks > 0) {
			Trace.trace(Trace.INFO, "Catching up to click: " + clicks);
			ui.moveTo(clicks);
		}
	}

	private void outputStats(List<ResolutionStep> steps, long time) {
		double totalTime = ResolutionUtil.getTotalTime(steps);
		long min = ((int) totalTime) / 60;
		long seconds = ((int) totalTime) % 60;
		Trace.trace(Trace.USER, "  Playback time: " + min + "m " + seconds + "s");
		if (speedFactor != 1.0) {
			totalTime *= speedFactor;
			min = ((int) totalTime) / 60;
			seconds = ((int) totalTime) % 60;
			Trace.trace(Trace.USER, "  Fast playback time: " + min + "m " + seconds + "s");
		}
		Trace.trace(Trace.USER, "  Total pauses: " + ResolutionUtil.getTotalPauses(steps));

		Trace.trace(Trace.INFO, "  Contest states: " + ResolutionUtil.getTotalContests(steps));
		Trace.trace(Trace.INFO, "  Total steps: " + steps.size());
		Trace.trace(Trace.INFO, "  Time to resolve: " + (System.currentTimeMillis() - time) + "ms");
	}

	private void sendSettings() {
		try {
			String clientArgs = speedFactor + "," + singleStepStartRow + "," + rowOffset;

			Trace.trace(Trace.INFO, "Sending settings: " + clientArgs);
			client.sendProperty(clients, DATA_RESOLVER_SETTINGS, clientArgs);
		} catch (Exception ex) {
			Trace.trace(Trace.WARNING, "Failed to send settings", ex);
		}
	}

	private void sendClicks() {
		if (!isPresenter || client == null)
			return;

		try {
			Trace.trace(Trace.INFO, "Sending clicks: " + clicks);

			client.sendProperty(clients, DATA_RESOLVER_CLICKS, Integer.toString(clicks));
		} catch (Exception ex) {
			Trace.trace(Trace.WARNING, "Failed to send clicks", ex);
		}
	}

	private void sendScroll(boolean pause) {
		if (!isPresenter || client == null)
			return;

		try {
			Trace.trace(Trace.INFO, "Sending scroll: " + pause);

			client.sendProperty(clients, DATA_RESOLVER_SCROLL, Boolean.toString(pause));
		} catch (Exception ex) {
			Trace.trace(Trace.WARNING, "Failed to send clicks", ex);
		}
	}

	private void sendSpeedFactor(double d) {
		if (!isPresenter || client == null)
			return;

		try {
			Trace.trace(Trace.INFO, "Sending speed: " + d);

			client.sendProperty(clients, DATA_RESOLVER_SPEED, Double.toString(d));
		} catch (Exception ex) {
			Trace.trace(Trace.WARNING, "Failed to send clicks", ex);
		}
	}

	/**
	 * Check that the contest is valid, has no unjugded submissions, and a valid freeze time.
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

		// check freeze time
		int freeze = contest.getFreezeDuration();
		if (freeze < 0 || freeze > contest.getDuration())
			Trace.trace(Trace.WARNING, "Warning: Contest has no freeze time, will assume default");
	}

	protected static Contest removeUnnecessaryTypes(Contest contest) {
		List<ContestType> types = new ArrayList<>();
		types.add(ContestType.CONTEST);
		types.add(ContestType.STATE);
		types.add(ContestType.TEAM);
		types.add(ContestType.TEAM_MEMBER);
		types.add(ContestType.ORGANIZATION);
		types.add(ContestType.GROUP);
		types.add(ContestType.PROBLEM);
		types.add(ContestType.SUBMISSION);
		types.add(ContestType.JUDGEMENT);
		types.add(ContestType.JUDGEMENT_TYPE);
		types.add(ContestType.LANGUAGE);
		types.add(ContestType.AWARD);
		TypeFilter filter = new TypeFilter(types);
		return contest.clone(false, filter);
	}
}