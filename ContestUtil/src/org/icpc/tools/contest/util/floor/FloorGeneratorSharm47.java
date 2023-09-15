package org.icpc.tools.contest.util.floor;

import java.io.File;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.DiskContestSource;

public class FloorGeneratorSharm47 extends FloorGenerator {
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
			for (int i = 0; i < 142; i++) {
				int x = i % 18;
				int y = i / 18;

				if (y % 2 == 1)
					x = 17 - i % 18;

				// set team number/label
				int n = i + 1;
				if (n == 109 || n == 110)
					continue;
				else if (n > 110)
					n -= 2;

				if (x > 8)
					x++;

				float xx = x * taw;
				float yy = -(y / 2) * (aisle + tad * 2);
				if (y % 2 == 1)
					yy -= tad / 2;

				floor.createTeam(47000 + n, xx, yy, (y % 2 == 0) ? FloorMap.N : FloorMap.S);
			}

			float left = -taw / 2 - aisle / 2;
			float middle = left + taw * 9 + aisle / 2 + taw / 2;
			float right = left + taw * 19 + aisle;
			float bottom = tad * 3 / 4 + aisle / 2;
			float top = bottom - aisle * 4 - tad * 8;
			// left, middle and right aisles
			floor.createAisle(left, bottom, left, top);
			floor.createAisle(middle, bottom, middle, top);
			floor.createAisle(right, bottom, right, top);

			// middle aisles
			for (int i = 0; i < 5; i++) {
				float yy = bottom - (tad * 2 + aisle) * i;
				floor.createAisle(left, yy, right, yy);
			}

			// extra shortcut aisles
			floor.createAisle(left + taw, top + tad * 2 + aisle, left + taw, top); // vertical
			floor.createAisle(left + taw, top, left, top + tad * 2 + aisle); // diagonal

			ITeam t = createAdjacentTeam(floor, 47109, -1, 0, -taw);
			floor.makeSpare(t);

			t = createAdjacentTeam(floor, 47140, -1, 0, -taw);
			floor.makeSpare(t);

			// TODO: currently rotated based on floor layout map. may want to switch horizontally
			// (remove this line - better on screen) or vertically (change to 90) for spectator's view
			floor.rotate(45);

			if (args != null && args.length > 0) {
				File f = new File(args[0]);
				DiskContestSource source = new DiskContestSource(f);
				IContest contest2 = source.getContest();
				source.waitForContest(10000);
				IProblem[] problems = contest2.getProblems();

				double bx = 0;
				double ix = 0 * 2 / (problems.length - 1);
				for (int i = 0; i < problems.length; i++)
					floor.createBalloon(problems[i].getId(), bx + i * ix, -taw * 6.5);

				floor.write(f);
			}

			long time = System.currentTimeMillis();
			/*ITeam t1 = floor.getTeam(10);
			ITeam t2 = floor.getTeam(47);
			ITeam t3 = floor.getTeam(57);
			Path path1 = floor.getPath(t1, t2);
			Path path2 = floor.getPath(t3, p);*/

			Trace.trace(Trace.USER, "Time: " + (System.currentTimeMillis() - time));

			// show(floor, 57, true, path1, path2);
			show(floor, 46057, true, null, null);
			// show(floor, 57, true);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}