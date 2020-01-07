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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.internal.Printer;
import org.icpc.tools.contest.model.internal.Problem;
import org.icpc.tools.contest.model.internal.Team;

public class FloorMap {
	public static final double N = 90;
	public static final double E = 0;
	public static final double S = 270;
	public static final double W = 180;

	private static final String NO_ID = "<-1>";

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

	private static final String COMMA = ",";
	private static final String TAB = "\t";

	public double tableWidth;
	public double tableDepth;
	public double teamAreaWidth;
	public double teamAreaDepth;
	private Rectangle2D.Double tBounds;
	private Rectangle2D.Double otBounds;

	static class Aisle {
		double x1, y1;
		double x2, y2;

		@Override
		public String toString() {
			return "Aisle: " + x1 + "," + y1 + " -> " + x2 + "," + y2;
		}
	}

	public static class AisleIntersection {
		Aisle a1;
		Aisle a2;
		public double x;
		public double y;

		@Override
		public String toString() {
			return "Intersection: " + a1 + " and " + a2 + " at " + x + "," + y;
		}
	}

	public final List<ITeam> teams = new ArrayList<>();
	public final List<IProblem> balloons = new ArrayList<>();
	private final List<Aisle> aisles = new ArrayList<>();
	private IPrinter printer;

	// computed
	private List<AisleIntersection> aisleIntersections = new ArrayList<>();

	private FloorMap() {
		// used internally
	}

	public FloorMap(InputStream in) throws IOException {
		load(in);
	}

	private static FloorMap instance;

	/**
	 * Load a floor map from disk.
	 */
	public static FloorMap getInstance() {
		if (instance != null)
			return instance;

		instance = new FloorMap();
		instance.loadFromContest(null);
		return instance;
	}

	public static FloorMap getInstance(IContest c) {
		if (instance != null)
			return instance;

		instance = new FloorMap();
		instance.loadFromContest(c);
		return instance;
	}

