package org.icpc.tools.presentation.contest.internal.presentations.map;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.Animator;
import org.icpc.tools.presentation.contest.internal.Animator3D;
import org.icpc.tools.presentation.contest.internal.ICPCFont;
import org.icpc.tools.presentation.contest.internal.ImageScaler;
import org.icpc.tools.presentation.contest.internal.TextHelper;

public class TeamIntroPresentation extends AbstractICPCPresentation {
	private static final long GROUP_INTRO_TIME = 4000;
	private static final long GROUP_SUMMARY_TIME = 7000;
	private static final long TIME_PER_GROUP = GROUP_INTRO_TIME + GROUP_SUMMARY_TIME;
	private static final long FADE = 500;

	private static final long FADE_IN_TIME = 1000;
	private static final long TEAM_TIME = 1500;
	private static final long FADE_OUT_TIME = 1000;
	private static final long TIME_PER_TEAM = FADE_IN_TIME + TEAM_TIME + FADE_OUT_TIME;

	private double timeFactor = 1;

	static class Position {
		double x;
		double y;
		double z;
		BufferedImage image;
		BufferedImage smImage;
		String label;

		double originalX = Double.NaN;
		double originalY = Double.NaN;

		// only for debugging
		double gridX = Double.NaN, gridY = Double.NaN;

		public Position(double x, double y, double z) {
			this(x, y, z, null);
		}

