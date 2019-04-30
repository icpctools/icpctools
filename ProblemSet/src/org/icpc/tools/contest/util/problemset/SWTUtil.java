package org.icpc.tools.contest.util.problemset;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * SWT utility class
 */
public class SWTUtil {
	/**
	 * Returns a width hint for a button control.
	 */
	public static int getButtonWidthHint(Button button) {
		return Math.max(80, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
	}

	/**
	 * Create a new button with the standard size.
	 *
	 * @param comp the component to add the button to
	 * @param label the button label
	 * @return a button
	 */
	public static Button createButton(Composite comp, String label) {
		Button b = new Button(comp, SWT.PUSH);
		b.setText(label);
		GridData data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		data.widthHint = getButtonWidthHint(b);
		b.setLayoutData(data);
		return b;
	}

	protected static Shell centerAndOpenDialog(Shell s, boolean wait) {

		s.pack();

		Rectangle r = s.getParent().getBounds();
		Point p = s.getSize();
		s.setLocation(new Point(r.x + (r.width - p.x) / 2, r.y + (r.height - p.y) / 2));

		s.open();

		if (wait) {
			Display display = s.getDisplay();
			while (!s.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		}

		return s;
	}
}