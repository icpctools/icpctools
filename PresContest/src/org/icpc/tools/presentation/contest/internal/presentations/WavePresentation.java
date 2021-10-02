package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;
import org.icpc.tools.presentation.contest.internal.ICPCColors;
import org.icpc.tools.presentation.contest.internal.standalone.TeamUtil;

public class WavePresentation extends AbstractICPCPresentation {
	private static final long WAVE_TIME_MS = 3500;
	private static final long WAVE_WIDTH_MS = 1750;
	private BufferedImage image;
	private double max, min, x;

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		image = getContest().getBannerImage((int) (width * 0.8), (int) (height * 0.6), true, true);
	}

	@Override
	public void init() {
		min = 100000;
		max = -100000;
		String teamId = TeamUtil.getTeamId();
		IContest contest = getContest();
		for (ITeam t : contest.getTeams()) {
			min = Math.min(min, t.getX());
			max = Math.max(max, t.getX());
		}
		ITeam t = contest.getTeamById(teamId);
		if (t != null)
			x = t.getX();
		Trace.trace(Trace.INFO, "Floor map team: " + min + " -> " + max + " " + x);
	}

	@Override
	public long getRepeat() {
		return (WAVE_TIME_MS * 2 + WAVE_WIDTH_MS * 2) * 3;
	}

	@Override
	public long getDelayTimeMs() {
		return 5;
	}

	@Override
	public void paint(Graphics2D g) {
		long time = getRepeatTimeMs();
		Color base = ICPCColors.BLUE;
		long cycle = WAVE_TIME_MS * 2 + WAVE_WIDTH_MS * 2;
		if (time > cycle * 2) {
			base = ICPCColors.YELLOW;
			time -= cycle * 2;
		} else if (time > cycle) {
			base = ICPCColors.RED;
			time -= cycle;
		}
		double start = (x - min) * WAVE_TIME_MS / (max - min);
		double start2 = WAVE_TIME_MS + WAVE_WIDTH_MS + (max - x) * WAVE_TIME_MS / (max - min);

		double d = -1;
		if (time > start && time < start + WAVE_WIDTH_MS)
			d = (time - start) / WAVE_WIDTH_MS;
		else if (time > start2 && time < start2 + WAVE_WIDTH_MS)
			d = (time - start2) / WAVE_WIDTH_MS;
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
}