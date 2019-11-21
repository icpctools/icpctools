package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ClientLauncher;
import org.icpc.tools.presentation.contest.internal.DigitalFont;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.standalone.TeamUtil;

public class TeamDisplayPresentation extends AbstractICPCPresentation {
	private static final int MARGIN = 15;
	private static final Color DARK_GRAY = new Color(8, 8, 8);

	private long touchTime = -100;
	private String touchType;
	private long touchTime2 = -100;
	private String touchType2;
	private static int leftTeam = -1;
	private static int rightTeam = -1;

	private Font font;
	private Font fontTouch;

	private BufferedImage image;
	private BufferedImage contestImage;
	private String teamName;
	private String teamId;

	private static final Color[] FADE;
	private static Job job;

	static {
		int SIZE = 25;
		FADE = new Color[SIZE];
		for (int i = 0; i < SIZE; i++) {
			int x = (int) (255f * (SIZE - i) / SIZE);
			FADE[i] = new Color(x, x / 8, x / 8, x);
		}
	}

	public TeamDisplayPresentation() {
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				touched("mouse");
			}
		});
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				touched("keyboard");
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				touched("mouse");
			}
		});
	}

	@Override
	public void init() {
		setTeam(TeamUtil.getTeamId());
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		final float dpi = 96;
		font = ICPCFont.getMasterFont().deriveFont(Font.BOLD, height * 36f / 6f / dpi);
		fontTouch = font;
	}

	public void setTeam(String id) {
		try {
			teamId = id;

			FloorMap map = FloorMap.getInstance(getContest());
			ITeam t = map.getTeamById(teamId + "");
			Trace.trace(Trace.INFO, "Floor map team: " + t);
			if (t != null) {
				ITeam tt = map.getTeamToLeftOf(t);
				if (tt != null)
					try {
						leftTeam = Integer.parseInt(tt.getId());
					} catch (Exception ex) {
						// ignore
					}
				tt = map.getTeamToRightOf(t);
				if (tt != null)
					try {
						rightTeam = Integer.parseInt(tt.getId());
					} catch (Exception ex) {
						// ignore
					}
				Trace.trace(Trace.INFO, leftTeam + " < > " + rightTeam);
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not determine teamId", e);
		}
	}

	protected void touched(String type) {
		touchType = type;
		touchTime = getTimeMs();

		// send over network
		if (leftTeam != -1 || rightTeam != -1) {
			if (job != null && !job.isComplete())
				return;

			job = execute(new Runnable() {
				@Override
				public void run() {
					if (leftTeam != -1)
						send(teamId, leftTeam);

					if (rightTeam != -1)
						send(teamId, rightTeam);

					// no more messages for at least 3s
					try {
						Thread.sleep(3000);
					} catch (Exception e) {
						// ignore
					}
				}
			});
		}
	}

	protected void send(String teamId2, int toTeamId) {
		try {
			ClientLauncher.getInstance().sendProperty(new int[] { toTeamId },
					"org.icpc.tools.presentation.contest.internal.presentations.TeamDisplayPresentation", "touch" + teamId2);
		} catch (IOException e) {
			System.out.println("Could not send message to team " + toTeamId);
		}
	}

	protected Long getClock() {
		IContest contest = getContest();
		if (contest == null)
			return null;

		Long startTime = contest.getStartStatus();
		if (startTime == null)
			return null;

		double timeMultiplier = contest.getTimeMultiplier();
		if (startTime < 0)
			return Math.round(startTime * timeMultiplier);

		return Math.round((getTimeMs() - startTime) * timeMultiplier);
	}

	@Override
	public long getDelayTimeMs() {
		// paint 20 times per second
		return 50;
	}

	@Override
	public void setProperty(String value) {
		if (value.startsWith("touch")) {
			long time = getTimeMs();

			try {
				String activeTeam = value.substring(5);
				Trace.trace(Trace.INFO, "Touch: " + activeTeam);
				int active = Integer.parseInt(activeTeam);
				if (active == leftTeam) {
					touchType2 = "Tell the team to the left to listen to instructions";
					touchTime2 = time;
				} else if (active == rightTeam) {
					touchType2 = "Tell the team to the right to listen to instructions";
					touchTime2 = time;
				}
			} catch (Exception e) {
				Trace.trace(Trace.ERROR, "Error reading property", e);
			}
		}
	}

	@Override
	public void aboutToShow() {
		execute(new Runnable() {
			@Override
			public void run() {
				cacheInfo();
			}
		});
	}

	protected void cacheInfo() {
		if (teamId == null || teamId.isEmpty() || image != null)
			return;

		ContestSource source = ContestSource.getInstance();
		if (source == null || !(source instanceof RESTContestSource))
			return;

		RESTContestSource restSource = (RESTContestSource) source;
		try {
			ITeam team = restSource.getTeam(teamId);

			IOrganization org = null;
			if (team != null && team.getOrganizationId() != null) {
				org = restSource.getOrganization(team.getOrganizationId());
				teamName = team.getActualDisplayName();
			}
			if (teamName == null)
				teamName = "Team " + teamId;

			// load logo
			if (image == null && org != null)
				image = org.getLogoImage((int) (width * 0.7), (int) ((height - MARGIN * 2) * 0.7), true, true);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not load team info", e);
		}
		if (image == null && contestImage == null)
			contestImage = getContest().getLogoImage((int) (width * 0.7), (int) ((height - MARGIN * 2) * 0.7), true, true);
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		if (image != null)
			g.drawImage(image, (width - image.getWidth()) / 2, (height - image.getHeight()) / 2, null);
		else if (contestImage != null)
			g.drawImage(contestImage, (width - contestImage.getWidth()) / 2, (height - contestImage.getHeight()) / 2,
					null);

		g.setColor(Color.WHITE);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		if (teamName != null) {
			String[] s = splitString(g, teamName, width - MARGIN * 2);
			for (int i = 0; i < s.length; i++) {
				g.drawString(s[i], (width - fm.stringWidth(s[i])) / 2,
						height - fm.getDescent() - MARGIN - (s.length - i - 1) * fm.getHeight());
			}
		}

		Long ms = getClock();
		if (ms != null) {
			String time = AbstractICPCPresentation.getTime(ms, true);

			int yh = (int) (height * 0.07f);
			int w = DigitalFont.stringWidth(time, yh);
			DigitalFont.drawString(g, time, width - w - MARGIN, MARGIN + yh, yh, Color.WHITE, DARK_GRAY);
		}

		g.setFont(fontTouch);
		fm = g.getFontMetrics();

		long delay = touchTime - getTimeMs() + 5000;
		long delay2 = touchTime2 - getTimeMs() + 5000;
		if (delay > 0) {
			if (delay <= 1500)
				g.setColor(FADE[(int) ((1f - delay / 1500f) * (FADE.length - 1))]);
			else
				g.setColor(FADE[0]);
			Stroke oldStroke = g.getStroke();
			g.setStroke(new BasicStroke(50));

			g.drawLine(0, 0, width - 0, height - 0);
			g.drawLine(0, height - 0, width - 0, 0);

			g.setStroke(oldStroke);
			String st = "Please don't touch the " + touchType;
			g.drawString(st, (width - fm.stringWidth(st)) / 2, fm.getAscent() + MARGIN);
		} else if (delay2 > 0) {
			if (delay2 <= 1500)
				g.setColor(FADE[(int) ((1f - delay2 / 1500f) * (FADE.length - 1))]);
			else
				g.setColor(FADE[0]);

			String st = touchType2;
			g.drawString(st, (width - fm.stringWidth(st)) / 2, fm.getAscent() + MARGIN);
		}
	}
}