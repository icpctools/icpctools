package org.icpc.tools.contest.model.util;

import java.awt.Graphics2D;

import io.nayuki.qrcodegen.QrCode;

public class QRCode {
	/**
	 * Draws a QR code representing the given text in a square at x, y, with height & width s.
	 */
	public static void drawQRCode(Graphics2D g, String text, int x, int y, int s) {
		QrCode qrCode = QrCode.encodeText(text, QrCode.Ecc.HIGH);
		int size = qrCode.size;
		int ss = (int) (s / (double) size);
		int p = (s - ss * size) / 2;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				if (qrCode.getModule(i, j)) {
					g.fillRect(x + p + +ss * i, y + p + ss * j, ss, ss);
				}
			}
		}
	}
}