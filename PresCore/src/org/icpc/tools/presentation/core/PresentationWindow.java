package org.icpc.tools.presentation.core;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.icpc.tools.presentation.core.internal.PresentationWindowImpl;

/**
 * A window capable of displaying presentations.
 */
public abstract class PresentationWindow extends Frame implements IPresentationHandler {
	private static final long serialVersionUID = 1L;

	protected Presentation currentPresentation = null;

	protected PresentationWindow(String title) {
		super(title);

		addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (currentPresentation != null)
					currentPresentation.fireMouseEvent(e, Presentation.MOUSE_CLICKED);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				if (currentPresentation != null)
					currentPresentation.fireMouseEvent(e, Presentation.MOUSE_ENTERED);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (currentPresentation != null)
					currentPresentation.fireMouseEvent(e, Presentation.MOUSE_EXITED);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (currentPresentation != null)
					currentPresentation.fireMouseEvent(e, Presentation.MOUSE_PRESSED);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (currentPresentation != null)
					currentPresentation.fireMouseEvent(e, Presentation.MOUSE_RELEASED);
			}
		});
		addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (currentPresentation != null)
					currentPresentation.fireMouseMotionEvent(e, Presentation.MOUSE_MOTION_DRAGGED);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				if (currentPresentation != null)
					currentPresentation.fireMouseMotionEvent(e, Presentation.MOUSE_MOTION_MOVED);
			}
		});
		addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if ((KeyEvent.VK_Q == e.getKeyCode() || KeyEvent.VK_ESCAPE == e.getKeyCode())
						&& (e.isControlDown() || e.isShiftDown()))
					System.exit(0);

				if (currentPresentation != null)
					currentPresentation.fireKeyEvent(e, Presentation.KEY_PRESSED);
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (currentPresentation != null)
					currentPresentation.fireKeyEvent(e, Presentation.KEY_RELEASED);
			}

			@Override
			public void keyTyped(KeyEvent e) {
				if (currentPresentation != null)
					currentPresentation.fireKeyEvent(e, Presentation.KEY_TYPED);
			}
		});
	}

	public static PresentationWindow create(String title, Image iconImage) {
		Rectangle r = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		return new PresentationWindowImpl(title, new Rectangle(r.x, r.y, r.width / 2, r.width * 9 / 32), iconImage);
	}

	public static PresentationWindow open(String title, Image iconImage) {
		PresentationWindowImpl window = (PresentationWindowImpl) create(title, iconImage);
		window.openIt();
		return window;
	}

	public abstract void openIt();

	public static PresentationWindow open() {
		BufferedImage iconImage = null;
		try {
			iconImage = ImageIO.read(PresentationWindow.class.getClassLoader().getResource("images/presIcon.png"));
		} catch (IOException e) {
			// could not set icon
		}
		return open("Presentations", iconImage);
	}

	public static PresentationWindow create() {
		BufferedImage iconImage = null;
		try {
			iconImage = ImageIO.read(PresentationWindow.class.getClassLoader().getResource("images/presIcon.png"));
		} catch (IOException e) {
			// could not set icon
		}
		return create("Presentations", iconImage);
	}

	/**
	 * Show one presentation with no transitions.
	 *
	 * @param presentation a presentation, may not be null
	 */
	@Override
	public abstract void setPresentation(Presentation presentation);

	/**
	 * Show a set of presentations in the given order. The list of transitions may be null (no
	 * transitions), a single entry (always use the same transition), or the same length as the
	 * number of presentations (to specify a unique transition between each presentation).
	 *
	 * @param newPresentations a list of presentations, may not be null or have null entries
	 * @param newTransitions null, a single transition, or a list of transitions the same length as
	 *           the presentation list with no null entries
	 */
	@Override
	public abstract void setPresentations(long time, Presentation[] newPresentations, Transition[] newTransitions);

	@Override
	public abstract void setNanoTimeDelta(long time);

	@Override
	public abstract BufferedImage createImage(float scale);
}