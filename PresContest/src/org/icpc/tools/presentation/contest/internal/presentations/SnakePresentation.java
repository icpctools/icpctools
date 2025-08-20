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
	private BufferedImage[] images;
	private int min, max, x;

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);

		images = new BufferedImage[2];
		images[0] = getContest().getBannerLightModeImage((int) (width * 0.8), (int) (height * 0.7), true, true, "dark");
		images[1] = getContest().getBannerLightModeImage((int) (width * 0.8), (int) (height * 0.7), true, true, "light");
		for (BufferedImage img : images) {
			if (img == null) {
				images = new BufferedImage[1];
				images[0] = getContest().getBannerImage((int) (width * 0.8), (int) (height * 0.7), true, true);
				break;
			}
		}
		//image = getContest().getBannerImage((int) (width * 0.8), (int) (height * 0.7), true, true);
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

		String teamId = TeamUtil.getTeamId(contest);
		try {
			x = Integer.parseInt(teamId);
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

	public float sRGBToLinear(float c){
		return (c <= 0.04045f) ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
	}

	public float[] sRGBToLinear(Color color) {
		float r = sRGBToLinear(color.getRed() / 255.0f);
		float g = sRGBToLinear(color.getGreen() / 255.0f);
		float b = sRGBToLinear(color.getBlue() / 255.0f);
		return new float[]{r, g, b};
	}

	public float luminance(float[] linearRGB) {
		return 0.2126f * linearRGB[0] + 0.7152f * linearRGB[1] + 0.0722f * linearRGB[2];
	}

	public float getRelativeLuminance(Color color) {
		float[] linear = sRGBToLinear(color);
		return luminance(linear);
	}

	public float contrastRatio(Color c1, Color c2) {
		float l1 = getRelativeLuminance(c1);
		float l2 = getRelativeLuminance(c2);
		return (Math.max(l1, l2) + 0.05f) / (Math.min(l1, l2) + 0.05f);
	}

	@Override
	public void paint(Graphics2D g) {
		double time = getRepeatTimeMs();
		Color color = ICPCColors.BLUE;
		double start = (double) (x - min) * WAVE_TIME_MS / (max - min);
		float cv = (float) (Math.sin(start * Math.PI / WAVE_WIDTH_MS) / 255.0);
		Color c = new Color(color.getRed() * cv, color.getGreen() * cv, color.getBlue() * cv);

		if (time > start && time < start + WAVE_WIDTH_MS * 3) {
			double dt = time - start;
			if (dt > WAVE_WIDTH_MS * 2) {
				color = ICPCColors.RED;
				dt -= WAVE_WIDTH_MS * 2;
			} else if (dt > WAVE_WIDTH_MS) {
				color = ICPCColors.YELLOW;
				dt -= WAVE_WIDTH_MS;
			}

			cv = (float) (Math.sin(dt * Math.PI / WAVE_WIDTH_MS) / 255.0);
			c = new Color(color.getRed() * cv, color.getGreen() * cv, color.getBlue() * cv);
			g.setColor(c);
			g.fillRect(0, 0, width, height);
		}

		int h = 0;
		if (images != null) {
			BufferedImage image = null;
			if (images.length == 1) {
				image = images[0];
			} else {
				// Draw the banner image which works best for this background (light or dark)
				float lightContract = contrastRatio(c, new Color(255, 255, 255));
				float darkContract = contrastRatio(c, new Color(0, 0, 0));
				if (lightContract > darkContract)
					image = images[0];
				else
					image = images[1];
			}
			int w = image.getWidth();
			h = image.getHeight();
			g.drawImage(image, (width - w) / 2, height - h - 20, null);
		}
	}
}