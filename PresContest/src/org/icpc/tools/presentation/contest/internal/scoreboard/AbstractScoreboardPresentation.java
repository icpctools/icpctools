package org.icpc.tools.presentation.contest.internal.scoreboard;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IProblemSummary;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.contest.model.resolver.SelectType;
import org.icpc.tools.contest.model.resolver.SubmissionInfo;
import org.icpc.tools.contest.model.util.AwardUtil;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator.Movement;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.ShadedRectangle;
import org.icpc.tools.presentation.contest.internal.TextHelper;
import org.icpc.tools.presentation.contest.internal.TextImage;
import org.icpc.tools.presentation.contest.internal.nls.Messages;
import org.icpc.tools.presentation.contest.internal.presentations.TitledPresentation;

public abstract class AbstractScoreboardPresentation extends TitledPresentation {
	protected static final Movement ROW_MOVEMENT = new Movement(4, 7);
	protected static final Color COLOR_TEAM_LIST = new Color(40, 192, 192);

	private static final int DEFAULT_TEAMS_PER_SCREEN = 12;
	protected static final int CUBE_INSET = 5;

	// the color for the blinking rounded rectangle outline on pending submissions
	private static final Color PENDING_SUBMISSION_BLINK_HILIGHT_COLOR = new Color(255, 255, 0, 255);

	protected Font headerFont;
	protected Font headerItalicsFont;
	protected Font rowFont;
	protected Font rowItalicsFont;
	public static Font statusFont;
	public static Font problemFont;

	protected SelectType selectType = SelectType.NORMAL;
	protected List<ITeam> selectedTeams = null;
	protected String focusOnTeamId;
	protected Animator focusOnTeamScroll = new Animator(0, new Movement(3, 8));
	protected int teamsPerScreen = DEFAULT_TEAMS_PER_SCREEN;

	protected boolean showMedals = false;
	protected SubmissionInfo selectedSubmission = null;

	protected static final Color BG_COLOR = new Color(60, 60, 60);

	protected int oldNumProblems = -1;
	protected float rowHeight = 20;
	protected int cubeHeight;
	protected int cubeWidth;

	/* A mapping from each team's number (id) to their vertical position on the scoreboard presentation.
	 * The first element of the float [] contains the current vertical position; the second element
	 * contains the current speed at which the entry is moving (zero unless the team is currently changing
	 * position)
	 */
	private final Map<String, Animator> teamYmap = new HashMap<>();

	protected final Map<String, BufferedImage> teamRowImages = new HashMap<>();

	@Override
	protected void setup() {
		final float dpi = 96;

		float size = (int) (height * 72.0 * 0.028 / dpi);
		headerFont = ICPCFont.deriveFont(Font.BOLD, size);
		headerItalicsFont = ICPCFont.deriveFont(Font.BOLD, size);

		headerHeight = (int) (height / 50.0);

		String s = getTitle();
		if (s == null)
			titleHeight = 0;
		else
			titleHeight = (int) (height / 30.0);

		float tempRowHeight = height / (float) teamsPerScreen;
		size = tempRowHeight * 36f * 0.95f / dpi;
		rowFont = ICPCFont.deriveFont(Font.BOLD, size * 1.25f);
		rowItalicsFont = ICPCFont.deriveFont(Font.BOLD, size * 1.25f);
		statusFont = ICPCFont.deriveFont(Font.BOLD, size * 0.7f);
		problemFont = ICPCFont.deriveFont(Font.PLAIN, size * 0.5f);

		rowHeight = (height - headerHeight - titleHeight) / (float) teamsPerScreen;
		cubeHeight = (int) (rowHeight / 2.5f) - CUBE_INSET;
		int newCubeWidth = (int) (((rowHeight / 1.8f) - CUBE_INSET) * 10f);
		IContest contest = getContest();
		if (contest != null) {
			oldNumProblems = contest.getProblems().length;
			while (newCubeWidth > 30 && (newCubeWidth + CUBE_INSET) * oldNumProblems > width - rowHeight * 3 - 70)
				newCubeWidth -= 2;
		}
		cubeWidth = newCubeWidth;

		super.setup();
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		if (d.width == 0 || d.height == 0)
			return;

		teamRowImages.clear();
		loadTeamLogos();
	}

	@Override
	public void aboutToShow() {
		super.aboutToShow();
		initTeamY();
	}

