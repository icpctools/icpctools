package org.icpc.tools.contest.util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.icpc.tools.contest.Trace;

public class LogoUtil extends Window {
	private static final long serialVersionUID = 0L;

	private Font font;
	private Cursor customCursor;
	private int scroll;
	private final BufferedImage[] logos;
	private int size;
	private int background;

	// private final int logosPerLine = -1;

	/**
	 * Point to a folder containing x.png files. Use '[]+-b' keys to view logos, and 'q' to exit.
	 */
	public static void main(String[] args) {
		Trace.init("ICPC Logo Util", "logoUtil", args);

		if (args == null || args.length != 1) {
			Trace.trace(Trace.ERROR, "Logo util must have a single argument: a directory containing X.png files");
			return;
		}

		int count = 0;
		BufferedImage[] logos = new BufferedImage[125];
		for (int i = 0; i < 125; i++) {
			logos[i] = loadImage(args[0], i + 1);
			if (logos[i] != null)
				count++;
		}

		Trace.trace(Trace.USER, count + " logos loaded");

		LogoUtil.open(logos);
	}

	protected static BufferedImage loadImage(String dir, int id) {
		InputStream in = null;
		try {
			File f = new File(dir, id + ".png");
			if (!f.exists())
				return null;

			in = new FileInputStream(f);
			return ImageIO.read(in);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "No logo found for team " + id, e);
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	protected LogoUtil(Frame owner, BufferedImage[] logos) {
		super(owner);

		this.logos = logos;

		owner.setVisible(true); // to enable key events

		setBackground(Color.BLACK);
		setVisible(true);
		toFront();

		setFullScreen(0);

		createBufferStrategy(2);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Trace.trace(Trace.USER, "Program terminated by mouseClicked() handler in class LogoUtil");
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
		if (e.getKeyChar() == '[') {
			scroll--;
			if (scroll < 0)
				scroll = 0;
			paintIt();
		}
		if (e.getKeyChar() == ']') {
			scroll++;
			paintIt();
		}

		if (e.getKeyChar() == 'b') {
			background++;
			background %= 3;
			paintIt();
		}

		if (e.getKeyChar() == '-' || e.getKeyChar() == '_') {
			size++;
			if (size > 4)
				size = 4;
			scroll = 0;
			paintIt();
		}

		if (e.getKeyChar() == '+' || e.getKeyChar() == '=') {
			size--;
			if (size < 0)
				size = 0;
			scroll = 0;
			paintIt();
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

	public static LogoUtil open(BufferedImage[] logos) {
		Frame frame = new Frame("Logo Utility");
		frame.setUndecorated(true);
		try {
			frame.setIconImage(ImageIO.read(LogoUtil.class.getClassLoader().getResource("images/contestUtilsIcon.png")));
		} catch (IOException e) {
			// could not set icon
		}
		return new LogoUtil(frame, logos);
	}

	public void paintIt() {
		BufferStrategy bs = getBufferStrategy();
		if (bs == null)
			return;

		Graphics g = bs.getDrawGraphics();
		paint(g);
		g.dispose();
		if (!bs.contentsRestored())
			bs.show();
		else
			repaint();
	}

	@Override
	public void paint(Graphics gg) {
		Graphics2D g = (Graphics2D) gg;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		final int GAP = 5;
		Rectangle b = getBounds();

		if (background == 0)
			g.setColor(Color.BLACK);
		else if (background == 1)
			g.setColor(Color.GRAY);
		else if (background == 2)
			g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());

		if (logos == null || logos.length == 0)
			return;

		if (font == null)
			font = g.getFont().deriveFont(Font.PLAIN, 15f);
		g.setFont(font);

		FontMetrics fm = g.getFontMetrics();
		g.setColor(Color.WHITE);
		if (background == 2)
			g.setColor(Color.BLACK);

		int logoSize = 600;
		if (size == 1)
			logoSize = 300;
		else if (size == 2)
			logoSize = 150;
		else if (size == 3)
			logoSize = 60;
		else if (size == 4)
			logoSize = 30;

		int rowHeight = (int) (logoSize + GAP * 1.5f + fm.getHeight());
		int x = GAP;
		int y = GAP - scroll * rowHeight;

		for (int i = 0; i < logos.length; i++) {
			if (y > -logoSize - fm.getHeight() - GAP && y < b.height) {
				if (logos[i] != null) {
					int w = logos[i].getWidth();
					int h = logos[i].getHeight();
					if (size == 1) {
						w /= 2;
						h /= 2;
					} else if (size == 2) {
						w /= 4;
						h /= 4;
					} else if (size == 3) {
						w /= 10;
						h /= 10;
					} else if (size == 4) {
						w /= 20;
						h /= 20;
					}
					g.drawImage(logos[i], x + (logoSize - w) / 2, y + (logoSize - h) / 2, w, h, null);
				}

				String s = (i + 1) + "";
				g.drawString(s, x + (logoSize - fm.stringWidth(s)) / 2, y + logoSize + GAP / 2 + fm.getAscent());
			}
			x += logoSize + GAP;
			if (x > b.width - logoSize) {
				x = GAP;
				y += rowHeight;
			}
		}
	}
}