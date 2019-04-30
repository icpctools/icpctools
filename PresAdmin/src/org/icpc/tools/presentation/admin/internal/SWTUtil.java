package org.icpc.tools.presentation.admin.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

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
		GridData data = new GridData(SWT.CENTER, SWT.BEGINNING, false, false);
		data.widthHint = getButtonWidthHint(b);
		b.setLayoutData(data);
		return b;
	}
}