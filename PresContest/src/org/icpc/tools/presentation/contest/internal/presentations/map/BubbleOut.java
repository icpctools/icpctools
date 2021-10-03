package org.icpc.tools.presentation.contest.internal.presentations.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BubbleOut {
    static final double DEGREES_PER_SECOND = 3;
    static final double OVERLAP = .2;

    public static void bubbleOut(TeamIntroPresentation.Position[] instPos, int teamInd,
                                 int width, int height, double imageScaleFactor, long dt) {
        teamInd = Math.min(teamInd, instPos.length);

        // Set original positions, find largest image diameter (assuming actual images are round)
        double maxD = 0;
        for (int i = 0; i < teamInd; i++) {
            TeamIntroPresentation.Position p = instPos[i];
            if (p == null || Double.isNaN(p.x) || Double.isNaN(p.y) || p.smImage == null) {
                continue;
            }
            if (Double.isNaN(p.originalX)) {
                p.originalX = p.x;
            }
            if (Double.isNaN(p.originalY)) {
                p.originalY = p.y;
            }
            // images are drawn 1:1 on the width x height area,
            // but all coordinates here are lat/lon coordinates
            // TODO: include a 2 * width / height factor to account for map stretching
            double scaledX = p.smImage.getWidth() * 360.0 / width;
            double scaledY = p.smImage.getHeight() * 180.0 / height;
            maxD = Math.max(maxD, Math.max(scaledX, scaledY));
        }
        maxD *= imageScaleFactor;
        maxD *= (1 - OVERLAP);
        maxD = Math.max(maxD, 0.001);

        // Put positions in a coarse grid
        Map<Integer, List<TeamIntroPresentation.Position>> grid = new HashMap<>();
        for (int i = 0; i < teamInd; i++) {
            TeamIntroPresentation.Position p = instPos[i];
            if (p == null || Double.isNaN(p.x) || Double.isNaN(p.y) || p.smImage == null) {
                continue;
            }
            int gridX = (int) Math.round(p.x / maxD);
            int gridY = (int) Math.round(p.y / maxD);
            int gridKey = gridKey(gridX, gridY);
            p.gridX = gridX * maxD;
            p.gridY = gridY * maxD;
            if (grid.get(gridKey) == null) {
                grid.put(gridKey, new ArrayList<>());
            }
            grid.get(gridKey).add(p);
        }

        // Iterate through neighbouring grids to bubble things apart
        for (int i = 0; i < teamInd; i++) {
            TeamIntroPresentation.Position p = instPos[i];
            if (p == null || Double.isNaN(p.x) || Double.isNaN(p.y) || p.smImage == null) {
                continue;
            }
            int gridX = (int) Math.round(p.x / maxD);
            int gridY = (int) Math.round(p.y / maxD);

            double dtf = DEGREES_PER_SECOND * Math.min(1, dt / 1000.0);
            double totalAdjust = 0;
            double minD = Double.POSITIVE_INFINITY;
            for (int dgx = -1; dgx <= 1; ++dgx) {
                for (int dgy = -1; dgy <= 1; ++dgy) {
                    int gridKey = gridKey(gridX + dgx, gridY + dgy);
                    List<TeamIntroPresentation.Position> gridList = grid.get(gridKey);
                    if (gridList == null) {
                        continue;
                    }
                    for (TeamIntroPresentation.Position q : gridList) {
                        if (p == q) {
                            continue;
                        }
                        // move away from other position
                        double dx = q.x - p.x;
                        double dy = q.y - p.y;
                        double d = dist(dx, dy);
                        minD = Math.min(minD, d);
                        if (d > maxD) {
                            continue;
                        }
                        if (d < 1e-6) {
                            dx += 1e-6 * (1 + Math.random());
                            dy += 1e-6 * (1 + Math.random());
                            d = dist(dx, dy);
                        }
                        double ff = 1.0 - d / maxD;
                        p.x -= dtf * ff * dx / d;
                        p.y -= dtf * ff * dy / d;
                        totalAdjust += ff;
                    }
                }
            }
            // stop things moving around
            final double adjustThreshold = .1;
            if (totalAdjust > adjustThreshold || minD > maxD) {
                // move towards original position
                double dx = p.originalX - p.x;
                double dy = p.originalY - p.y;
                double d = dist(dx, dy);
                double ff = .02;
                if (d > 1e-6) {
                    p.x += dtf * ff * dx / d;
                    p.y += dtf * ff * dy / d;
                }
                //System.out.println(String.format("%s %.3f adj %.3f %.2f scale %.2f", p.label.substring(0, 10), d, totalAdjust, minD / maxD, imageScaleFactor));
            }
        }
    }

    private static int gridKey(int gridX, int gridY) {
        return gridX + 1000 * gridY;
    }

    private static double dist(double dx, double dy) {
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static void restore(TeamIntroPresentation.Position[] instPos) {
        for (TeamIntroPresentation.Position p : instPos) {
            if (p == null || Double.isNaN(p.originalX) || Double.isNaN(p.originalY)) {
                continue;
            }
            p.x = p.originalX;
            p.y = p.originalY;
        }
    }
}
