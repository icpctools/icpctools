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

/**
 * team area: 8' x 6', desk: 6' x 2.5'
 */
public class FloorGeneratorDhaka extends FloorGenerator {
	// team area width (in meters). ICPC standard is 3.0
	private static final float taw = 2.44f;

	// team area depth (in meters). ICPC standard is 2.2
	private static final float tad = 1.83f;

	private static final float aisle = 2f + tad * 3 / 2;

	private static FloorMap floor = new FloorMap(taw, tad, 1.83, 0.76);

	protected static void createAisle(List<Point2D.Double> list, int i, int j) {
		Point2D.Double p1 = list.get(i);
		Point2D.Double p2 = list.get(j);
		floor.createAisle(p1.x, p1.y, p2.x, p2.y);
	}

	protected static float createAisleOfTeams(int teamNumber, float xx, float yy, int num) {
		float y = yy;
		y += aisle / 2;
		floor.createTeamRow(num, teamNumber, 00, y, FloorMap.S, false, true);
		y += tad / 2;
		floor.createTeamRow(num, teamNumber + num * 2 - 1, 00, y, FloorMap.N, true, false);
		y += aisle / 2;

		return y;
	}

	public static void main(String[] args) {
		Trace.init("ICPC Floor Map Generator", "floorMap", args);

		try {
			float y = 0;

			floor.createAisle(-taw * 9, y, taw * 9, y);

			y += aisle / 2;
			floor.createTeamRow(6, 0, taw * 6, y, FloorMap.S, false, true);
			floor.createTeamRow(7, 6, -taw * 1, y, FloorMap.S, false, true);

			y += tad / 2;
			floor.createTeamRow(7, 13, -taw * 7, y, FloorMap.N, false, true);
			floor.createTeamRow(6, 20, taw * 1, y, FloorMap.N, false, true);
			y += aisle / 2;

			floor.createAisle(-taw * 9, y, taw * 9, y);
			float aisle1 = y;

			y += aisle / 2;
			floor.createTeamRow(8, 26, taw * 8, y, FloorMap.S, false, true);
			floor.createTeamRow(8, 34, -taw * 1, y, FloorMap.S, false, true);

			y += tad / 2;
			floor.createTeamRow(8, 42, -taw * 8, y, FloorMap.N, false, true);
			floor.createTeamRow(8, 50, taw * 1, y, FloorMap.N, false, true);
			y += aisle / 2;

			floor.createAisle(-taw * 9, y, taw * 9, y);

			y += aisle / 2;
			floor.createTeamRow(8, 58, taw * 8, y, FloorMap.S, false, true);
			floor.createTeamRow(8, 66, -taw * 1, y, FloorMap.S, false, true);

			y += tad / 2;
			floor.createTeamRow(8, 74, -taw * 8, y, FloorMap.N, false, true);
			floor.createTeamRow(8, 82, taw * 1, y, FloorMap.N, false, true);
			y += aisle / 2;

			floor.createAisle(-taw * 9, y, taw * 9, y);

			y += aisle / 2;
			floor.createTeamRow(8, 90, taw * 8, y, FloorMap.S, false, true);
			floor.createTeamRow(8, 98, -taw * 1, y, FloorMap.S, false, true);

			y += tad / 2;
			floor.createTeamRow(8, 106, -taw * 8, y, FloorMap.N, false, true);
			floor.createTeamRow(8, 114, taw * 1, y, FloorMap.N, false, true);
			y += aisle / 2;

			floor.createAisle(-taw * 9, y, taw * 9, y);
			float aisle4 = y;

			y += aisle / 2;
			floor.createTeamRow(4, 122, taw * 6, y, FloorMap.S, false, true);
			floor.createTeamRow(6, 126, -taw * 2, y, FloorMap.S, false, true);

			y += tad / 2;
			floor.createTeamRow(6, 132, -taw * 7, y, FloorMap.N, false, true);
			floor.createTeamRow(4, 138, taw * 3, y, FloorMap.N, false, true);
			y += aisle / 2;

			floor.createAisle(-taw * 9, y, taw * 9, y);

			// left, center, right aisles
			floor.createAisle(-taw * 9, 0, -taw * 9, y);
			floor.createAisle(0, 0, 0, y);
			floor.createAisle(taw * 9, 0, taw * 9, y);

			// corner shortcuts
			floor.createAisle(-taw * 9, aisle1, -taw * 8, 0);
			floor.createAisle(taw * 9, aisle1, taw * 7, 0);
			floor.createAisle(-taw * 9, aisle4, -taw * 8, y);
			floor.createAisle(taw * 9, aisle4, taw * 7, y);

			floor.createAisle(taw * 2, aisle4, -taw, y);
			floor.createAisle(-taw, aisle4, taw * 2, y);

			IPrinter p = floor.createPrinter(taw * 10, aisle1);

			// convert spares
			floor.makeSpare(0);
			floor.makeSpare(141);

			if (args != null && args.length > 0) {
				File f = new File(args[0]);
				DiskContestSource source = new DiskContestSource(f);
				IContest contest2 = source.getContest();
				source.waitForContest(10000);
				IProblem[] problems = contest2.getProblems();

				double b0 = 0;
				for (int i = 0; i < problems.length; i++)
					floor.createBalloon(problems[i].getId(), b0 + i * 2, -8);

				floor.write(f);
			}

			long time = System.currentTimeMillis();
			ITeam t1 = floor.getTeam(10);
			ITeam t2 = floor.getTeam(107);
			ITeam t3 = floor.getTeam(135);
			Path path1 = floor.getPath(t1, t2);
			Path path2 = floor.getPath(t3, p);

			Trace.trace(Trace.USER, "Time: " + (System.currentTimeMillis() - time));

			show(floor, 57, true, path1, path2);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}