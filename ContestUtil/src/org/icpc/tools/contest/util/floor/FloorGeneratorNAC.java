package org.icpc.tools.contest.util.floor;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.Path;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IPrinter;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.DiskContestSource;

public class FloorGeneratorNAC extends FloorGenerator {
	// team area width (in meters). ICPC standard is 3.0
	private static final float taw = 2.2f;

	// team area depth (in meters). ICPC standard is 2.2
	private static final float tad = 2.0f;

	private static FloorMap floor = new FloorMap(taw, tad, 1.8, 0.8);

	protected static void createTeamPod(int num, int startingId, float x, float y, int skipTable) {
		int teamId = startingId;
		for (int i = 0; i < num; i++) {
			short rotation = (short) ((45 * i + 135) % 360);
			double tx = x + 3.2 * Math.sin(Math.toRadians(rotation));
			double ty = y + 3.2 * Math.cos(Math.toRadians(rotation));
			if (skipTable == i)
				continue;
			floor.createTeam(teamId, tx, ty, (rotation + 90) % 360);
			teamId++;
		}
	}

	protected static void createTeamPodAisles(float x, float y, List<Point2D.Double> list, int num) {
		double AISLE = 6.5;
		for (int i = 0; i < num; i++) {
			short r1 = (short) ((45 * (i + 3) + 135 + 22) % 360);
			double x1 = x + AISLE * Math.sin(Math.toRadians(r1));
			double y1 = y + AISLE * Math.cos(Math.toRadians(r1));
			list.add(new Point2D.Double(x1, y1));
		}
	}

	protected static void createTeamPodAisles(float x, float y, List<Point2D.Double> list) {
		createTeamPodAisles(x, y, list, 8);
	}

	protected static void createAisle(List<Point2D.Double> list, int i, int j) {
		Point2D.Double p1 = list.get(i);
		Point2D.Double p2 = list.get(j);
		floor.createAisle(p1.x, p1.y, p2.x, p2.y);
	}

	public static void main(String[] args) {
		Trace.init("ICPC Floor Map Generator", "floorMap", args);

		try {
			/*float dyRow = aisleWidth;
			float rowX = td + elecGap;*/

			float x = 0;
			float y = 0;

			/*double rightAisle = x + taw / 2 + centerAisleWidth / 2;
			double leftAisle = x - 19.5 * taw - centerAisleWidth * 3 / 2;*/

			float GAP = 17;
			createTeamPod(7, 8, x, y, 3);
			createTeamPod(7, 1, x + GAP, y, -1);

			createTeamPod(7, 14, x + GAP / 2, y + GAP / 2, -1);

			createTeamPod(7, 28, x, y + GAP, 3);
			createTeamPod(7, 21, x + GAP, y + GAP, -1);

			createTeamPod(7, 34, x + GAP / 2, y + GAP * 3 / 2, -1);

			createTeamPod(7, 48, x, y + GAP * 2, 3);
			createTeamPod(7, 41, x + GAP, y + GAP * 2, -1);

			createTeamPod(7, 55, x + GAP / 2, y + GAP * 5 / 2, 0);

			List<Point2D.Double> ai = new ArrayList<>();
			createTeamPodAisles(x, y, ai);
			createTeamPodAisles(x + GAP, y, ai);
			createTeamPodAisles(x, y + GAP, ai);
			createTeamPodAisles(x + GAP, y + GAP, ai);
			createTeamPodAisles(x, y + GAP * 2, ai);
			createTeamPodAisles(x + GAP, y + GAP * 2, ai);
			createTeamPodAisles(x + GAP / 2, y + GAP * 5 / 2, ai, 4);

			int size = ai.size();
			for (int i = 0; i < size - 1; i++) {
				for (int j = i + 1; j < size; j++) {
					Point2D.Double p1 = ai.get(i);
					Point2D.Double p2 = ai.get(j);
					double dx = p1.x - p2.x;
					double dy = p1.y - p2.y;
					if (Math.sqrt(dx * dx + dy * dy) < 7.5)
						floor.createAisle(p1.x, p1.y, p2.x, p2.y);
				}
			}
			createAisle(ai, 5, 14);

			createAisle(ai, 0, 23);
			createAisle(ai, 11, 28);

			createAisle(ai, 16, 39);
			createAisle(ai, 27, 44);

			// createAisle(ai, 32, 48);
			// createAisle(ai, 42, 51);

			// floor.createAisle(rightAisle, y, leftAisle, y);

			IPrinter p = floor.createPrinter(25, 5);

			// fix teams
			// ((Team) floor.getTeam(54)).add("id", "<-1>");
			// ((Team) floor.getTeam(61)).add("id", "<-1>");
			// ((Team) floor.getTeam(62)).add("id", "<-1>");
			// ((Team) floor.getTeam(63)).add("id", "<-1>");

			if (args != null && args.length > 0) {
				File f = new File(args[0]);
				DiskContestSource source = new DiskContestSource(f);
				IContest contest2 = source.getContest();
				source.waitForContest(10000);
				IProblem[] problems = contest2.getProblems();

				double bx = 0;
				for (int i = 0; i < problems.length; i++)
					floor.createBalloon(problems[i].getId(), bx + i * 2, -8);

				floor.rotate(-90);

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