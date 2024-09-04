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

public class FloorGeneratorAstana48 extends FloorGenerator {
	// team area width (in meters). ICPC standard is 3.0
	private static final float taw = 3.0f;

	// team area depth (in meters). ICPC standard is 2.2
	private static final float tad = 2.2f;

	private static final float aisle = 1.5f;

	private static FloorMap floor = new FloorMap(taw, tad, 1.8, 0.9);

	public static void main(String[] args) {
		Trace.init("ICPC Floor Map Generator", "floorMap", args);

		try {
			// create grid of teams, with special spacing and gaps
			double x = 0;
			double y = 0;
			double[] a = new double[9];

			double ax1 = x - taw / 2 - aisle / 2;
			double ax2 = x + taw * 9.5 + aisle / 2;
			floor.createAisle(ax1, y, ax2, y);
			a[0] = y;
			y += aisle / 2 + tad;
			double offset = taw * 2 / 3;

			floor.createTeamRow(9, 9, x + offset, y, FloorMap.S, true, false);
			y += tad / 2;
			floor.createTeamRow(9, 10, x + offset, y, FloorMap.N, false, true);

			y += aisle / 2 + tad;
			floor.createAisle(ax1, y, ax2, y);
			a[1] = y;
			y += aisle / 2 + tad;

			floor.createTeamRow(9, 27, x + offset, y, FloorMap.S, true, false);
			y += tad / 2;
			floor.createTeamRow(9, 28, x + offset, y, FloorMap.N, false, true);

			y += aisle / 2 + tad;
			floor.createAisle(ax1, y, ax2, y);
			a[2] = y;
			y += aisle / 2 + tad;

			floor.createTeamRow(9, 45, x + offset, y, FloorMap.S, true, false);
			y += tad / 2;
			floor.createTeamRow(9, 46, x + offset, y, FloorMap.N, false, true);

			y += aisle / 2 + tad;
			floor.createAisle(ax1, y, ax2, y);
			a[3] = y;
			y += aisle / 2 + tad;

			floor.createTeamRow(9, 63, x, y, FloorMap.S, true, false);
			y += tad / 2;
			floor.createTeamRow(9, 64, x, y, FloorMap.N, false, true);

			y += aisle / 2 + tad + 0.5;
			floor.createAisle(ax1, y, ax2, y);
			a[4] = y;
			y += aisle / 2 + tad + 0.5;

			floor.createTeamRow(9, 81, x, y, FloorMap.S, true, false);
			y += tad / 2;
			floor.createTeamRow(9, 82, x, y, FloorMap.N, false, true);

			y += aisle / 2 + tad;
			floor.createAisle(ax1, y, ax2, y);
			a[5] = y;
			y += aisle / 2 + tad;

			floor.createTeamRow(9, 99, x + offset, y, FloorMap.S, true, false);
			y += tad / 2;
			floor.createTeamRow(9, 100, x + offset, y, FloorMap.N, false, true);

			y += aisle / 2 + tad;
			floor.createAisle(ax1, y, ax2, y);
			a[6] = y;
			y += aisle / 2 + tad;

			floor.createTeamRow(9, 117, x + offset, y, FloorMap.S, true, false);
			y += tad / 2;
			floor.createTeamRow(9, 118, x + offset, y, FloorMap.N, false, true);

			y += aisle / 2 + tad;
			floor.createAisle(ax1, y, ax2, y);
			a[7] = y;
			y += aisle / 2 + tad;

			floor.createTeamRow(8, 134, x + offset, y, FloorMap.S, true, false);
			y += tad / 2;
			floor.createTeamRow(8, 135, x + offset, y, FloorMap.N, false, true);

			y += aisle / 2 + tad;
			floor.createAisle(ax1, y, ax2, y);
			a[8] = y;

			floor.createAisle(ax1, a[0], ax1, a[8]);
			floor.createAisle(ax2, a[0], ax2, a[8]);

			// spares
			ITeam t = createAdjacentTeam(floor, 127, -1, taw, 0, FloorMap.S);
			floor.makeSpare(t);
			t = createAdjacentTeam(floor, 142, -1, taw, 0, FloorMap.N);
			floor.makeSpare(t);

			Printer p = floor.createPrinter(ax2 + taw, a[5]);

			if (args != null && args.length > 0) {
				File f = new File(args[0]);
				DiskContestSource source = new DiskContestSource(f);
				IContest contest2 = source.getContest();
				source.waitForContest(10000);
				IProblem[] problems = contest2.getProblems();
				System.out.println("problems: " + problems.length);

				double ix = (a[7] - a[1]) / (problems.length - 1);
				for (int i = 0; i < problems.length; i++)
					floor.createBalloon(problems[i].getId(), ax2 + taw / 2, a[1] + i * ix);

				floor.convertSpares(contest2);
				floor.write(f);
			}

			long time = System.currentTimeMillis();
			ITeam t1 = floor.getTeam(10);
			ITeam t3 = floor.getTeam(57);
			IProblem pr = floor.getBalloon("C");
			Path path1 = floor.getPath(t1, pr);
			Path path2 = floor.getPath(t3, p);

			Trace.trace(Trace.USER, "Time: " + (System.currentTimeMillis() - time));

			show(floor, 57, true, path1, path2);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}