package org.icpc.tools.contest.util;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.FloorMap;
import org.icpc.tools.contest.model.FloorMap.Path;
import org.icpc.tools.contest.model.IPrinter;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.util.floor.FloorGenerator;

public class FloorGeneratorTest extends FloorGenerator {
	// table width (in meters). ICPC standard is 1.8
	private static final float tw = 1.8f;

	// table depth (in meters). ICPC standard is 0.8
	private static final float td = 0.8f;

	// team area width (in meters). ICPC standard is 3.0
	private static final float taw = 3.0f;

	// team area depth (in meters). ICPC standard is 2.2
	private static final float tad = 2.2f;

	private static FloorMap floor = new FloorMap(taw, tad, tw, td);

	private static final String balloon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	protected static void createTeamRow(int num, int startingId, float x, float y, float dx, float dy, short rotation) {
		for (int i = 0; i < num; i++) {
			floor.createTeam(startingId + i, x + dx * i, y + dy * i, rotation);
		}
	}

	public static void main(String[] args) {
		Trace.init("ICPC Floor Map Generator", "floorMap", args);

		try {
			final double TR = taw * 2.5;
			final int NUM_TEAMS = 14;
			for (int i = 0; i < NUM_TEAMS; i++) {
				floor.createTeam(i + 1, Math.sin(i * 2.0 * Math.PI / NUM_TEAMS) * TR,
						Math.cos(i * 2.0 * Math.PI / NUM_TEAMS) * TR, 270 - i * 360 / NUM_TEAMS);
			}

			final double AR = taw * 3.5;
			double[] ax = new double[NUM_TEAMS];
			double[] ay = new double[NUM_TEAMS];
			for (int i = 0; i < NUM_TEAMS; i++) {
				double ii = i + 0.5;
				ax[i] = Math.sin(ii * 2.0 * Math.PI / NUM_TEAMS) * AR;
				ay[i] = Math.cos(ii * 2.0 * Math.PI / NUM_TEAMS) * AR;
			}

			for (int i = 0; i < NUM_TEAMS; i++)
				floor.createAisle(ax[i], ay[i], ax[(i + 1) % NUM_TEAMS], ay[(i + 1) % NUM_TEAMS]);

			final double BR = taw * 4;
			IPrinter p = floor.createPrinter(0, BR);

			for (int i = 1; i < NUM_TEAMS; i++) {
				double bx = Math.sin(i * 2.0 * Math.PI / NUM_TEAMS) * BR;
				double by = Math.cos(i * 2.0 * Math.PI / NUM_TEAMS) * BR;
				floor.createBalloon(balloon.charAt(i - 1) + "", bx, by);
			}

			floor.writeTSV(System.out);

			Trace.trace(Trace.USER, "------------------");

			long time = System.currentTimeMillis();
			ITeam t1 = floor.getTeam(2);
			ITeam t2 = floor.getTeam(7);
			Path path1 = floor.getPath(t1, t2);
			Path path2 = floor.getPath(t1, p);
			// Trace.trace(Trace.USER, "Left of 32: " +
			// floor.getTeamToLeftOf(floor.getTeam(32)).getLabel());

			Trace.trace(Trace.USER, "Time: " + (System.currentTimeMillis() - time));

			show(floor, 57, true, path1, path2);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error generating floor map", e);
		}
	}
}