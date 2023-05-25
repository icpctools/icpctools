package org.icpc.tools.contest.util.floor;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.Path;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IPrinter;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.DiskContestSource;

public class FloorGeneratorNAC23 extends FloorGenerator {
	// team area width (in meters). ICPC standard is 3.0
	private static final float taw = 2.2f;

	// team area depth (in meters). ICPC standard is 2.2
	private static final float tad = 2.0f;

	private static final float aisle = 2f + tad * 3 / 2;

	private static FloorMap floor = new FloorMap(taw, tad, 1.8, 0.8);

	protected static void createAisle(List<Point2D.Double> list, int i, int j) {
		Point2D.Double p1 = list.get(i);
		Point2D.Double p2 = list.get(j);
		floor.createAisle(p1.x, p1.y, p2.x, p2.y);
	}

	protected static float createAisleOfTeams(int teamNumber, float xx, float yy, int num) {
		float x = xx;
		x += aisle / 2;
		floor.createTeamRow(num, teamNumber, x, yy, FloorMap.E, false, true);
		x += tad / 2;
		floor.createTeamRow(num, teamNumber + num * 2 - 1, x, yy, FloorMap.W, true, false);
		x += aisle / 2;

		return x;
	}

	public static void main(String[] args) {
		Trace.init("ICPC Floor Map Generator", "floorMap", args);

		try {
			float x = 0;
			float y = 0;

			floor.createAisle(x, y + taw, x, y - taw * 7);

			x = createAisleOfTeams(1, x, y, 7);

			floor.createAisle(x, y + taw, x, y - taw * 7);

			x = createAisleOfTeams(15, x, y, 7);

			floor.createAisle(x, y + taw, x, y - taw * 7);

			// bottom 1
			floor.createAisle(0, y + taw, x, y + taw);

			float xx = x;
			y -= taw;

			x = createAisleOfTeams(29, x, y, 6);

			floor.createAisle(x, y + taw, x, y - taw * 6);

			x = createAisleOfTeams(41, x, y, 6);

			floor.createAisle(x, y + taw, x, y - taw * 6);

			// top
			floor.createAisle(0, y - taw * 6, x, y - taw * 6);

			// bottom 2
			floor.createAisle(xx, y + taw, x, y + taw);

			IPrinter p = floor.createPrinter(25, 5);

			// add spares next to 8 and 46
			createAdjacentTeam(floor, 8, -1, 0, -taw);
			createAdjacentTeam(floor, 46, -1, 0, -taw);

			// remove team 52
			// ITeam team = floor.getTeam(52);
			// ((Contest) contest).removeFromHistory(team);

			if (args != null && args.length > 0) {
				File f = new File(args[0]);
				DiskContestSource source = new DiskContestSource(f);
				IContest contest2 = source.getContest();
				source.waitForContest(10000);
				IProblem[] problems = contest2.getProblems();

				double bx = 0;
				for (int i = 0; i < problems.length; i++)
					floor.createBalloon(problems[i].getId(), bx + i * 2, -8);

				floor.write(f);
			}

			long time = System.currentTimeMillis();
			ITeam t1 = floor.getTeam(10);
			ITeam t2 = floor.getTeam(47);
			ITeam t3 = floor.getTeam(57);
			Path path1 = floor.getPath(t1, t2);
			Path path2 = floor.getPath(t3, p);

			Trace.trace(Trace.USER, "Time: " + (System.currentTimeMillis() - time));

			show(floor, 57, true, path1, path2);
			// show(floor, 57, true, null, null);
			// show(floor, 57, true);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}