	@Override
	/**
	 * Receives a parameter 'dt' (delta-time) specifying the amount of time, in milliseconds, that
	 * has elapsed since the last time the current presentation was updated; updates the Y position
	 * of all teams by moving each team a little bit, based on the elapsed time, toward its target
	 * location (where it belongs on the scoreboard based on current standings). Also applies
	 * "scrolling" to move the entire scoreboard up/down based on what team (row) has been selected.
	 *
	 * @param dt - the number of ms that have elapsed since the last call to this method
	 */
	public void incrementTimeMs(long dt) {
		// update Y locations (move each team a little bit toward their target location
		// based on their current standings order)
		updateTeamY(dt);

		if (focusOnTeamId != null) {
			IContest contest = getContest();
			if (contest != null) {
				ITeam team = contest.getTeamById(focusOnTeamId);
				if (team != null) {
					float target = Math.max(0, contest.getOrderOf(team) - (teamsPerScreen - 2));
					focusOnTeamScroll.setTarget(target);
					focusOnTeamScroll.incrementTimeMs(dt);
				}
			}
		}

		super.incrementTimeMs(dt);
	}

	protected double getTeamY(ITeam team) {
		Animator anim = teamYmap.get(team.getId());
		if (anim == null)
			return 0.0;
		return anim.getValue() * rowHeight;
	}

	/**
	 * Initializes a map of each team's vertical (Y) position on the scoreboard display so that it
	 * corresponds to the team's current position (standing) in the contest. The constructed map
	 * contains an entry for each team (using the team ID as the key); each entry contains two float
	 * elements: current scoreboard location, and speed. The method assigns a "speed" of zero for
	 * each team; the speed value is used later when moving teams around on the scoreboard display.
	 */
	protected void initTeamY() {
		IContest contest = getContest();
		if (contest == null)
			return;

		ITeam[] teams = contest.getOrderedTeams();
		double[] targets = getTeamYTargets(teams);

		for (int i = 0; i < teams.length; i++) {
			double target = targets[i];
			teamYmap.put(teams[i].getId(), new Animator(target, ROW_MOVEMENT));
		}
	}

	protected double[] getTeamYTargets(ITeam[] teams) {
		int size = teams.length;
		double[] targets = new double[size];
		for (int i = 0; i < size; i++) {
			targets[i] = i;
		}
		return targets;
	}

	/**
	 * Computes the desired screen Y position (the "target position") for each team based on the
	 * team's current standing (order in the contest standings), then calls
	 * {@link org.icpc.tools.presentation.contest.internal.SmoothUtil#update(float[], float, float, float, float)}
	 * to move the team a little bit in the direction of its target position (that is, to update the
	 * team's current position based on its speed). The new current position of the team is stored
	 * into the global map "teamYmap".
	 *
	 * @param dt - the elapsed time, in seconds, since the last time the team's position was
	 *           updated. (Note that this does not mean the team will arrive at its desired position
	 *           in that time; only that it moves for the specified amount of time toward its target
	 *           position based on its current speed.)
	 */
	protected void updateTeamY(long dt) {
		IContest contest = getContest();
		if (contest == null)
			return;

		ITeam[] teams = contest.getOrderedTeams();
		double[] targets = getTeamYTargets(teams);

		int size = teams.length;
		for (int i = 0; i < size; i++) {
			// determine the target location -- the position the team is ultimately headed for
			ITeam team = teams[i];
			double target = targets[i];

			// get the team's CURRENT location
			String id = team.getId();
			Animator current = teamYmap.get(id);

			// update the team's location based on current location, target location, speed and
			// time
			if (current == null) // initial set
				current = new Animator(target, ROW_MOVEMENT);
			else {
				current.setTarget(target);

				// move the team from its current position a little bit toward the target position
				current.incrementTimeMs(dt);
			}

			// at this point the team's current location is updated, either
			// because of the initial assignment or the reference-feedback from
			// SmoothUtil.update() which modifies the local array "current";
			// put the new current location back into the map
			teamYmap.put(id, current);
		}
	}

	public float getRowHeight() {
		return rowHeight;
	}

	public void setFocusOnTeam(String focusOnTeamId) {
		this.focusOnTeamId = focusOnTeamId;
		selectType = SelectType.NORMAL;

		IContest contest = getContest();
		if (contest != null) {
			ITeam team = contest.getTeamById(focusOnTeamId);
			if (team != null) {
				focusOnTeamScroll.reset(Math.max(0, contest.getOrderOf(team) - (teamsPerScreen - 2)));
			}
		}
	}

	public void setSelectedTeam(ITeam team, SelectType type) {
		selectType = type;
		if (team == null)
			selectedTeams = null;
		else {
			selectedTeams = new ArrayList<>(1);
			selectedTeams.add(team);
		}
	}

	public void setSelectedTeams(List<ITeam> teams, SelectType type) {
		selectType = type;
		selectedTeams = teams;
	}

