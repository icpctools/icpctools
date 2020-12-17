package org.icpc.tools.coachview;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import org.icpc.tools.client.core.BasicClient;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.IResult;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.ITeamMember;
import org.icpc.tools.contest.model.Status;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.util.ArgumentParser;
import org.icpc.tools.contest.model.util.ArgumentParser.OptionParser;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

public class CoachView extends Panel {
	private static final long serialVersionUID = 7407274035553017050L;

	private static final Color NO_IMAGE = new Color(32, 32, 32);

	private static Map<String, Locale> localeMap;

	static {
		String[] countries = Locale.getISOCountries();
		localeMap = new HashMap<String, Locale>(countries.length);
		for (String country : countries) {
			Locale locale = new Locale("", country);
			localeMap.put(locale.getISO3Country().toUpperCase(), locale);
		}
	}

	private static class WiderCheckbox extends Checkbox {
		private static final long serialVersionUID = 1L;

		public WiderCheckbox(String label, CheckboxGroup group) {
			super(label, group, false);
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension d = super.getPreferredSize();
			return new Dimension(d.width + 15, d.height);
		}
	}

	private MediaPlayerFactory mediaPlayerFactory;
	private EmbeddedMediaPlayer mediaPlayerCamera;
	private EmbeddedMediaPlayer mediaPlayerScreen;
	private BufferedImage imageWebcam;
	private BufferedImage imageDesktop;
	private int[] webcamBuffer;
	private int[] desktopBuffer;

	private enum DisplayMode {
		WEBCAM, DESKTOP, BOTH, PIP, ACTIVITY, DETAILS, NONE
	}

	private DisplayMode displayMode = DisplayMode.NONE;

	private Canvas mainPanel;
	private Choice teamList;
	private Checkbox[] modes;
	private ITeam[] teams;
	private boolean showDetails = true;

	private BufferedImage footerImage;

	private Font sansSerif;
	private Font sansSerifBig;
	protected static Font statusFont;
	protected static Font problemFont;

	private RESTContestSource contestSource;
	private String user;
	private String pwd;

	protected ITeam currentTeam = null;
	protected IContest contest;
	protected BufferedImage logoImg;

	protected Map<Rectangle, String> reactions = new HashMap<>();

	public CoachView() {
		sansSerif = ICPCFont.getMasterFont().deriveFont(Font.BOLD, 16);
		sansSerifBig = ICPCFont.getMasterFont().deriveFont(Font.PLAIN, 48);
		statusFont = ICPCFont.getMasterFont().deriveFont(Font.PLAIN, 16);
		problemFont = ICPCFont.getMasterFont().deriveFont(Font.PLAIN, 14);
	}

	protected void createUI() {
		Panel headerPanel = createHeaderPanel();

		mainPanel = new Canvas() {
			private static final long serialVersionUID = 3647047360117026676L;

			@Override
			public void update(Graphics g) {
				paint(g);
			}

			private void drawImage(Graphics2D g, BufferedImage img, Dimension d) {
				int w = 16;
				int h = 9;
				if (img != null) {
					w = img.getWidth();
					h = img.getHeight();
				}
				float scale = Math.min(d.width / (float) w, d.height / (float) h);
				int nw = (int) (w * scale);
				int nh = (int) (h * scale);

				if (img != null)
					g.drawImage(img, (d.width - nw) / 2, (d.height - nh) / 2, nw, nh, null);
				else {
					g.setColor(NO_IMAGE);
					g.fillRect((d.width - nw) / 2, (d.height - nh) / 2, nw, nh);
				}

				g.setColor(Color.LIGHT_GRAY);
				g.drawRect((d.width - nw) / 2 - 1, (d.height - nh) / 2 - 1, nw + 1, nh + 1);

				if (contest.getState().isFrozen() && contest.getState().getEnded() == null) {
					g.setColor(Color.WHITE);
					g.setFont(sansSerifBig);
					FontMetrics fm = g.getFontMetrics();
					String s = "Contest is frozen";
					g.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 2 + fm.getAscent() / 2);
				} else if (contest.getState().getEnded() != null) {
					g.setColor(Color.WHITE);
					g.setFont(sansSerifBig);
					FontMetrics fm = g.getFontMetrics();
					String s = "Contest is over";
					g.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 2 + fm.getAscent() / 2);
				}
			}

