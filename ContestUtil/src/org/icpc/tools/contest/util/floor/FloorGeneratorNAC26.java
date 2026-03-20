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

public class FloorGeneratorNAC26 extends FloorGenerator {
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
			float y = -taw;

			floor.createAisle(x, y + 2 * taw, x, y - taw * 6);

			y += taw;

			// left
			floor.createAisle(x, y - taw * 7.5, x, y + taw);

			x = createAisleOfTeams(1, x, y, 7);

			// second row
			floor.createAisle(x, y - taw * 7.5, x, y + taw * 2);

			// bottom 1
			floor.createAisle(0, y + taw, x, y + taw);

			y -= (float) (taw / 2.0);

			float xx = x;
			float printerX = x;

			x = createAisleOfTeams(15, x, y, 7);

			floor.createAisle(x, y + taw, x, y - taw * 7);

			float center = x;

			x = createAisleOfTeams(29, x, y, 7);

			floor.createAisle(x, y + taw, x, y - taw * 7);

			// bottom 2
			floor.createAisle(xx, y + taw, x, y + taw);
			xx = x;

			y -= (float) (taw * 3.0 / 2.0);

			x = createAisleOfTeams(43, x, y, 5);

			floor.createAisle(x, y + taw, x, y - taw * 5.5);

			// top
			floor.createAisle(0, y - taw * 5.5, x, y - taw * 5.5);

			// bottom 3
			floor.createAisle(xx, y + taw, x, y + taw);

			IPrinter p = floor.createPrinter(printerX, 5);

			IProblem prob = null;

			if (args != null && args.length > 0) {
				File f = new File(args[0]);
				DiskContestSource source = new DiskContestSource(f);
				IContest contest2 = source.getContest();
				source.waitForContest(10000);
				IProblem[] problems = contest2.getProblems();
				prob = problems[problems.length - 1];

				double bx = center - problems.length + 1;
				for (int i = 0; i < problems.length; i++)
					floor.createBalloon(problems[i].getId(), bx + i * 2, y + taw * 4);

				// aisle along problems connecting to printer aisle
				double lastBx = bx + (problems.length - 1) * 2;
				floor.createAisle(0, y + taw * 4, lastBx, y + taw * 4);

				floor.write(f);
			}

			long time = System.currentTimeMillis();
			ITeam t1 = floor.getTeam(4);
			ITeam t2 = floor.getTeam(47);
			ITeam t3 = floor.getTeam(11);
			Path path1 = floor.getPath(t1, prob);
			Path path2 = floor.getPath(t3, p);

			Trace.trace(Trace.USER, "Time: " + (System.currentTimeMillis() - time));

			show(floor, 11, true, path1, path2);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}
