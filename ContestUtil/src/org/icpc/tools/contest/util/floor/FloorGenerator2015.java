package org.icpc.tools.contest.util.floor;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.Path;
import org.icpc.tools.contest.model.IPrinter;

public class FloorGenerator2015 extends FloorGenerator {
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

	protected static void createTeamRow(int num, int startingId, double x, double y, double dx, double dy,
			double rotation) {
		for (int i = 0; i < num; i++) {
			floor.createTeam(startingId + i, x + dx * i, y + dy * i, rotation);
		}
	}

	protected static void createTeamRows(float xx, float yy, int start) {
		float vAisle = 2.0f;
		float vSpacing = tad - 1.0f;
		float x = xx + taw / 2f;
		float y = yy - tad / 2f + 0.5f;
		createTeamRow(6, start, x, y, taw, 0, FloorMap.S);
		createTeamRow(6, start + 6, x + 6 * taw + vAisle, y, taw, 0, FloorMap.S);
		createTeamRow(4, start + 12, x + 12 * taw + 2 * vAisle, y, taw, 0, FloorMap.S);

		createTeamRow(4, start + 16, x + 15 * taw + 2 * vAisle, y - vSpacing, -taw, 0, FloorMap.N);
		createTeamRow(6, start + 20, x + 11 * taw + vAisle, y - vSpacing, -taw, 0, FloorMap.N);
		createTeamRow(6, start + 26, x + 5 * taw, y - vSpacing, -taw, 0, FloorMap.N);
	}

	public static void main(String[] args) {
		Trace.init("ICPC Floor Map Generator", "floorMap", args);

		try {
			float hAisle = 1.5f + 2 * tad;
			float hCenterAisle = 2.75f + 2 * tad;
			float vAisle = 2.0f;

			float y = 0;
			createTeamRows(0, y, 1);

			y -= hAisle;
			createTeamRows(0, y, 33);

			y -= hCenterAisle;
			createTeamRows(0, y, 65);

			y -= hAisle;
			createTeamRows(0, y, 97);

			float maxX = taw * 16 + vAisle * 2;
			float maxY = -(tad * 8 + 2.75f + 3f);

			// vertical aisles
			float b = 0.7f;
			floor.createAisle(-b, b, -b, maxY - b);
			floor.createAisle(maxX + b, b, maxX + b, maxY - b);
			float x = taw * 6 + vAisle / 2;
			floor.createAisle(x, b, x, maxY - b);
			x += taw * 6 + vAisle;
			floor.createAisle(x, b, x, maxY - b);

			// horizontal aisles
			floor.createAisle(-b, b, maxX + b, b);
			floor.createAisle(-b, maxY - b, maxX + b, maxY - b);
			y = -(tad * 2 + 1.5f / 2);
			floor.createAisle(-b, y, maxX + b, y);
			y -= tad * 2 + 1.5f / 2 + 2.75f / 2;
			floor.createAisle(-b, y, maxX + b, y);
			y -= tad * 2 + 1.5f / 2 + 2.75f / 2;
			floor.createAisle(-b, y, maxX + b, y);

			IPrinter p = floor.createPrinter(floor.getTeam(16).getX() + taw / 2, b * 2);

			// balloons - a complete guess right now
			int max = 14;
			for (int i = 0; i < max; i++)
				floor.createBalloon(balloon.substring(i, i + 1), -b * 3, maxY * i / (max - 1));

			floor.writeTSV(System.out);

			Trace.trace(Trace.USER, "------------------");

			Path path1 = floor.getPath(floor.getTeam(43), floor.getTeam(128));
			Path path2 = floor.getPath(p, floor.getTeam(84));
			show(floor, 57, true, path1, path2);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}