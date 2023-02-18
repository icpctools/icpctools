package org.icpc.tools.presentation.admin.internal;

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

public class DisplayDialog extends Dialog {
	protected Shell shell;
	protected String value;

	public DisplayDialog(Shell parent, String value) {
		super(parent);
		this.value = value;
	}

	public boolean open() {
		shell = new Shell(getParent());
		shell.setImage(getParent().getImage());
		shell.setText("Custom Display String");
		createUI(shell);

		shell.pack();
		shell.open();

		Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		return (value != null);
	}

	protected void createUI(Composite comp) {
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 3;
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		comp.setLayout(layout);

		Label label = new Label(comp, SWT.NONE);
		label.setText("Display:");
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final Text text = new Text(comp, SWT.BORDER);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 125;
		text.setLayoutData(data);
		text.setText(value);

		final Button set = new Button(comp, SWT.PUSH);
		set.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		set.setText("Set");
		set.setEnabled(true);

		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				set.setEnabled(validate(text.getText()));
			}
		});

		set.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				value = text.getText();
				shell.close();
			}
		});
	}

	private static boolean validate(String value) {
		if (value == null || value.isEmpty() || value.length() > 2)
			return false;

		try {
			Integer.parseInt(value.substring(0, 1));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public String getValue() {
		return value;
	}
}