package org.icpc.tools.contest.util.floor;

import java.io.File;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.Path;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.DiskContestSource;
import org.icpc.tools.contest.model.internal.MapInfo.Printer;

public class FloorGeneratorLuxor46 extends FloorGenerator {
	// team area width (in meters). ICPC standard is 3.0
	private static final float taw = 3.0f;

	// team area depth (in meters). ICPC standard is 2.2
	private static final float tad = 2.0f;

	private static final float aisle = 1.5f;

	private static FloorMap floor = new FloorMap(taw, tad, 1.8, 0.9);

	public static void main(String[] args) {
		Trace.init("ICPC Floor Map Generator", "floorMap", args);

		try {
			// create grid of teams, with special spacing and gaps
			double x = 0;
			double y = 0;
			double[] a = new double[9];

			floor.createAisle(x, y + taw / 2 + aisle / 2, x, y - taw * 9 + aisle / 2);
			a[0] = x;
			x += aisle / 2 + tad;

			floor.createTeamRow(8, 46008, x, y, FloorMap.E, false, false);
			x += tad / 2;
			floor.createTeamRow(8, 46009, x, y, FloorMap.W, true, true);

			x += aisle / 2 + tad;
			floor.createAisle(x, y + taw / 2 + aisle / 2, x, y - taw * 9 + aisle / 2);
			a[1] = x;
			x += aisle / 2 + tad;

			floor.createTeamRow(8, 46024, x, y, FloorMap.E, false, false);
			x += tad / 2;
			floor.createTeamRow(8, 46025, x, y, FloorMap.W, true, true);

			x += aisle / 2 + tad;
			floor.createAisle(x, y + taw / 2 + aisle / 2, x, y - taw * 9 + aisle / 2);
			a[2] = x;
			x += aisle / 2 + tad;

			floor.createTeamRow(8, 46040, x, y, FloorMap.E, false, false);
			x += tad / 2;
			floor.createTeamRow(8, 46041, x, y, FloorMap.W, true, true);

			x += aisle / 2 + tad;
			floor.createAisle(x, y + taw / 2 + aisle / 2, x, y - taw * 9 + aisle / 2);
			a[3] = x;
			x += aisle / 2 + tad;

			floor.createTeamRow(8, 46056, x, y, FloorMap.E, false, false);
			x += tad / 2;
			floor.createTeamRow(9, 46057, x, y, FloorMap.W, true, true);

			x += aisle / 2 + tad;
			floor.createAisle(x, y + taw / 2 + aisle / 2, x, y - taw * 9 + aisle / 2);
			a[4] = x;
			x += aisle / 2 + tad;

			floor.createTeamRow(9, 46074, x, y, FloorMap.E, false, false);
			x += tad / 2;
			floor.createTeamRow(9, 46075, x, y, FloorMap.W, true, true);

			x += aisle / 2 + tad;
			floor.createAisle(x, y + taw / 2 + aisle / 2, x, y - taw * 9 + aisle / 2);
			a[5] = x;
			x += aisle / 2 + tad;

			floor.createTeamRow(8, 46091, x, y, FloorMap.E, false, false);
			x += tad / 2;
			floor.createTeamRow(8, 46092, x, y, FloorMap.W, true, true);

			x += aisle / 2 + tad;
			floor.createAisle(x, y + taw / 2 + aisle / 2, x, y - taw * 9 + aisle / 2);
			a[6] = x;
			x += aisle / 2 + tad;

			floor.createTeamRow(8, 46107, x, y, FloorMap.E, false, false);
			x += tad / 2;
			floor.createTeamRow(8, 46108, x, y, FloorMap.W, true, true);

			x += aisle / 2 + tad;
			floor.createAisle(x, y + taw / 2 + aisle / 2, x, y - taw * 9 + aisle / 2);
			a[7] = x;
			x += aisle / 2 + tad;

			floor.createTeamRow(8, 46123, x, y, FloorMap.E, false, false);
			x += tad / 2;
			floor.createTeamRow(8, 46124, x, y, FloorMap.W, true, true);

			x += aisle / 2 + tad;
			floor.createAisle(x, y + taw / 2 + aisle / 2, x, y - taw * 9 + aisle / 2);
			a[8] = x;

			floor.createAisle(0, taw / 2 + aisle / 2, x, y + taw / 2 + aisle / 2);
			floor.createAisle(0, -taw * 9 + aisle / 2, x, -taw * 9 + aisle / 2);
			floor.createAisle(0, -taw * 8 + aisle / 2, a[3], -taw * 8 + aisle / 2);
			floor.createAisle(a[5], -taw * 8 + aisle / 2, x, -taw * 8 + aisle / 2);

			for (int i = 0; i < 8; i++) {
				if (i == 3 || i == 4)
					continue;
				floor.createAisle(a[i], -taw * 8 + aisle / 2, a[i + 1], -taw * 9 + aisle / 2);
				floor.createAisle(a[i], -taw * 9 + aisle / 2, a[i + 1], -taw * 8 + aisle / 2);
			}

			// spares
			ITeam t = createAdjacentTeam(floor, 46033, -1, 0, -taw);
			floor.makeSpare(t);
			t = createAdjacentTeam(floor, 46048, -2, 0, -taw);
			floor.makeSpare(t);
			t = createAdjacentTeam(floor, 46049, -3, 0, -taw);
			floor.makeSpare(t);

			Printer p = floor.createPrinter(x, -taw * 9);

			if (args != null && args.length > 0) {
				File f = new File(args[0]);
				DiskContestSource source = new DiskContestSource(f);
				IContest contest2 = source.getContest();
				source.waitForContest(10000);

				IProblem[] problems = contest2.getProblems();

				double ix = (x - aisle * 2) / (problems.length - 1);
				for (int i = 0; i < problems.length; i++)
					floor.createBalloon(problems[i].getId(), aisle + i * ix, -taw * 9);

				floor.convertSpares(contest2);
				floor.write(f);
			}

			long time = System.currentTimeMillis();
			ITeam t1 = floor.getTeam(46010);
			// ITeam t2 = floor.getTeam(46047);
			ITeam t3 = floor.getTeam(46057);
			IProblem pr = floor.getBalloon("M");
			Path path1 = floor.getPath(t1, pr);
			Path path2 = floor.getPath(t3, p);

			Trace.trace(Trace.USER, "Time: " + (System.currentTimeMillis() - time));

			show(floor, 46057, true, path1, path2);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}