	/**
	 * Create a new floor map.
	 */
	public FloorMap(double taw, double tad, double tw, double td) {
		teamAreaWidth = taw;
		teamAreaDepth = tad;
		tableWidth = tw;
		tableDepth = td;
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

		for (ITeam t : teams) {
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
			for (IProblem b : balloons) {
				x1 = Math.min(x1, b.getX() - bd);
				y1 = Math.min(y1, b.getY() - bd);
				x2 = Math.max(x2, b.getX() + bd);
				y2 = Math.max(y2, b.getY() + bd);
			}

			for (Aisle a : aisles) {
				x1 = Math.min(x1, a.x1);
				y1 = Math.min(y1, a.y1);
				x2 = Math.max(x2, a.x1);
				y2 = Math.max(y2, a.y1);
				x1 = Math.min(x1, a.x2);
				y1 = Math.min(y1, a.y2);
				x2 = Math.max(x2, a.x2);
				y2 = Math.max(y2, a.y2);
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
		int size = aisles.size();
		for (int i = 0; i < size - 1; i++) {
			for (int j = i + 1; j < size; j++) {
				Aisle a1 = aisles.get(i);
				Aisle a2 = aisles.get(j);

				double xd1 = a1.x2 - a1.x1;
				double yd1 = a1.y2 - a1.y1;

				double xd2 = a2.x2 - a2.x1;
				double yd2 = a2.y2 - a2.y1;

				double denom = xd2 * yd1 - yd2 * xd1;

				if (denom != 0f) {
					AisleIntersection ai = new AisleIntersection();
					ai.a1 = a1;
					ai.a2 = a2;
					double s1 = (yd2 * (a1.x1 - a2.x1) - xd2 * (a1.y1 - a2.y1)) / denom;

					if (s1 >= 0f && s1 <= 1f) {
						double s2 = (yd1 * (a1.x1 - a2.x1) - xd1 * (a1.y1 - a2.y1)) / denom;
						if (s2 >= 0f && s2 <= 1f) {
							ai.x = a1.x1 + s1 * xd1;
							ai.y = a1.y1 + s1 * yd1;
							aisleIntersections.add(ai);
						}
					}

					// TODO - check if point is on the other line!!
				}
			}
		}
	}

	protected List<AisleIntersection> getIntersections(Aisle a) {
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

		public void addPath(Path p2) {
			list.addAll(p2.list);
			dist += p2.dist;
		}

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
			double dd = d; // * dist;
			while (dd >= 0) {
				if (dd > segLength[i]) {
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
		best.list.add(0, start);

		AisleIntersection end = new AisleIntersection();
		end.x = t2.getX();
		end.y = t2.getY();
		best.list.add(end);

		return best;
	}

	public void print(Path path) {
		System.out.println("Path:");
		for (AisleIntersection ai : path.list) {
			System.out.println("  " + ai.x + "," + ai.y);
		}
		System.out.println("Distance: " + path.dist);
	}

	public AisleIntersection getClosestAisle(double x, double y) {
		AisleIntersection ai = new AisleIntersection();
		double dist = Double.MAX_VALUE;
		for (Aisle a : aisles) {
			double xd = a.x2 - a.x1;
			double yd = a.y2 - a.y1;

			double k = xd * x - xd * a.x1 + yd * y - yd * a.y1;
			k /= (xd * xd + yd * yd);

			if (k < 0f)
				k = 0f;
			else if (k > 1f)
				k = 1f;

			double xc = a.x1 + k * xd;
			double yc = a.y1 + k * yd;

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

	public List<ITeam> getTeams() {
		return teams;
	}

	public List<IProblem> getProblems() {
		return balloons;
	}

	public ITeam getTeam(int teamNum) {
		return getTeamById(teamNum + "");
	}

	public ITeam getTeamById(String id) {
		if (id == null)
			return null;

		for (ITeam t : teams) {
			if (id.equals(t.getId()))
				return t;
		}
		return null;
	}

	public IProblem getBalloon(String problemLabel) {
		if (problemLabel == null)
			return null;
		for (IProblem b : balloons) {
			if (problemLabel.equals(b.getId()))
				return b;
		}
		return null;
	}

	public IPrinter getPrinter() {
		return printer;
	}

	public void drawFloor(Graphics2D g, Rectangle r, String teamId, boolean showAisles, Path... paths) {
		Rectangle2D.Double bounds = getBounds(false);
		double scale = Math.min(r.width / bounds.width, r.height / bounds.height);
		int x1 = r.x - (int) (bounds.x * scale);
		int y1 = r.y - (int) (bounds.y * scale);

		if (showAisles) {
			g.setColor(Color.LIGHT_GRAY);
			for (Aisle a : aisles) {
				int ax1 = r.x + (int) ((a.x1 - bounds.x) * scale);
				int ay1 = r.y + (int) ((a.y1 - bounds.y) * scale);
				int ax2 = r.x + (int) ((a.x2 - bounds.x) * scale);
				int ay2 = r.y + (int) ((a.y2 - bounds.y) * scale);
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

		g.setColor(Color.BLACK);
		for (ITeam t : teams) {
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

		for (IProblem b : balloons) {
			double dim = 1.5f;
			double d = dim * scale;
			int x = r.x + (int) ((b.getX() - bounds.x) * scale);
			int y = r.y + (int) ((b.getY() - bounds.y) * scale);
			g.setColor(Color.WHITE);
			g.fillOval(x - (int) (d / 2f), y - (int) (d / 2f), (int) d, (int) d);
			g.setColor(Color.BLACK);
			g.drawOval(x - (int) (d / 2f), y - (int) (d / 2f), (int) d, (int) d);
			FontMetrics fm = g.getFontMetrics();
			g.setColor(Color.BLACK);
			g.drawString(b.getId(), x - fm.stringWidth(b.getId()) / 2f, y + (fm.getAscent() / 2.5f));
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

	public void drawFloor(Graphics2D g, Rectangle r, FloorColors colors, boolean rotateText) {
		Rectangle2D.Double bounds = getBounds(true);
		double scale = Math.min(r.width / bounds.width, r.height / bounds.height);
		int ww = (int) (bounds.width * scale);
		int hh = (int) (bounds.height * scale);
		int x1 = (r.width - ww) / 2 + r.x - (int) (bounds.x * scale);
		int y1 = (r.height - hh) / 2 + r.y - (int) (bounds.y * scale);

		g.setFont(g.getFont().deriveFont((float) (tableWidth * scale / 2.75f)));
		FontMetrics fm = g.getFontMetrics();

		for (ITeam t : teams) {
			Graphics2D gg = createGraphics(g, t, x1, y1, scale);

			Rectangle2D.Double tr1 = new Rectangle2D.Double(-(teamAreaDepth + 1.0f) * scale / 2f,
					-teamAreaWidth * scale / 2f, teamAreaDepth * scale, teamAreaWidth * scale);
			// Shape s = transform.createTransformedShape(tr);
			// gg.setTransform(transform);
			draw(gg, tr1, colors.getTeamAreaFillColor(), colors.getTeamAreaOutlineColor());

			// seats
			double c = tableWidth * scale / 8f;
			float rnd = 0f;// c * 0.5f;
			for (int i = 0; i < 3; i++) {
				RoundRectangle2D.Double tr = new RoundRectangle2D.Double(-tableDepth * scale * 0.75f,
						-tableWidth * scale / 2f + c * i * 2.5f + c / 2f, tableDepth * scale * 0.25f, c * 2f, rnd, rnd);
				// Shape s = transform.createTransformedShape(tr);
				draw(gg, tr, colors.getSeatFillColor(), colors.getSeatOutlineColor());
			}

			// table
			Rectangle2D.Double tr = new Rectangle2D.Double(-tableDepth * scale / 2f, -tableWidth * scale / 2f,
					tableDepth * scale, tableWidth * scale);
			// Shape s = transform.createTransformedShape(tr);
			// gg.setTransform(transform);
			String id = t.getId();
			draw(gg, tr, colors.getDeskFillColor(id), colors.getDeskOutlineColor(id));
			BufferedImage img = colors.getTeamLogo(id);
			if (img != null)
				gg.drawImage(img, -img.getWidth() / 2, -img.getHeight() / 2, null);

			gg.setColor(colors.getTextColor());

			if (id != null) {
				if (rotateText) {
					AffineTransform transform2 = AffineTransform.getRotateInstance(Math.toRadians(90));
					AffineTransform at = gg.getTransform();
					at.concatenate(transform2);
					gg.setTransform(at);
				} else {
					AffineTransform transform2 = AffineTransform.getRotateInstance(Math.toRadians(t.getRotation()));
					AffineTransform at = gg.getTransform();
					at.concatenate(transform2);
					gg.setTransform(at);
				}
				gg.drawString(id, -fm.stringWidth(id) / 2f, (fm.getAscent() - 3.5f) / 2f);
			}
			gg.dispose();
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

		double rad = Math.toRadians(current.getRotation() + angle);
		float x = (float) (current.getX() + Math.cos(rad) * teamAreaWidth);
		float y = (float) (current.getY() - Math.sin(rad) * teamAreaWidth);
		for (ITeam t : teams) {
			if (!current.equals(t) && Math.abs(t.getX() - x) < tableDepth && Math.abs(t.getY() - y) < tableDepth)
				return t;
		}
		return null;
	}

	public static FloorMap importMap(File root) throws IOException {
		File f = new File(root, "config" + File.separator + "floor-map.tsv");
		if (!f.exists()) {
			Trace.trace(Trace.USER, "No floor map found");
			return null;
		}

		FloorMap map = new FloorMap();
		map.load(new FileInputStream(f));
		return map;
	}

	private void loadFromContest(IContest contest) {
		ContestSource source = ContestSource.getInstance();
		if (source == null)
			return;

		try {
			File f = source.getFile("/contests/" + source.getContestId() + "/floor-map.tsv");
			if (f == null || !f.exists()) {
				Trace.trace(Trace.WARNING, "No floor map found");
				return;
			}

			load(new FileInputStream(f));
		} catch (FileNotFoundException e) {
			Trace.trace(Trace.WARNING, "Floor map does not exist");
		} catch (IOException e) {
			Trace.trace(Trace.ERROR, "Error reading floor map", e);
		}
	}

	private void load(InputStream in) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		String s = br.readLine();
		StringTokenizer st = new StringTokenizer(s, ",\t", false);
		teamAreaWidth = Float.parseFloat(st.nextToken());
		teamAreaDepth = Float.parseFloat(st.nextToken());
		tableWidth = Float.parseFloat(st.nextToken());
		tableDepth = Float.parseFloat(st.nextToken());
		s = br.readLine();

		while (s != null) {
			st = new StringTokenizer(s, ",\t", false);
			String type = st.nextToken().trim();

			if ("team".equals(type)) {
				String id = st.nextToken();
				if (NO_ID.equals(id))
					id = "";
				double x = Double.parseDouble(st.nextToken());
				double y = Double.parseDouble(st.nextToken());
				double rotation = Double.parseDouble(st.nextToken());
				createTeam(id, x, y, rotation);
			} else if ("balloon".equals(type)) {
				String id = st.nextToken();
				double x = Double.parseDouble(st.nextToken());
				double y = Double.parseDouble(st.nextToken());
				createBalloon(id, x, y);
			} else if ("printer".equals(type)) {
				double x = Double.parseDouble(st.nextToken());
				double y = Double.parseDouble(st.nextToken());
				createPrinter(x, y);
			} else if ("aisle".equals(type)) {
				Aisle a = new Aisle();
				a.x1 = Double.parseDouble(st.nextToken());
				a.y1 = Double.parseDouble(st.nextToken());
				a.x2 = Double.parseDouble(st.nextToken());
				a.y2 = Double.parseDouble(st.nextToken());
				aisles.add(a);
			}

			s = br.readLine();
		}
		computeAisleIntersections();
	}

	public ITeam createTeam(int num, double x, double y, double rotation) {
		if (num < 0)
			return createTeam("", x, y, rotation);
		return createTeam(num + "", x, y, rotation);
	}

	public ITeam createTeam(String id, double x, double y, double rotation) {
		Team t = new Team();
		t.add("id", id);
		t.add("x", x + "");
		t.add("y", y + "");
		t.add("rotation", rotation + "");
		teams.add(t);
		return t;
	}

	public Aisle createAisle(double x1, double y1, double x2, double y2) {
		Aisle a = new Aisle();
		a.x1 = x1;
		a.y1 = y1;
		a.x2 = x2;
		a.y2 = y2;
		aisles.add(a);
		computeAisleIntersections();
		return a;
	}

	public IProblem createBalloon(String id, double x, double y) {
		Problem p = new Problem();
		p.add("id", id);
		p.add("label", id);
		p.add("x", x + "");
		p.add("y", y + "");

		balloons.add(p);
		return p;
	}

	public Printer createPrinter(double x, double y) {
		Printer p = new Printer();
		p.add("x", x + "");
		p.add("y", y + "");
		printer = p;
		return p;
	}

	private static String rnd(double d) {
		return Math.round(d * 100.0) / 100.0 + "";
	}

	public void rotate180() {
		for (ITeam tt : teams) {
			Team t = (Team) tt;
			t.add("x", -t.getX() + "");
			t.add("y", -t.getY() + "");
			double r = t.getRotation() + 180;
			t.add("rotation", (r % 360) + "");
		}

		for (IProblem pp : balloons) {
			Problem p = (Problem) pp;
			p.add("x", -p.getX() + "");
			p.add("y", -p.getY() + "");
		}

		for (Aisle a : aisles) {
			a.x1 = -a.x1;
			a.y1 = -a.y1;
			a.x2 = -a.x2;
			a.y2 = -a.y2;
		}

		if (printer != null) {
			Printer p = (Printer) printer;
			p.add("x", -p.getX() + "");
			p.add("y", -p.getY() + "");
		}

		computeAisleIntersections();
	}

	public void writeCSV(PrintStream out) {
		out.print(rnd(teamAreaWidth));
		out.print(COMMA);
		out.print(rnd(teamAreaDepth));
		out.print(COMMA);
		out.print(rnd(tableWidth));
		out.print(COMMA);
		out.println(rnd(tableDepth));

		for (ITeam t : teams) {
			out.print("team");
			out.print(COMMA);
			String id = t.getId();
			if (id == null || id.isEmpty())
				id = NO_ID;
			out.print(id);
			out.print(COMMA);
			out.print(rnd(t.getX()));
			out.print(COMMA);
			out.print(rnd(t.getY()));
			out.print(COMMA);
			out.println(rnd(t.getRotation()));
		}

		for (IProblem b : balloons) {
			out.print("balloon");
			out.print(COMMA);
			out.print(b.getId());
			out.print(COMMA);
			out.print(rnd(b.getX()));
			out.print(COMMA);
			out.println(rnd(b.getY()));
		}

		for (Aisle a : aisles) {
			out.print("aisle");
			out.print(COMMA);
			out.print(rnd(a.x1));
			out.print(COMMA);
			out.print(rnd(a.y1));
			out.print(COMMA);
			out.print(rnd(a.x2));
			out.print(COMMA);
			out.println(rnd(a.y2));
		}

		if (printer != null) {
			out.print("printer");
			out.print(COMMA);
			out.print(rnd(printer.getX()));
			out.print(COMMA);
			out.println(rnd(printer.getY()));
		}
	}

	public void writeTSV(PrintStream out) {
		out.print(teamAreaWidth);
		out.print(TAB);
		out.print(teamAreaDepth);
		out.print(TAB);
		out.print(tableWidth);
		out.print(TAB);
		out.println(tableDepth);

		for (ITeam t : teams) {
			out.print("team");
			out.print(TAB);
			String id = t.getId();
			if (id == null || id.isEmpty())
				id = NO_ID;
			out.print(id);
			out.print(TAB);
			out.print(rnd(t.getX()));
			out.print(TAB);
			out.print(rnd(t.getY()));
			out.print(TAB);
			out.println(rnd(t.getRotation()));
		}

		for (IProblem b : balloons) {
			out.print("balloon");
			out.print(TAB);
			out.print(b.getId());
			out.print(TAB);
			out.print(rnd(b.getX()));
			out.print(TAB);
			out.println(rnd(b.getY()));
		}

		for (Aisle a : aisles) {
			out.print("aisle");
			out.print(TAB);
			out.print(rnd(a.x1));
			out.print(TAB);
			out.print(rnd(a.y1));
			out.print(TAB);
			out.print(rnd(a.x2));
			out.print(TAB);
			out.println(rnd(a.y2));
		}

		if (printer != null) {
			out.print("printer");
			out.print(TAB);
			out.print(rnd(printer.getX()));
			out.print(TAB);
			out.println(rnd(printer.getY()));
		}
	}

	private static String escape(String s) {
		if (s == null)
			return "";
		boolean found = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ("\"\\".indexOf(c) >= 0)
				found = true;
		}
		if (!found)
			return s;

		StringBuilder out = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ("\"\\".indexOf(c) >= 0)
				out.append("\\");

			out.append(c);
		}
		return out.toString();
	}

	private static void write(PrintStream out, String name, String value) {
		out.append("\"" + escape(name) + "\":\"");
		out.append(escape(value));
		out.append("\"");
	}

	public void write(PrintStream out) {
		out.append("\"floorMap\": {\n  ");
		write(out, "teamWidth", rnd(teamAreaWidth));
		out.append(",\n  ");
		write(out, "teamDepth", rnd(teamAreaDepth));
		out.append(",\n  ");
		write(out, "tableWidth", rnd(tableWidth));
		out.append(",\n  ");
		write(out, "tableDepth", rnd(tableDepth));
		out.append(",\n");

		for (ITeam t : teams) {
			out.append("  \"team\": {");
			String id = t.getId();
			if (id == null || id.isEmpty())
				id = NO_ID;
			write(out, "id", id);
			out.append(", ");
			write(out, "x", rnd(t.getX()));
			out.append(", ");
			write(out, "y", rnd(t.getY()));
			out.append(", ");
			write(out, "angle", rnd(t.getRotation()));
			out.append("},\n");
		}

		for (Aisle a : aisles) {
			out.append("  \"aisle\": {");
			write(out, "x1", rnd(a.x1));
			out.append(", ");
			write(out, "y1", rnd(a.y1));
			out.append(", ");
			write(out, "x2", rnd(a.x2));
			out.append(", ");
			write(out, "y2", rnd(a.y2));
			out.append("},\n");
		}

		for (IProblem b : balloons) {
			out.append("  \"balloon\": {");
			write(out, "id", b.getId());
			out.append(", ");
			write(out, "x", rnd(b.getX()));
			out.append(", ");
			write(out, "y", rnd(b.getY()));
			out.append("},\n");
		}

		if (printer != null) {
			out.append("  \"printer\": {");
			write(out, "x", rnd(printer.getX()));
			out.append(", ");
			write(out, "y", rnd(printer.getY()));
			out.append("}\n");
		}
		out.append("}");
	}

	public void resetOrigin() {
		double ox = Double.MAX_VALUE;
		double oy = Double.MAX_VALUE;

		for (Aisle a : aisles) {
			ox = Math.min(ox, a.x1);
			oy = Math.min(oy, a.y1);
			ox = Math.min(ox, a.x2);
			oy = Math.min(oy, a.y2);
		}

		for (ITeam t : teams) {
			ox = Math.min(ox, t.getX());
			oy = Math.min(oy, t.getY());
		}

		if (ox == 0 && oy == 0)
			return;

		// move everything
		for (Aisle a : aisles) {
			a.x1 -= ox;
			a.y1 -= oy;
			a.x2 -= ox;
			a.y2 -= oy;
		}

		for (ITeam t : teams) {
			((Team) t).add("x", t.getX() - ox);
			((Team) t).add("y", t.getY() - oy);
		}

		for (IProblem p : balloons) {
			((Problem) p).add("x", p.getX() - ox);
			((Problem) p).add("y", p.getY() - oy);
		}

		if (printer != null) {
			((Printer) printer).add("x", printer.getX() - ox);
			((Printer) printer).add("y", printer.getY() - oy);
		}

		computeAisleIntersections();
	}
}