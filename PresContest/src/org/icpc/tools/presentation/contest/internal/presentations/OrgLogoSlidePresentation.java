package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;

public class OrgLogoSlidePresentation extends AbstractICPCPresentation {
	private static final long TIME_DT = 15000;
	private Map<String, BufferedImage> map = new HashMap<>();
	private int size;

	@Override
	public void aboutToShow() {
		IContest contest = getContest();
		if (contest == null)
			return;

		IOrganization[] orgs = contest.getOrganizations();
		if (orgs.length == 0) {
			size = 25;
			return;
		}

		map.clear();
		size = height / 4;

		// rescale logo images
		int size2 = (int) (size * 0.9);
		for (IOrganization org : orgs) {
			map.put(org.getId(), org.getLogoImage(size2, size2, true, true));
		}
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		map.clear();
	}

	@Override
	public void paint(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
		g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		IContest contest = getContest();
		if (contest == null)
			return;

		IOrganization[] orgs = contest.getOrganizations();
		int[] numInRow = new int[4];
		int[] rowStart = new int[4];
		int numPerRow = orgs.length / 4;
		for (int i = 0; i < 4; i++)
			numInRow[i] = numPerRow;

		int c = 0;
		while (numPerRow * 4 + c < orgs.length)
			numInRow[c++]++;

		for (int i = 1; i < 4; i++)
			rowStart[i] = rowStart[i - 1] + numInRow[i - 1];

		int numW = width / size;
		int dy = (height - size * 4) / 2;

		double ds = ((double) size * (getTimeMs() % TIME_DT) / TIME_DT);
		long colOffset = getTimeMs() / TIME_DT;
		for (int row = 0; row < 4; row++) {
			int y = dy + row * size;
			for (int col = 0; col <= numW + 1; col++) {
				if (numInRow[row] == 0)
					continue;

				double x = col * size;
				int num = 0;
				if (row % 2 == 0) {
					x -= ds;
					num = rowStart[row] + (int) ((col + colOffset) % numInRow[row]);
				} else {
					x += ds - size;
					num = rowStart[row] + (int) ((col - colOffset % orgs.length + orgs.length) % numInRow[row]);
				}

				IOrganization org = orgs[num];
				BufferedImage img = map.get(org.getId());
				g.translate(x, 0);
				if (img != null)
					g.drawImage(img, (size - img.getWidth()) / 2, y + (size - img.getHeight()) / 2, null);
				g.translate(-x, 0);
			}
		}
	}
}