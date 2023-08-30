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

public class OrgLogoWallPresentation extends AbstractICPCPresentation {
	private Map<String, BufferedImage> map = new HashMap<>();
	private int size;

	@Override
	public long getRepeat() {
		return 11000L;
	}

	@Override
	public long getDelayTimeMs() {
		return 100;
	}

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

		// determine size
		map.clear();
		size = (int) (Math.sqrt(width * height / orgs.length) * 1.0);

		int num = width / size * (height / size) - ((height / size) / 2);
		while (num < orgs.length && size > 5) {
			size--;
			num = (width / size) * (height / size) - ((height / size) / 2);
		}

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
	public void paintImpl(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		IContest contest = getContest();
		if (contest == null)
			return;

		IOrganization[] orgs = contest.getOrganizations();

		int dx = (width - (width / size) * size) / 2;
		int dy = (height - (height / size) * size) / 2;

		int i = dx;
		int j = dy;
		boolean oddRow = false;
		// Graphics2D gg = g.create();
		// long time = getTimeMs();
		// gg.translate(, 0);
		for (IOrganization org : orgs) {
			BufferedImage img = map.get(org.getId());
			if (img != null) {
				g.drawImage(img, i + (size - img.getWidth()) / 2, j + (size - img.getHeight()) / 2, null);
				i += size;
				if (i > width - size) {
					i = dx;
					if (!oddRow)
						i += size / 2;

					oddRow = !oddRow;
					j += size;
					if (j > height - size * 2) {
						// center last row?
					}
				}
			}
		}
	}
}