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
public class FloorGenerator49Baku extends FloorGenerator {
	// team area width (in meters). ICPC standard is 3.0
	private static final float taw = 2.5f;

	// team area depth (in meters). ICPC standard is 2.2
	private static final float tad = 2f;

	private static final float aisle = 1.5f;

	private static FloorMap floor = new FloorMap(taw, tad, 1.8, 0.8);

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
			float laisle = -aisle * 2 - taw * 8;
			float raisle = aisle * 2 + taw * 9;

			floor.createAisle(laisle, y, 0, y);
			y -= tad / 4;

			y -= (aisle + tad) / 2;

			// left
			floor.createTeamRow(6, 1, -aisle - taw * 0.5, y, FloorMap.N, true, true);
			y -= tad / 4;
			floor.createAisle(0, y, raisle, y);
			y -= tad / 4;
			floor.createTeamRow(6, 7, -aisle - taw * 5.5, y, FloorMap.S, true, true);

			y -= (aisle + tad) / 2;

			// right
			floor.createTeamRow(9, 13, aisle + taw * 0.5, y, FloorMap.N, false, true);
			y -= tad / 4;
			floor.createAisle(laisle, y, 0, y);
			y -= tad / 4;
			floor.createTeamRow(9, 22, aisle + taw * 8.5, y, FloorMap.S, false, true);

			y -= (aisle + tad) / 2;

			// left
			floor.createTeamRow(8, 31, -aisle - taw * 0.5, y, FloorMap.N, true, true);
			y -= tad / 4;
			floor.createAisle(0, y, raisle, y);
			y -= tad / 4;
			floor.createTeamRow(8, 39, -aisle - taw * 7.5, y, FloorMap.S, true, true);

			y -= (aisle + tad) / 2;

			// right
			floor.createTeamRow(9, 47, aisle + taw * 0.5, y, FloorMap.N, false, true);
			y -= tad / 4;
			floor.createAisle(laisle, y, 0, y);
			y -= tad / 4;
			floor.createTeamRow(9, 56, aisle + taw * 8.5, y, FloorMap.S, false, true);

			y -= (aisle + tad) / 2;

			// left
			floor.createTeamRow(8, 65, -aisle - taw * 0.5, y, FloorMap.N, true, true);
			y -= tad / 4;
			floor.createAisle(0, y, raisle, y);
			y -= tad / 4;
			floor.createTeamRow(8, 73, -aisle - taw * 7.5, y, FloorMap.S, true, true);

			y -= (aisle + tad) / 2;

			// right
			floor.createTeamRow(9, 81, aisle + taw * 0.5, y, FloorMap.N, false, true);
			y -= tad / 4;
			floor.createAisle(laisle, y, 0, y);
			y -= tad / 4;
			floor.createTeamRow(9, 90, aisle + taw * 8.5, y, FloorMap.S, false, true);

			y -= (aisle + tad) / 2;

			// left
			floor.createTeamRow(8, 99, -aisle - taw * 0.5, y, FloorMap.N, true, true);
			y -= tad / 4;
			floor.createAisle(0, y, raisle, y);
			y -= tad / 4;
			floor.createTeamRow(8, 107, -aisle - taw * 7.5, y, FloorMap.S, true, true);

			y -= (aisle + tad) / 2;

			// right
			floor.createTeamRow(9, 115, aisle + taw * 0.5, y, FloorMap.N, false, true);
			y -= tad / 4;
			floor.createAisle(laisle, y, 0, y);
			y -= tad / 4;
			floor.createTeamRow(9, 124, aisle + taw * 8.5, y, FloorMap.S, false, true);

			y -= (aisle + tad) / 2;

			// left
			floor.createTeamRow(6, 133, -aisle - taw * 0.5, y, FloorMap.N, true, true);
			y -= tad / 4;
			floor.createAisle(0, y, raisle, y);
			y -= tad / 4;
			floor.createTeamRow(6, 139, -aisle - taw * 5.5, y, FloorMap.S, true, true);

			y -= (aisle + tad) / 2;

			y -= tad / 4;
			floor.createAisle(laisle, y, 0, y);

			// diagonal aisles
			floor.createAisle(laisle, -aisle - tad * 2, -aisle * 2 - taw * 6, 0); // bottom left
			floor.createAisle(laisle, y + aisle + tad * 2, -aisle * 2 - taw * 6, y); // top left
			float h = (aisle + tad) / 2 + tad / 2;
			floor.createAisle(0, 0, taw, -h); // bottom middle
			floor.createAisle(0, y, taw, y + h); // top middle

			// vertical aisles
			floor.createAisle(0, 0, 0, y);
			floor.createAisle(raisle, 0, raisle, y);
			floor.createAisle(laisle, 0, laisle, y);

			IPrinter p = floor.createPrinter(-aisle, 1);

			// convert spares
			floor.makeSpare(6);
			floor.makeSpare(7);
			floor.makeSpare(138);
			floor.makeSpare(139);

			IProblem pp = null;

			if (args != null && args.length > 0) {
				File f = new File(args[0]);
				DiskContestSource source = new DiskContestSource(f);
				IContest contest2 = source.getContest();
				source.waitForContest(10000);
				IProblem[] problems = contest2.getProblems();

				double b0 = y / 2 + (tad * 6 + aisle * 4) / 2;
				double bs = (tad * 6 + aisle * 4) / (problems.length - 1);
				for (int i = 0; i < problems.length; i++) {
					floor.createBalloon(problems[i].getId(), laisle - aisle, b0 - i * bs);
				}

				floor.write(f);

				pp = problems[problems.length - 2];
			}

			long time = System.currentTimeMillis();
			ITeam t1 = floor.getTeam(142);
			ITeam t2 = floor.getTeam(122);
			ITeam t3 = floor.getTeam(22);
			Path path1 = floor.getPath(t1, t2);
			Path path2 = null;
			Path path3 = null;
			if (pp != null) {
				path2 = floor.getPath(t3, pp);
				path3 = floor.getPath(p, pp);
			}

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