package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IOrganization;
import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;

public class OrgLogoFadePresentation extends AbstractICPCPresentation {
	private static final int ROWS = 3;
	private static final int MS_PER_LOGO = 30000;
	private static final int FADE = 750;

	private Map<String, BufferedImage> map = new HashMap<>();
	private int size;
	private int[] randomOrder;

	@Override
	public long getDelayTimeMs() {
		return 25;
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
		size = height / ROWS;

		execute(new Runnable() {
			@Override
			public void run() {
				int size2 = (int) (size * 0.9);
				for (IOrganization org : orgs) {
					map.put(org.getId(), org.getLogoImage(size2, size2, true, true));
				}
			}
		});
	}

	@Override
	public void paint(Graphics2D g) {
		IContest contest = getContest();
		if (contest == null)
			return;

		IOrganization[] orgs = contest.getOrganizations();
		int evenRow = (width / size);
		int oddRow = (width - size / 2) / size;
		int numPerScreen = evenRow * (ROWS + 1) / 2 + oddRow * ROWS / 2;
		if (randomOrder == null || randomOrder.length != numPerScreen) {
			randomOrder = new int[numPerScreen];
			for (int i = 0; i < numPerScreen; i++)
				randomOrder[i] = i;

			Random r = new Random();
			for (int i = 0; i < 1000; i++) {
				int a = r.nextInt(numPerScreen);
				int b = r.nextInt(numPerScreen);
				int temp = randomOrder[a];
				randomOrder[a] = randomOrder[b];
				randomOrder[b] = temp;
			}
		}
		int indx = (width - width / size * size) / 2;
		int x = indx;
		int y = (height - size * ROWS) / 2;
		boolean odd = false;

		for (int i = 0; i < numPerScreen; i++) {
			long base = (getTimeMs() + (numPerScreen + randomOrder[i]) * MS_PER_LOGO / numPerScreen);
			long baseTime = base % MS_PER_LOGO;
			int org = (int) (i + base / MS_PER_LOGO * numPerScreen) % orgs.length;

			BufferedImage img = map.get(orgs[org].getId());
			if (img != null && x > -size && x < width) {
				float tr = 1f;
				if (baseTime < FADE)
					tr = (float) baseTime / FADE;
				else if (baseTime > MS_PER_LOGO - FADE)
					tr = 1f - (float) (baseTime - (MS_PER_LOGO - FADE)) / (float) FADE;
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setComposite(AlphaComposite.SrcOver.derive(tr));
				g2.drawImage(img, x + (size - img.getWidth()) / 2, y + (size - img.getHeight()) / 2, null);
				g2.dispose();
			}

			x += size;
			if (x + size > width) {
				if (!odd)
					x = indx + size / 2;
				else
					x = indx;
				odd = !odd;
				y += size;
			}
		}
	}
}