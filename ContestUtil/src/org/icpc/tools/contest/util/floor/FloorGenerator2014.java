package org.icpc.tools.contest.util.floor;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.Path;
import org.icpc.tools.contest.model.IPrinter;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ITeam;

public class FloorGenerator2014 extends FloorGenerator {
	private static final float tw = 1.8f; // table width
	private static final float td = 0.8f; // table depth
	private static final float taw = 3.0f; // team area width
	private static final float tad = 2.2f; // team area depth

	private static FloorMap floor = new FloorMap(taw, tad, tw, td);
	private static final String balloon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int[] left = new int[] { 1, 7, 3, 8, 2, 9 };
	private static final int[] right = new int[] { 4, 10, 5, 11, 6, 12 };

	protected static void createTeamRow(int num, int startingId, double x, double y, double dx, double dy,
			double rotation) {
		for (int i = 0; i < num; i++) {
			floor.createTeam(startingId + i, x + dx * i, y + dy * i, rotation);
		}
	}

	protected static void createAdjacentTeam(int id, int newId, double dx, double dy) {
		ITeam t = floor.getTeam(id);
		floor.createTeam(newId, t.getX() + dx, t.getY() + dy, t.getRotation());
	}

	public static void main(String[] args) {
		Trace.init("ICPC Floor Map Generator", "floorMap", args);

		try {
			float rowWidth = td + 0.4f;

			// float rowDelta = tad * 2f + 1.5f;
			float aisleWidth = tad * 2f + 1.5f - rowWidth;
			// float tx7 = td + 0.4f;

			float tyTop = 0f;
			float tyBottom = tyTop + taw * 8f;

			float topAisleY = tyTop - taw;
			float bottomAisleY = tyBottom + taw;

			float rowX = aisleWidth + rowWidth;

			floor.createAisle(rowX, tyTop + taw * 1.6f, rowX - aisleWidth - rowWidth, tyTop + taw * 1.6f);
			floor.createAisle(rowX, tyTop + taw * 7.6f, rowX - aisleWidth - rowWidth, tyTop + taw * 7.6f);

			floor.createAisle(rowX, tyTop + taw * 1.6f, rowX, tyTop + taw * 7.6f);
			rowX -= aisleWidth / 2f;

			createTeamRow(5, 1, rowX, tyBottom - 1.4f * taw, 0, -taw, FloorMap.W);
			rowX -= rowWidth;
			createTeamRow(5, 6, rowX, tyTop + 2.6f * taw, 0, taw, FloorMap.E);

			rowX -= aisleWidth / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			rowX -= aisleWidth / 2f;

			createTeamRow(9, 11, rowX, tyBottom, 0, -taw, FloorMap.W);
			rowX -= rowWidth;
			createTeamRow(9, 20, rowX, tyTop, 0, taw, FloorMap.E);

			rowX -= aisleWidth / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			rowX -= aisleWidth / 2f;

			createTeamRow(9, 29, rowX, tyBottom, 0, -taw, FloorMap.W);
			rowX -= rowWidth;
			createTeamRow(9, 38, rowX, tyTop, 0, taw, FloorMap.E);

			rowX -= aisleWidth / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			rowX -= aisleWidth / 2f;

			createTeamRow(9, 47, rowX, tyBottom, 0, -taw, FloorMap.W);
			rowX -= rowWidth;
			createTeamRow(9, 56, rowX, tyTop, 0, taw, FloorMap.E);

			rowX -= aisleWidth / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			rowX -= aisleWidth / 2f;

			createTeamRow(9, 65, rowX, tyBottom, 0, -taw, FloorMap.W);
			rowX -= rowWidth;
			createTeamRow(9, 74, rowX, tyTop, 0, taw, FloorMap.E);

			rowX -= aisleWidth / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			rowX -= aisleWidth / 2f;

			createTeamRow(9, 83, rowX, tyBottom, 0, -taw, FloorMap.W);
			rowX -= rowWidth;
			createTeamRow(9, 92, rowX, tyTop, 0, taw, FloorMap.E);

			rowX -= aisleWidth / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			rowX -= aisleWidth / 2f;

			createTeamRow(9, 101, rowX, tyBottom, 0, -taw, FloorMap.W);
			rowX -= rowWidth;
			createTeamRow(9, 110, rowX, tyTop, 0, taw, FloorMap.E);

			rowX -= aisleWidth / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			float wid = rowX;
			rowX -= aisleWidth / 2f;

			// last rows of teams
			createTeamRow(2, 119, rowX, tyBottom - 5.4f * taw, 0, -taw, FloorMap.W);
			rowX -= rowWidth;
			createTeamRow(2, 121, rowX, tyTop + 1.6f * taw, 0, taw, FloorMap.E);

			createAdjacentTeam(119, -1, 0, taw);
			createAdjacentTeam(122, -1, 0, taw);

			rowX -= aisleWidth / 2f;
			floor.createAisle(rowX, tyTop + taw * 0.6f, rowX, tyTop + taw * 4.6f);

			floor.createAisle(rowX, tyTop + taw * 0.6f, rowX + aisleWidth + rowWidth, tyTop + taw * 0.6f);
			floor.createAisle(rowX, tyTop + taw * 4.6f, rowX + aisleWidth + rowWidth, tyTop + taw * 4.6f);

			// primary top and bottom aisle
			floor.createAisle(0, topAisleY, wid, topAisleY);
			floor.createAisle(0, bottomAisleY, wid, bottomAisleY);

			// create balloons across bottom
			// floor.createAisle(0, topAisleY, rowX, topAisleY);
			float dy = wid;
			IProblem b = null;
			int numBalloons = 13;
			for (int i = 0; i < 6; i++) {
				int n = left[i] - 1;
				b = floor.createBalloon(balloon.substring(n, n + 1), dy * i / (numBalloons - 1),
						bottomAisleY + aisleWidth / 3);
			}
			for (int i = 0; i < 6; i++) {
				int n = right[i] - 1;
				b = floor.createBalloon(balloon.substring(n, n + 1), dy * (i + 7) / (numBalloons - 1),
						bottomAisleY + aisleWidth / 3);
			}

			IPrinter p = floor.createPrinter(wid - aisleWidth / 2, bottomAisleY);

			floor.rotate(180);
			floor.writeTSV(System.out);

			Trace.trace(Trace.USER, "------------------");

			Path path1 = floor.getPath(p, b);
			Path path2 = floor.getPath(b, floor.getTeam(57));
			// path2.addPath(path2);

			show(floor, 57, true, path1, path2);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}