	public List<ITeam> getSelectedTeams() {
		return selectedTeams;
	}

	public int getNumRows() {
		return teamsPerScreen;
	}

	public void setNumRows(int numRows) {
		this.teamsPerScreen = numRows;

		// reset screen
		setSize(getSize());
	}

	public void setSelectedSubmission(SubmissionInfo s) {
		selectedSubmission = s;
	}

	public SubmissionInfo getSelectedSubmission() {
		return selectedSubmission;
	}

	public void setShowMedals(boolean b) {
		showMedals = b;
	}

	@Override
	protected void drawHeader(Graphics2D g) {
		g.setFont(rowFont);
		FontMetrics fm = g.getFontMetrics();
		g.setFont(headerFont);
		FontMetrics fm2 = g.getFontMetrics();

		g.setColor(Color.black);
		g.fillRect(0, 0, width, headerHeight + 2);

		g.setColor(Color.white);
		g.drawLine(0, headerHeight - 1, width, headerHeight - 1);
		int y = headerHeight - 3;

		g.setFont(headerItalicsFont);
		g.drawString(Messages.rank, BORDER + (fm.stringWidth("199") - fm2.stringWidth(Messages.rank)) / 2, y);
		g.setFont(headerFont);
		g.drawString(Messages.name, BORDER + fm.stringWidth("199 ") + (int) rowHeight, y);
		g.setFont(headerItalicsFont);
		g.drawString(Messages.solved,
				width - BORDER - fm.stringWidth(" 9999") - (fm2.stringWidth(Messages.solved) + fm.stringWidth("99")) / 2,
				y);
		g.setFont(headerFont);
		g.drawString(Messages.time, width - BORDER - (fm2.stringWidth(Messages.time) + fm.stringWidth("9999")) / 2, y);
	}

	public void drawBackground(Graphics2D g, int row, boolean oddRow) {
		IContest contest = getContest();
		if (oldNumProblems != contest.getProblems().length)
			setup();

		Color bg = null;
		IAward[] awards = contest.getAwards();
		if (showMedals && awards != null) {
			int[] num = AwardUtil.getMedalCounts(contest);
			if (row < num[0]) {
				bg = ICPCColors.GOLD2;
			} else if (row < num[0] + num[1]) {
				bg = ICPCColors.SILVER2;
			} else if (row < num[0] + num[1] + num[2]) {
				bg = ICPCColors.BRONZE2;
			}
		} else if (oddRow)
			bg = BG_COLOR;

		if (bg != null) {
			g.setColor(bg);
			int x = 0;
			if (isInTransition())
				x = width - (int) (getTransitionTime() * width);
			g.fillRect(x, 0, width, (int) rowHeight);
		}
	}

	public void drawRowBackground(Graphics2D g, int y) {
		g.setColor(BG_COLOR);
		g.fillRect(0, y, width, (int) rowHeight);
	}

	protected void loadTeamLogos() {
		execute(new Runnable() {
			@Override
			public void run() {
				ITeam[] teams = getContest().getOrderedTeams();
				for (ITeam t : teams) {
					getSmallTeamLogo(t, true);
				}
			}
		});
	}

