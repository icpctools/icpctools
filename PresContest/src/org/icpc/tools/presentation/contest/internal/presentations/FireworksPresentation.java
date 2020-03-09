package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.MemoryImageSource;
import java.util.Random;

import org.icpc.tools.presentation.core.Presentation;

public class FireworksPresentation extends Presentation {
	private static final int ru = 90;
	private static final int rv = 50;
	private static final Random rand = new Random();
	private static final int bits = 10000;

	private static final long DELAY = 1000;

	private long nextTime;

	private int m_nAppX;
	private int m_nAppY;

	private int pix0[];
	private MemoryImageSource offImage;
	private Image dbImg;
	private int pixls;
	private int pixls2;
	private final double bit_px[];
	private final double bit_py[];
	private final double bit_vx[];
	private final double bit_vy[];
	private final int bit_sx[];
	private final int bit_sy[];
	private final int bit_l[];
	private final int bit_f[];
	private final int bit_p[];
	private final int bit_c[];
	private static final int bit_max = 300;

	public FireworksPresentation() {
		// isInitialized = false;
		bit_px = new double[bits];
		bit_py = new double[bits];
		bit_vx = new double[bits];
		bit_vy = new double[bits];
		bit_sx = new int[bits];
		bit_sy = new int[bits];
		bit_l = new int[bits];
		bit_f = new int[bits];
		bit_p = new int[bits];
		bit_c = new int[bits];
	}

	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		m_nAppX = width;
		m_nAppY = height;
		pixls = m_nAppX * m_nAppY;
		pixls2 = pixls - m_nAppX * 2;
		pix0 = new int[pixls];
		offImage = new MemoryImageSource(m_nAppX, m_nAppY, pix0, 0, m_nAppX);
		offImage.setAnimated(true);
		Toolkit tk = Toolkit.getDefaultToolkit();
		dbImg = tk.createImage(offImage);
		for (int i = 0; i < pixls; i++)
			// pix0[i] = 0xff000000;
			pix0[i] = 0x00000000;

		for (int j = 0; j < bits; j++)
			bit_f[j] = 0;

		// isInitialized = true;
	}

	protected void runImpl() {
		for (int j = 0; j < pixls2; j++) {
			int k = pix0[j];
			int l = pix0[j + 1];
			int i1 = pix0[j + m_nAppX];
			int j1 = pix0[j + m_nAppX + 1];

			int i = (k & 0xff0000) >> 16;
			int k1 = ((((l & 0xff0000) >> 16) - i) * ru >> 8) + i;
			i = (k & 0xff00) >> 8;
			int l1 = ((((l & 0xff00) >> 8) - i) * ru >> 8) + i;
			i = k & 0xff;
			int i2 = (((l & 0xff) - i) * ru >> 8) + i;
			i = (i1 & 0xff0000) >> 16;
			int j2 = ((((j1 & 0xff0000) >> 16) - i) * ru >> 8) + i;
			i = (i1 & 0xff00) >> 8;
			int k2 = ((((j1 & 0xff00) >> 8) - i) * ru >> 8) + i;
			i = i1 & 0xff;
			int l2 = (((j1 & 0xff) - i) * ru >> 8) + i;
			int r = ((j2 - k1) * rv >> 8) + k1;
			int g = ((k2 - l1) * rv >> 8) + l1;
			int b = ((l2 - i2) * rv >> 8) + i2;

			// int a = (k & 0xff000000) >> 24 + (l & 0xff000000) >> 24 +
			// (i1 & 0xff000000) >> 24 + (j1 & 0xff000000) >> 24;
			// a /= 4;
			/*int a = (k & 0xff000000) >> 24;
			if (r+g+b > 250)
				a = 255;
			else
				if (a > 5)
					a -= 5;*/
			// int a = Math.min(255, (int) ((r+g+b) / 1.5f));
			int a = Math.min(255, (int) ((r + g + b) / 3.5f));

			pix0[j] = a << 24 | r << 16 | g << 8 | b;
			// pix0[j] = r << 16 | g << 8 | b | 0xff000000;

			// if ((pix0[j] & 0xffffff) == 0)
			// pix0[j] = 0x00000000;
		}

		rend();
		offImage.newPixels(0, 0, m_nAppX, m_nAppY);
	}

	private void rend() {
		for (int k = 0; k < bits; k++)
			switch (bit_f[k]) {
				default:
					break;

				case 1: // '\001'
					bit_vy[k] += rand.nextDouble() / 50D;
					bit_px[k] += bit_vx[k];
					bit_py[k] += bit_vy[k];
					bit_l[k]--;
					if (bit_l[k] == 0 || bit_px[k] < 0.0D || bit_py[k] < 0.0D || bit_px[k] > m_nAppX
							|| bit_py[k] > (m_nAppY - 3)) {
						bit_c[k] = 0xff000000;
						bit_f[k] = 0;
					} else if (bit_p[k] == 0) {
						if ((int) (rand.nextDouble() * 2D) == 0)
							bit_set((int) bit_px[k], (int) bit_py[k], -1);
					} else {
						bit_set((int) bit_px[k], (int) bit_py[k], bit_c[k]);
					}
					break;

				case 2: // '\002'
					// bit_sy[k] -= 5;
					bit_sy[k] -= 10;
					if (bit_sy[k] <= bit_py[k]) {
						bit_f[k] = 1;
					}
					if ((int) (rand.nextDouble() * 20D) == 0) {
						int i = (int) (rand.nextDouble() * 2D);
						// int j = (int) (rand.nextDouble() * 5D);
						int j = (int) (rand.nextDouble() * 10D);
						bit_set(bit_sx[k] + i, bit_sy[k] + j, -1);
					}
					break;
			}
	}

	private void bit_set(int i, int j, int k) {
		pix0[i + j * m_nAppX] = k;
	}

	@Override
	public void incrementTimeMs(final long dt) {
		super.incrementTimeMs(dt);

		long time = getTimeMs();
		if (time >= nextTime) {
			nextTime = time + DELAY;
			int m_mouseX = rand.nextInt(getSize().width - 100) + 50;
			int m_mouseY = rand.nextInt(getSize().height - 200) + 50;

			int r = (int) (rand.nextDouble() * 256D);
			int g = (int) (rand.nextDouble() * 256D);
			int b = (int) (rand.nextDouble() * 256D);
			int j1 = r << 16 | g << 8 | b | 0xff000000;
			int k1 = 0;
			for (int l1 = 0; l1 < bits; l1++) {
				if (bit_f[l1] != 0)
					continue;
				bit_px[l1] = m_mouseX;
				bit_py[l1] = m_mouseY;
				double d = rand.nextDouble() * 6.2800000000000002D;
				double d1 = rand.nextDouble();
				bit_vx[l1] = Math.sin(d) * d1;
				bit_vy[l1] = Math.cos(d) * d1;
				bit_l[l1] = (int) (rand.nextDouble() * 100D) + 100;
				bit_p[l1] = (int) (rand.nextDouble() * 3D);
				bit_c[l1] = j1;
				bit_sx[l1] = m_mouseX;
				bit_sy[l1] = m_nAppY - 5;
				bit_f[l1] = 2;
				if (++k1 == bit_max)
					break;
			}
		}
	}

	@Override
	public void aboutToShow() {
		nextTime = 0;
	}

	@Override
	public void paint(Graphics2D g) {
		runImpl();

		g.drawImage(dbImg, 0, 0, null);
	}
}