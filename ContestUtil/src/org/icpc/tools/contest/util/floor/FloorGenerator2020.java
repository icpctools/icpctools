package org.icpc.tools.contest.util.floor;

import java.io.File;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.Path;
import org.icpc.tools.contest.model.IPrinter;
import org.icpc.tools.contest.model.ITeam;

public class FloorGenerator2020 extends FloorGenerator {
	// team area width (in meters). ICPC standard is 3.0
	private static final float taw = 3.3f;

	// team area depth (in meters). ICPC standard is 2.2
	private static final float tad = 2.5f;

	private static final float aisle = 2f + tad * 3 / 2;

	private static FloorMap floor = new FloorMap(taw, tad, 3.3, 0.95);

	private static final String balloon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	protected static float createAisleOfTeams(int teamNumber, float xx, float y) {
		float x = xx;
		x += aisle / 2;
		floor.createTeamRow(6, teamNumber, x, y, FloorMap.E, true, true);
		x += tad / 2;
		floor.createTeamRow(6, teamNumber + 11, x, y, FloorMap.W, false, false);
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

			x = createAisleOfTeams(1, x, y);

			x = createAisleOfTeams(13, x, y);

			x = createAisleOfTeams(25, x, y);

			x = createAisleOfTeams(37, x, y);

			x = createAisleOfTeams(49, x, y);

			x = createAisleOfTeams(61, x, y);

			x = createAisleOfTeams(73, x, y);

			x = createAisleOfTeams(85, x, y);

			x = createAisleOfTeams(97, x, y);

			x = createAisleOfTeams(109, x, y);

			floor.createAisle(0, -taw, x, -taw);
			floor.createAisle(0, taw * 6, x, taw * 6);

			floor.makeSpare(120);

			IPrinter p = floor.createPrinter(x - taw, y - taw - 2);

			double bx = x - taw * 8;
			for (int i = 0; i < 13; i++)
				floor.createBalloon(balloon.charAt(i) + "", bx + 2 * i, y - taw - 2);

			if (args != null && args.length > 0)
				floor.write(new File(args[0]));

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