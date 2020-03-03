package org.icpc.tools.contest.util.floor;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.Path;
import org.icpc.tools.contest.model.IPrinter;
import org.icpc.tools.contest.model.ITeam;

public class FloorGenerator2020 extends FloorGenerator {
	// table width (in meters). ICPC standard is 1.8
	private static final float tw = 1.8f;

	// table depth (in meters). ICPC standard is 0.8
	private static final float td = 0.8f;

	// team area width (in meters). ICPC standard is 3.0
	private static final float taw = 2.2f;

	// team area depth (in meters). ICPC standard is 2.2
	private static final float tad = 2.0f;

	private static final float aisle = 3.75f + td;

	private static FloorMap floor = new FloorMap(taw, tad, tw, td);

	private static final String balloon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	protected static void createTeamRow(int num, int startingId, double x, double y, double dx, double dy,
			double rotation) {
		for (int i = 0; i < num; i++) {
			floor.createTeam(startingId + i, x + dx * i, y + dy * i, rotation);
		}
	}

	protected static void createTeamRowRev(int num, int startingId, double x, double y, double dx, double dy,
			double rotation) {
		for (int i = 0; i < num; i++) {
			floor.createTeam(startingId - i, x + dx * i, y + dy * i, rotation);
		}
	}

	protected static void createAdjacentTeam(int teamNumber, int newId, double dx, double dy, int rot) {
		ITeam t = floor.getTeam(teamNumber);
		floor.createTeam(newId, t.getX() + dx, t.getY() + dy, rot);
	}

	protected static void createAdjacentTeam(int teamNumber, int newId, double dx, double dy) {
		ITeam t = floor.getTeam(teamNumber);
		floor.createTeam(newId, t.getX() + dx, t.getY() + dy, t.getRotation());
	}

	public static void main(String[] args) {
		Trace.init("ICPC Floor Map Generator", "floorMap", args);

		try {
			float x = 0;
			float y = 0;

			float fx = tad * 7 / 2 + aisle * 7;
			floor.createAisle(x, y - taw, x + fx, y - taw);
			floor.createAisle(x, y + taw * 10, x + fx, y + taw * 10);

			floor.createAisle(x, y - taw, x, y + taw * 10);

			x += aisle / 2;
			createTeamRow(10, 1, x, y, 0, taw, FloorMap.E);
			x += tad / 2;
			createTeamRowRev(10, 20, x, y, 0, taw, FloorMap.W);
			x += aisle / 2;

			floor.createAisle(x, y - taw, x, y + taw * 10);

			x += aisle / 2;
			createTeamRow(10, 21, x, y, 0, taw, FloorMap.E);
			x += tad / 2;
			createTeamRowRev(10, 40, x, y, 0, taw, FloorMap.W);
			x += aisle / 2;

			floor.createAisle(x, y - taw, x, y + taw * 10);

			x += aisle / 2;
			createTeamRow(10, 41, x, y, 0, taw, FloorMap.E);
			x += tad / 2;
			createTeamRowRev(10, 60, x, y, 0, taw, FloorMap.W);
			x += aisle / 2;

			floor.createAisle(x, y - taw, x, y + taw * 10);

			x += aisle / 2;
			createTeamRow(10, 61, x, y, 0, taw, FloorMap.E);
			x += tad / 2;
			createTeamRowRev(10, 80, x, y, 0, taw, FloorMap.W);
			x += aisle / 2;

			floor.createAisle(x, y - taw, x, y + taw * 10);

			x += aisle / 2;
			createTeamRow(10, 81, x, y, 0, taw, FloorMap.E);
			x += tad / 2;
			createTeamRowRev(10, 100, x, y, 0, taw, FloorMap.W);
			x += aisle / 2;

			floor.createAisle(x, y - taw, x, y + taw * 10);

			x += aisle / 2;
			createTeamRow(10, 101, x, y, 0, taw, FloorMap.E);
			x += tad / 2;
			createTeamRowRev(10, 120, x, y, 0, taw, FloorMap.W);
			x += aisle / 2;

			floor.createAisle(x, y - taw, x, y + taw * 10);

			x += aisle / 2;
			createTeamRow(10, 121, x, y, 0, taw, FloorMap.E);
			x += tad / 2;
			createTeamRowRev(10, 140, x, y, 0, taw, FloorMap.W);
			x += aisle / 2;

			floor.createAisle(x, y - taw, x, y + taw * 10);

			IPrinter p = floor.createPrinter(x, y - taw - 2);

			double bx = 0;
			for (int i = 0; i < 11; i++)
				floor.createBalloon(balloon.charAt(i) + "", bx + 2 * i, y - taw - 2);

			floor.resetOrigin();

			floor.writeTSV(System.out);

			Trace.trace(Trace.USER, "------------------");

			long time = System.currentTimeMillis();
			ITeam t1 = floor.getTeam(57);
			ITeam t2 = floor.getTeam(9);
			Path path1 = floor.getPath(t1, t2);
			Path path2 = floor.getPath(t1, p);

			System.out.println("left: " + floor.getTeamToLeftOf(floor.getTeam(49)).getId());
			System.out.println("right: " + floor.getTeamToRightOf(floor.getTeam(49)).getId());

			System.out.println("left: " + floor.getTeamToLeftOf(floor.getTeam(32)).getId());
			System.out.println("right: " + floor.getTeamToRightOf(floor.getTeam(32)).getId());

			Trace.trace(Trace.USER, "Time: " + (System.currentTimeMillis() - time));

			show(floor, 57, true, path1, path2);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}