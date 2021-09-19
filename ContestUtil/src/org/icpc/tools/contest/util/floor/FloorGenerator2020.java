package org.icpc.tools.contest.util.floor;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.Path;
import org.icpc.tools.contest.model.IPrinter;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Team;

public class FloorGenerator2020 extends FloorGenerator {
	// table width (in meters). ICPC standard is 1.8
	private static final float tw = 3.3f;

	// table depth (in meters). ICPC standard is 0.85
	private static final float td = 0.95f;

	// team area width (in meters). ICPC standard is 3.0
	private static final float taw = 3.3f;

	// team area depth (in meters). ICPC standard is 2.2
	private static final float tad = 2.5f;

	private static final float aisle = 2f + tad * 3 / 2;

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

	protected static float createAisleOfTeams(int teamNumber, float xx, float y) {
		float x = xx;
		x += aisle / 2;
		createTeamRow(6, teamNumber, x, y, 0, taw, FloorMap.E);
		x += tad / 2;
		createTeamRowRev(6, teamNumber + 11, x, y, 0, taw, FloorMap.W);
		x += aisle / 2;

		floor.createAisle(x, y - taw, x, y + taw * 6);

		return x;
	}

	public static void main(String[] args) {
		Trace.init("ICPC Floor Map Generator", "floorMap", args);

		try {
			float x = 0;
			float y = 0;

			floor.createAisle(x, y - taw, x, y + taw * 6);

			x = createAisleOfTeams(0, x, y);

			x = createAisleOfTeams(12, x, y);

			x = createAisleOfTeams(24, x, y);

			x = createAisleOfTeams(36, x, y);

			x = createAisleOfTeams(48, x, y);

			x = createAisleOfTeams(60, x, y);

			x = createAisleOfTeams(72, x, y);

			x = createAisleOfTeams(84, x, y);

			x = createAisleOfTeams(96, x, y);

			x = createAisleOfTeams(108, x, y);

			floor.createAisle(0, -taw, x, -taw);
			floor.createAisle(0, taw * 6, x, taw * 6);

			((Team) floor.getTeam(0)).add("id", "-1");
			((Team) floor.getTeam(119)).add("id", "-1");

			IPrinter p = floor.createPrinter(x - taw, y - taw - 2);

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

			/*System.out.println("left: " + floor.getTeamToLeftOf(floor.getTeam(49)).getId());
			System.out.println("right: " + floor.getTeamToRightOf(floor.getTeam(49)).getId());
			
			System.out.println("left: " + floor.getTeamToLeftOf(floor.getTeam(32)).getId());
			System.out.println("right: " + floor.getTeamToRightOf(floor.getTeam(32)).getId());*/

			Trace.trace(Trace.USER, "Time: " + (System.currentTimeMillis() - time));

			show(floor, 57, true, path1, path2);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}