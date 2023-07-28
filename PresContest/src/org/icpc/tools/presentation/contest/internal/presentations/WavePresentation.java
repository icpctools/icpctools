package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.standalone.TeamUtil;

public class WavePresentation extends AbstractICPCPresentation {
	private enum Direction {
		X, Y, HV, VH
	}

	private static final long WAVE_TIME_MS = 3500;
	private static final long WAVE_WIDTH_MS = 2000;
	private BufferedImage image;
	private double max, min, v;
	private Direction dir = Direction.X;
	private double speed = 3.0;
	private boolean hard = true;

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		image = getContest().getBannerImage((int) (width * 0.8), (int) (height * 0.5), true, true);
	}

	@Override
	public void init() {
		min = Double.MAX_VALUE;
		max = Double.MIN_VALUE;
		String teamId = TeamUtil.getTeamId();
		IContest contest = getContest();
		for (ITeam t : contest.getTeams()) {
			double vv = 0;
			if (dir == Direction.X)
				vv = t.getX();
			else if (dir == Direction.Y)
				vv = t.getY();
			else if (dir == Direction.HV)
				vv = t.getX() + t.getY();
			else if (dir == Direction.VH)
				vv = t.getX() - t.getY();
			if (!Double.isNaN(vv)) {
				min = Math.min(min, vv);
				max = Math.max(max, vv);
			}
		}

		// account for team members
		min -= 1.05;
		max += 1.05;

		ITeam t = contest.getTeamById(teamId);
		if (t != null) {
			Point2D p = TeamUtil.getLocation(t);
			if (dir == Direction.X)
				v = p.getX();
			else if (dir == Direction.Y)
				v = p.getY();
			else if (dir == Direction.HV)
				v = t.getX() + t.getY();
			else if (dir == Direction.VH)
				v = t.getX() - t.getY();
		}

		Trace.trace(Trace.INFO, "Floor map team: " + min + " -> " + max + " " + v);
	}

	@Override
	public long getRepeat() {
		return (int) ((WAVE_TIME_MS * 2 + WAVE_WIDTH_MS * 2) * 3 / speed);
	}

	@Override
	public long getDelayTimeMs() {
		return 5;
	}

	@Override
	public void paintImpl(Graphics2D g) {
		long timeMs = getRepeatTimeMs();
		double time = timeMs * speed;
		long waveWidth = WAVE_WIDTH_MS;
		if (hard)
			waveWidth /= 3;

		Color base = ICPCColors.BLUE;
		long cycle = WAVE_TIME_MS * 2 + waveWidth * 2;
		if (time > cycle * 2) {
			base = ICPCColors.YELLOW;
			time -= cycle * 2;
		} else if (time > cycle) {
			base = ICPCColors.RED;
			time -= cycle;
		}
		double start = (v - min) * waveWidth / (max - min);
		double start2 = WAVE_TIME_MS + waveWidth + (max - v) * WAVE_TIME_MS / (max - min);

		double d = -1;
		if (time > start && time < start + waveWidth)
			d = (time - start) / waveWidth;
		else if (time > start2 && time < start2 + waveWidth)
			d = (time - start2) / waveWidth;
		if (d > 0) {
			float cv = (float) (Math.sin(d * Math.PI) / 255.0);
			Color c = new Color(base.getRed() * cv, base.getGreen() * cv, base.getBlue() * cv);
			g.setColor(c);
			g.fillRect(0, 0, width, height);
		}

		int h = 0;
		if (image != null) {
			int w = image.getWidth();
			h = image.getHeight();
			g.drawImage(image, (width - w) / 2, height - h - 20, null);
		}
	}

	@Override
	public void setProperty(String value) {
		super.setProperty(value);
		if (value == null || value.isEmpty())
			return;

		if ("x".equals(value) || "h".equals(value))
			dir = Direction.X;
		else if ("y".equals(value) || "v".equals(value))
			dir = Direction.Y;
		else if ("hv".equals(value))
			dir = Direction.HV;
		else if ("vh".equals(value))
			dir = Direction.VH;
		else if (value.startsWith("speed:"))
			try {
				speed = Double.parseDouble(value.substring(6));
			} catch (Exception e) {
				// ignore
			}
		else if ("hard".equals(value))
			hard = true;
		else if ("soft".equals(value))
			hard = false;
	}
}