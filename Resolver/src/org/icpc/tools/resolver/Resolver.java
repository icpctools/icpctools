package org.icpc.tools.resolver;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.icpc.tools.client.core.IPropertyListener;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IResolveInfo;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.Scoreboard;
import org.icpc.tools.contest.model.TimeFilter;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ResolveInfo;
import org.icpc.tools.contest.model.resolver.ResolutionUtil;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ResolutionStep;
import org.icpc.tools.contest.model.resolver.ResolverLogic;
import org.icpc.tools.contest.model.util.ArgumentParser;
import org.icpc.tools.contest.model.util.ArgumentParser.OptionParser;
import org.icpc.tools.contest.model.util.AwardUtil;
import org.icpc.tools.contest.model.util.Taskbar;
import org.icpc.tools.contest.model.util.TeamDisplay;
import org.icpc.tools.presentation.contest.internal.PresentationClient;
import org.icpc.tools.presentation.core.DisplayConfig;
import org.icpc.tools.presentation.core.PresentationWindow;
import org.icpc.tools.presentation.core.internal.PresentationWindowImpl;
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
	private static final String DATA_RESOLVER_ACTIVE_UI = "org.icpc.tools.presentation.contest.resolver.ui";

	// contest/resolving variables
	private ContestSource[] contestSources;
	private Contest[] finalContest;

	// UI variables
	private int activeUI;
	private ResolverUI[] ui;
	private String[] contestIds;
	private boolean isPresenter;
	private Screen screen = null;
	private String displayStr;
	private String multiDisplayStr;
	private boolean show_info;
	private boolean judgeQueue;
	private boolean test;
	private boolean lightMode;
	private String displayName;
	private String[] groupList;
	private String[] problemList;
	private String[] problemIdList;

	// client/server variables
	private PresentationClient client;
	private int[] clients = new int[0];

	protected static void showHelp() {
		System.out.println("Usage: resolver.bat/sh contestURL user password [options]");
		System.out.println("   or: resolver.bat/sh contestPackagePath [options]");
		System.out.println("   or: resolver.bat/sh eventFeedPath [options]");
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
		System.out.println("         Use the specified display");
		System.out.println("         1 = primary display, 2 = secondary display, etc.");
		System.out.println("     --multi-display p@wxh");
		System.out.println("         Stretch the presentation across multiple clients. Use \"2@3x2\"");
		System.out.println("         to indicate this client is position 2 (top middle) in a 3x2 grid");
		System.out.println("     --display_name template");
		System.out.println("         Change the way teams are displayed using a template. Parameters:");
		System.out.println("         {team.display_name), {team.name), {org.formal_name}, and {org.name}");
		System.out.println("     --groups");
		System.out.println("         Resolve only the groups in the given regex pattern for ids");
		System.out.println("         If multiple groups are given, each is resolved separately");
		System.out.println("     --pause #");
		System.out.println("         Start at the given pause #. Useful for testing/preview");
		System.out.println("     --judgeQueue");
		System.out.println("         Start the resolution using a judge queue. Must have at least one list award");
		System.out.println("     --test");
		System.out.println("         Test on an unfinished contest. Ignores (removes) all unjudged runs");
		System.out.println("     --light");
		System.out.println("         Use light mode");
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
		String log = "resolver";
		List<String> argList = Arrays.asList(args);
		if (argList.contains("--client"))
			log += "-client";
		else if (argList.contains("--team"))
			log += "-team";
		Trace.init("ICPC Resolver", log, args);

		// create the Resolver object
		final Resolver r = new Resolver();
		final ResolveInfo resolveInfo = new ResolveInfo();
		ContestSource[] contestSources = ArgumentParser.parseMulti(args, new OptionParser() {
			@Override
			public boolean setOption(String option, List<Object> options) throws IllegalArgumentException {
				return r.processOption(option, options, resolveInfo);
			}

			@Override
			public void showHelp() {
				Resolver.showHelp();
			}
		});

		if (contestSources == null) {
			showHelp();
			return;
		}

		System.setProperty("apple.awt.application.name", "Resolver");
		BufferedImage iconImage = null;
		try {
			iconImage = ImageIO.read(r.getClass().getClassLoader().getResource("images/resolverIcon.png"));
		} catch (Exception e) {
			// could not set title or icon
		}
		Taskbar.setTaskbarImage(iconImage);

		int numContests = 0;
		for (ContestSource cs : contestSources) {
			cs.outputValidation();
			numContests++;
		}

		r.ui = new ResolverUI[numContests];
		r.contestIds = new String[numContests];
		r.contestSources = contestSources;
		r.finalContest = new Contest[numContests];

		int i = 0;
		for (ContestSource cs : contestSources) {
			List<ResolutionStep> steps = new ArrayList<ResolutionUtil.ResolutionStep>();
			r.loadFromSource(i);

			if (r.isPresenter)
				r.sendSettings(resolveInfo);
			String g = null;
			String p = null;
			String pId = null;
			if (r.groupList != null && r.groupList.length > 0)
				g = r.groupList[i % r.groupList.length];
			if (r.problemList != null && r.problemList.length > 0)
				p = r.problemList[i % r.problemList.length];
			if (r.problemIdList != null && r.problemIdList.length > 0)
				pId = r.problemIdList[i % r.problemIdList.length];
			r.init(steps, g, p, pId, i, resolveInfo.getSingleStepRow());

			Trace.trace(Trace.INFO, "Resolution steps:");
			for (ResolutionStep step : steps)
				Trace.trace(Trace.INFO, "  " + step);

			r.ui[i] = r.createUI(steps);
			r.contestIds[i] = cs.getContestId();

			i++;
		}

		try {
			r.connectToCDS();
		} catch (NumberFormatException e) {
			Trace.trace(Trace.ERROR, "Could not connect to CDS");
			System.exit(2);
		}

		r.launch();
	}

	private void connectToCDS() {
		if (!isPresenter && screen == null)
			return;

		// all of the sources need to be on a CDS, otherwise we can't switch
		for (int i = 0; i < contestSources.length; i++)
			if (!(contestSources[i] instanceof RESTContestSource)) {
				Trace.trace(Trace.ERROR, "Source argument must be a CDS");
				System.exit(1);
			}

		RESTContestSource cdsSource = (RESTContestSource) contestSources[0];
		try {
			String role = "staff";
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

					// re-send the activeUI to all clients
					sendActiveUI();
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
					if (DATA_RESOLVER_ACTIVE_UI.equals(key)) {
						try {
							String contestId = value;
							if (contestIds[activeUI].equals(contestId))
								return;

							int newActiveUI = -1;
							for (int i = 0; i < contestIds.length; i++) {
								if (contestIds[i].equals(contestId))
									newActiveUI = i;
							}

							if (newActiveUI == -1)
								return;

							Trace.trace(Trace.INFO, "Switching active UI to " + newActiveUI);
							ui[activeUI].setActive(false, activeUI);
							ui[newActiveUI].setActive(true, newActiveUI);
							activeUI = newActiveUI;
						} catch (Exception e) {
							Trace.trace(Trace.ERROR, "Couldn't switch active UI", e);
						}
					}
				}
			});
			client.connect();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Client error", e);
		}
	} // end connectToServer

	private void sendActiveUI() {
		try {
			Trace.trace(Trace.INFO, "Sending active UI: " + activeUI + " " + contestIds[activeUI]);
			client.sendProperty(clients, DATA_RESOLVER_ACTIVE_UI, contestIds[activeUI]);
		} catch (Exception ex) {
			Trace.trace(Trace.WARNING, "Failed to send active UI", ex);
		}
	}

	private boolean processOption(String option, List<Object> options, ResolveInfo resolveInfo) {
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
				resolveInfo.setSpeedFactor(fastVal);
		} else if ("--singlestep".equalsIgnoreCase(option)) {
			// --singleStep option: indicate row where single-stepping should start
			ArgumentParser.expectOptions(option, options, "startRow:int");

			// check if arguments specify a specific row on which to start single-stepping
			// get the row on which single-stepping should start; subtract 1 for zero-base
			int singleStepStartRow = (int) options.get(0) - 1;
			if (singleStepStartRow <= 0)
				Trace.trace(Trace.ERROR, "Illegal --singleStep value ignored");
			else
				resolveInfo.setSingleStepRow(singleStepStartRow);
		} else if ("--info".equalsIgnoreCase(option)) {
			ArgumentParser.expectNoOptions(option, options);
			// --info option: display extra commentary information visible only on the Presenter
			show_info = true;
		} else if ("--pause".equalsIgnoreCase(option)) {
			ArgumentParser.expectOptions(option, options, "#:int");
			resolveInfo.setClicks((int) options.get(0));
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
			ArgumentParser.expectOptions(option, options, "display:string");
			displayStr = (String) options.get(0);
		} else if ("--multi-display".equalsIgnoreCase(option)) {
			ArgumentParser.expectOptions(option, options, "p@wxh:string");
			multiDisplayStr = (String) options.get(0);
		} else if ("--display_name".equalsIgnoreCase(option)) {
			ArgumentParser.expectOptions(option, options, "display_name:string");
			displayName = (String) options.get(0);
		} else if ("--groups".equalsIgnoreCase(option)) {
			ArgumentParser.expectOptions(option, options, "groups:string", "*");
			groupList = options.toArray(new String[0]);
		} else if ("--problems".equalsIgnoreCase(option)) {
			ArgumentParser.expectOptions(option, options, "problems:string", "*");
			problemList = options.toArray(new String[0]);
		} else if ("--problemIds".equalsIgnoreCase(option)) {
			ArgumentParser.expectOptions(option, options, "problemIds:string", "*");
			problemIdList = options.toArray(new String[0]);
		} else if ("--rowDisplayOffset".equalsIgnoreCase(option)) {
			// causes rows to be moved up the screen so they are not blocked by people on
			// stage

			ArgumentParser.expectOptions(option, options, "rows:int");
			// get the number of rows to move row displays up
			int numRows = (int) options.get(0);
			if (numRows <= 0) {
				// illegal value; ignore and use default
				Trace.trace(Trace.ERROR, "Illegal --rowDisplayOffset value ignored (must be positive)");
				resolveInfo.setRowOffset(DEFAULT_ROW_OFFSET_WHEN_ENABLED);
			} else
				resolveInfo.setRowOffset(numRows);
		} else if ("--judgeQueue".equalsIgnoreCase(option)) {
			ArgumentParser.expectNoOptions(option, options);
			judgeQueue = true;
		} else if ("--test".equalsIgnoreCase(option)) {
			ArgumentParser.expectNoOptions(option, options);
			test = true;
		} else if ("--light".equals(option)) {
			lightMode = true;
			return true;

		} else {
			// the argument read from the command line was unrecognized...
			return false;
		}

		return true;
	}

	private Resolver() {
		// do not create
	}

	private void loadFromSource(int con) {
		Trace.trace(Trace.INFO, "Loading from " + contestSources[con]);

		int showHour = -1;
		// we're a stand-alone resolver; load the event feed and create a Contest object
		try {
			contestSources[con].outputValidation();

			finalContest[con] = contestSources[con].loadContest(new IContestListener() {
				@Override
				public void contestChanged(IContest contest, IContestObject obj, Delta delta) {
					if (delta != Delta.DELETE && obj instanceof IResolveInfo) {
						IResolveInfo resolveInfo = (IResolveInfo) obj;
						ResolverUI ui2 = ui[activeUI];
						if (ui2 != null) {
							if (resolveInfo.getClicks() >= 0)
								ui2.moveTo(resolveInfo.getClicks());
							ui2.setScrollPause(resolveInfo.isAnimationPaused());
							if (!Double.isNaN(resolveInfo.getSpeedFactor()))
								ui2.setSpeedFactor(resolveInfo.getSpeedFactor());
							if (!Double.isNaN(resolveInfo.getScrollSpeedFactor()))
								ui2.setScrollSpeedFactor(resolveInfo.getScrollSpeedFactor());
						}
					}
				}
			});
			if (displayName != null)
				TeamDisplay.overrideDisplayName(finalContest[con], displayName);

			if (test) {
				contestSources[con].waitForContest(15000);
				if (finalContest[con].getState().isFinal()) {
					Trace.trace(Trace.ERROR, "Test mode cannot be used on contests that are finalized.");
					System.exit(1);
				}
				Trace.trace(Trace.INFO, "Test mode active");
			} else {
				// not test mode
				if (!contestSources[con].waitForContest(20000))
					Trace.trace(Trace.ERROR, "Could not load complete contest");
			}

			// check for unjudged runs. if we're in test mode warn the user and delete them.
			// if not in test mode, fail
			int num = finalContest[con].removeUnjudgedSubmissions();
			if (num > 0) {
				if (test)
					Trace.trace(Trace.WARNING, "Warning: " + num + " unjudged submissions discarded.");
				else {
					Trace.trace(Trace.ERROR,
							num + " unjudged submissions! Use --test if running against an incomplete contest");
					System.exit(1);
				}
			}

			if (!test && !finalContest[con].getState().isFinal()) {
				Trace.trace(Trace.WARNING, "Contest is not over. Use --test if running against an incomplete contest");
				System.exit(1);
			}

			if (finalContest[con].isDoneUpdating() && isPresenter && contestSources[con] instanceof RESTContestSource) {
				Trace.trace(Trace.ERROR, "Contest is already resolved, nothing to do");
				System.exit(1);
			}

			validateContest(finalContest[con]);

			if (isPresenter) {
				try {
					File logFolder = new File("logs");
					File scoreFile = new File(logFolder, "scoreboard.json");
					PrintWriter pw = new PrintWriter(new FileWriter(scoreFile));
					Scoreboard.writeScoreboard(pw, finalContest[con]);
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
			finalContest[con] = finalContest[con].clone(false, new TimeFilter(finalContest[con], showHour * 3600000));
	}

	private void init(List<ResolutionStep> steps, String groups, String problems, String problemIds, int con, int singleStepRow) {
		Trace.trace(Trace.INFO, "Initializing resolver...");

		 ResolveInfo resolveInfo = new ResolveInfo();
		resolveInfo.setSingleStepRow(singleStepRow);
		// resolveInfo.predeterminedSteps = predeterminedSteps;
		 finalContest[con].add(resolveInfo);

		if (groups != null || problems != null || problemIds != null) {
			IAward[] fullAwards = finalContest[con].getAwards();
			boolean hasAwards = fullAwards != null && fullAwards.length > 0;
			Contest cc = finalContest[con].clone(true);
			if (groups != null) {
				Trace.trace(Trace.INFO, "Resolving for group ids " + groups + " (Initial teams: " + cc.getNumTeams() + ")");

				// set the current group to be visible and all others hidden
				Pattern pattern = Pattern.compile(groups.trim());
				for (IGroup g : cc.getGroups()) {
					if (pattern.matcher(g.getId()).matches())
						cc.setGroupIsHidden(g, false);
					else if (!g.isHidden())
						cc.setGroupIsHidden(g, true);
				}

				cc.removeHiddenTeams();
				Trace.trace(Trace.INFO, "Resolved for group ids. Teams left: " + cc.getNumTeams());
			}

			if (problems != null) {
				Trace.trace(Trace.INFO,
						"Resolving for problem labels " + problems + " (Initial problems: " + cc.getNumProblems() + ")");
				List<String> removeProblems = new ArrayList<String>();
				IProblem[] probs = cc.getProblems();

				Pattern pattern = Pattern.compile(problems.trim());
				for (int i = 0; i < probs.length; i++) {
					if (!pattern.matcher(probs[i].getLabel()).matches())
						removeProblems.add(probs[i].getId());
				}
				cc.removeProblems(removeProblems);

				Trace.trace(Trace.INFO, "Resolved for problems labels. Problems left: " + cc.getNumProblems());
			}

			if (problemIds != null) {
				Trace.trace(Trace.INFO,
						"Resolving for problem ids " + problemIds + " (Initial problems: " + cc.getNumProblems() + ")");
				List<String> removeProblems = new ArrayList<String>();
				IProblem[] probs = cc.getProblems();

				Pattern pattern = Pattern.compile(problemIds.trim());
				for (int i = 0; i < probs.length; i++) {
					if (!pattern.matcher(probs[i].getId()).matches())
						removeProblems.add(probs[i].getId());
				}
				cc.removeProblems(removeProblems);

				Trace.trace(Trace.INFO, "Resolved for problems ids. Problems left: " + cc.getNumProblems());
			}

			// error if no teams
			if (cc.getTeams().length == 0) {
				Trace.trace(Trace.ERROR, "Contest has no teams, exiting.");
				System.exit(2);
			}

			if (cc.getProblems().length == 0) {
				Trace.trace(Trace.ERROR, "Contest has no problems, exiting.");
				System.exit(2);
			}

			if (!hasAwards) {
				Trace.trace(Trace.USER, "Generating awards");
				AwardUtil.createDefaultAwards(cc);
			}

			// create the official scoreboard
			cc.officialResults();

			ResolverLogic logic = new ResolverLogic(cc, show_info);

			long time = System.currentTimeMillis();
			List<ResolutionStep> subSteps = logic.resolveFrom(judgeQueue);
			steps.addAll(subSteps);
			outputStats(steps, time, con);
		} else {
			IAward[] contestAwards = finalContest[con].getAwards();
			if (contestAwards == null || contestAwards.length == 0) {
				Trace.trace(Trace.USER, "Generating awards");
				AwardUtil.createDefaultAwards(finalContest[con]);
			}

			// create the official scoreboard
			finalContest[con].officialResults();

			ResolverLogic logic = new ResolverLogic(finalContest[con], show_info);

			int showHour = -1;
			if (showHour > 0) {
				// IInfo info = finalContest.getInfo();
				// ((Info)info).setFreezeDuration();
				// contestTime = (showHour - 1) * 3600;
			}

			long time = System.currentTimeMillis();
			List<ResolutionStep> subSteps = logic.resolveFrom(judgeQueue);
			steps.addAll(subSteps);
			outputStats(steps, time, con);
		}
	}

	protected String getPlace(String place) {
		try {
			int n = Integer.parseInt(place);
			return AwardUtil.getPlaceString(n);
		} catch (Exception e) {
			return place;
		}
	}

	protected ResolverUI createUI(List<ResolutionStep> steps) {
		ResolverUI ui2 = new ResolverUI(show_info, isPresenter, screen, new ClickListener() {
			@Override
			public void clicked(int num) {
				sendClicks(num);
			}

			@Override
			public void scroll(boolean pause) {
				sendScroll(pause);
			}

			@Override
			public void speedFactor(double d) {
				sendSpeedFactor(d);
			}

			@Override
			public void scrollSpeedFactor(double d) {
				sendScrollSpeedFactor(d);
			}

			@Override
			public void swap() {
				if (ui.length == 1)
					return;

				ui[activeUI].setActive(false, activeUI);
				activeUI++;
				if (activeUI >= ui.length)
					activeUI = 0;

				Trace.trace(Trace.USER, "Switching to contest " + activeUI);
				sendActiveUI();

				ui[activeUI].setActive(true, activeUI);
			}
		});

		ui2.setup(steps);
		return ui2;
	}

	protected void launch() {
		BufferedImage iconImage = null;
		try {
			iconImage = ImageIO.read(getClass().getClassLoader().getResource("images/resolverIcon.png"));
		} catch (Exception e) {
			// could not set title or icon
		}
		PresentationWindow window = PresentationWindow.open("Resolver", iconImage);
		((PresentationWindowImpl) window).setLightMode(lightMode);

		DisplayConfig displayConfig = new DisplayConfig(displayStr, multiDisplayStr);
		try {
			window.setDisplayConfig(displayConfig);
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Invalid display option: " + displayConfig + " " + e.getMessage());
		}
		window.setControlable(false);

		// display (initialize) any secondary contests and set them inactive
		for (int i = 1; i < ui.length; i++) {
			ui[i].display(window);
			ui[i].setActive(false, i);
		}
		// display the primary contest
		ui[0].display(window);
	}

	private void outputStats(List<ResolutionStep> steps, long time, int con) {
		double totalTime = ResolutionUtil.getTotalTime(steps);
		long min = ((int) totalTime) / 60;
		long seconds = ((int) totalTime) % 60;
		Trace.trace(Trace.USER, "  Playback time: " + min + "m " + seconds + "s");
		IResolveInfo resolveInfo = finalContest[con].getResolveInfo();
		double speedFactor = 1.0;
		if (resolveInfo != null) {
			speedFactor = resolveInfo.getSpeedFactor();
		}
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

	private void sendSettings(ResolveInfo resolveInfo) {
		putResolve(resolveInfo);
	}

	private void sendClicks(int clicks2) {
		if (!isPresenter || client == null)
			return;

		ResolveInfo resolveInfo = new ResolveInfo();
		resolveInfo.setClicks(clicks2);
		putResolve(resolveInfo);
	}

	private void sendScroll(boolean pause) {
		if (!isPresenter || client == null)
			return;

		ResolveInfo resolveInfo = new ResolveInfo();
		resolveInfo.setAnimationPause(pause);
		putResolve(resolveInfo);
	}

	private void sendSpeedFactor(double factor) {
		if (!isPresenter || client == null)
			return;

		ResolveInfo resolveInfo = new ResolveInfo();
		resolveInfo.setSpeedFactor(factor);
		putResolve(resolveInfo);
	}

	private void sendScrollSpeedFactor(double factor) {
		if (!isPresenter || client == null)
			return;

		ResolveInfo resolveInfo = new ResolveInfo();
		resolveInfo.setScrollSpeedFactor(factor);
		putResolve(resolveInfo);
	}

	private void putResolve(ResolveInfo resolveInfo) {
		if (contestSources[activeUI] instanceof RESTContestSource) {
			try {
				Trace.trace(Trace.INFO, "PUTting resolve info: " + resolveInfo.toString());
				((RESTContestSource) contestSources[activeUI]).put(resolveInfo);
			} catch (Exception ex) {
				Trace.trace(Trace.WARNING, "Failed to set resolve", ex);
			}
		} else {
			finalContest[activeUI].add(resolveInfo);
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
		Long freeze = contest.getFreezeDuration();
		if (freeze == null || freeze < 0 || freeze > contest.getDuration())
			Trace.trace(Trace.WARNING, "Warning: Contest has no freeze time, will assume default");
	}
}
