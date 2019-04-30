package org.icpc.tools.presentation.admin.internal;

import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DisplayAreaDialog extends Dialog {
	protected Shell shell;
	protected String rect;

	public DisplayAreaDialog(Shell parent, String rect) {
		super(parent);
		this.rect = rect;
	}

	public boolean open() {
		shell = new Shell(getParent());
		shell.setImage(getParent().getImage());
		shell.setText("Custom Display Area");
		createUI(shell);

		shell.pack();
		shell.open();

		Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		return (rect != null);
	}

	protected void createUI(Composite comp) {
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 3;
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		comp.setLayout(layout);

		Label label = new Label(comp, SWT.NONE);
		label.setText("Display area (x,y,w,h):");
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		label = new Label(comp, SWT.NONE);

		final Text text = new Text(comp, SWT.BORDER);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 125;
		text.setLayoutData(data);
		text.setText(rect);

		final Button set = new Button(comp, SWT.PUSH);
		set.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		set.setText("Set");
		set.setEnabled(true);

		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				set.setEnabled(validateRect(text.getText()));
			}
		});

		set.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				rect = text.getText();
				shell.close();
			}
		});
	}

	private static boolean validateRect(String value) {
		if (value == null || value.isEmpty())
			return true;

		try {
			StringTokenizer st = new StringTokenizer(value, ",");
			Integer.parseInt(st.nextToken().trim());
			Integer.parseInt(st.nextToken().trim());
			Integer.parseInt(st.nextToken().trim());
			Integer.parseInt(st.nextToken().trim());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public String getRect() {
		return rect;
	}
}