	/**
	 * Draws the line on this presentation describing the specified team, including the team's
	 * current rank, logo, name, number solved, and penalty points. Also draws a "white-box outline"
	 * around the team and problem grid if the current "selectType" is a HIGHLIGHT type.
	 *
	 * @param g - the graphics object to be used for drawing
	 * @param team - the team whose data is to be displayed
	 */
	protected void drawTeamGrid(Graphics2D g, ITeam team) {
		// make sure we have selected a team
		if ((selectedTeams != null && selectedTeams.contains(team))
				|| (focusOnTeamId != null && focusOnTeamId.equals(team.getId()))) {

			// we have a selected team; choose selection (background) color based on whether they
			// are an FTS-Only team
			if (selectType == SelectType.FTS || selectType == SelectType.FTS_HIGHLIGHT)
				g.setColor(ICPCColors.FIRST_TO_SOLVE_COLOR);
			else if (selectType == SelectType.TEAM_LIST)
				g.setColor(COLOR_TEAM_LIST);
			else
				g.setColor(ICPCColors.SELECTION_COLOR);

			// fill the team's bar with the appropriate selection color
			g.fillRect(0, 0, width, (int) (rowHeight + 0.9f));

			// check if the selection type indicates there should be a white outline box
			if (selectType == SelectType.HIGHLIGHT || selectType == SelectType.FTS_HIGHLIGHT
					|| selectType == SelectType.TEAM_LIST) {
				g.setColor(Color.WHITE);
				g.drawRect(-1, 0, width + 2, (int) (rowHeight));
			}
		}

		g.setFont(rowFont);
		FontMetrics fm = g.getFontMetrics();

		IStanding standing = getContest().getStanding(team);
		String s = standing.getRank();
		g.setColor(Color.white);
		g.setFont(rowItalicsFont);
		if (s != null)
			TextImage.drawString(g, s, BORDER + (fm.stringWidth("199") - fm.stringWidth(s)) / 2, 5);

		BufferedImage img = getSmallTeamLogo(team, true);
		if (img != null) {
			int nx = (int) ((rowHeight - img.getWidth()) / 2f);
			int ny = (int) ((rowHeight - img.getHeight()) / 2f);
			g.drawImage(img, BORDER + fm.stringWidth("199 ") + nx, ny, null);
		}

		s = team.getActualDisplayName();
		if (s == null)
			s = "";

		g.setColor(Color.white);
		g.setFont(rowFont);
		fm = g.getFontMetrics();

		int xx = BORDER + fm.stringWidth("199 ") + (int) rowHeight;
		TextHelper text = new TextHelper(g, s, (int) (width - BORDER * 2 - fm.stringWidth("199 9 9999 ") - rowHeight));
		text.draw(xx, fm.getAscent() + 5);

		int n = standing.getNumSolved();

		g.setColor(Color.white);
		g.setFont(rowItalicsFont);
		if (n > 0) {
			s = n + "";
			TextImage.drawString(g, s,
					width - BORDER - fm.stringWidth(" 9999") - (fm.stringWidth("99") + fm.stringWidth(s)) / 2, 5);
		}

		n = standing.getTime();

		g.setColor(Color.white);
		g.setFont(rowFont);
		if (n > 0) {
			s = n + "";
			TextImage.drawString(g, s, width - BORDER - (fm.stringWidth("9999") + fm.stringWidth(s)) / 2, 5);
		}
	}

	/**
	 * This method fills in a single "row" on the presentation with a team description including the
	 * team name and the team's status for each of the contest problems.
	 *
	 * @param g - the Graphics object to use for drawing
	 * @param team - the team to be displayed in the specified row
	 */
	protected void drawTeamAndProblemGrid(Graphics2D g, ITeam team) {
		long count = getTimeMs();
		// draw team rank/logo/name/numSolved/points, plus white-box highlight if appropriate
		drawTeamGrid(g, team);

		FontMetrics fm = g.getFontMetrics(); // row font

		int indent = BORDER + fm.stringWidth("199 ") + (int) rowHeight;
		int rowH = (int) (rowHeight * 0.6f);
		IContest contest = getContest();
		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;

		// draw a rounded-rectangle representation for each problem
		for (int curProblem = 0; curProblem < numProblems; curProblem++) {
			// compute horizontal starting position of current problem representation (rectangle)
			int xx = indent + curProblem * (cubeWidth + CUBE_INSET);

			SubmissionInfo runInfo = selectedSubmission;
			if (runInfo != null && runInfo.getTeam().equals(team) && runInfo.getProblemIndex() == curProblem) {
				g.setColor(PENDING_SUBMISSION_BLINK_HILIGHT_COLOR);
				g.setStroke(new BasicStroke(3));

				// use elapsed time to decide whether to draw a border around the problem
				// (makes pending runs blink)
				float t = (count * 1.75f / 1000f) - (float) Math.floor(count * 1.75f / 1000f);
				if (t < 0.6)
					// draw a rounded rectangle outline around the current problem
					g.fillRoundRect(xx - 3, rowH + CUBE_INSET / 2 - 4, cubeWidth + 6, cubeHeight + 6, 14, 14);

				g.setStroke(new BasicStroke(1));
			}

			IResult r = contest.getResult(team, curProblem);
			if (r.getStatus() != Status.UNATTEMPTED) {
				// the team has a status other than null for the current problem; build a string
				// to go in the round rectangle displaying the submission status
				String s = "";
				if (r.getNumSubmissions() > 0) {
					// the team has some submissions for the current problem; add the number of
					// submissions to the
					// string
					s = r.getNumSubmissions() + "";
					if (r.getContestTime() > 0)
						// add a dash, surrounded on both sides by a Unicode "HairSpace" (the thinnest
						// available), followed by the time of the submission
						s += "\u200A-\u200A" + ContestUtil.getTime(r.getContestTime());
				}

				// fill in the center of the oval with the appropriate color and string
				ShadedRectangle.drawRoundRect(g, xx, rowH + CUBE_INSET / 2 - 1, cubeWidth, cubeHeight, contest, r,
						getTimeMs(), s);
			} else {
				// the team has no result for the current problem;
				// draw a round rectangle containing the problem identifier string
				ShadedRectangle.drawRoundRectPlain(g, xx, rowH + CUBE_INSET / 2 - 1, cubeWidth, cubeHeight,
						problems[curProblem].getLabel());
			}
		}
	}

