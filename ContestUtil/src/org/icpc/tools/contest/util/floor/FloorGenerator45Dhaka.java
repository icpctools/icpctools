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
public class FloorGenerator45Dhaka extends FloorGenerator {
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
			floor.createTeamRow(5, 0, taw * 6, y, FloorMap.S, false, true);
			floor.createTeamRow(6, 5, -taw * 2, y, FloorMap.S, false, true);

			y += tad / 2;
			floor.createTeamRow(6, 11, -taw * 7, y, FloorMap.N, false, true);
			floor.createTeamRow(5, 17, taw * 2, y, FloorMap.N, false, true);
			y += aisle / 2;

			floor.createAisle(-taw * 9, y, taw * 9, y);
			float aisle1 = y;

			y += aisle / 2;
			floor.createTeamRow(8, 22, taw * 8, y, FloorMap.S, false, true);
			floor.createTeamRow(8, 30, -taw * 1, y, FloorMap.S, false, true);

			y += tad / 2;
			floor.createTeamRow(8, 38, -taw * 8, y, FloorMap.N, false, true);
			floor.createTeamRow(8, 46, taw * 1, y, FloorMap.N, false, true);
			y += aisle / 2;

			floor.createAisle(-taw * 9, y, taw * 9, y);

			y += aisle / 2;
			floor.createTeamRow(8, 54, taw * 8, y, FloorMap.S, false, true);
			floor.createTeamRow(8, 62, -taw * 1, y, FloorMap.S, false, true);

			y += tad / 2;
			floor.createTeamRow(8, 70, -taw * 8, y, FloorMap.N, false, true);
			floor.createTeamRow(8, 78, taw * 1, y, FloorMap.N, false, true);
			y += aisle / 2;

			floor.createAisle(-taw * 9, y, taw * 9, y);

			y += aisle / 2;
			floor.createTeamRow(8, 86, taw * 8, y, FloorMap.S, false, true);
			floor.createTeamRow(8, 94, -taw * 1, y, FloorMap.S, false, true);

			y += tad / 2;
			floor.createTeamRow(8, 102, -taw * 8, y, FloorMap.N, false, true);
			floor.createTeamRow(8, 110, taw * 1, y, FloorMap.N, false, true);
			y += aisle / 2;

			floor.createAisle(-taw * 9, y, taw * 9, y);
			float aisle4 = y;

			y += aisle / 2;
			floor.createTeamRow(5, 118, taw * 6, y, FloorMap.S, false, true);
			floor.createTeamRow(6, 123, -taw * 2, y, FloorMap.S, false, true);

			y += tad / 2;
			floor.createTeamRow(6, 129, -taw * 7, y, FloorMap.N, false, true);
			floor.createTeamRow(5, 135, taw * 2, y, FloorMap.N, false, true);
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

			// X aisle at top
			floor.createAisle(taw, aisle1, -taw, 0);
			floor.createAisle(-taw, aisle1, taw, 0);

			// X aisle at bottom
			floor.createAisle(taw, aisle4, -taw, y);
			floor.createAisle(-taw, aisle4, taw, y);

			IPrinter p = floor.createPrinter(0, -3);

			// convert spares
			floor.makeSpare(0);
			floor.makeSpare(32);
			floor.makeSpare(37);
			floor.makeSpare(87);
			floor.makeSpare(134);
			floor.makeSpare(137);
			floor.makeSpare(138);
			floor.makeSpare(139);

			IProblem pp = null;

			if (args != null && args.length > 0) {
				File f = new File(args[0]);
				DiskContestSource source = new DiskContestSource(f);
				IContest contest2 = source.getContest();
				source.waitForContest(10000);
				IProblem[] problems = contest2.getProblems();

				floor.createAisle(-1 * (problems.length - 1), -2, problems.length - 1, -2);

				double b0 = -1 * (problems.length - 1);
				for (int i = 0; i < problems.length; i++) {
					floor.createBalloon(problems[i].getId(), b0 + i * 2, -1);
					floor.createAisle(b0 + i * 2, -2, b0 + i * 2, 0);
				}

				floor.write(f);

				pp = problems[problems.length - 2];
			}

			long time = System.currentTimeMillis();
			ITeam t1 = floor.getTeam(10);
			ITeam t2 = floor.getTeam(107);
			// ITeam t3 = floor.getTeam(135);
			ITeam t4 = floor.getTeam(22);
			Path path1 = floor.getPath(t1, t2);
			Path path2 = floor.getPath(t4, pp);
			Path path3 = null;
			if (pp != null)
				path3 = floor.getPath(p, pp);

			Trace.trace(Trace.USER, "Time: " + (System.currentTimeMillis() - time));

			if (path3 != null)
				show(floor, 57, true, path1, path2, path3);
			else
				show(floor, 57, true, path1, path2);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}