			@Override
			public void paint(Graphics g) {
				BufferStrategy bs = getBufferStrategy();
				if (bs == null) {
					createBufferStrategy(2);
					bs = getBufferStrategy();
				}

				Graphics2D bg = (Graphics2D) bs.getDrawGraphics();
				paintImpl(bg);
				if (!bs.contentsRestored())
					bs.show();
			}

			public void paintImpl(Graphics gg) {
				Graphics2D g = (Graphics2D) gg;

				Dimension d = getSize();
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, d.width, d.height);
				g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

				if (currentTeam == null) {
					g.setColor(Color.WHITE);
					g.setFont(sansSerifBig);
					FontMetrics fm = g.getFontMetrics();

					String s = "Select a team from the list above";
					g.drawString(s, (d.width - fm.stringWidth(s)) / 2, (d.height + fm.getAscent()) / 2);
					return;
				}

				if (displayMode == DisplayMode.WEBCAM) {
					drawImage(g, imageWebcam, d);
				} else if (displayMode == DisplayMode.DESKTOP) {
					drawImage(g, imageDesktop, d);
				} else if (displayMode == DisplayMode.BOTH) {
					int w = 450;
					int h = w * 3 / 4;

					Dimension dd = new Dimension(d.width - w - 5, d.height);
					g.translate(1, 0);
					drawImage(g, imageDesktop, dd);

					dd = new Dimension(450, h);
					g.translate(d.width - w - 2, (d.height - dd.height) / 2);
					drawImage(g, imageWebcam, dd);
					g.translate(w + 1 - d.width, -(d.height - dd.height) / 2);
				} else if (displayMode == DisplayMode.PIP) {
					drawImage(g, imageDesktop, d);

					Dimension dd = new Dimension(d.width / 5, d.height / 5);
					if (imageWebcam != null)
						dd = new Dimension(imageWebcam.getWidth() * 3 / 10, imageWebcam.getHeight() * 3 / 10);
					int x = d.width - dd.width - 20;

					g.translate(x, 20);
					drawImage(g, imageWebcam, dd);
					g.translate(-x, -20);
				} else if (displayMode == DisplayMode.ACTIVITY) {
					paintActivity(g, currentTeam, d);
				} else if (displayMode == DisplayMode.DETAILS) {
					paintDetails(g, currentTeam, d);
				} else { // displayMode == DisplayMode.NONE) {
					// do nothing
				}

