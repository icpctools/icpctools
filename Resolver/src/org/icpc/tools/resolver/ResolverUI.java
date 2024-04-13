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

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IResolveInfo;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.resolver.ResolutionControl;
import org.icpc.tools.contest.model.resolver.ResolutionControl.IResolutionListener;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.AwardStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ContestStateStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ListAwardStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.PauseStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.PresentationStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ResolutionStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ScrollStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ScrollTeamListStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.SubmissionSelectionStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.SubmissionSelectionStep2;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.TeamSelectionStep;
import org.icpc.tools.contest.model.resolver.ResolutionUtil.ToJudgeStep;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.TextHelper;
import org.icpc.tools.presentation.contest.internal.presentations.BrandingPresentation;
import org.icpc.tools.presentation.contest.internal.presentations.MessagePresentation;
import org.icpc.tools.presentation.contest.internal.presentations.StaticLogoPresentation;
import org.icpc.tools.presentation.contest.internal.presentations.resolver.JudgePresentation2;
import org.icpc.tools.presentation.contest.internal.presentations.resolver.OrgsPresentation;
import org.icpc.tools.presentation.contest.internal.presentations.resolver.SplashPresentation;
import org.icpc.tools.presentation.contest.internal.presentations.resolver.TeamAwardPresentation;
import org.icpc.tools.presentation.contest.internal.presentations.resolver.TeamListPresentation;
import org.icpc.tools.presentation.contest.internal.presentations.resolver.TeamLogoPresentation;
import org.icpc.tools.presentation.contest.internal.scoreboard.ScoreboardPresentation;
import org.icpc.tools.presentation.core.DisplayConfig;
import org.icpc.tools.presentation.core.Presentation;
import org.icpc.tools.presentation.core.PresentationWindow;
import org.icpc.tools.presentation.core.internal.PresentationWindowImpl;

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

		public void scrollSpeedFactor(double d);

		public void swap();
	}

	private static enum Action {
		FORWARD, REVERSE, FAST_FORWARD, FAST_REVERSE, BEGIN
	}

	public static enum Screen {
		MAIN, TEAM, SIDE, ORG
	}

	private static final String[] MESSAGES = new String[] { "John, click me!", "I'm waiting for you...",
			"To pause dramatic; to click divine", "CLICK ME PLEASE!", "Are you still there?",
			"Loading, please wait... (just kidding - click me)", "DO NOT TOUCH ANYTHING! (also just kidding)",
			"I dozed off.... did you?", "Anybody out there? If so, please click!", "Hello.......?",
			"Gimme a C! Gimme a L! Or maybe just a click..." };
	private Font messageFont;
	private int messageNum;

	private ResolutionControl control;
	private boolean isPresenter;
	private Screen screen = Screen.MAIN;
	private DisplayConfig displayConfig;
	private boolean showInfo;
	private boolean pauseScroll = false;

	private ClickListener listener;

	private List<ResolutionStep> steps = new ArrayList<>();

	private PresentationWindow window;
	private AbstractICPCPresentation splashPresentation;
	private ScoreboardPresentation scoreboardPresentation;
	private TeamAwardPresentation awardPresentation;
	private TeamLogoPresentation teamLogoPresentation;
	private TeamListPresentation teamListPresentation;
	private MessagePresentation messagePresentation;
	private JudgePresentation2 judgePresentation;
	private StaticLogoPresentation logoPresentation;
	private OrgsPresentation orgPresentation;
	private Presentation currentPresentation;

	private long lastClickTime = -1;
	private Thread thread;
	private boolean light;
	private int firstStep = -1;

	public ResolverUI(boolean showInfo, DisplayConfig displayConfig, boolean isPresenter, Screen screen,
			ClickListener listener, boolean light) {
		this.showInfo = showInfo;
		this.displayConfig = displayConfig;
		this.isPresenter = isPresenter;
		this.screen = screen;
		if (screen == null)
			this.screen = Screen.MAIN;
		this.listener = listener;
		this.light = light;
	}

	public void setup(List<ResolutionStep> steps) {
		this.steps = steps;

		control = new ResolutionControl(steps);
		IContest contest = getFirstContest();
		IResolveInfo resolveInfo = contest.getResolveInfo();
		if (resolveInfo != null) {
			if (!Double.isNaN(resolveInfo.getSpeedFactor()))
				setSpeedFactor(resolveInfo.getSpeedFactor());

			if (!Double.isNaN(resolveInfo.getScrollSpeedFactor()))
				setScrollSpeedFactor(resolveInfo.getScrollSpeedFactor());

			if (resolveInfo.getClicks() >= 0)
				firstStep = resolveInfo.getClicks() % 1000 + 1000;
		}

		control.addListener(new IResolutionListener() {
			@Override
			public void toPause(int pause, boolean includeDelays) {
				if (listener == null)
					return;

				if (!includeDelays)
					listener.clicked(pause + 1000);
				else
					listener.clicked(pause);
			}

			@Override
			public void atStep(ResolutionStep step) {
				processStep(step);
			}

			@Override
			public void atPause(int pause) {
				// ignore
			}
		});
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
		int currentPause = control.getCurrentPause();
		Trace.trace(Trace.INFO, "Move to: " + currentPause + " -> " + pause);

		boolean includeDelays = true;
		if (pause > 999) {
			pause -= 1000;
			includeDelays = false;
		} else if (Math.abs(pause - currentPause) > 1)
			includeDelays = false;

		control.moveToPause(pause, includeDelays);
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
		((PresentationWindowImpl) window).setLightMode(light);

		try {
			window.setDisplayConfig(displayConfig);
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Invalid display option: " + displayConfig + " " + e.getMessage());
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
					setSpeedFactorImpl(control.getSpeedFactor() * 0.8);
				else if ('-' == e.getKeyChar() || '_' == e.getKeyChar() || KeyEvent.VK_DOWN == e.getKeyCode())
					setSpeedFactorImpl(control.getSpeedFactor() * 1.2); // TODO should only affect the
																							// 'current' animation??
				else if ('[' == e.getKeyChar() || '{' == e.getKeyChar())
					setScrollSpeedFactorImpl(control.getScrollSpeedFactor() * 0.8);
				else if (']' == e.getKeyChar() || '}' == e.getKeyChar())
					setScrollSpeedFactorImpl(control.getScrollSpeedFactor() * 1.2);
				else if ('j' == e.getKeyChar() || 'J' == e.getKeyChar()) {
					// TODO should reset to what the command line is??
					setSpeedFactorImpl(1.0);
					setScrollSpeedFactor(1.0);
				} else if ('p' == e.getKeyChar()) {
					setScrollPauseImpl(!pauseScroll);
				} else if ('i' == e.getKeyChar())
					scoreboardPresentation.setShowSubmissionInfo(!scoreboardPresentation.getShowSubmissionInfo());
				else if (' ' == e.getKeyChar() || 'f' == e.getKeyChar() || 'F' == e.getKeyChar())
					processAction(Action.FORWARD);

				else if ('s' == e.getKeyChar() && listener != null)
					listener.swap();
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
		window.setControlable(false);

		if (screen == Screen.TEAM || screen == Screen.SIDE || screen == Screen.ORG) {
			logoPresentation = new StaticLogoPresentation();
			splashPresentation = logoPresentation;

			if (screen == Screen.SIDE) {
				teamLogoPresentation = new TeamLogoPresentation();

				messagePresentation = new MessagePresentation();
			}
			if (screen == Screen.ORG) {
				orgPresentation = new OrgsPresentation();
				orgPresentation.setContest(contest);
			}
		} else {
			splashPresentation = new SplashPresentation();
			splashPresentation.setContest(contest);
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

		judgePresentation = new JudgePresentation2() {
			@Override
			public void paint(Graphics2D g) {
				super.paint(g);
				paintHook(g);
			}
		};
		judgePresentation.setProperty("clockOff");

		awardPresentation = new TeamAwardPresentation() {
			@Override
			public void paint(Graphics2D g) {
				super.paint(g);
				paintHook(g);
			}
		};
		awardPresentation.setSize(window.getSize());
		awardPresentation.cacheAwards(steps);
		awardPresentation.setShowInfo(showInfo);

		teamListPresentation = new TeamListPresentation() {
			@Override
			public void paint(Graphics2D g) {
				super.paint(g);
				paintHook(g);
			}
		};
		teamListPresentation.setSize(window.getSize());
		teamListPresentation.setContest(contest);

		final float dpi = 96;
		float size = (window.getHeight() / 14f) * 36f / dpi;
		messageFont = ICPCFont.deriveFont(Font.PLAIN, size);

		if (firstStep >= 0)
			moveTo(firstStep);
		else
			moveTo(0);
	}

	public void setVisible(boolean b) {
		if (messageFont == null)
			display();

		window.setVisible(b);
	}

	private String getStatusInfo() {
		StringBuilder sb = new StringBuilder();
		if (thread != null && thread.isAlive())
			sb.append("Processing: ");
		else
			sb.append("Next step: ");

		int n = control.getCurrentStep() + 1;
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
			TextHelper.drawString(g, s, 10, d.height - fm.getDescent() - 5);

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

		control.setSpeedFactor(d);
	}

	public void setSpeedFactor(double d) {
		control.setSpeedFactor(d);
	}

	private void setScrollSpeedFactorImpl(double d) {
		if (listener != null)
			listener.scrollSpeedFactor(d);

		control.setScrollSpeedFactor(d);

		setScrollSpeedFactor(d);
	}

	public void setScrollSpeedFactor(double d) {
		control.setScrollSpeedFactor(d);

		if (teamListPresentation != null)
			teamListPresentation.setScrollSpeed(d);

		if (scoreboardPresentation != null)
			scoreboardPresentation.setScrollSpeed(d);
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

	private int processStep(ResolutionStep step) {
		if (step instanceof ContestStateStep) {
			ContestStateStep state = (ContestStateStep) step;
			splashPresentation.setContest(state.contest);
			scoreboardPresentation.setContest(state.contest);
			teamListPresentation.setContest(state.contest);
			judgePresentation.setContest(state.contest);
			awardPresentation.setContest(state.contest);
			if (teamLogoPresentation != null)
				teamLogoPresentation.setContest(state.contest);
			if (orgPresentation != null)
				orgPresentation.setContest(state.contest);
		} else if (step instanceof TeamSelectionStep) {
			TeamSelectionStep sel = (TeamSelectionStep) step;
			scoreboardPresentation.setSelectedTeams(sel.teams, sel.type);
			if (orgPresentation != null)
				orgPresentation.setSelectedTeams(sel.teams, sel.type);
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
		} else if (step instanceof ListAwardStep) {
			ListAwardStep teamList = (ListAwardStep) step;
			teamListPresentation.setAward(teamList.award);
			if (messagePresentation != null)
				messagePresentation.setProperty(teamList.award.getCitation());
		} else if (step instanceof ScrollTeamListStep) {
			ScrollTeamListStep scroll = (ScrollTeamListStep) step;
			teamListPresentation.scrollIt(scroll.top);
		} else if (step instanceof ScrollStep) {
			ScrollStep scroll = (ScrollStep) step;
			IContest contest = getFirstContest();
			IResolveInfo resolveInfo = contest.getResolveInfo();
			int rowOffset = 0;
			if (resolveInfo != null && resolveInfo.getRowOffset() >= 0)
				rowOffset = resolveInfo.getRowOffset();
			int row = Math.max(0, scroll.row - scoreboardPresentation.getNumRows() - rowOffset + 3);
			scoreboardPresentation.setScrollToRow(row);
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
		int currentPause = control.getCurrentPause();
		Trace.trace(Trace.INFO, "**Action: " + action.name() + " from " + currentPause + "**");

		if (action == Action.FORWARD || action == Action.FAST_FORWARD)
			control.forward(action == Action.FORWARD);
		else if (action == Action.REVERSE || action == Action.FAST_REVERSE)
			control.rewind(action == Action.REVERSE);
		else if (action == Action.BEGIN)
			control.reset();

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
				pres2 = messagePresentation;
			if (pres == awardPresentation)
				pres2 = teamLogoPresentation;
		} else if (screen == Screen.ORG) {
			pres2 = orgPresentation;
		}

		if (currentPresentation == pres2)
			return;

		currentPresentation = pres2;

		try {
			String brand = System.getProperty("ICPC_BRANDING_PRES");
			if (brand == null)
				brand = System.getenv("ICPC_BRANDING_PRES");
			if (brand != null) {
				Class<?> bc = getClass().getClassLoader().loadClass(brand);
				Presentation bp = (Presentation) bc.getDeclaredConstructor().newInstance();
				if (bp != null && bp instanceof BrandingPresentation) {
					BrandingPresentation bp2 = (BrandingPresentation) bp;
					bp2.setChildPresentation(pres2);
					pres2 = bp2;
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading branding", e);
		}

		window.setPresentation(pres2);
		// Transition t = new SlidesTransition();
		// long time = System.currentTimeMillis() + 1000;
		// window.setPresentations(time, new Presentation[] { pres2 }, new Transition[] { t });
	}
}