package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ContestData;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.standalone.TeamUtil;

public class SnakePresentation extends AbstractICPCPresentation {
	private static final long WAVE_TIME_MS = 5000;
	private static final long WAVE_WIDTH_MS = 1750;
	private BufferedImage image;
	private int min, max, x;

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		image = getContest().getBannerImage((int) (width * 0.8), (int) (height * 0.7), true, true);
	}

	@Override
	public void init() {
		min = 100000;
		max = -100000;
		IContest contest = ContestData.getContest();
		for (ITeam t : contest.getTeams()) {
			try {
				int tx = Integer.parseInt(t.getId());
				min = Math.min(min, tx);
				max = Math.max(max, tx);
			} catch (Exception e) {
				// ignore
			}
		}

		String teamLabel = TeamUtil.getTeamId();
		try {
			x = Integer.parseInt(teamLabel);
		} catch (Exception e) {
			// ignore
		}

		Trace.trace(Trace.INFO, "Team position: " + min + " -> " + max + " " + x);
	}

	@Override
	public long getRepeat() {
		return (WAVE_TIME_MS + WAVE_WIDTH_MS * 2);
	}

	@Override
	public long getDelayTimeMs() {
		return 50;
	}

	@Override
	public void paint(Graphics2D g) {
		double time = getRepeatTimeMs();
		Color color = ICPCColors.BLUE;
		double start = (double) (x - min) * WAVE_TIME_MS / (max - min);

		if (time > start && time < start + WAVE_WIDTH_MS * 3) {
			double dt = time - start;
			if (dt > WAVE_WIDTH_MS * 2) {
				color = ICPCColors.RED;
				dt -= WAVE_WIDTH_MS * 2;
			} else if (dt > WAVE_WIDTH_MS) {
				color = ICPCColors.YELLOW;
				dt -= WAVE_WIDTH_MS;
			}

			float cv = (float) (Math.sin(dt * Math.PI / WAVE_WIDTH_MS) / 255.0);
			Color c = new Color(color.getRed() * cv, color.getGreen() * cv, color.getBlue() * cv);
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
}