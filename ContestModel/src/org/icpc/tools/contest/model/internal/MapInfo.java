package org.icpc.tools.contest.model.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icpc.tools.contest.model.IAisle;
import org.icpc.tools.contest.model.IMapInfo;
import org.icpc.tools.contest.model.IPrinter;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.JSONEncoder;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;

public class MapInfo extends ContestObject implements IMapInfo {
	private static final String TABLE_WIDTH = "table_width";
	private static final String TABLE_DEPTH = "table_depth";
	private static final String TEAM_AREA_WIDTH = "team_area_width";
	private static final String TEAM_AREA_DEPTH = "team_area_depth";
	private static final String AISLES = "aisles";
	private static final String SPARE_TEAMS = "spare_teams";
	private static final String PRINTER = "printer";

	public static class Aisle implements IAisle {
		private static final String X1 = "x1";
		private static final String Y1 = "y1";
		private static final String X2 = "x2";
		private static final String Y2 = "y2";

		private double x1 = Double.NaN;
		private double y1 = Double.NaN;
		private double x2 = Double.NaN;
		private double y2 = Double.NaN;

		@Override
		public double getX1() {
			return x1;
		}

		@Override
		public double getY1() {
			return y1;
		}

		@Override
		public double getX2() {
			return x2;
		}

		@Override
		public double getY2() {
			return y2;
		}

		public void set(double x1, double y1, double x2, double y2) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}

		protected boolean addImpl(String name2, Object value) throws Exception {
			if (X1.equals(name2)) {
				x1 = parseDouble(value);
				return true;
			} else if (Y1.equals(name2)) {
				y1 = parseDouble(value);
				return true;
			}
			if (X2.equals(name2)) {
				x2 = parseDouble(value);
				return true;
			} else if (Y2.equals(name2)) {
				y2 = parseDouble(value);
				return true;
			}
			return false;
		}

		protected void writeBody(JSONEncoder je) {
			if (!Double.isNaN(x1))
				je.encode(X1, round(x1));
			if (!Double.isNaN(y1))
				je.encode(Y1, round(y1));

			if (!Double.isNaN(x2))
				je.encode(X2, round(x2));
			if (!Double.isNaN(y2))
				je.encode(Y2, round(y2));
		}

