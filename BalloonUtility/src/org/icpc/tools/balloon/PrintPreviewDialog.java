package org.icpc.tools.balloon;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class PrintPreviewDialog extends Dialog {
	protected Image img;

	public PrintPreviewDialog(Shell parent, Image img) {
		super(parent);
		this.img = img;
	}

	public int open() {
		Shell s = new Shell(getParent());
		s.setImage(getParent().getImage());
		s.setText("Balloon Print Preview");
		createUI(s);

		s.pack();
		s.open();

		Display display = s.getDisplay();
		while (!s.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		return 0;
	}

	protected void createUI(Composite comp) {
		comp.setLayout(new GridLayout(1, false));

		Label label = new Label(comp, SWT.NONE);
		label.setImage(img);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		Rectangle r = img.getBounds();
		data.widthHint = r.width;
		data.heightHint = r.height;
		label.setLayoutData(data);
	}
}
