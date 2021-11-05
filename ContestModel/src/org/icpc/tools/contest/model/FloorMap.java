package org.icpc.tools.contest.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.feed.ContestWriter;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.internal.ContestObject;
import org.icpc.tools.contest.model.internal.MapInfo;
import org.icpc.tools.contest.model.internal.MapInfo.Aisle;
import org.icpc.tools.contest.model.internal.MapInfo.Printer;
import org.icpc.tools.contest.model.internal.Problem;
import org.icpc.tools.contest.model.internal.Team;

public class FloorMap {
	public static final double N = 90;
	public static final double E = 0;
	public static final double S = 270;
	public static final double W = 180;

	// default colors for printing
	public static class FloorColors {
		public Color getTeamAreaFillColor() {
			return null;
		}

		public Color getTeamAreaOutlineColor() {
			return Color.LIGHT_GRAY;
		}

		public Color getSeatFillColor() {
			return null;
		}

		public Color getSeatOutlineColor() {
			return Color.LIGHT_GRAY;
		}

		public BufferedImage getTeamLogo(String id) {
			return null;
		}

		public Color getDeskOutlineColor(String id) {
			return Color.BLACK;
		}

		public Color getDeskFillColor(String id) {
			return Color.WHITE;
		}

		public Color getTextColor() {
			return Color.BLACK;
		}
	}

	public static class ScreenColors extends FloorColors {
		@Override
		public Color getTeamAreaFillColor() {
			return Color.DARK_GRAY;
		}

		@Override
		public Color getTeamAreaOutlineColor() {
			return null;
		}

		@Override
		public Color getSeatFillColor() {
			return Color.DARK_GRAY;
		}

		@Override
		public Color getSeatOutlineColor() {
			return Color.DARK_GRAY;
		}

		@Override
		public Color getDeskOutlineColor(String id) {
			return Color.WHITE;
		}

		@Override
		public Color getDeskFillColor(String id) {
			return Color.BLACK;
		}

		@Override
		public Color getTextColor() {
			return Color.WHITE;
		}
	}

	private Rectangle2D.Double tBounds;
	private Rectangle2D.Double otBounds;
	private int aisleCounter;

	public static class AisleIntersection {
		IAisle a1;
		IAisle a2;
		public double x;
		public double y;

		@Override
		public String toString() {
			return "Intersection: " + a1 + " and " + a2 + " at " + x + "," + y;
		}
	}

	protected IContest contest;

	// computed
	private List<AisleIntersection> aisleIntersections = new ArrayList<>();

	public FloorMap(IContest c) {
		contest = c;
		computeAisleIntersections();
	}

	/**
	 * Create a new floor map with the given team area and team desk widths and depths, in meters.
	 *
	 * The ICPC standards are: Team area: 1.8 x 0.85, Team desk: 3.0 x 2.2.
	 */
	public FloorMap(double taw, double tad, double tw, double td) {
		contest = new Contest();
		createMapInfo(taw, tad, tw, td);
	}

	public void makeSpare(int teamNum) {
		ITeam team = contest.getTeamById(teamNum + "");
		((Contest) contest).removeFromHistory(team);
		((Team) team).add("id", "-1");
		MapInfo mapInfo = (MapInfo) contest.getMapInfo();
		mapInfo.addSpareTeam(team);
	}

	/**
	 * Write the map data to the given contest archive format base folder.
	 *
	 *
	 * @param folder
	 */
	public void write(File folder) {
		resetOrigin();

		ContestWriter.write(contest, new File(folder, "extend-floor"));
	}

