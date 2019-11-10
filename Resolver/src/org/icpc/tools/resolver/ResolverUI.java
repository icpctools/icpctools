package org.icpc.tools.resolver;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.TeamUtil.Style;
import org.icpc.tools.presentation.contest.internal.presentations.StaticLogoPresentation;
import org.icpc.tools.presentation.contest.internal.scoreboard.ScoreboardPresentation;
import org.icpc.tools.presentation.core.IPresentationHandler.DeviceMode;
import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.PresentationWindow;
import org.icpc.tools.resolver.ResolutionUtil.AwardStep;
import org.icpc.tools.resolver.ResolutionUtil.ContestStateStep;
import org.icpc.tools.resolver.ResolutionUtil.DelayStep;
import org.icpc.tools.resolver.ResolutionUtil.PauseStep;
import org.icpc.tools.resolver.ResolutionUtil.PresentationStep;
import org.icpc.tools.resolver.ResolutionUtil.ResolutionStep;
import org.icpc.tools.resolver.ResolutionUtil.ScrollStep;
import org.icpc.tools.resolver.ResolutionUtil.ScrollTeamListStep;
import org.icpc.tools.resolver.ResolutionUtil.SubmissionSelectionStep;
import org.icpc.tools.resolver.ResolutionUtil.SubmissionSelectionStep2;
import org.icpc.tools.resolver.ResolutionUtil.TeamListStep;
import org.icpc.tools.resolver.ResolutionUtil.TeamSelectionStep;
import org.icpc.tools.resolver.ResolutionUtil.ToJudgeStep;

/**
 * A Resolver contains two types of Presentations: a "ScoreboardPresentation", which is a grid
 * showing teams and the problems they've solved; and a "TeamAwardPresentation", which is a
 * separate display showing a picture of the team (if available) along with a list of the award(s)
 * they've earned. The Resolver displays the ProblemsPresentation by default while it goes through
 * the "resolve" process, then switches to an appropriate TeamAwardPresentation each time a team
 * arrives at the point in the process where they should receive their award. A Resolver also
 * optionally (and by default) contains a third "SplashPresentation" which is displayed on startup.
 */
public class ResolverUI {
	public static interface ClickListener {
		public void clicked(int num);

		public void scroll(boolean pause);

		public void speedFactor(double d);
	}

	private static enum Action {
		FORWARD, REVERSE, FAST_FORWARD, FAST_REVERSE, BEGIN
	}

	public static enum Screen {
		MAIN, TEAM, SIDE
	}

	private static final String[] MESSAGES = new String[] { "John, click me!", "I'm waiting for you...",
			"To pause dramatic; to click divine", "CLICK ME PLEASE!", "Are you still there?",
			"Loading, please wait... (just kidding - click me)", "DO NOT TOUCH ANYTHING! (also just kidding)",
			"I dozed off.... did you?", "Anybody out there? If so, please click!", "Hello.......?",
			"Gimme a C! Gimme a L! Or maybe just a click..." };
	private Font messageFont;
	private int messageNum;

	private int currentPause;
	private int currentStep;
	private boolean isPresenter;
	private Screen screen = Screen.MAIN;
	private String displayStr;
	private double speedFactor;
	private boolean showInfo;
	private boolean pauseScroll = false;
	private Style style;

	private ClickListener listener;

	private List<ResolutionStep> steps = new ArrayList<>();

	private PresentationWindow window;
	private AbstractICPCPresentation splashPresentation;
	private ScoreboardPresentation scoreboardPresentation;
	private TeamAwardPresentation awardPresentation;
	private TeamLogoPresentation teamLogoPresentation;
	private TeamListPresentation teamListPresentation;
	private TeamListSidePresentation teamListSidePresentation;
	private JudgePresentation2 judgePresentation;
	private StaticLogoPresentation logoPresentation;
	private Presentation currentPresentation;

	private long lastClickTime = -1;
	private Thread thread;

	public ResolverUI(List<ResolutionStep> steps, boolean showInfo, String display, boolean isPresenter, Screen screen,
			ClickListener listener, Style style) {
		this.steps = steps;
		this.showInfo = showInfo;
		this.displayStr = display;
		this.isPresenter = isPresenter;
		this.screen = screen;
		if (screen == null)
			this.screen = Screen.MAIN;
		this.listener = listener;
		this.style = style;
	}

