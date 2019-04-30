package org.icpc.tools.contest.util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;

public class ScreenMaskUtil extends Window {
	private static final long serialVersionUID = 0L;

	private Font font;
	private Cursor customCursor;
	private Rectangle rect;

	/**
	 * Use 'wdxa' and 'o;.k' keys to move the rectangle, q to exit
	 */
	public static void main(String[] args) {
		Trace.init("ICPC Screen Masking Util", "maskUtil", args);

		ScreenMaskUtil.open();
	}

	protected ScreenMaskUtil(Frame owner) {
		super(owner);

		owner.setVisible(true); // to enable key events

		setBackground(Color.BLACK);
		setVisible(true);
		toFront();

		setFullScreen(0);

		createBufferStrategy(2);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Trace.trace(Trace.USER, "Program terminated by mouseClicked() handler in class ScreenMaskUtil");
				System.exit(0);
			}
		});
		owner.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				//
			}

			@Override
			public void keyReleased(KeyEvent e) {
				//
			}

			@Override
			public void keyTyped(KeyEvent e) {
				react(e);
			}
		});
	}

	protected void react(KeyEvent e) {
		if (e.getKeyChar() == 'w') {
			rect.y--;
			rect.height++;
			repaint();
		}
		if (e.getKeyChar() == 'd') {
			rect.x++;
			rect.width--;
			repaint();
		}
		if (e.getKeyChar() == 'a') {
			rect.x--;
			rect.width++;
			repaint();
		}
		if (e.getKeyChar() == 'x') {
			rect.y++;
			rect.height--;
			repaint();
		}

		if (e.getKeyChar() == 'o') {
			rect.height--;
			repaint();
		}
		if (e.getKeyChar() == ';') {
			rect.width++;
			repaint();
		}
		if (e.getKeyChar() == '.') {
			rect.height++;
			repaint();
		}
		if (e.getKeyChar() == 'k') {
			rect.width--;
			repaint();
		}

		if (e.getKeyChar() == 'q')
			System.exit(0);
	}

	private Cursor getCustomCursor() {
		if (customCursor == null) {
			Toolkit t = Toolkit.getDefaultToolkit();
			Dimension d = t.getBestCursorSize(1, 1);
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice gd = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gd.getDefaultConfiguration();
			BufferedImage image = gc.createCompatibleImage(d.width, d.height, Transparency.TRANSLUCENT);
			// im = alphaMultiply(im, 0.0);*/
			customCursor = t.createCustomCursor(image, new Point(0, 0), "no cursor");
		}
		return customCursor;
	}

	protected void setFullScreen(int screen) {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gds = ge.getScreenDevices();

		// find the first graphics device and its current mode
		GraphicsDevice device = null;
		int count = 0;
		for (GraphicsDevice gd : gds) {
			if (gd.getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
				if (screen == count)
					device = gd;

				count++;
			}
		}

		if (device == null)
			return;

		device.setFullScreenWindow(this);
		// TODO set display mode?
		// -Dsun.java2d.noddraw=true

		setCursor(getCustomCursor());
	}

	public static ScreenMaskUtil open() {
		Frame frame = new Frame("Screen Masking Utility");
		frame.setUndecorated(true);
		try {
			frame.setIconImage(
					ImageIO.read(ScreenMaskUtil.class.getClassLoader().getResource("images/contestUtilsIcon.png")));
		} catch (IOException e) {
			// could not set icon
		}
		return new ScreenMaskUtil(frame);
	}

	@Override
	public void paint(Graphics g) {
		final int GAP = 5;
		Rectangle b = getBounds();
		if (rect == null)
			rect = b;

		// g.setColor(Color.BLACK);
		// g.fillRect(0, 0, b.width, b.height);

		/*g.setColor(Color.DARK_GRAY);
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
			}
		}*/

		g.setColor(Color.WHITE);
		g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);

		if (font == null)
			font = g.getFont().deriveFont(Font.PLAIN, 30f);
		g.setFont(font);

		FontMetrics fm = g.getFontMetrics();
		String s = rect.x + " , " + rect.y;
		g.drawString(s, rect.x + GAP, rect.y + fm.getAscent() + GAP);

		s = rect.width + " x " + rect.height;
		g.drawString(s, rect.x + (rect.width - fm.stringWidth(s)) / 2, rect.y + rect.height / 2);

		s = (rect.x + rect.width) + ", " + (rect.y + rect.height);
		g.drawString(s, rect.x + rect.width - fm.stringWidth(s) - GAP, rect.y + rect.height - GAP);
	}
}