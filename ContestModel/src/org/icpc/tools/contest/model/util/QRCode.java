package org.icpc.tools.contest.model.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;

import io.nayuki.qrcodegen.QrCode;

/**
 * Some handy QR codes from the readers we used in Baku: Battery life: ^&037&^, Reset to default:
 * ^&002&^, Volume off: ^&03A&^, Vibrate on: ^&039&^.
 */
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

	/*
	 * Test QR codes by creating the first command line args as a QR code and writing it to the second arg as a filament.
	 */
	public static void main(String[] args) throws IOException {
		if (args == null || args.length != 2) {
			Trace.trace(Trace.ERROR,
					"Arguments should be [QRcode] [filename], e.g. '0 zero' which will create zero.png with a QR code containing the number 0");
			return;
		}
		int size = 1000;

		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, size, size);
		g.setColor(Color.BLACK);
		QRCode.drawQRCode(g, args[0], 0, 0, size);
		g.dispose();
		File f = new File(args[1] + ".png");
		ImageIO.write(image, "png", f);
	}
}