				if (currentTeam != null)
					paintOverlay(g, currentTeam, d);
			}
		};
		mainPanel.setBackground(Color.BLACK);
		mainPanel.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if ((KeyEvent.VK_Q == e.getKeyCode() || KeyEvent.VK_ESCAPE == e.getKeyCode())
						&& (e.isControlDown() || e.isShiftDown()))
					System.exit(0);
			}
		});

		setLayout(new BorderLayout());
		add(headerPanel, BorderLayout.NORTH);
		add(mainPanel, BorderLayout.CENTER);

		try {
			Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
			footerImage = contest.getBannerImage(d.width, d.height / 10, true, true);

			if (footerImage != null) {
				Canvas footerPanel = new Canvas() {
					private static final long serialVersionUID = 3647047360117026676L;

					@Override
					public void paint(Graphics g) {
						super.paint(g);

						int w = getSize().width;
						g.drawImage(footerImage, (w - footerImage.getWidth()) / 2, 5, null);
					}
				};
				footerPanel.setPreferredSize(new Dimension(footerImage.getWidth(), footerImage.getHeight() + 5));
				footerPanel.setBackground(Color.WHITE);
				add(footerPanel, BorderLayout.SOUTH);
			}
		} catch (Exception e) {
			// could not load image
		}
	}

	protected void paintOverlay(Graphics2D g, ITeam team, Dimension d) {
		g.setColor(Color.WHITE);
		g.setFont(sansSerifBig);
		FontMetrics fm = g.getFontMetrics();

		int BORDER = 5;
		IStanding standing = contest.getStanding(team);
		String s = standing.getRank() + "";
		int col1 = BORDER + fm.stringWidth("199") / 2;
		g.drawString(s, col1 - fm.stringWidth(s) / 2, d.height - BORDER);
		s = standing.getNumSolved() + "";
		int col2 = d.width - BORDER - fm.stringWidth(" 1999") - fm.stringWidth("99") / 2;
		g.drawString(s, col2 - fm.stringWidth(s) / 2, d.height - BORDER);
		s = standing.getTime() + "";
		int col3 = d.width - BORDER - fm.stringWidth("1999") / 2;
		g.drawString(s, col3 - fm.stringWidth(s) / 2, d.height - BORDER);

		int h = d.height - BORDER - fm.getAscent() - 5;
		g.setFont(problemFont);
		fm = g.getFontMetrics();

		g.setColor(Color.LIGHT_GRAY);
		s = "Rank";
		g.drawString(s, col1 - fm.stringWidth(s) / 2, h);
		s = "Solved";
		g.drawString(s, col2 - fm.stringWidth(s) / 2, h);
		s = "Penalty";
		g.drawString(s, col3 - fm.stringWidth(s) / 2, h);

		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;
		int GAP = 5;
		int cubeWidth = d.width / 25;
		int cubeHeight = d.height / 20;
		int y = d.height - BORDER - cubeHeight;
		for (int curProblem = 0; curProblem < numProblems; curProblem++) {
			int xx = (d.width - (cubeWidth + GAP) * numProblems) / 2 + curProblem * (cubeWidth + GAP);

			IResult r = contest.getResult(team, curProblem);
			if (r.getStatus() != Status.UNATTEMPTED) {
				s = r.getNumSubmissions() + "";
				if (r.getContestTime() > 0)
					s += "\u200A-\u200A" + ContestUtil.getTime(r.getContestTime());

				ShadedRectangle.drawRoundRect(g, xx, y, cubeWidth, cubeHeight, contest, r, 0, s);
			} else {
				ShadedRectangle.drawRoundRectPlain(g, xx, y, cubeWidth, cubeHeight, problems[curProblem].getLabel());
			}
		}
	}

	protected void paintDetails(Graphics2D g, ITeam team, Dimension d) {
		int y = 30;
		y = drawLine(g, y, "Team", team.getActualDisplayName());

		IOrganization org = contest.getOrganizationById(team.getOrganizationId());
		if (org != null) {
			int BORDER = 15;
			if (logoImg != null)
				g.drawImage(logoImg, d.width - logoImg.getWidth() - BORDER, BORDER, null);

			y = drawLine(g, y, "Organization", org.getActualFormalName());
			y = drawLine(g, y, null, "(" + org.getName() + ")");
			String country = null;
			if (org.getCountry() != null)
				country = localeMap.get(org.getCountry()).getDisplayCountry();
			if (country != null)
				y = drawLine(g, y, "Country", country);
			y = drawLine(g, y, "URL", org.getURL());
			y = drawLine(g, y, "Hashtag", org.getHashtag());
		}

		ITeamMember[] members = contest.getTeamMembersByTeamId(team.getId());
		if (members != null && members.length > 0) {
			List<ITeamMember> contestants = new ArrayList<>();
			List<ITeamMember> staff = new ArrayList<>();
			for (int i = 0; i < members.length; i++) {
				if (members[i].getRole().equalsIgnoreCase("contestant"))
					contestants.add(members[i]);
				else
					staff.add(members[i]);
			}

			String[] names = new String[contestants.size()];
			for (int i = 0; i < contestants.size(); i++) {
				ITeamMember tm = contestants.get(i);
				names[i] = tm.getFirstName() + " " + tm.getLastName();
			}
			drawLine(g, y, "Contestants", names);

			int x = (int) (d.width * 0.4);
			g.translate(x, 0);
			names = new String[staff.size()];
			for (int i = 0; i < staff.size(); i++) {
				ITeamMember tm = staff.get(i);
				names[i] = tm.getFirstName() + " " + tm.getLastName() + " (" + tm.getRole() + ")";
			}
			drawLine(g, y, "Staff", names);
			g.translate(-x, 0);
		}
	}

	private int drawLine(Graphics2D g, int y, String label, String value) {
		if (value == null)
			return y;
		return drawLine(g, y, label, new String[] { value });
	}

	private int drawLine(Graphics2D g, int y, String label, String[] values) {
		g.setColor(Color.WHITE);
		g.setFont(sansSerifBig);
		FontMetrics fm = g.getFontMetrics();
		int x = fm.stringWidth("99999");
		int h = (int) (fm.getHeight() * 1.05);
		for (int i = 0; i < values.length; i++)
			g.drawString(values[i], x, y + fm.getAscent() + i * h);

		g.setColor(Color.LIGHT_GRAY);
		g.setFont(problemFont);
		fm = g.getFontMetrics();
		if (label != null)
			g.drawString(label, x - fm.stringWidth(label) - 20, y + fm.getAscent() + 4);

		return y + h * values.length + 8;
	}

	protected void paintActivity(Graphics2D g, ITeam team, Dimension d) {
		// get team's submissions
		List<ISubmission> teamSubs = new ArrayList<>();
		for (ISubmission submission : contest.getSubmissions()) {
			if (submission.getTeamId().equals(currentTeam.getId()))
				teamSubs.add(submission);
		}

		int BORDER = 5;
		g.setFont(problemFont);

		IProblem[] problems = contest.getProblems();
		int numProblems = problems.length;
		int GAP = 5;
		int cubeWidth = d.width / 25;
		int cubeHeight = d.height / 20;
		int headerCol = (int) (cubeWidth * 1.75);
		Color BG = new Color(20, 20, 20);
		int cubeY = 44;
		int yy = d.height / 2 - numProblems * cubeY / 2 - cubeY;
		for (int i = 0; i < numProblems; i++) {
			int y = yy + i * cubeY;
			g.setColor(BG);
			g.fillRect(20, y, d.width, cubeHeight);
			ShadedRectangle.drawRoundRectPlain(g, 5, y, headerCol, cubeHeight, "Problem " + problems[i].getLabel());
		}

		int size = teamSubs.size();
		for (int c = 0; c < size; c++) {
			ISubmission sub = teamSubs.get(c);
			IProblem p = contest.getProblemById(sub.getProblemId());
			String s = ContestUtil.getTimeInMin(sub.getContestTime()) + "";
			int x = 5 + headerCol + GAP + c * (cubeWidth + GAP);
			int y = yy + p.getOrdinal() * cubeY;
			ShadedRectangle.drawRoundRect(g, x, y, cubeWidth, cubeHeight, contest, sub, 0, s);
			if (sub.getReactionURL() != null)
				reactions.put(new Rectangle(x, y, cubeWidth, cubeHeight), sub.getReactionURL());
			x += cubeWidth + GAP;
			if (x + cubeWidth > d.width - BORDER) {
				x = BORDER;
				y += cubeHeight + GAP;
			}
		}
	}

	protected void loadTeamList() {
		ITeam[] newTeams = contest.getTeams();
		if (newTeams == null)
			return;

		// remove hidden teams
		List<ITeam> publicTeamList = new ArrayList<>();
		for (ITeam t : newTeams) {
			if (!contest.isTeamHidden(t))
				publicTeamList.add(t);
		}

		newTeams = publicTeamList.toArray(new ITeam[0]);
		if (teams != null && newTeams.length == teams.length)
			return;

		teams = ContestUtil.sort(newTeams);

		updateTeamList();
	}

	protected void connectToCDS() {
		try {
			contest.addListener(new IContestListener() {
				protected Thread updateThread;

				@Override
				public void contestChanged(IContest contest2, IContestObject e, Delta d) {
					if (e instanceof ITeam) {
						if (updateThread != null)
							return;

						updateThread = new Thread("Update team list") {
							@Override
							public void run() {
								try {
									Thread.sleep(500);
								} catch (Exception ex) {
									// ignore
								}
								updateThread = null;

								loadTeamList();
							}
						};
						updateThread.setDaemon(true);
						updateThread.start();
					} else if (e instanceof ISubmission) {
						ISubmission s = (ISubmission) e;
						if (showDetails && currentTeam != null && currentTeam.getId().equals(s.getTeamId()))
							mainPanel.repaint();
					} else if (e instanceof IJudgement) {
						IJudgement sj = (IJudgement) e;
						ISubmission s = contest.getSubmissionById(sj.getSubmissionId());
						if (s != null && showDetails && currentTeam != null && currentTeam.getId().equals(s.getTeamId()))
							mainPanel.repaint();
					}
				}
			});

			if (contest.getNumTeams() > 0)
				loadTeamList();

			contestSource.waitForContestLoad();

			if (contestSource.isCDS()) {
				BasicClient client = new BasicClient(contestSource, "Coach");
				client.connect(true);
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Connection to CDS was not successful, check credentials/URL", e);
			System.exit(1);
		}
	}

	protected synchronized void updateTeamList() {
		if (teamList == null)
			return;

		int sel = teamList.getSelectedIndex();

		teamList.removeAll();
		teamList.add("Select a team to view");

		if (teams != null) {
			for (ITeam team : teams) {
				teamList.add(team.getId() + ": " + team.getActualDisplayName());
			}
		}

		if (sel > 0)
			teamList.select(sel);
	}

	protected Panel createHeaderPanel() {
		Panel headerPanel = new Panel();
		headerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 40, 15));

		Panel teamPanel = new Panel();
		teamPanel.setFont(sansSerif);
		teamPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
		Label label = new Label("Team:");
		teamPanel.add(label);

		teamList = new Choice();
		teamList.setFont(sansSerif);
		teamList.add("Connecting...");
		teamList.setPreferredSize(new Dimension(500, 20));
		teamPanel.add(teamList);
		updateTeamList();
		headerPanel.add(teamPanel);

		Panel modePanel = new Panel();
		modePanel.setFont(sansSerif);
		modePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

		label = new Label("Display:");
		modePanel.add(label);
		CheckboxGroup group = new CheckboxGroup();

		modes = new Checkbox[6];
		modes[0] = new WiderCheckbox("Webcam", group);
		modePanel.add(modes[0]);
		modes[1] = new WiderCheckbox("Desktop", group);
		modePanel.add(modes[1]);
		modes[2] = new WiderCheckbox("Both", group);
		modePanel.add(modes[2]);
		modes[3] = new WiderCheckbox("PiP", group);
		modePanel.add(modes[3]);
		modes[4] = new WiderCheckbox("Activity", group);
		modePanel.add(modes[4]);
		modes[5] = new WiderCheckbox("Detail", group);
		modePanel.add(modes[5]);

		for (int i = 0; i < modes.length; i++) {
			modes[i].setFont(sansSerif);
			final int ii = i;
			modes[i].addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					displayMode = DisplayMode.values()[ii];
					Trace.trace(Trace.INFO, "Switch to mode: " + displayMode.name());

					mainPanel.repaint();
				}
			});
		}

		headerPanel.add(modePanel);

		setModeEnablement(null);

		teamList.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {
				if (teams == null)
					return;

				int newTeamIndex = teamList.getSelectedIndex();

				// get the team id directly in case there are any missing/skipped team ids
				ITeam newTeam = null;
				if (teams == null || newTeamIndex == 0 || newTeamIndex > teams.length)
					return;

				newTeam = teams[newTeamIndex - 1];

				if ((newTeam == null && currentTeam == null)
						|| (newTeam != null && currentTeam != null && newTeam.equals(currentTeam)))
					return;

				connectToTeam(newTeam);

				mainPanel.repaint();
			}

			private void connectToTeam(ITeam newTeam) {
				if (mediaPlayerScreen != null) {
					mediaPlayerScreen.release();
					mediaPlayerScreen = null;
					imageDesktop = null;
				}
				if (mediaPlayerCamera != null) {
					mediaPlayerCamera.release();
					mediaPlayerCamera = null;
					imageWebcam = null;
				}

				currentTeam = newTeam;
				setModeEnablement(currentTeam);

				if (currentTeam == null) {
					displayMode = DisplayMode.NONE;
					return;
				}

				try {
					String webcamURL = addAuth(currentTeam.getWebcamURL());
					String desktopURL = addAuth(currentTeam.getDesktopURL());
					logoImg = null;

					Trace.trace(Trace.INFO,
							"Connecting team " + currentTeam.getId() + " [" + webcamURL + " / " + desktopURL + "]");

					// auto switch to an appropriate mode if necessary
					if (displayMode == DisplayMode.NONE)
						displayMode = DisplayMode.BOTH;
					if (!modes[displayMode.ordinal()].isEnabled())
						displayMode = DisplayMode.DETAILS;
					modes[displayMode.ordinal()].setState(true);

					if (desktopURL != null) {
						if (mediaPlayerScreen == null)
							mediaPlayerScreen = createMediaPlayer(true);
						mediaPlayerScreen.media().play(desktopURL);
					}
					if (webcamURL != null) {
						if (mediaPlayerCamera == null)
							mediaPlayerCamera = createMediaPlayer(false);
						mediaPlayerCamera.media().play(webcamURL);
					}

					logoImg = null;

					IOrganization org = contest.getOrganizationById(currentTeam.getOrganizationId());
					if (org != null) {
						Dimension d = getSize();
						logoImg = org.getLogoImage(d.height / 3, d.height / 3, true, true);
					}
				} catch (Throwable t) {
					Trace.trace(Trace.ERROR, "Error playing media", t);
				}
			}
		});

		return headerPanel;
	}

	protected String addAuth(String urlStr) {
		if (urlStr == null)
			return null;

		try {
			URL url = new URL(urlStr);
			StringBuilder sb = new StringBuilder(url.getProtocol());
			sb.append("://");
			if (user != null)
				sb.append(user + ":" + pwd + "@");
			sb.append(url.getHost());
			if (url.getPort() >= 0)
				sb.append(":" + url.getPort());
			sb.append(url.getPath());
			return sb.toString();
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Could not parse URL: " + urlStr, e);
		}
		return urlStr;
	}

	protected void setModeEnablement(ITeam team) {
		for (int i = 0; i < 4; i++)
			modes[i].setEnabled(false);

		modes[4].setEnabled(team != null);
		modes[5].setEnabled(team != null);

		if (team == null)
			return;

		String webcamURL = team.getWebcamURL();
		String desktopURL = team.getDesktopURL();

		modes[0].setEnabled(webcamURL != null);
		modes[1].setEnabled(desktopURL != null);
		modes[2].setEnabled(webcamURL != null && desktopURL != null);
		modes[3].setEnabled(webcamURL != null && desktopURL != null);
	}

	private EmbeddedMediaPlayer createMediaPlayer(final boolean screen) {
		if (mediaPlayerFactory == null)
			mediaPlayerFactory = new MediaPlayerFactory();

		BufferFormatCallback bufferCallback = new BufferFormatCallbackAdapter() {
			@Override
			public BufferFormat getBufferFormat(int width, int height) {
				if (screen) {
					imageDesktop = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
					imageDesktop.setAccelerationPriority(1);
					desktopBuffer = new int[width * height];
				} else {
					imageWebcam = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
					imageWebcam.setAccelerationPriority(1);
					webcamBuffer = new int[width * height];
				}

				return new RV32BufferFormat(width, height);
			}
		};

		RenderCallback renderCallback = new RenderCallback() {
			@Override
			public void display(MediaPlayer player, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
				IntBuffer ib = nativeBuffers[0].asIntBuffer();

				if (screen) {
					ib.get(desktopBuffer);
					imageDesktop.setRGB(0, 0, imageDesktop.getWidth(), imageDesktop.getHeight(), desktopBuffer, 0,
							imageDesktop.getWidth());
				} else {
					ib.get(webcamBuffer);
					imageWebcam.setRGB(0, 0, imageWebcam.getWidth(), imageWebcam.getHeight(), webcamBuffer, 0,
							imageWebcam.getWidth());
				}

				mainPanel.repaint();
			}
		};

		EmbeddedMediaPlayer mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
		mediaPlayer.videoSurface()
				.set(mediaPlayerFactory.videoSurfaces().newVideoSurface(bufferCallback, renderCallback, true));
		return mediaPlayer;
	}

	protected static void showHelp() {
		System.out.println("Usage: coachView.bat/sh contestURL user password [options]");
		System.out.println();
		System.out.println("  Options:");
		System.out.println("     --display #");
		System.out.println("         Use the specified display");
		System.out.println("         1 = primary display, 2 = secondary display, etc.");
		System.out.println("     --help");
		System.out.println("         Shows this message");
		System.out.println("     --version");
		System.out.println("         Displays version information");
	}

	public static void main(String[] args) {
		Trace.init("ICPC Coach View", "coachView", args);

		String[] displayStr2 = new String[1];
		ContestSource contestSource = ArgumentParser.parse(args, new OptionParser() {
			@Override
			public boolean setOption(String option, List<Object> options) throws IllegalArgumentException {
				if ("--display".equals(option)) {
					ArgumentParser.expectOptions(option, options, "#:string");
					displayStr2[0] = (String) options.get(0);
					return true;
				}
				return false;
			}

			@Override
			public void showHelp() {
				CoachView.showHelp();
			}
		});
		String displayStr = displayStr2[0];

		RESTContestSource restSource = RESTContestSource.ensureContestAPI(contestSource);
		restSource.outputValidation();
		if (restSource.isCDS())
			restSource.checkForUpdates("coachview-");

		CoachView cv = new CoachView();
		cv.contestSource = restSource;
		cv.contest = contestSource.getContest();

		Frame frame = new Frame("Coach View");
		frame.setUndecorated(true);
		try {
			BufferedImage image = ImageIO.read(CoachView.class.getClassLoader().getResource("images/coachViewIcon.png"));
			frame.setIconImage(image);
			setMacIconImage(image);
		} catch (Exception e) {
			// could not set icon
		}

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		frame.setLayout(new BorderLayout());
		frame.add(cv, BorderLayout.CENTER);
		cv.createUI();
		cv.connectToCDS();

		try {
			int display = 0;
			if (displayStr != null)
				display = Integer.parseInt(displayStr.substring(0, 1)) - 1;
			GraphicsDevice[] gds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
			if (display >= gds.length)
				throw new IllegalArgumentException("Invalid display: " + display);

			if (displayStr != null && displayStr.length() == 2) {
				Rectangle r = gds[display].getDefaultConfiguration().getBounds();

				char c = displayStr.charAt(1);
				if (c == 'a')
					frame.setBounds(r.x, r.y, r.width / 2, r.height / 2);
				else if (c == 'b')
					frame.setBounds(r.x + r.width / 2, r.y, r.width / 2, r.height / 2);
				else if (c == 'c')
					frame.setBounds(r.x, r.y + r.height / 2, r.width / 2, r.height / 2);
				else if (c == 'd')
					frame.setBounds(r.x + r.width / 2, r.y + r.height / 2, r.width / 2, r.height / 2);
				else if (c == 'e')
					frame.setBounds(r.x, r.y, r.width * 2 / 3, r.height * 2 / 3);
				else if (c == 'w')
					frame.setBounds(r.x, r.y, r.width, r.height);
				else if (c == 't') {
					r = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
					frame.setBounds(r.x, r.y, r.width, r.height * 7 / 8);
				} else
					throw new IllegalArgumentException("Invalid display option: " + c);
			} else
				cv.setFullScreen(frame, display);
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "Invalid display option: " + displayStr + " " + e.getMessage());
		}

		frame.setVisible(true);
		frame.repaint();
	}

	private void setFullScreen(Window window, int screen) {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gds = ge.getScreenDevices();

		// find the first graphics device and its current mode
		java.awt.DisplayMode bestMode = null;
		int count = 0;
		GraphicsDevice device = null;
		for (GraphicsDevice gd : gds) {
			if (gd.getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
				if (screen == count) {
					device = gd;

					int area = 0;
					for (java.awt.DisplayMode mode : gd.getDisplayModes()) {
						if (mode.getWidth() * mode.getHeight() > area) {
							area = mode.getWidth() * mode.getHeight();
							bestMode = mode;
						}
					}
				}

				count++;
			}
		}

		if (device == null)
			return;

		device.setFullScreenWindow(window);

		if (bestMode != null)
			device.setDisplayMode(bestMode);

		requestFocus();
	}

	private static void setMacIconImage(Image iconImage) {
		// call com.apple.eawt.Application.getApplication().setDockIconImage(img) without a direct
		// dependency
		try {
			Class<?> c = Class.forName("com.apple.eawt.Application");
			Method m = c.getDeclaredMethod("getApplication");
			Object o = m.invoke(null);
			m = c.getDeclaredMethod("setDockIconImage", Image.class);
			m.invoke(o, iconImage);
		} catch (Exception e) {
			// ignore, we're not on Mac
		}
	}

	@Override
	public void update(Graphics g) {
		paint(g);
	}
}