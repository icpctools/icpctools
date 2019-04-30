package org.icpc.tools.contest.util.floor;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.Path;
import org.icpc.tools.contest.model.IPrinter;
import org.icpc.tools.contest.model.ITeam;

public class FloorGenerator2019 extends FloorGenerator {
	// table width (in meters). ICPC standard is 1.8
	private static final float tw = 1.8f;

	// table depth (in meters). ICPC standard is 0.8
	private static final float td = 0.8f;

	// team area width (in meters). ICPC standard is 3.0
	private static final float taw = 2.2f;

	// team area depth (in meters). ICPC standard is 2.2
	private static final float tad = 2.0f;

	private static final float elecGap = 0.3f;
	private static final float aisleWidth = 1.44f / 2f + td / 2 + 1.05f; // aisle width + table
																								// depth +
																								// 1.05
	private static final float centerAisleWidth = 1.86f + 0.4f; // aisle width + extra signage

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
			float dyRow = aisleWidth;
			float rowX = td + elecGap;

			float x = 0;
			float y = 0;

			double rightAisle = x + taw / 2 + centerAisleWidth / 2;
			double leftAisle = x - 19.5 * taw - centerAisleWidth * 3 / 2;

			floor.createAisle(rightAisle, y, leftAisle, y);

			y += dyRow;
			createTeamRow(12, 1, x, y, -taw, 0, FloorMap.S);
			createTeamRow(8, 13, x - taw * 12 - centerAisleWidth, y, -taw, 0, FloorMap.S);
			y += rowX;
			createTeamRowRev(12, 40, x, y, -taw, 0, FloorMap.N);
			createTeamRowRev(8, 28, x - taw * 12 - centerAisleWidth, y, -taw, 0, FloorMap.N);
			y += dyRow;

			floor.createAisle(rightAisle, y, leftAisle, y);

			y += dyRow;
			createTeamRow(12, 41, x, y, -taw, 0, FloorMap.S);
			createTeamRow(8, 53, x - taw * 12 - centerAisleWidth, y, -taw, 0, FloorMap.S);
			y += rowX;
			createTeamRowRev(12, 80, x, y, -taw, 0, FloorMap.N);
			createTeamRowRev(8, 68, x - taw * 12 - centerAisleWidth, y, -taw, 0, FloorMap.N);
			y += dyRow;

			floor.createAisle(rightAisle, y, leftAisle, y);

			y += dyRow;
			createTeamRow(12, 81, x, y, -taw, 0, FloorMap.S);
			createTeamRow(8, 93, x - taw * 12 - centerAisleWidth, y, -taw, 0, FloorMap.S);
			y += rowX;
			createTeamRowRev(12, 120, x, y, -taw, 0, FloorMap.N);
			createTeamRowRev(8, 108, x - taw * 12 - centerAisleWidth, y, -taw, 0, FloorMap.N);
			y += dyRow;

			floor.createAisle(rightAisle, y, leftAisle, y);
			double lha = y;

			y += dyRow;
			createTeamRow(3, 121, x - taw * 4, y, -taw, 0, FloorMap.S);
			double newAisle2 = x - taw * 7;
			createTeamRow(4, 124, x - taw * 8, y, -taw, 0, FloorMap.S);
			createTeamRow(8, 128, x - taw * 12 - centerAisleWidth, y, -taw, 0, FloorMap.S);

			// spares
			// createAdjacentTeam(121, -1, taw, 0);
			// createAdjacentTeam(121, -1, taw * 4.25, 0, FloorMap.E);
			floor.createTeam(-1, x + 2, y - 0.3, FloorMap.E);

			// balloons
			/*double by = y * 0.8;
			for (int i = 0; i < 8; i++)
				floor.createBalloon(balloon.charAt(i) + "", leftAisle - tw, 1 + by - by * i / 7f);
			for (int i = 8; i < 16; i++)
				floor.createBalloon(balloon.charAt(i) + "", rightAisle + tw, 1 + by * (i - 8) / 7f);*/

			// 3 vertical aisles
			y += tad;
			double newAisle = rightAisle - 2.5;
			floor.createAisle(rightAisle, 0, rightAisle, lha);
			floor.createAisle(newAisle, lha, newAisle, y);
			floor.createAisle(newAisle2, lha, newAisle2, y);
			floor.createAisle(x - 11.5 * taw - centerAisleWidth / 2, 0, x - 11.5 * taw - centerAisleWidth / 2, y);
			floor.createAisle(x - 11.5 * taw - centerAisleWidth / 2, y, newAisle, y);
			floor.createAisle(leftAisle, 0, leftAisle, y);
			IPrinter p = floor.createPrinter(newAisle2 + taw * 6, y);

			double bx = x - 11.5 * taw - centerAisleWidth / 2;
			for (int i = 0; i < 11; i++)
				floor.createBalloon(balloon.charAt(i) + "", bx + (newAisle2 + taw * 4 - bx) * i / 10, y + tad / 2);

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
			// show(floor, 57, true, null, null);
			// show(floor, 57, true);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}