	/**
	 * This method fills in a single row with a summary for each problem.
	 *
	 * @param g - the Graphics object to use for drawing
	 */
	protected void drawStatsRow(Graphics2D g) {
		g.setColor(Color.GRAY);
		g.setFont(rowItalicsFont);
		FontMetrics fm = g.getFontMetrics();

		int indent = BORDER + fm.stringWidth("199 ") + (int) rowHeight;
		int rowH = (int) (rowHeight * 0.6f);
		IContest contest = getContest();
		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;

		// draw a rounded-rectangle representation for each problem
		for (int curProblem = 0; curProblem < numProblems; curProblem++) {
			// compute horizontal starting position of current problem representation (rectangle)
			int xx = indent + curProblem * (cubeWidth + CUBE_INSET);

			IProblemSummary ps = contest.getProblemSummary(curProblem);
			if (ps.getNumPending() > 0)
				ShadedRectangle.drawRoundRect(g, xx, rowH + CUBE_INSET / 2 - 1 - cubeHeight * 5 / 4, cubeWidth, cubeHeight,
						contest, Status.SUBMITTED, ps.getPendingContestTime(), getTimeMs(), ps.getNumPending() + "");
			else
				ShadedRectangle.drawRoundRectPlain(g, xx, rowH + CUBE_INSET / 2 - 1 - cubeHeight * 5 / 4, cubeWidth,
						cubeHeight, "");

			if (ps.getNumSolved() > 0)
				ShadedRectangle.drawRoundRect(g, xx, rowH + CUBE_INSET / 2 - 1, cubeWidth, cubeHeight, contest,
						Status.SOLVED, ps.getSolvedContestTime(), getTimeMs(), ps.getNumSolved() + "");
			else
				ShadedRectangle.drawRoundRectPlain(g, xx, rowH + CUBE_INSET / 2 - 1, cubeWidth, cubeHeight, "");
		}
	}

	protected BufferedImage getSmallTeamLogo(ITeam team, boolean forceLoad) {
		String id = team.getId();
		BufferedImage smImg = teamRowImages.get(id);
		if (smImg != null || !forceLoad)
			return smImg;

		IOrganization org = getContest().getOrganizationById(team.getOrganizationId());
		if (org == null)
			return null;

		smImg = org.getLogoImage((int) rowHeight - 10, (int) rowHeight - 10, true, true);
		if (smImg != null)
			teamRowImages.put(id, smImg);
		return smImg;
	}

	@Override
	protected void paintImpl(Graphics2D g) {
		IContest contest = getContest();
		if (contest == null)
			return;

		ITeam[] teams = contest.getOrderedTeams();

		// draw row backgrounds
		for (int i = 0; i < teamsPerScreen; i += 2)
			drawRowBackground(g, (int) (rowHeight * i));

		// draw teams - fill in each team's bar (rectangle) with team and problem info
		// in the manner defined by the parent class's drawTeamAndProblemGrid() method
		for (int i = teams.length - 1; i >= 0; i--) {
			ITeam team = teams[i];
			double y = getTeamY(team);
			if ((y + rowHeight) > 0 && y < (height - headerHeight)) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.translate(0, (int) y);
				drawTeamAndProblemGrid(g2, team);
				g2.dispose();
			}
		}
	}

	protected void paintLegend(Graphics2D g) {
		// do nothing
	}

	@Override
	public void paint(Graphics2D g) {
		super.paint(g);

		long time = getRepeatTimeMs();
		if (time < 6000) {
			Graphics2D g2 = (Graphics2D) g.create();
			// fade legends in and out
			if (time < 1500)
				g2.setComposite(AlphaComposite.SrcOver.derive(time / 1500f));
			else if (time > 4500)
				g2.setComposite(AlphaComposite.SrcOver.derive((6000 - time) / 1500f));

			paintLegend(g2);
			g2.dispose();
		}
	}

	@Override
	public void setProperty(String value) {
		if (value.startsWith("focusTeam:")) {
			try {
				setFocusOnTeam(value.substring(11)); // TODO 2017 look up team by number
			} catch (Exception e) {
				setFocusOnTeam(null);
			}
		} else if (value.startsWith("rows:")) {
			try {
				setNumRows(Integer.parseInt(value.substring(5)));
			} catch (Exception e) {
				// ignore
			}
		} else
			super.setProperty(value);
	}
}