		public Position(double x, double y, double z, String label) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.label = label;
		}
	}

	static class GroupZoom {
		long startTime;
		long endTime;
		Position pos;
		String name;
		Position[] instPos;
	}

	protected Animator3D anim = new Animator3D(0.0, new Animator.Movement(60, 180), 0.0, new Animator.Movement(30, 90),
			1.0, new Animator.Movement(0.5, 1.5));

	protected Font groupFont;
	protected Font font;
	protected GroupZoom[] zooms;

	@Override
	public void init() {
		WorldMap.load(getClass());
	}

	@Override
	public long getRepeat() {
		setup();

		if (zooms == null)
			return 0;

		int numTeams = 0;
		for (GroupZoom gz : zooms)
			numTeams += gz.instPos.length;

		long repeat = numTeams * TIME_PER_TEAM + (zooms.length + 1) * TIME_PER_GROUP;
		return (long) (repeat / timeFactor);
	}

	@Override
	public void setProperty(String value) {
		if (value == null || value.isEmpty())
			return;
		if (value.startsWith("timeFactor:"))
			timeFactor = Double.parseDouble(value.replace("timeFactor:", ""));
		else
			super.setProperty(value);
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		final float dpi = 96;
		groupFont = ICPCFont.deriveFont(Font.BOLD, height * 72f * 0.12f / dpi);
		font = ICPCFont.deriveFont(Font.BOLD, height * 72f * 0.07f / dpi);
	}

	@Override
	public void aboutToShow() {
		setup();
	}

	private void setup() {
		if (zooms != null)
			return;

		IContest contest = getContest();
		if (contest == null)
			return;

		IGroup[] groups = contest.getGroups();
		int numGroups = groups.length;
		zooms = new GroupZoom[numGroups];
		long time = 0;
		for (int i = 0; i < numGroups; i++) {
			zooms[i] = setTargets(getContest(), groups[i].getId(), height);
			zooms[i].name = groups[i].getName();
			zooms[i].startTime = time;
			time += TIME_PER_GROUP + zooms[i].instPos.length * TIME_PER_TEAM;
			zooms[i].endTime = time;
		}
	}

	public static GroupZoom setTargets(IContest contest, String groupId, int height) {
		GroupZoom zoom = new GroupZoom();

		double minLat = 90;
		double maxLat = -90;
		double minLon = 180;
		double maxLon = -180;

		ITeam[] teams = contest.getTeams();
		List<Position> pos = new ArrayList<>();
		for (ITeam t : teams) {
			IOrganization org = contest.getOrganizationById(t.getOrganizationId());
			String[] groupIds = t.getGroupIds();
			if (org != null && (groupId == null || GroupPresentation.belongsToGroup(groupIds, groupId))) {
				double lat = org.getLatitude();
				if (!Double.isNaN(lat)) {
					minLat = Math.min(minLat, lat);
					maxLat = Math.max(maxLat, lat);
				}

				double lon = org.getLongitude();
				if (!Double.isNaN(lon)) {
					minLon = Math.min(minLon, lon);
					maxLon = Math.max(maxLon, lon);
				}
				String label = t.getId() + " - " + t.getActualDisplayName();
				Position p = new Position(lon, lat, 1, label);
				createOrgLogo(p, org, height);
				pos.add(p);
			}
		}
		zoom.instPos = pos.toArray(new Position[0]);

		// sort institutions by position
		Arrays.sort(zoom.instPos, (p1, p2) -> Double.compare(p1.x - p1.y, p2.x - p2.y));

		if (minLat == 90 || minLon == 180) {
			zoom.pos = new Position(0, 0, 1);
			return zoom;
		}

		// add some margin to the zoom-out, since logos may push eachother out of the bounding box
		final int GROUP_ZOOM_MARGIN_DEG = 3;
		minLon -= GROUP_ZOOM_MARGIN_DEG;
		maxLon += GROUP_ZOOM_MARGIN_DEG;
		minLat -= GROUP_ZOOM_MARGIN_DEG;
		maxLat += GROUP_ZOOM_MARGIN_DEG;

		double dLon = maxLon - minLon;
		double dLat = maxLat - minLat;
		zoom.pos = new Position(minLon + dLon / 2.0, minLat + dLat / 2.0,
				Math.min(5, Math.min(360.0 / (dLon + 20.0), 180.0 / (dLat + 10.0))));

		for (Position p : zoom.instPos) {
			// p.x = (p.x + zoom.pos.x) / 2.0;
			// p.y = (p.y + zoom.pos.y) / 2.0;
			p.z = zoom.pos.z * 1.15;
		}

		return zoom;
	}

	@Override
	public void incrementTimeMs(long dt) {
		dt *= timeFactor;
		if (zooms == null || zooms.length == 0)
			return;

		long time = (long) (getRepeatTimeMs() * timeFactor);
		int g = 0;
		GroupZoom gz = zooms[g];
		while (gz != null && time > gz.endTime) {
			g++;
			if (g > zooms.length - 1)
				gz = null;
			else
				gz = zooms[g];
		}
		if (gz == null) {
			anim.setTarget(0, 0, 1);
		} else {
			long gzTime = time - gz.startTime;
			if (gzTime < GROUP_INTRO_TIME || gzTime > GROUP_INTRO_TIME + gz.instPos.length * TIME_PER_TEAM) {
				Position p = gz.pos;
				anim.setTarget(p.x, p.y, p.z);

				if (gzTime < GROUP_INTRO_TIME) {
					// need to restore positions to original positions for teams not drift out
					// into the sea when placed on the second run
					BubbleOut.restore(gz.instPos);
				} else {
					BubbleOut.bubbleOut(gz.instPos, gz.instPos.length, width, height, 1 / anim.getZValue(), dt);
				}
			} else {
				gzTime -= GROUP_INTRO_TIME;
				int teamInd = (int) (gzTime / TIME_PER_TEAM);
				if (teamInd >= 0 && teamInd < gz.instPos.length) {
					Position p = gz.instPos[teamInd];
					anim.setTarget(p.x, p.y, p.z);
				}

				BubbleOut.bubbleOut(gz.instPos, teamInd, width, height, 1 / anim.getZValue(), dt);
			}
		}

		anim.incrementTimeMs(dt);
	}

	private static void createOrgLogo(Position p, IOrganization org, int height) {
		int logoSize2 = height / 4;
		p.image = org.getLogoImage(logoSize2, logoSize2, true, true);
		if (p.image != null) {
			int logoSize = height / 12;
			p.smImage = ImageScaler.scaleImage(p.image, logoSize, logoSize);
		}
	}

	@Override
	public void paint(Graphics2D g) {
		IContest contest = getContest();
		if (contest == null)
			return;

		long time = (long) (getRepeatTimeMs() * timeFactor);
		int gr = 0;

		double sc = anim.getZValue();
		double oX = width / 2.0 - (width * (anim.getXValue() + 180) * sc / 360.0);
		double oY = height / 2.0 - (height * (90 - anim.getYValue()) * sc / 180.0);

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		WorldMap.drawMap(g, (int) oX, (int) oY, width, height, sc);

		Graphics2D g2 = (Graphics2D) g.create();
		g2.translate((int) oX, (int) oY);

		if (zooms == null || zooms.length == 0)
			return;

		GroupZoom gz = zooms[gr];
		while (gz != null && time > gz.endTime) {
			gr++;
			if (gr > zooms.length - 1)
				gz = null;
			else
				gz = zooms[gr];
		}

		if (gz == null)
			return;

		// draw group name
		int border = height / 40;

		long gzTime = time - gz.startTime;
		if (gzTime < GROUP_INTRO_TIME) {
			if (gzTime < 1000)
				g.setComposite(AlphaComposite.SrcOver.derive(gzTime / 1000f));
			else if (gzTime > GROUP_INTRO_TIME - 1000)
				g.setComposite(AlphaComposite.SrcOver.derive((GROUP_INTRO_TIME - gzTime) / 1000f));
			String s = gz.name;
			g.setFont(groupFont);
			FontMetrics fm = g.getFontMetrics();
			g.setColor(Color.DARK_GRAY);
			g.drawString(s, (width - fm.stringWidth(s)) / 2, (height + fm.getAscent()) / 2 + 2);
			g.drawString(s, (width - fm.stringWidth(s)) / 2 + 2, (height + fm.getAscent()) / 2);
			g.setColor(Color.WHITE);
			g.drawString(s, (width - fm.stringWidth(s)) / 2, (height + fm.getAscent()) / 2);
			return;
		} else if (gzTime > GROUP_INTRO_TIME + gz.instPos.length * TIME_PER_TEAM) {
			String s = gz.name;
			g.setFont(font);
			FontMetrics fm = g.getFontMetrics();
			g.setColor(Color.DARK_GRAY);
			g.drawString(s, (width - fm.stringWidth(s)) / 2 + 2, height - border);
			g.drawString(s, (width - fm.stringWidth(s)) / 2, height - border + 2);
			g.setColor(Color.WHITE);
			g.drawString(s, (width - fm.stringWidth(s)) / 2, height - border);
		}

		gzTime -= GROUP_INTRO_TIME;
		long teamInd = gzTime / TIME_PER_TEAM;

		// draw past teams
		if (gzTime > gz.instPos.length * TIME_PER_TEAM) {
			long ttime = gzTime - gz.instPos.length * TIME_PER_TEAM;
			if (ttime < 500)
				g2.setComposite(AlphaComposite.SrcOver.derive(0.75f + ttime / 2000f));
			else if (ttime > GROUP_SUMMARY_TIME - 1000)
				g2.setComposite(AlphaComposite.SrcOver.derive((GROUP_SUMMARY_TIME - ttime) / 1000f));
		} else
			g2.setComposite(AlphaComposite.SrcOver.derive(0.75f));
		for (int i = 0; i < teamInd; i++) {
			if (gz.instPos.length > i && gz.instPos[i] != null && gz.instPos[i].smImage != null) {
				int x = (int) (width * (gz.instPos[i].x + 180.0) * sc / 360.0);
				int y = (int) (height * (90 - gz.instPos[i].y) * sc / 180.0);
				g2.drawImage(gz.instPos[i].smImage, x - gz.instPos[i].smImage.getWidth() / 2,
						y - gz.instPos[i].smImage.getHeight() / 2, null);
			}
		}

		if (gzTime > gz.instPos.length * TIME_PER_TEAM)
			return;

		// draw current team
		long teamTime = gzTime - (long) Math.floor(teamInd) * TIME_PER_TEAM;
		if (teamTime < FADE) {
			g.setComposite(AlphaComposite.SrcOver.derive((float) teamTime / FADE));
			g2.setComposite(AlphaComposite.SrcOver.derive((float) teamTime / FADE));
		} else if (teamTime > TIME_PER_TEAM - FADE) {
			long tt = teamTime - (TIME_PER_TEAM - FADE);
			g.setComposite(AlphaComposite.SrcOver.derive(0.333f + (FADE - tt) * 2f / (FADE * 3f)));
			g2.setComposite(AlphaComposite.SrcOver.derive(0.75f + (FADE - tt) / (FADE * 4f)));
		} else {
			g2.setComposite(AlphaComposite.SrcOver.derive(1f));
		}
		int team = (int) teamInd;
		if (team < 0 || team > gz.instPos.length - 1)
			return;

		Position pos = gz.instPos[team];
		if (pos.image != null) {
			int w = pos.image.getWidth();
			int h = pos.image.getHeight();
			int x = (int) (width * (pos.x + 180.0) * sc / 360.0);
			int y = (int) (height * (90 - pos.y) * sc / 180.0);

			if (teamTime > TIME_PER_TEAM - FADE) {
				long tt = teamTime - (TIME_PER_TEAM - FADE);
				double scale = 0.5 + (FADE - tt) / (FADE * 2.0);
				double ww = w * scale / 2.0;
				double hh = h * scale / 2.0;
				g2.drawImage(pos.image, x - (int) ww, y - (int) hh, x + (int) ww, y + (int) hh, 0, 0, w, h, null);
			} else
				g2.drawImage(pos.image, x - w / 2, y - h / 2, null);
		}
		g2.dispose();

		// draw team name and logo across the bottom
		g.setFont(font);
		TextHelper text = new TextHelper(g);
		if (pos.smImage != null) {
			text.addImage(pos.smImage);
			text.addSpacer(border);
		}

		text.addString(pos.label);
		int h = Math.max(border, height / 12) + border;
		int y = height - border - h;

		g.setComposite(AlphaComposite.SrcOver.derive(0.5f));
		g.setColor(Color.BLACK);
		g.fillRect(0, y, width, h);
		g.setComposite(AlphaComposite.SrcOver.derive(1f));
		g.setColor(Color.WHITE);
		text.drawFit(Math.max(border, width - text.getWidth()) / 2, y + (h - text.getHeight()) / 2, width - border * 2);
	}
}