		@Override
		public String toString() {
			return "Aisle: " + x1 + "," + y1 + " -> " + x2 + "," + y2;
		}
	}

	public static class Printer implements IPrinter {
		private static final String X = "x";
		private static final String Y = "y";

		private double x = Double.NaN;
		private double y = Double.NaN;

		@Override
		public double getX() {
			return x;
		}

		@Override
		public double getY() {
			return y;
		}

		protected boolean addImpl(String name2, Object value) throws Exception {
			if (X.equals(name2)) {
				x = parseDouble(value);
				return true;
			} else if (Y.equals(name2)) {
				y = parseDouble(value);
				return true;
			}
			return false;
		}

		public void setLocation(double x, double y) {
			this.x = x;
			this.y = y;
		}

		protected void writeBody(JSONEncoder je) {
			if (!Double.isNaN(x))
				je.encode(X, round(x));
			if (!Double.isNaN(y))
				je.encode(Y, round(y));
		}
	}

	// table width (in meters). ICPC standard is 1.8
	public double tableWidth = Double.NaN;

	// table depth (in meters). ICPC standard is 0.8
	public double tableDepth = Double.NaN;

	// team area width (in meters). ICPC standard is 3.0
	public double teamAreaWidth = Double.NaN;

	// team area depth (in meters). ICPC standard is 2.
	public double teamAreaDepth = Double.NaN;

	private List<IAisle> aisles = new ArrayList<>();
	private List<ITeam> spareTeams = new ArrayList<>();
	private IPrinter printer = null;

	@Override
	public ContestType getType() {
		return ContestType.MAP_INFO;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public double getTableWidth() {
		return tableWidth;
	}

	@Override
	public double getTableDepth() {
		return tableDepth;
	}

	@Override
	public double getTeamAreaWidth() {
		return teamAreaWidth;
	}

	@Override
	public double getTeamAreaDepth() {
		return teamAreaDepth;
	}

	@Override
	public List<IAisle> getAisles() {
		return aisles;
	}

	@Override
	public List<ITeam> getSpareTeams() {
		return spareTeams;
	}

	@Override
	public IPrinter getPrinter() {
		return printer;
	}

	public void addAisle(Aisle a) {
		aisles.add(a);
	}

	public void setPrinter(Printer p) {
		printer = p;
	}

	public void addSpareTeam(ITeam t) {
		spareTeams.add(t);
	}

	@Override
	protected boolean addImpl(String name, Object value) throws Exception {
		if (TABLE_WIDTH.equals(name)) {
			tableWidth = parseDouble(value);
			return true;
		} else if (TABLE_DEPTH.equals(name)) {
			tableDepth = parseDouble(value);
			return true;
		} else if (TEAM_AREA_WIDTH.equals(name)) {
			teamAreaWidth = parseDouble(value);
			return true;
		} else if (TEAM_AREA_DEPTH.equals(name)) {
			teamAreaDepth = parseDouble(value);
			return true;
		} else if (AISLES.equals(name)) {
			Object[] arr = JSONParser.getOrReadArray(value);
			aisles = new ArrayList<>();
			for (Object ob : arr) {
				JsonObject obj = (JsonObject) ob;
				Aisle a = new Aisle();
				double x1 = obj.getDouble("x1");
				double y1 = obj.getDouble("y1");
				double x2 = obj.getDouble("x2");
				double y2 = obj.getDouble("y2");
				a.set(x1, y1, x2, y2);
				aisles.add(a);
			}
			return true;
		} else if (PRINTER.equals(name)) {
			JsonObject obj = JSONParser.getOrReadObject(value);
			Printer p = new Printer();
			p.setLocation(obj.getDouble("x"), obj.getDouble("y"));
			printer = p;
			return true;
		} else if (SPARE_TEAMS.equals(name)) {
			Object[] arr = JSONParser.getOrReadArray(value);
			spareTeams = new ArrayList<>();
			for (Object ob : arr) {
				JsonObject obj = (JsonObject) ob;
				Team t = new Team();
				t.setLocation(obj.getDouble("x"), obj.getDouble("y"), obj.getDouble("rotation"));
				spareTeams.add(t);
			}
			return true;
		}
		return false;
	}

	@Override
	protected void getPropertiesImpl(Map<String, Object> props) {
		super.getPropertiesImpl(props);
		if (!Double.isNaN(tableWidth))
			props.put(TABLE_WIDTH, round(tableWidth));
		if (!Double.isNaN(tableDepth))
			props.put(TABLE_DEPTH, round(tableDepth));

		if (!Double.isNaN(teamAreaWidth))
			props.put(TEAM_AREA_WIDTH, round(teamAreaWidth));
		if (!Double.isNaN(teamAreaDepth))
			props.put(TEAM_AREA_DEPTH, round(teamAreaDepth));
	}

	@Override
	public void writeBody(JSONEncoder je) {
		if (!Double.isNaN(tableWidth))
			je.encode(TABLE_WIDTH, round(tableWidth));
		if (!Double.isNaN(tableDepth))
			je.encode(TABLE_DEPTH, round(tableDepth));

		if (!Double.isNaN(teamAreaWidth))
			je.encode(TEAM_AREA_WIDTH, round(teamAreaWidth));
		if (!Double.isNaN(teamAreaDepth))
			je.encode(TEAM_AREA_DEPTH, round(teamAreaDepth));

		if (!aisles.isEmpty()) {
			List<String> list = new ArrayList<String>();
			for (IAisle a : aisles) {
				StringBuilder sb = new StringBuilder();
				sb.append("{\"x1\":" + round(a.getX1()));
				sb.append(",\"y1\":" + round(a.getY1()));
				sb.append(",\"x2\":" + round(a.getX2()));
				sb.append(",\"y2\":" + round(a.getY2()) + "}");
				list.add(sb.toString());
			}
			je.encodePrimitive(AISLES, "[" + String.join(",", list) + "]");
		}
		if (printer != null) {
			StringBuilder sb = new StringBuilder();
			sb.append("{\"x\":" + round(printer.getX()));
			sb.append(",\"y\":" + round(printer.getY()) + "}");
			je.encodePrimitive(PRINTER, sb.toString());
		}
		if (!spareTeams.isEmpty()) {
			List<String> list = new ArrayList<String>();
			for (ITeam t : spareTeams) {
				StringBuilder sb = new StringBuilder();
				sb.append("{\"x\":" + round(t.getX()));
				sb.append(",\"y\":" + round(t.getY()));
				sb.append(",\"rotation\":" + round(t.getRotation()) + "}");
				list.add(sb.toString());
			}
			je.encodePrimitive(SPARE_TEAMS, "[" + String.join(",", list) + "]");
		}
	}

	private static String round(double d) {
		return Math.round(d * 100.0) / 100.0 + "";
	}

	@Override
	public String toString() {
		return "MapInfo";
	}
}