	public Rectangle2D.Double getBounds(boolean onlyTeams) {
		if (onlyTeams && otBounds != null)
			return otBounds;
		if (!onlyTeams && tBounds != null)
			return tBounds;
		double x1 = 0;
		double y1 = 0;
		double x2 = 0;
		double y2 = 0;

		IMapInfo mapInfo = contest.getMapInfo();
		if (mapInfo == null)
			return null;

		double tableWidth = mapInfo.getTableWidth();
		double tableDepth = mapInfo.getTableDepth();

		for (ITeam t : contest.getTeams()) {
			if (Double.isNaN(t.getX()) || Double.isNaN(t.getY()))
				continue;

			AffineTransform transform = AffineTransform.getTranslateInstance(t.getX(), t.getY());
			transform.concatenate(AffineTransform.getRotateInstance(Math.toRadians(t.getRotation())));

			Rectangle2D.Double r = new Rectangle2D.Double(-tableDepth / 2f, -tableWidth / 2f, tableDepth, tableWidth);
			Shape s = transform.createTransformedShape(r);
			Rectangle2D r2 = s.getBounds2D();
			x1 = Math.min(x1, r2.getMinX());
			y1 = Math.min(y1, r2.getMinY());
			x2 = Math.max(x2, r2.getMaxX());
			y2 = Math.max(y2, r2.getMaxY());
		}

		if (!onlyTeams) {
			int bd = 2;
			for (IProblem b : contest.getProblems()) {
				x1 = Math.min(x1, b.getX() - bd);
				y1 = Math.min(y1, b.getY() - bd);
				x2 = Math.max(x2, b.getX() + bd);
				y2 = Math.max(y2, b.getY() + bd);
			}

			for (IAisle a : mapInfo.getAisles()) {
				x1 = Math.min(x1, a.getX1());
				y1 = Math.min(y1, a.getY1());
				x2 = Math.max(x2, a.getX1());
				y2 = Math.max(y2, a.getY1());
				x1 = Math.min(x1, a.getX2());
				y1 = Math.min(y1, a.getY2());
				x2 = Math.max(x2, a.getX2());
				y2 = Math.max(y2, a.getY2());
			}
		}

		// add small gap
		// TODO gap should be configurable
		double gap = 2f;
		x1 -= gap;
		y1 -= gap;
		x2 += gap;
		y2 += gap;
		Rectangle2D.Double b = new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);
		if (onlyTeams)
			otBounds = b;
		else
			tBounds = b;
		return b;
	}

	private void computeAisleIntersections() {
		aisleIntersections = new ArrayList<>();
		IMapInfo mapInfo = contest.getMapInfo();
		if (mapInfo == null)
			return;

		List<IAisle> aisles = mapInfo.getAisles();
		int size = aisles.size();
		for (int i = 0; i < size - 1; i++) {
			for (int j = i + 1; j < size; j++) {
				IAisle a1 = aisles.get(i);
				IAisle a2 = aisles.get(j);

				double xd1 = a1.getX2() - a1.getX1();
				double yd1 = a1.getY2() - a1.getY1();

				double xd2 = a2.getX2() - a2.getX1();
				double yd2 = a2.getY2() - a2.getY1();

				double denom = xd2 * yd1 - yd2 * xd1;

				if (denom != 0f) {
					AisleIntersection ai = new AisleIntersection();
					ai.a1 = a1;
					ai.a2 = a2;
					double s1 = (yd2 * (a1.getX1() - a2.getX1()) - xd2 * (a1.getY1() - a2.getY1())) / denom;

					if (s1 >= 0f && s1 <= 1f) {
						double s2 = (yd1 * (a1.getX1() - a2.getX1()) - xd1 * (a1.getY1() - a2.getY1())) / denom;
						if (s2 >= 0f && s2 <= 1f) {
							ai.x = a1.getX1() + s1 * xd1;
							ai.y = a1.getY1() + s1 * yd1;
							aisleIntersections.add(ai);
						}
					}

					// TODO - check if point is on the other line!!
				}
			}
		}
	}

	protected List<AisleIntersection> getIntersections(IAisle a) {
		List<AisleIntersection> list = new ArrayList<>(3);
		if (a == null)
			return list;

		for (AisleIntersection ai : aisleIntersections) {
			if (ai.a1 == a || ai.a2 == a)
				list.add(ai);
		}
		return list;
	}

	public static class Path {
		public final List<AisleIntersection> list = new ArrayList<>();
		public double[] segLength;
		private double dist;

		public double getDistance() {
			return dist;
		}

		public Point2D getInterimPosition(double d) {
			if (d < 0 || d > dist)
				return null;

			if (segLength == null) {
				segLength = new double[list.size() - 1];
				for (int i = 0; i < list.size() - 1; i++) {
					AisleIntersection ai1 = list.get(i);
					AisleIntersection ai2 = list.get(i + 1);
					double dx = ai1.x - ai2.x;
					double dy = ai1.y - ai2.y;
					segLength[i] = Math.sqrt(dx * dx + dy * dy);
				}
			}
			int i = 0;
			double dd = d;
			while (dd >= 0) {
				if (i >= segLength.length) {
					// we're at the end, but over due to floating point precision
					AisleIntersection ai2 = list.get(list.size() - 1);
					return new Point2D.Double(ai2.x, ai2.y);
				} else if (dd > segLength[i]) {
					dd -= segLength[i];
				} else {
					AisleIntersection ai1 = list.get(i);
					AisleIntersection ai2 = list.get(i + 1);
					double x = (ai1.x - ai2.x) * (dd / segLength[i]);
					double y = (ai1.y - ai2.y) * (dd / segLength[i]);
					return new Point2D.Double(ai1.x - x, ai1.y - y);
				}
				i++;
			}
			return null;
		}

		@Override
		public String toString() {
			String s = "Path (dist: " + dist + ", segments: " + list.size() + "):";
			for (AisleIntersection ai : list)
				s += "\n  " + ai.x + "," + ai.y;
			return s;
		}
	}

	private static Path copyPath(Path path) {
		Path newPath = new Path();
		for (AisleIntersection ai : path.list)
			newPath.list.add(ai);

		newPath.dist = path.dist;
		return newPath;
	}

	private void calculateShortestPaths(Map<AisleIntersection, Path> paths, AisleIntersection from,
			AisleIntersection end) {
		Path path = paths.get(from);
		double curDist = path.dist;

		// otherwise go another step
		List<AisleIntersection> list = getIntersections(from.a1);
		if (from.a1 == end.a1)
			list.add(end);
		for (AisleIntersection ai : list) {
			Path p = paths.get(ai);
			double dx = from.x - ai.x;
			double dy = from.y - ai.y;
			double dist = curDist + Math.sqrt(dx * dx + dy * dy);

			if (p == null || dist < p.dist) {
				Path newPath = copyPath(path);
				newPath.list.add(ai);
				newPath.dist = dist;
				paths.put(ai, newPath);

				calculateShortestPaths(paths, ai, end);
			}
		}

		list = getIntersections(from.a2);
		if (from.a2 == end.a1)
			list.add(end);
		for (AisleIntersection ai : list) {
			Path p = paths.get(ai);
			double dx = from.x - ai.x;
			double dy = from.y - ai.y;
			double dist = curDist + Math.sqrt(dx * dx + dy * dy);

			if (p == null || dist < p.dist) {
				Path newPath = copyPath(path);
				newPath.list.add(ai);
				newPath.dist = dist;
				paths.put(ai, newPath);

				calculateShortestPaths(paths, ai, end);
			}
		}
	}

	public Path getPath(IPosition t1, IPosition t2) {
		if (t1 == null || t2 == null)
			return null;

		AisleIntersection startIntersection = getClosestAisle(t1.getX(), t1.getY());
		AisleIntersection endIntersection = getClosestAisle(t2.getX(), t2.getY());

		if (startIntersection == null || endIntersection == null)
			return null;

		Path path = new Path();
		path.list.add(startIntersection);

		Map<AisleIntersection, Path> map = new HashMap<>();
		map.put(startIntersection, path);
		calculateShortestPaths(map, startIntersection, endIntersection);

		Path best = map.get(endIntersection);
		if (best == null)
			return null;

		AisleIntersection start = new AisleIntersection();
		start.x = t1.getX();
		start.y = t1.getY();
		double dx = startIntersection.x - start.x;
		double dy = startIntersection.y - start.y;
		best.dist += Math.sqrt(dx * dx + dy * dy);
		best.list.add(0, start);

		AisleIntersection end = new AisleIntersection();
		end.x = t2.getX();
		end.y = t2.getY();
		dx = endIntersection.x - end.x;
		dy = endIntersection.y - end.y;
		best.dist += Math.sqrt(dx * dx + dy * dy);
		best.list.add(end);

		return best;
	}

	public AisleIntersection getClosestAisle(double x, double y) {
		AisleIntersection ai = new AisleIntersection();
		double dist = Double.MAX_VALUE;
		IMapInfo mapInfo = contest.getMapInfo();
		if (mapInfo == null)
			return null;

		for (IAisle a : mapInfo.getAisles()) {
			double xd = a.getX2() - a.getX1();
			double yd = a.getY2() - a.getY1();

			double k = xd * x - xd * a.getX1() + yd * y - yd * a.getY1();
			k /= (xd * xd + yd * yd);

			if (k < 0f)
				k = 0f;
			else if (k > 1f)
				k = 1f;

			double xc = a.getX1() + k * xd;
			double yc = a.getY1() + k * yd;

			double dx = (x - xc);
			double dy = (y - yc);
			double dist2 = Math.sqrt(dx * dx + dy * dy);
			// System.out.println("point: " + xc + "," + yc + " - " + dist);

			if (dist2 < dist) {
				ai.a1 = a;
				ai.x = xc;
				ai.y = yc;
				dist = dist2;
			}
		}
		// System.out.println("Closest: " + ai + " " + dist);
		return ai;
	}

	public ITeam getTeam(int teamNum) {
		return contest.getTeamById(teamNum + "");
	}

	public void drawFloor(Graphics2D g, Rectangle r, String teamId, boolean showAisles, Path... paths) {
		Rectangle2D.Double bounds = getBounds(false);
		double scale = Math.min(r.width / bounds.width, r.height / bounds.height);
		int x1 = r.x - (int) (bounds.x * scale);
		int y1 = r.y - (int) (bounds.y * scale);

		IMapInfo mapInfo = contest.getMapInfo();
		if (mapInfo == null)
			return;

		if (showAisles) {
			g.setColor(Color.LIGHT_GRAY);
			for (IAisle a : mapInfo.getAisles()) {
				int ax1 = r.x + (int) ((a.getX1() - bounds.x) * scale);
				int ay1 = r.y + (int) ((a.getY1() - bounds.y) * scale);
				int ax2 = r.x + (int) ((a.getX2() - bounds.x) * scale);
				int ay2 = r.y + (int) ((a.getY2() - bounds.y) * scale);
				g.drawLine(ax1, ay1, ax2, ay2);
			}

			for (AisleIntersection a : aisleIntersections) {
				int ax = r.x + (int) ((a.x - bounds.x) * scale);
				int ay = r.y + (int) ((a.y - bounds.y) * scale);
				g.fillRect(ax - 1, ay - 1, 3, 3);
			}
		}

		if (paths != null) {
			for (Path p : paths) {
				if (p != null) {
					Graphics2D gg = (Graphics2D) g.create();
					gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

					gg.translate(r.x - (int) (bounds.x * scale), r.y - (int) (bounds.y * scale));
					gg.setStroke(new BasicStroke(3));
					gg.setColor(Color.GRAY);
					int x = 0;
					int y = 0;
					boolean first = true;
					for (AisleIntersection ai : p.list) {
						int nx = (int) (scale * ai.x);
						int ny = (int) (scale * ai.y);
						if (!first)
							gg.drawLine(x, y, nx, ny);
						else
							first = false;
						x = nx;
						y = ny;
					}
					gg.dispose();
				}
			}
		}

		double tableWidth = mapInfo.getTableWidth();
		double tableDepth = mapInfo.getTableDepth();
		double teamAreaWidth = mapInfo.getTeamAreaWidth();
		double teamAreaDepth = mapInfo.getTeamAreaDepth();

		g.setColor(Color.BLACK);
		for (ITeam t : contest.getTeams()) {
			if (Double.isNaN(t.getX()) || Double.isNaN(t.getY()))
				continue;

			Graphics2D gg = createGraphics(g, t, x1, y1, scale);

			// team area
			// Rectangle2D.Float tr1 = new Rectangle2D.Float(-(teamAreaDepth + 1.0f) * scale / 2f,
			// -teamAreaWidth * scale
			Rectangle2D.Double tr1 = new Rectangle2D.Double(-(teamAreaDepth + 1f) * scale / 2f,
					-teamAreaWidth * scale / 2f, teamAreaDepth * scale, teamAreaWidth * scale);
			// Shape s = transform.createTransformedShape(tr);
			// gg.setTransform(transform);
			gg.setColor(Color.WHITE);
			gg.fill(tr1);
			gg.setColor(Color.LIGHT_GRAY);
			gg.draw(tr1);
			gg.setColor(Color.BLACK);

			// chairs
			double c = tableWidth * scale / 8f;
			double rnd = 0f;// c * 0.5f;
			for (int i = 0; i < 3; i++) {
				// RoundRectangle2D.Float tr = new RoundRectangle2D.Float(-tableDepth * scale * 0.75f,
				// -tableWidth * scale
				RoundRectangle2D.Double tr = new RoundRectangle2D.Double(-tableDepth * scale,
						-tableWidth * scale / 2f + c * i * 2.5f + c / 2f, tableDepth * scale * 0.25f, c * 2f, rnd, rnd);
				// Shape s = transform.createTransformedShape(tr);
				// gg.draw(s);
				gg.draw(tr);
			}

			// table
			Rectangle2D.Double tr = new Rectangle2D.Double(-tableDepth * scale / 2f, -tableWidth * scale / 2f,
					tableDepth * scale, tableWidth * scale);
			// Shape s = transform.createTransformedShape(tr);
			// gg.setTransform(transform);
			String id = t.getId();
			if (teamId.equals(id)) {
				gg.setColor(Color.DARK_GRAY);
				gg.fill(tr);
				gg.setColor(Color.BLACK);
				gg.draw(tr);
				gg.setColor(Color.WHITE);
			} else
				gg.draw(tr);

			FontMetrics fm = gg.getFontMetrics();
			if (id != null) {
				AffineTransform transform2 = AffineTransform.getRotateInstance(Math.toRadians(90));
				AffineTransform at = gg.getTransform();
				at.concatenate(transform2);
				gg.setTransform(at);
				gg.drawString(id, -fm.stringWidth(id) / 2f, (fm.getAscent() - 3.5f) / 2f);
			}
			gg.dispose();
		}

		for (IProblem p : contest.getProblems()) {
			double dim = 1.5f;
			double d = dim * scale;
			int x = r.x + (int) ((p.getX() - bounds.x) * scale);
			int y = r.y + (int) ((p.getY() - bounds.y) * scale);
			g.setColor(Color.WHITE);
			g.fillOval(x - (int) (d / 2f), y - (int) (d / 2f), (int) d, (int) d);
			g.setColor(Color.BLACK);
			g.drawOval(x - (int) (d / 2f), y - (int) (d / 2f), (int) d, (int) d);
			FontMetrics fm = g.getFontMetrics();
			g.setColor(Color.BLACK);
			g.drawString(p.getId(), x - fm.stringWidth(p.getId()) / 2f, y + (fm.getAscent() / 2.5f));
		}
	}

	private static Graphics2D createGraphics(Graphics2D g, ITeam t, int x1, int y1, double scale) {
		Graphics2D gg = (Graphics2D) g.create();
		gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		gg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		gg.translate(x1 + t.getX() * scale, y1 + t.getY() * scale);
		// AffineTransform transform = AffineTransform.getTranslateInstance(x1 + t.x * scale, y1 +
		// t.y * scale);
		// transform.concatenate(AffineTransform.getRotateInstance(Math.toRadians(t.rotation)));

		AffineTransform transform = AffineTransform.getRotateInstance(-Math.toRadians(t.getRotation()));
		// transform.concatenate(AffineTransform.getTranslateInstance(x1 + t.x * scale, y1 + t.y *
		// scale));
		AffineTransform at = gg.getTransform();
		at.concatenate(transform);
		gg.setTransform(at);
		return gg;
	}

	private static void draw(Graphics2D g, Shape s, Color fill, Color outline) {
		if (fill != null) {
			g.setColor(fill);
			g.fill(s);
		}
		if (outline != null) {
			g.setColor(outline);
			g.draw(s);
		}
	}

	public Point2D getPosition(Rectangle r, Point2D p) {
		Rectangle2D.Double bounds = getBounds(true);
		double scale = Math.min(r.width / bounds.width, r.height / bounds.height);
		int ww = (int) (bounds.width * scale);
		int hh = (int) (bounds.height * scale);
		int x1 = (r.width - ww) / 2 + r.x - (int) ((bounds.x - p.getX()) * scale);
		int y1 = (r.height - hh) / 2 + r.y - (int) ((bounds.y - p.getY()) * scale);
		return new Point2D.Double(x1, y1);
		// return new Point2D.Double(r.x - (int) ((bounds.x - p.getX()) * scale),
		// r.y - (int) ((bounds.y - p.getY()) * scale));
	}

	public Point2D getPosition(Rectangle r, ITeam team) {
		double teamAreaDepth = 0;
		IMapInfo mapInfo = contest.getMapInfo();
		if (mapInfo != null)
			teamAreaDepth = mapInfo.getTeamAreaDepth();

		double rad = Math.toRadians(team.getRotation() + 180);
		double x = (team.getX() + Math.cos(rad) * teamAreaDepth / 3);
		double y = (team.getY() - Math.sin(rad) * teamAreaDepth / 3);
		return getPosition(r, x, y);
	}

	public Point2D getPosition(Rectangle r, double x, double y) {
		Rectangle2D.Double bounds = getBounds(true);
		double scale = Math.min(r.width / bounds.width, r.height / bounds.height);
		int ww = (int) (bounds.width * scale);
		int hh = (int) (bounds.height * scale);
		int x1 = (r.width - ww) / 2 + r.x - (int) ((bounds.x - x) * scale);
		int y1 = (r.height - hh) / 2 + r.y - (int) ((bounds.y - y) * scale);
		return new Point2D.Double(x1, y1);
		// return new Point2D.Double(r.x - (int) ((bounds.x - x) * scale), r.y - (int) ((bounds.y -
		// y) * scale));
	}

	public void drawPath(Graphics2D g, Rectangle r, Path p) {
		Rectangle2D.Double bounds = getBounds(true);
		double scale = Math.min(r.width / bounds.width, r.height / bounds.height);
		int ww = (int) (bounds.width * scale);
		int hh = (int) (bounds.height * scale);
		int x1 = (r.width - ww) / 2 + r.x - (int) (bounds.x * scale);
		int y1 = (r.height - hh) / 2 + r.y - (int) (bounds.y * scale);

		Graphics2D gg = (Graphics2D) g.create();
		gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		// gg.translate(r.x - (int) (bounds.x * scale), r.y - (int) (bounds.y * scale));
		gg.translate(x1, y1);
		int x = 0;
		int y = 0;
		boolean first = true;
		for (AisleIntersection ai : p.list) {
			int nx = (int) (scale * ai.x);
			int ny = (int) (scale * ai.y);
			if (!first)
				gg.drawLine(x, y, nx, ny);
			else
				first = false;
			x = nx;
			y = ny;
		}
		gg.dispose();
	}

	public void drawFloor(Graphics2D g, Rectangle r, FloorColors colors) {
		Rectangle2D.Double bounds = getBounds(true);
		double scale = Math.min(r.width / bounds.width, r.height / bounds.height);
		int ww = (int) (bounds.width * scale);
		int hh = (int) (bounds.height * scale);
		int x1 = (r.width - ww) / 2 + r.x - (int) (bounds.x * scale);
		int y1 = (r.height - hh) / 2 + r.y - (int) (bounds.y * scale);

		IMapInfo mapInfo = contest.getMapInfo();
		if (mapInfo == null)
			return;

		double tableWidth = mapInfo.getTableWidth();
		double tableDepth = mapInfo.getTableDepth();
		double teamAreaWidth = mapInfo.getTeamAreaWidth();
		double teamAreaDepth = mapInfo.getTeamAreaDepth();

		g.setFont(g.getFont().deriveFont((float) (tableWidth * scale / 2.75f)));
		FontMetrics fm = g.getFontMetrics();

		for (ITeam t : contest.getTeams()) {
			if (Double.isNaN(t.getX()) || Double.isNaN(t.getY()))
				continue;

			Graphics2D gg = createGraphics(g, t, x1, y1, scale);

			Rectangle2D.Double tr1 = new Rectangle2D.Double(-(teamAreaDepth + 1.0f) * scale / 2f,
					-teamAreaWidth * scale / 2f, teamAreaDepth * scale, teamAreaWidth * scale);
			draw(gg, tr1, colors.getTeamAreaFillColor(), colors.getTeamAreaOutlineColor());

			// seats
			double c = tableWidth * scale / 8f;
			float rnd = 0f;// c * 0.5f;
			for (int i = 0; i < 3; i++) {
				RoundRectangle2D.Double tr = new RoundRectangle2D.Double(-tableDepth * scale * 0.75f,
						-tableWidth * scale / 2f + c * i * 2.5f + c / 2f, tableDepth * scale * 0.25f, c * 2f, rnd, rnd);
				draw(gg, tr, colors.getSeatFillColor(), colors.getSeatOutlineColor());
			}

			// table
			Rectangle2D.Double tr = new Rectangle2D.Double(-tableDepth * scale / 2f, -tableWidth * scale / 2f,
					tableDepth * scale, tableWidth * scale);
			String id = t.getId();
			draw(gg, tr, colors.getDeskFillColor(id), colors.getDeskOutlineColor(id));
			BufferedImage img = colors.getTeamLogo(id);

			gg.setColor(colors.getTextColor());

			if (id != null && img == null) {
				AffineTransform transform2 = AffineTransform.getRotateInstance(Math.toRadians(90));
				if (t.getRotation() == 270)
					transform2 = AffineTransform.getRotateInstance(Math.toRadians(t.getRotation()));
				AffineTransform at = gg.getTransform();
				at.concatenate(transform2);
				gg.setTransform(at);
				gg.drawString(id, -fm.stringWidth(id) / 2f, (fm.getAscent() - 3.5f) / 2f);
			}
			gg.dispose();
		}

		for (ITeam t : contest.getTeams()) {
			if (Double.isNaN(t.getX()) || Double.isNaN(t.getY()))
				continue;

			String id = t.getId();
			BufferedImage img = colors.getTeamLogo(id);

			if (img != null) {
				double rad = Math.toRadians(t.getRotation() + 180);
				double x = x1 + (t.getX() + Math.cos(rad) * teamAreaDepth / 3) * scale;
				double y = y1 + (t.getY() - Math.sin(rad) * teamAreaDepth / 3) * scale;

				g.drawImage(img, (int) (x - img.getWidth() / 2), (int) (y - img.getHeight() / 2), null);
			}
		}
	}

	public ITeam getTeamToLeftOf(ITeam team) {
		return getTeamInDirection(team, 90);
	}

	public ITeam getTeamToRightOf(ITeam team) {
		return getTeamInDirection(team, 270);
	}

	private ITeam getTeamInDirection(ITeam current, int angle) {
		if (current == null)
			return null;

		IMapInfo mapInfo = contest.getMapInfo();
		if (mapInfo == null)
			return null;

		double tableDepth = mapInfo.getTableDepth();
		double teamAreaWidth = mapInfo.getTeamAreaWidth();

		double rad = Math.toRadians(current.getRotation() + angle);
		float x = (float) (current.getX() + Math.cos(rad) * teamAreaWidth);
		float y = (float) (current.getY() - Math.sin(rad) * teamAreaWidth);
		for (ITeam t : contest.getTeams()) {
			if (!current.equals(t) && Math.abs(t.getX() - x) < tableDepth && Math.abs(t.getY() - y) < tableDepth)
				return t;
		}
		return null;
	}

	public IMapInfo createMapInfo(double taw, double tad, double tw, double td) {
		MapInfo mapInfo = new MapInfo();
		mapInfo.add("table_width", tw + "");
		mapInfo.add("table_depth", td + "");
		mapInfo.add("team_area_width", taw + "");
		mapInfo.add("team_area_depth", tad + "");
		((Contest) contest).add(mapInfo);
		return mapInfo;
	}

	public ITeam createTeam(int num, double x, double y, double rotation) {
		if (num < 0)
			return createTeam("", x, y, rotation);
		return createTeam(num + "", x, y, rotation);
	}

	public ITeam createTeam(String id, double x, double y, double rotation) {
		Team t = new Team();
		t.add("id", id);
		JsonObject obj = new JsonObject();
		obj.props.put("x", x + "");
		obj.props.put("y", y + "");
		obj.props.put("rotation", rotation + "");
		t.add("location", obj);
		((Contest) contest).add(t);
		return t;
	}

	/**
	 * Create a series of teams, starting from startingId at position x, y, rotation. Create a total
	 * of numTeams, moving right (or left if moveRight is false) and increment ids.
	 */
	public void createTeamRow(int numTeams, int startingId, double x, double y, double rotation, boolean moveRight,
			boolean increaseIds) {
		if (contest.getMapInfo() == null)
			return;

		double taw = contest.getMapInfo().getTeamAreaWidth();
		double rad = 0;
		if (moveRight)
			rad = Math.toRadians(rotation + 90);
		else
			rad = Math.toRadians(rotation + 270);

		for (int i = 0; i < numTeams; i++) {
			double dx = Math.cos(rad) * taw * i;
			double dy = Math.sin(rad) * taw * i;

			if (increaseIds)
				createTeam(startingId + i, x + dx, y + dy, rotation);
			else
				createTeam(startingId - i, x + dx, y + dy, rotation);
		}
	}

	public IAisle createAisle(double x1, double y1, double x2, double y2) {
		return createAisle("aisle" + ++aisleCounter, x1, y1, x2, y2);
	}

	public IAisle createAisle(String id, double x1, double y1, double x2, double y2) {
		if (contest.getMapInfo() == null)
			return null;

		Aisle a = new Aisle();
		a.set(x1, y1, x2, y2);
		((MapInfo) contest.getMapInfo()).addAisle(a);
		computeAisleIntersections();
		return a;
	}

	public IProblem createBalloon(String id, double x, double y) {
		Problem p = new Problem();
		p.add("id", id);
		p.add("label", id);
		p.add("x", x + "");
		p.add("y", y + "");

		((Contest) contest).add(p);
		return p;
	}

	public Printer createPrinter(double x, double y) {
		if (contest.getMapInfo() == null)
			return null;

		Printer p = new Printer();
		p.set(x, y);
		((MapInfo) contest.getMapInfo()).setPrinter(p);
		return p;
	}

	private static void rotate(IPosition p, double rad) {
		ContestObject co = (ContestObject) p;
		double dx = Math.cos(rad) * p.getX() - Math.sin(rad) * p.getY();
		double dy = Math.sin(rad) * p.getX() + Math.cos(rad) * p.getY();
		co.add("x", dx + "");
		co.add("y", dy + "");
	}

	/**
	 * Rotate the floor map clockwise by the given number of degrees. The value should be between
	 * -359 and 359.
	 *
	 * @param angle an angle in degrees
	 */
	public void rotate(int angle) {
		double rad = Math.toRadians(angle);
		for (ITeam tt : contest.getTeams()) {
			rotate(tt, rad);
			double r = tt.getRotation() - angle + 720;
			((Team) tt).add("rotation", (r % 360) + "");
		}

		for (IProblem problem : contest.getProblems())
			rotate(problem, rad);

		IMapInfo mapInfo = contest.getMapInfo();
		if (mapInfo != null) {
			for (IAisle a : mapInfo.getAisles()) {
				double dx1 = Math.cos(rad) * a.getX1() - Math.sin(rad) * a.getY1();
				double dy1 = Math.sin(rad) * a.getX1() + Math.cos(rad) * a.getY1();
				double dx2 = Math.cos(rad) * a.getX2() - Math.sin(rad) * a.getY2();
				double dy2 = Math.sin(rad) * a.getX2() + Math.cos(rad) * a.getY2();
				((Aisle) a).set(dx1, dy1, dx2, dy2);
			}

			if (mapInfo.getPrinter() != null)
				rotate(mapInfo.getPrinter(), rad);
		}

		computeAisleIntersections();
	}

	public void resetOrigin() {
		double ox = Double.MAX_VALUE;
		double oy = Double.MAX_VALUE;

		IMapInfo mapInfo = contest.getMapInfo();
		if (mapInfo != null) {
			for (IAisle a : mapInfo.getAisles()) {
				ox = Math.min(ox, a.getX1());
				oy = Math.min(oy, a.getY1());
				ox = Math.min(ox, a.getX2());
				oy = Math.min(oy, a.getY2());
			}
		}

		for (ITeam t : contest.getTeams()) {
			ox = Math.min(ox, t.getX());
			oy = Math.min(oy, t.getY());
		}

		if (ox == 0 && oy == 0)
			return;

		// move everything
		if (mapInfo != null) {
			for (IAisle a : mapInfo.getAisles()) {
				((Aisle) a).set(a.getX1() - ox, a.getY1() - oy, a.getX2() - ox, a.getY2() - oy);
			}
		}

		for (ITeam t : contest.getTeams()) {
			JsonObject obj = new JsonObject();
			obj.props.put("x", t.getX() - ox + "");
			obj.props.put("y", t.getY() - oy + "");
			obj.props.put("rotation", t.getRotation() + "");
			((Team) t).add("location", obj);
		}

		for (IProblem p : contest.getProblems()) {
			((Problem) p).add("x", p.getX() - ox);
			((Problem) p).add("y", p.getY() - oy);
		}

		if (mapInfo != null && mapInfo.getPrinter() != null) {
			IPosition printer = mapInfo.getPrinter();
			((Printer) printer).set(printer.getX() - ox, printer.getY() - oy);
		}

		computeAisleIntersections();
	}
}