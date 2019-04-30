package org.icpc.tools.contest.util.floor;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.IPrinter;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.FloorMap.Path;

public class FloorGenerator2016 extends FloorGenerator {
	// table width (in meters). ICPC standard is 1.8
	private static final float tw = 1.8f;

	// table depth (in meters). ICPC standard is 0.8
	private static final float td = 0.8f;

	// team area width (in meters). ICPC standard is 3.0
	private static final float taw = 2.4f;

	// team area depth (in meters). ICPC standard is 2.2
	private static final float tad = 2.0f;

	private static FloorMap floor = new FloorMap(taw, tad, tw, td);

	private static final String balloon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	protected static void createTeamRow(int num, int startingId, float x, float y, float dx, float dy, short rotation) {
		for (int i = 0; i < num; i++) {
			floor.createTeam(startingId + i, x + dx * i, y + dy * i, rotation);
		}
	}

	protected static void createAdjacentTeam(int teamNumber, int newId, double dx, double dy) {
		ITeam t = floor.getTeam(teamNumber);
		floor.createTeam(newId, t.getY() + dx, t.getY() + dy, t.getRotation());
	}

	public static void main(String[] args) {
		Trace.init("ICPC Floor Map Generator", "floorMap", args);

		try {
			float dxRow = tad + 0.6f - 0.25f;
			float dyRow = taw / 2 + 0.8f;
			float tx = td + 0.25f;

			float x = 0;
			float y = 0;

			// left aisle
			for (int i = 0; i < 6; i++)
				floor.createBalloon(balloon.charAt(5 - i) + "", x - tx, y + (10f * taw) * (i / 5f) - taw);

			/*floor.createBalloon("C", x - tx, y - dyRow - taw + 0.5f);
			floor.createBalloon("B", x - tx, y + dyRow);
			floor.createBalloon("F", x - tx, y - dyRow - taw + 2.25f);
			floor.createBalloon("E", x - tx, y + dyRow + 1.75f);*/
			floor.createAisle(x, y - dyRow - taw, x, y + dyRow + 9 * taw);
			floor.createAisle(x, y - dyRow - taw, x + dxRow * 2 + tx, y - dyRow - taw);
			floor.createAisle(x, y + dyRow + 9 * taw, x + dxRow * 2 + tx, y + dyRow + 9 * taw);
			x += dxRow;

			createTeamRow(10, 1, x, y, 0, taw, FloorMap.E);
			x += tx;
			createTeamRow(10, 11, x, y + taw * 9, 0, -taw, FloorMap.W);

			createAdjacentTeam(1, -1, 0, -taw);
			createAdjacentTeam(20, -1, 0, -taw);

			x += dxRow;
			floor.createAisle(x, y - dyRow - taw, x, y + dyRow + 9 * taw);
			x += dxRow;

			y -= 0.7;

			createTeamRow(10, 21, x, y, 0, taw, FloorMap.E);
			x += tx;
			createTeamRow(10, 31, x, y + taw * 9, 0, -taw, FloorMap.W);

			x += dxRow;
			floor.createAisle(x, y - dyRow, x, y + dyRow + 9 * taw);
			floor.createAisle(x, y - dyRow, x - dxRow * 2 - tx, y - dyRow);
			floor.createAisle(x, y + dyRow + 9 * taw, x - dxRow * 2 - tx, y + dyRow + 9 * taw);

			y += 2.7;

			floor.createAisle(x, y - dyRow, x + dxRow * 6 + tx * 3 + 0.01f, y - dyRow);
			floor.createAisle(x, y + dyRow + 7 * taw, x + dxRow * 6 + tx * 3 + 0.01f, y + dyRow + 7 * taw);
			x += dxRow;

			createTeamRow(8, 41, x, y, 0, taw, FloorMap.E);
			x += tx;
			createTeamRow(8, 49, x, y + taw * 7, 0, -taw, FloorMap.W);

			x += dxRow;
			floor.createAisle(x, y - dyRow, x, y + dyRow + 7 * taw);
			x += dxRow;

			createTeamRow(8, 57, x, y, 0, taw, FloorMap.E);
			x += tx;
			createTeamRow(8, 65, x, y + taw * 7, 0, -taw, FloorMap.W);

			x += dxRow;
			floor.createAisle(x, y - dyRow, x, y + dyRow + 7 * taw);
			x += dxRow;

			createTeamRow(8, 73, x, y, 0, taw, FloorMap.E);
			x += tx;
			createTeamRow(8, 81, x, y + taw * 7, 0, -taw, FloorMap.W);

			y -= 2.7;

			x += dxRow;
			floor.createAisle(x, y - dyRow, x, y + dyRow + 9 * taw);
			floor.createAisle(x, y - dyRow, x + dxRow * 4 + tx * 2, y - dyRow);
			floor.createAisle(x, y + dyRow + 9 * taw, x + dxRow * 4 + tx * 2, y + dyRow + 9 * taw);
			x += dxRow;

			createTeamRow(10, 89, x, y, 0, taw, FloorMap.E);
			x += tx;
			createTeamRow(10, 99, x, y + taw * 9, 0, -taw, FloorMap.W);

			x += dxRow;
			floor.createAisle(x, y - dyRow, x, y + dyRow + 9 * taw);
			x += dxRow;

			createTeamRow(10, 109, x, y, 0, taw, FloorMap.E);
			x += tx;
			createTeamRow(10, 119, x, y + taw * 9, 0, -taw, FloorMap.W);

			x += dxRow;
			floor.createAisle(x, y - dyRow, x, y + dyRow + 9 * taw);

			IPrinter p = floor.createPrinter(x + taw, y + dyRow + 9 * taw);

			for (int i = 0; i < 7; i++)
				floor.createBalloon(balloon.charAt(6 + i) + "", x + tx, y + (11f * taw) * (i / 7f) - taw / 2);

			/*floor.createBalloon("A", x + tx, y - dyRow + 0.5f);
			floor.createBalloon("D", x + tx, y - dyRow + 2.25f);*/

			floor.write(System.out);

			Trace.trace(Trace.USER, "------------------");

			long time = System.currentTimeMillis();

			ITeam t1 = floor.getTeam(57);
			ITeam t2 = floor.getTeam(9);
			Path path1 = floor.getPath(t1, t2);
			Path path2 = floor.getPath(t1, p);
			Trace.trace(Trace.USER, "Left of 32: " + floor.getTeamToLeftOf(floor.getTeam(32)).getId());

			Trace.trace(Trace.USER, "Time: " + (System.currentTimeMillis() - time));

			show(floor, 57, true, path1, path2);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}