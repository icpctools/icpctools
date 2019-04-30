package org.icpc.tools.contest.util.floor;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.IPrinter;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.FloorMap.Path;

public class FloorGenerator2013 extends FloorGenerator {
	private static final float tw = 1.8f; // table width
	private static final float td = 0.8f; // table depth
	private static final float taw = 3f; // team area width
	private static final float tad = 2.2f; // team area depth

	private static FloorMap floor = new FloorMap(taw, tad, tw, td);
	private static final String balloon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	protected static void createTeamRow(int num, int startingId, float x, float y, float dx, float dy, short rotation) {
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
			float rowDelta = tad * 2f + 1.25f;
			float tx = td + 0.4f;

			float t1y = 5f; // team 1 y pos
			float t2y = t1y + taw * 7f; // team 8 y pos

			float topAisleY = t1y - taw;
			float bottomAisleY = t2y + taw;

			float rowX = 0;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			rowX += rowDelta / 2f;

			createTeamRow(6, 1, rowX, t1y + taw / 2, 0, taw, FloorMap.E);
			rowX += tx;
			createTeamRow(7, 8, rowX, t2y - taw / 2, 0, -taw, FloorMap.W);

			rowX += rowDelta / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			rowX += rowDelta / 2f;

			createTeamRow(8, 15, rowX, t1y, 0, taw, FloorMap.E);
			rowX += tx;
			createTeamRow(8, 23, rowX, t2y, 0, -taw, FloorMap.W);

			rowX += rowDelta / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			rowX += rowDelta / 2f;

			createTeamRow(8, 31, rowX, t1y, 0, taw, FloorMap.E);
			rowX += tx;
			createTeamRow(8, 39, rowX, t2y, 0, -taw, FloorMap.W);

			rowX += rowDelta / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			rowX += rowDelta / 2f;

			createTeamRow(8, 47, rowX, t1y, 0, taw, FloorMap.E);
			rowX += tx;
			createTeamRow(7, 55, rowX, t2y, 0, -taw, FloorMap.W);

			rowX += rowDelta / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			rowX += rowDelta / 2f;

			createTeamRow(7, 62, rowX, t1y + taw, 0, taw, FloorMap.E);
			rowX += tx;
			createTeamRow(8, 69, rowX, t2y, 0, -taw, FloorMap.W);

			rowX += rowDelta / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			rowX += rowDelta / 2f;

			createTeamRow(8, 77, rowX, t1y, 0, taw, FloorMap.E);
			rowX += tx;
			createTeamRow(8, 85, rowX, t2y, 0, -taw, FloorMap.W);

			rowX += rowDelta / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			rowX += rowDelta / 2f;

			createTeamRow(7, 93, rowX, t1y + taw / 2, 0, taw, FloorMap.E);
			rowX += tx;
			createTeamRow(7, 100, rowX, t2y - taw / 2, 0, -taw, FloorMap.W);

			rowX += rowDelta / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);
			rowX += rowDelta / 2f;

			createTeamRow(7, 107, rowX, t1y + taw / 2, 0, taw, FloorMap.E);
			rowX += tx;
			createTeamRow(7, 114, rowX, t2y - taw / 2, 0, -taw, FloorMap.W);

			rowX += rowDelta / 2f;
			floor.createAisle(rowX, topAisleY, rowX, bottomAisleY);

			// spare teams
			createAdjacentTeam(61, 7, 0, -taw);
			createAdjacentTeam(62, -1, 0, -taw);

			// create top and bottom aisle
			floor.createAisle(0, topAisleY, rowX, topAisleY);
			floor.createAisle(0, bottomAisleY, rowX, bottomAisleY);

			// create balloons on left
			float dy = bottomAisleY - topAisleY;
			IProblem b = null;
			int numBalloons = 12;
			for (int i = 0; i < numBalloons; i++)
				b = floor.createBalloon(balloon.substring(i, i + 1), -rowDelta / 2f,
						bottomAisleY - dy * i / (numBalloons - 1));

			// balloons on right

			// rowX += rowDelta / 2f;
			// for (int i = 0; i < 7; i++)
			// b = floor.createBalloon(balloon.substring(i + 7, i + 8), rowX + rowDelta / 2f,
			// topAisleY + dy * i / 6);

			IPrinter p = floor.createPrinter(rowX, topAisleY - rowDelta / 3f);

			floor.write(System.out);

			Trace.trace(Trace.USER, "------------------");

			Trace.trace(Trace.USER, "Closest to " + rowX + "," + t1y);
			// FloorMap.aisle
			FloorMap.AisleIntersection ai = floor.getClosestAisle(rowX, t1y);
			Trace.trace(Trace.USER, ai + "");

			Path path1 = floor.getPath(p, b);
			// path = floor.getPath(b, floor.getTeam(58));
			Path path2 = floor.getPath(b, floor.getTeam(57));
			// path2.addPath(path2);

			show(floor, 57, true, path1, path2);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}