	public void moveTo(int pause2) {
		// if thread is still running, ignore
		if (thread != null || messageFont == null)
			return;

		// fire off a new thread to handle the click
		thread = new Thread("Resolver") {
			@Override
			public void run() {
				moveToImpl(pause2);
				thread = null;
			}
		};
		thread.setDaemon(true);
		thread.start();
	}

	private void moveToImpl(int pause2) {
		int pause = pause2;
		Trace.trace(Trace.INFO, "Move to: " + currentPause + " " + pause);

		boolean includeDelays = true;
		if (pause > 999) {
			pause -= 1000;
			includeDelays = false;
		} else if (Math.abs(pause - currentPause) > 1)
			includeDelays = false;

		while (currentPause < pause)
			forward(includeDelays);

		while (currentPause > pause)
			reverse(true, includeDelays);
	}

	public void display() {
		if (steps == null || steps.isEmpty())
			throw new IllegalArgumentException("Nothing to resolve");

		Contest contest = getFirstContest();
		if (contest == null)
			throw new IllegalArgumentException("No contest to resolve");

		ContestUtil.flashPending = false;

		BufferedImage iconImage = null;
		try {
			iconImage = ImageIO.read(getClass().getClassLoader().getResource("images/resolverIcon.png"));
		} catch (Exception e) {
			// could not set title or icon
		}
		window = PresentationWindow.open("Resolver", iconImage);

		try {
			window.setWindow(new DeviceMode(displayStr));
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Invalid display option: " + displayStr + " " + e.getMessage());
		}

		window.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (teamListPresentation == null || !isPresenter)
					return;

				if ('r' == e.getKeyChar() || 'R' == e.getKeyChar() || 'b' == e.getKeyChar() || 'B' == e.getKeyChar())
					processAction(Action.REVERSE);
				else if ('1' == e.getKeyChar())
					processAction(Action.FAST_REVERSE);
				else if ('2' == e.getKeyChar())
					processAction(Action.FAST_FORWARD);
				else if ('0' == e.getKeyChar())
					processAction(Action.BEGIN);
				else if ('+' == e.getKeyChar() || '=' == e.getKeyChar() || KeyEvent.VK_UP == e.getKeyCode())
					setSpeedFactorImpl(speedFactor * 0.8);
				else if ('-' == e.getKeyChar() || '_' == e.getKeyChar() || KeyEvent.VK_DOWN == e.getKeyCode())
					setSpeedFactorImpl(speedFactor * 1.2);
				else if ('j' == e.getKeyChar() || 'J' == e.getKeyChar())
					setSpeedFactorImpl(1.0);
				else if ('p' == e.getKeyChar()) {
					setScrollPauseImpl(!pauseScroll);
				} else if ('i' == e.getKeyChar())
					scoreboardPresentation.setShowSubmissionInfo(!scoreboardPresentation.getShowSubmissionInfo());
				else if (' ' == e.getKeyChar() || 'f' == e.getKeyChar() || 'F' == e.getKeyChar())
					processAction(Action.FORWARD);
			}
		});

		window.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (teamListPresentation == null || !isPresenter)
					return;

				processAction(Action.FORWARD);
			}
		});

		MouseAdapter nullMouse = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// ignore
			}
		};

		if (screen == Screen.TEAM || screen == Screen.SIDE) {
			logoPresentation = new StaticLogoPresentation();
			logoPresentation.addMouseListener(nullMouse);
			splashPresentation = logoPresentation;

			if (screen == Screen.SIDE) {
				teamLogoPresentation = new TeamLogoPresentation();
				teamLogoPresentation.addMouseListener(nullMouse);

				teamListSidePresentation = new TeamListSidePresentation();
				teamListSidePresentation.addMouseListener(nullMouse);
			}
		} else {
			splashPresentation = new SplashPresentation();
			splashPresentation.setContest(contest);
			splashPresentation.addMouseListener(nullMouse);
		}

		scoreboardPresentation = new ScoreboardPresentation() {
			@Override
			public String getTitle() {
				return null;
			}

			@Override
			public void paint(Graphics2D g) {
				super.paint(g);
				paintHook(g);
			}
		};
		scoreboardPresentation.setScrollToRow(-1);
		scoreboardPresentation.setShowLegend(true); // show legend initially
		if (showInfo)
			scoreboardPresentation.setShowSubmissionInfo(true);
		scoreboardPresentation.setProperty("clockOff");
		scoreboardPresentation.setContest(contest);
		scoreboardPresentation.addMouseListener(nullMouse);
		if (style != null)
			scoreboardPresentation.setStyle(style);

		judgePresentation = new JudgePresentation2() {
			@Override
			public void paint(Graphics2D g) {
				super.paint(g);
				paintHook(g);
			}
		};
		judgePresentation.addMouseListener(nullMouse);
		if (style != null)
			judgePresentation.setStyle(style);

		awardPresentation = new TeamAwardPresentation() {
			@Override
			public void paint(Graphics2D g) {
				super.paint(g);
				paintHook(g);
			}
		};
		awardPresentation.setSize(window.getSize());
		awardPresentation.cacheAwards(contest, steps);
		awardPresentation.addMouseListener(nullMouse);
		awardPresentation.setShowInfo(showInfo);

		teamListPresentation = new TeamListPresentation() {
			@Override
			public void paint(Graphics2D g) {
				super.paint(g);
				paintHook(g);
			}
		};
		teamListPresentation.setSize(window.getSize());
		teamListPresentation.loadCache(ResolutionUtil.getTeamListIds(steps));
		teamListPresentation.addMouseListener(nullMouse);
		teamListPresentation.setContest(contest);
		if (style != null)
			teamListPresentation.setStyle(style);

		final float dpi = 96;
		float size = (window.getHeight() / 14f) * 36f / dpi;
		messageFont = ICPCFont.getMasterFont().deriveFont(Font.PLAIN, size);

		processAction(Action.FORWARD);
	}

	private String getStatusInfo() {
		StringBuilder sb = new StringBuilder();
		if (thread != null && thread.isAlive())
			sb.append("Processing: ");
		else
			sb.append("Next step: ");

		int n = currentStep + 1;
		while (n < steps.size() && !(steps.get(n) instanceof PauseStep)) {
			n++;
		}

		// what was the previous thing?
		ResolutionStep step = steps.get(n - 1);
		sb.append(step.toString());

		return sb.toString();
	}

	private void paintHook(Graphics2D g) {
		if (!showInfo)
			return;

		try {
			// show what's next
			String s = getStatusInfo();
			Dimension d = scoreboardPresentation.getSize();
			g.setFont(messageFont);
			FontMetrics fm = g.getFontMetrics();
			g.setComposite(AlphaComposite.SrcOver.derive(0.7f));
			g.setColor(Color.BLACK);
			g.fillRect(0, d.height - fm.getHeight() - 10, d.width, fm.getHeight() + 10);
			g.setComposite(AlphaComposite.SrcOver);
			g.setColor(Color.WHITE);
			g.drawString(s, 10, d.height - fm.getDescent() - 5);

			// poke fun at John
			if (thread != null && thread.isAlive())
				return;

			String user = System.getProperty("user");
			if (user == null || !user.equals("clevenger"))
				return;

			g.setFont(messageFont);
			fm = g.getFontMetrics();
			s = MESSAGES[messageNum];

			g.setComposite(AlphaComposite.SrcOver.derive(0.6f));
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, fm.stringWidth(s) + 20, fm.getAscent() + 20);
			g.setComposite(AlphaComposite.SrcOver);
			g.setColor(Color.WHITE);
			g.drawRect(0, 0, fm.stringWidth(s) + 20, fm.getAscent() + 20);
			g.drawString(s, 10, fm.getAscent() + 10);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error painting message overlay", e);
		}
	}

	private void setSpeedFactorImpl(double d) {
		if (listener != null)
			listener.speedFactor(d);

		speedFactor = d;
	}

	public void setSpeedFactor(double d) {
		speedFactor = d;
	}

	private void setScrollPauseImpl(boolean pause) {
		if (pause == pauseScroll)
			return;

		if (listener != null)
			listener.scroll(pause);

		setScrollPause(pause);
	}

	public void setScrollPause(boolean pause) {
		pauseScroll = pause;
		if (teamListPresentation != null)
			teamListPresentation.setScrollPause(pause);
		if (scoreboardPresentation != null)
			scoreboardPresentation.setScrollPause(pause);
	}

	private Contest getFirstContest() {
		int i = 0;
		while (i < steps.size()) {
			ResolutionStep step = steps.get(i);
			if (step instanceof ContestStateStep)
				return ((ContestStateStep) step).contest;
			i++;
		}

		return null;
	}

	private void forward(boolean includeDelays) {
		if (currentStep == steps.size() - 1)
			return;

		long[] startTime = new long[] { System.nanoTime() };
		if (!includeDelays || screen != Screen.MAIN)
			startTime = null;

		if (listener != null) {
			// notify listener which pause we're going to
			if (!includeDelays)
				listener.clicked(currentPause + 1 + 1000);
			else
				listener.clicked(currentPause + 1);
		}

		while (currentStep < steps.size() - 1) {
			currentStep++;
			ResolutionStep step = steps.get(currentStep);
			Trace.trace(Trace.INFO, "Step " + currentStep + " > " + step);

			int num = processStep(startTime, step);
			if (num >= 0) {
				currentPause = num;
				return;
			}
		}
	}

	private void reverse(boolean stopAtPause, boolean includeDelays) {
		if (currentStep == 0)
			return;

		long[] startTime = new long[] { System.nanoTime() };
		if (!includeDelays || screen != Screen.MAIN)
			startTime = null;

		if (listener != null) {
			// notify listener which pause we're going to
			if (!stopAtPause && !includeDelays) {
				listener.clicked(1000);
			} else {
				if (!includeDelays)
					listener.clicked(currentPause - 1 + 1000);
				else
					listener.clicked(currentPause - 1);
			}
		}

		while (currentStep > 0) {
			currentStep--;
			ResolutionStep step = steps.get(currentStep);

			ResolutionStep previousStep = findPrevious(currentStep, step);
			if (step.equals(previousStep))
				Trace.trace(Trace.INFO, "Step " + currentStep + " <  " + step);
			else
				Trace.trace(Trace.INFO, "Step " + currentStep + " <  " + step + " reverted to " + previousStep);

			int num = processStep(startTime, previousStep);
			if (num >= 0) {
				currentPause = num;
				if (stopAtPause)
					return;
			}
		}
	}

	private ResolutionStep findPrevious(int stepNum, ResolutionStep step) {
		if (step instanceof DelayStep || step instanceof PauseStep)
			return step;

		Class<? extends ResolutionStep> cl = step.getClass();
		int num = stepNum;
		while (num > 0) {
			num--;
			ResolutionStep step2 = steps.get(num);
			if (cl.isInstance(step2))
				return step2;
		}
		return step;
	}

	private int processStep(long[] startTime, ResolutionStep step) {
		if (step instanceof ContestStateStep) {
			ContestStateStep state = (ContestStateStep) step;
			splashPresentation.setContest(state.contest);
			scoreboardPresentation.setContest(state.contest);
			judgePresentation.setContest(state.contest);
			awardPresentation.setContest(state.contest);
			if (teamLogoPresentation != null)
				teamLogoPresentation.setContest(state.contest);
		} else if (step instanceof PauseStep) {
			PauseStep pause = (PauseStep) step;
			return pause.num;
		} else if (step instanceof DelayStep) {
			DelayStep delay = (DelayStep) step;
			if (startTime != null)
				wait(startTime, delay.type.ordinal());
		} else if (step instanceof TeamSelectionStep) {
			TeamSelectionStep sel = (TeamSelectionStep) step;
			scoreboardPresentation.setSelectedTeams(sel.teams, sel.type);
		} else if (step instanceof SubmissionSelectionStep2) {
			SubmissionSelectionStep2 sel = (SubmissionSelectionStep2) step;
			judgePresentation.handleSubmission(sel.submissionId);
			judgePresentation.setSelectedSubmissionId(sel.submissionId);
		} else if (step instanceof ToJudgeStep) {
			ToJudgeStep sel = (ToJudgeStep) step;
			judgePresentation.handleSubmissions(sel.submissionIds);
		} else if (step instanceof SubmissionSelectionStep) {
			SubmissionSelectionStep sel = (SubmissionSelectionStep) step;
			scoreboardPresentation.setSelectedSubmission(sel.subInfo);
			judgePresentation.setSelectedSubmission(sel.subInfo);
		} else if (step instanceof AwardStep) {
			AwardStep award = (AwardStep) step;
			awardPresentation.setTeam(award.teamId);
			if (teamLogoPresentation != null)
				teamLogoPresentation.setTeam(award.teamId);
		} else if (step instanceof TeamListStep) {
			TeamListStep teamList = (TeamListStep) step;
			teamListPresentation.setTeams(teamList);
			if (teamListSidePresentation != null)
				teamListSidePresentation.setTeams(teamList);
		} else if (step instanceof ScrollTeamListStep) {
			ScrollTeamListStep scroll = (ScrollTeamListStep) step;
			teamListPresentation.scrollIt(scroll.top);
		} else if (step instanceof ScrollStep) {
			ScrollStep scroll = (ScrollStep) step;
			scoreboardPresentation.setScrollToRow(scroll.row);
		} else if (step instanceof PresentationStep) {
			PresentationStep pstep = (PresentationStep) step;
			if (pstep.p == PresentationStep.Presentations.SPLASH)
				setPresentation(splashPresentation);
			else if (pstep.p == PresentationStep.Presentations.SCOREBOARD)
				setPresentation(scoreboardPresentation);
			else if (pstep.p == PresentationStep.Presentations.JUDGE)
				setPresentation(judgePresentation);
			else if (pstep.p == PresentationStep.Presentations.TEAM_AWARD)
				setPresentation(awardPresentation);
			else if (pstep.p == PresentationStep.Presentations.TEAM_LIST)
				setPresentation(teamListPresentation);
		} else {
			Trace.trace(Trace.ERROR, "Unknown resolution step!");
			System.exit(1);
		}
		return -1;
	}

	/**
	 * Wait the specified time in seconds. The specified time is adjusted by the current
	 * "speedfactor" before waiting (for example, if a wait of 1.5 seconds is requested and the
	 * current speedfactor is 0.5, the actual wait time will be 1.5*0.5 = 0.75 seconds.
	 *
	 * @param startTime - an array of 1 containing the system nano time to start the delay from (the
	 *           current nano time may be later)
	 * @param type - the type of pause to take
	 */
	private void wait(long[] startTime, int type) {
		long delay = Math.round(ResolutionUtil.DELAY_TIMES[type] * speedFactor * 1000000000.0);
		startTime[0] += delay;
		LockSupport.parkNanos(startTime[0] - System.nanoTime());
	}

	private void processAction(final Action action) {
		// if user clicks twice within 0.2s (or 0.4s when advancing normally), ignore - it's likely
		// an accidental click
		long now = System.currentTimeMillis();
		if (lastClickTime != -1 && (lastClickTime > now - 200) || (action == Action.FORWARD && lastClickTime > now - 400))
			return;

		lastClickTime = now;

		// if thread is still running, ignore
		if (thread != null)
			return;

		// fire off a new thread to handle the click
		thread = new Thread("Resolver") {
			@Override
			public void run() {
				actionImpl(action);
				thread = null;
			}
		};

		messageNum++;
		messageNum %= MESSAGES.length;

		thread.setDaemon(true);
		thread.start();

		setScrollPauseImpl(false);
	}

	private void actionImpl(Action action) {
		Trace.trace(Trace.INFO, "**Action: " + action.name() + " from " + currentPause + "**");

		if (action == Action.FORWARD) {
			forward(true);
		} else if (action == Action.REVERSE) {
			reverse(true, true);
		} else if (action == Action.FAST_FORWARD) {
			forward(false);
		} else if (action == Action.FAST_REVERSE) {
			reverse(true, false);
		} else if (action == Action.BEGIN) {
			reverse(false, false);
		}
		Trace.trace(Trace.INFO, "**Paused at " + currentPause + "**");
	}

	private void setPresentation(Presentation pres) {
		if (currentPresentation == pres)
			return;

		Presentation pres2 = pres;
		if (screen == Screen.TEAM && pres == scoreboardPresentation) {
			pres2 = logoPresentation;
		} else if (screen == Screen.SIDE) {
			if (pres == scoreboardPresentation || pres == judgePresentation)
				pres2 = logoPresentation;
			if (pres == teamListPresentation)
				pres2 = teamListSidePresentation;
			if (pres == awardPresentation)
				pres2 = teamLogoPresentation;
		}

		if (currentPresentation == pres2)
			return;

		currentPresentation = pres2;
		window.setPresentation(pres2);
	}
}