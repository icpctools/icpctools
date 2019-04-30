package org.icpc.tools.contest.util.floor;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.IPrinter;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.FloorMap.Path;

public class FloorGenerator2018 extends FloorGenerator {
	// table width (in meters). ICPC standard is 1.8
	private static final float tw = 1.8f;

	// table depth (in meters). ICPC standard is 0.8
	private static final float td = 0.8f;

	// team area width (in meters). ICPC standard is 3.0
	private static final float taw = 3.0f;

	// team area depth (in meters). ICPC standard is 2.2
	private static final float tad = 2.2f;

	private static FloorMap floor = new FloorMap(taw, tad, tw, td);

	private static final String balloon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	protected static void createTeamRow(int num, int startingId, float x, float y, float dx, float dy, short rotation) {
		for (int i = 0; i < num; i++) {
			floor.createTeam(startingId + i, x + dx * i, y + dy * i, rotation);
		}
	}

	protected static void createTeamRowRev(int num, int startingId, float x, float y, float dx, float dy,
			short rotation) {
		for (int i = 0; i < num; i++) {
			floor.createTeam(startingId - i, x + dx * i, y + dy * i, rotation);
		}
	}

	protected static void createAdjacentTeam(int teamNumber, int newId, double dx, double dy) {
		ITeam t = floor.getTeam(teamNumber);
		floor.createTeam(newId, t.getX() + dx, t.getY() + dy, t.getRotation());
	}

	public static void main(String[] args) {
		Trace.init("ICPC Floor Map Generator", "floorMap", args);

		try {
			float dyRow = taw / 2 + 1.1f;
			float rowX = tad - 1.0f;

			float x = 0;
			float y = 0;

			for (int i = 0; i < 16; i++)
				floor.createBalloon(balloon.charAt(i) + "", x + taw + 2, y + 20f * i / 12f);

			floor.createAisle(x + taw + 1, y, x - 10 * taw, y);

			y += dyRow;
			createTeamRow(10, 1, x, y, -taw, 0, FloorMap.N);
			y += rowX;
			createTeamRow(10, 11, x - taw * 9, y, taw, 0, FloorMap.S);
			y += dyRow;

			floor.createAisle(x + taw + 1, y, x - 10 * taw, y);

			y += dyRow;
			createTeamRow(10, 21, x, y, -taw, 0, FloorMap.N);
			y += rowX;
			createTeamRow(10, 31, x - taw * 9, y, taw, 0, FloorMap.S);
			y += dyRow;

			floor.createAisle(x + taw + 1, y, x - 10 * taw, y);

			IPrinter p = floor.createPrinter(x - 12 * taw, y);

			y += dyRow;
			createTeamRow(10, 41, x, y, -taw, 0, FloorMap.N);
			y += rowX;
			createTeamRow(10, 51, x - taw * 9, y, taw, 0, FloorMap.S);
			y += dyRow;

			floor.createAisle(x + taw + 1, y, x - 10 * taw, y);

			y += dyRow;
			createTeamRow(10, 61, x, y, -taw, 0, FloorMap.N);
			y += rowX;
			createTeamRow(10, 71, x - taw * 9, y, taw, 0, FloorMap.S);
			y += dyRow;

			floor.createAisle(x + taw + 1, y, x - 10 * taw, y);

			y += dyRow;
			createTeamRow(10, 81, x, y, -taw, 0, FloorMap.N);
			y += rowX;
			createTeamRow(10, 91, x - taw * 9, y, taw, 0, FloorMap.S);
			y += dyRow;

			floor.createAisle(x + taw + 1, y, x - 10 * taw, y);

			y += dyRow;
			createTeamRow(10, 101, x, y, -taw, 0, FloorMap.N);
			y += rowX;
			createTeamRow(10, 111, x - taw * 9, y, taw, 0, FloorMap.S);
			y += dyRow;

			floor.createAisle(x + taw + 1, y, x - 10 * taw, y);

			y += dyRow;
			createTeamRow(10, 121, x - 1, y, -taw, 0, FloorMap.N);
			createAdjacentTeam(121, -1, taw, 0);
			y += rowX;
			createTeamRow(10, 131, x - 1 - taw * 9, y, taw, 0, FloorMap.S);
			createAdjacentTeam(140, -1, taw, 0);
			y += dyRow;

			floor.createAisle(x + taw + 1, y, x - 10 * taw, y);

			floor.createAisle(x + taw + 1, y, x + taw + 1, 0);
			floor.createAisle(x - 10 * taw, y, x - 10 * taw, 0);

			floor.writeCSV(System.out);

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
			// show(floor, 57, true);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}