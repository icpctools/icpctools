package org.icpc.tools.contest.util.floor;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.Path;
import org.icpc.tools.contest.model.IPrinter;
import org.icpc.tools.contest.model.ITeam;

public class FloorGenerator2017 extends FloorGenerator {
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

	protected static void createAdjacentTeam(int teamNumber, int newId, double dx, double dy) {
		ITeam t = floor.getTeam(teamNumber);
		floor.createTeam(newId, t.getX() + dx, t.getY() + dy, t.getRotation());
	}

	public static void main(String[] args) {
		Trace.init("ICPC Floor Map Generator", "floorMap", args);

		try {
			float dyRow = taw / 2 + 0.8f;
			float rowX = tad - 1.0f;
			float aisleX = (tad + 0.5f) / 2f + 0.5f;
			float bx = 0.8f;
			float by = taw * 5f / 6;

			float x = 0;
			float y = 0;

			for (int i = 0; i < 12; i++)
				floor.createBalloon(balloon.charAt(i) + "", x - bx, y - by * i * 9 / 12 + taw * 3 / 2);

			IPrinter p = floor.createPrinter(x, y - dyRow - 5 * taw);

			floor.createAisle(x, y + dyRow, x, y - dyRow - 5 * taw);
			float xx = x + aisleX * 2 + rowX;
			floor.createAisle(x, y + dyRow, xx, y + dyRow);
			floor.createAisle(x, y - dyRow - 4 * taw, xx, y - dyRow - 4 * taw);

			x += aisleX;
			createTeamRowRev(4, 133, x, y - taw, 0, -taw, FloorMap.E);
			createAdjacentTeam(133, -1, 0, taw);
			x += rowX;
			createTeamRow(5, 125, x, y, 0, -taw, FloorMap.W);
			x += aisleX;
			y += taw;

			floor.createAisle(x, y + dyRow, x, y - dyRow - 6 * taw);
			xx = x + aisleX * 16 + rowX * 8;
			floor.createAisle(x, y + dyRow, xx, y + dyRow);
			floor.createAisle(x - aisleX * 2 - rowX, y - dyRow - 6 * taw, xx, y - dyRow - 6 * taw);

			x += aisleX;
			createTeamRowRev(7, 124, x, y, 0, -taw, FloorMap.E);
			x += rowX;
			createTeamRow(7, 111, x, y, 0, -taw, FloorMap.W);
			x += aisleX;

			floor.createAisle(x, y + dyRow, x, y - dyRow - 6 * taw);

			x += aisleX;
			createTeamRowRev(7, 110, x, y, 0, -taw, FloorMap.E);
			x += rowX;
			createTeamRow(7, 97, x, y, 0, -taw, FloorMap.W);
			x += aisleX;

			floor.createAisle(x, y + dyRow, x, y - dyRow - 6 * taw);

			x += aisleX;
			createTeamRowRev(7, 96, x, y, 0, -taw, FloorMap.E);
			x += rowX;
			createTeamRow(7, 83, x, y, 0, -taw, FloorMap.W);
			x += aisleX;

			floor.createAisle(x, y + dyRow, x, y - dyRow - 6 * taw);

			x += aisleX;
			createTeamRowRev(7, 82, x, y, 0, -taw, FloorMap.E);
			x += rowX;
			createTeamRow(7, 69, x, y, 0, -taw, FloorMap.W);
			x += aisleX;

			floor.createAisle(x, y + dyRow, x, y - dyRow - 6 * taw);

			x += aisleX;
			createTeamRowRev(7, 68, x, y, 0, -taw, FloorMap.E);
			x += rowX;
			createTeamRow(7, 55, x, y, 0, -taw, FloorMap.W);
			x += aisleX;

			floor.createAisle(x, y + dyRow, x, y - dyRow - 6 * taw);

			x += aisleX;
			createTeamRowRev(7, 54, x, y, 0, -taw, FloorMap.E);
			x += rowX;
			createTeamRow(7, 41, x, y, 0, -taw, FloorMap.W);
			x += aisleX;

			floor.createAisle(x, y + dyRow, x, y - dyRow - 6 * taw);

			x += aisleX;
			createTeamRowRev(7, 40, x, y, 0, -taw, FloorMap.E);
			x += rowX;
			createTeamRow(7, 27, x, y, 0, -taw, FloorMap.W);
			x += aisleX;

			floor.createAisle(x, y + dyRow, x, y - dyRow - 6 * taw);

			x += aisleX;
			createTeamRowRev(7, 26, x, y, 0, -taw, FloorMap.E);
			x += rowX;
			createTeamRow(7, 13, x, y, 0, -taw, FloorMap.W);
			x += aisleX;

			floor.createAisle(x, y + dyRow, x, y - dyRow - 6 * taw);

			x += aisleX;
			y -= taw / 2f;
			createTeamRowRev(6, 12, x, y, 0, -taw, FloorMap.E);
			x += rowX;
			createTeamRow(6, 1, x, y, 0, -taw, FloorMap.W);
			x += aisleX;

			floor.createAisle(x, y + dyRow, x, y - dyRow - 5 * taw);
			xx = x - aisleX * 2 - rowX;
			floor.createAisle(x, y + dyRow, xx, y + dyRow);
			floor.createAisle(x, y - dyRow - 5 * taw, xx, y - dyRow - 5 * taw);

			// for (int i = 0; i < 7; i++)
			// floor.createBalloon(balloon.charAt(6 - i) + "", x + bx, y - by * i);

			floor.writeTSV(System.out);

			Trace.trace(Trace.USER, "------------------");

			long time = System.currentTimeMillis();
			ITeam t1 = floor.getTeam(57);
			ITeam t2 = floor.getTeam(9);
			Path path1 = floor.getPath(t1, t2);
			Path path2 = floor.getPath(t1, p);
			Trace.trace(Trace.USER, "Left of 32: " + floor.getTeamToLeftOf(floor.getTeam(32)).getId());

			Trace.trace(Trace.USER, "Time: " + (System.currentTimeMillis() - time));

			System.out.println("left: " + floor.getTeamToLeftOf(floor.getTeam(49)).getId());
			System.out.println("right: " + floor.getTeamToRightOf(floor.getTeam(49)).getId());

			System.out.println("left: " + floor.getTeamToLeftOf(floor.getTeam(31)).getId());
			System.out.println("right: " + floor.getTeamToRightOf(floor.getTeam(31)).getId());

			show(floor, 57, false, path1, path2);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}