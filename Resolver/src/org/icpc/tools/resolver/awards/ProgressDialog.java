package org.icpc.tools.resolver.awards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

public class ProgressDialog extends Dialog {
	protected String text;

	public ProgressDialog(Shell parent, String text) {
		super(parent);
		this.text = text;
	}

	public Shell open() {
		Shell parent = getParent();
		Shell s = new Shell(parent, SWT.DIALOG_TRIM);
		s.setText(parent.getText());
		s.setImage(parent.getImage());
		createUI(s);

		return SWTUtil.centerAndOpenDialog(s, false);
	}

	protected void createUI(Composite comp) {
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		comp.setLayout(layout);

		Label label = new Label(comp, SWT.NONE);
		label.setText(text);
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		ProgressBar progress = new ProgressBar(comp, SWT.HORIZONTAL | SWT.INDETERMINATE);
		progress.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		// buttons
		Composite buttonBar = new Composite(comp, SWT.NONE);
		GridData data = new GridData(SWT.RIGHT, SWT.BOTTOM, false, true);
		data.verticalIndent = 20;
		buttonBar.setLayoutData(data);

		layout = new GridLayout(2, true);
		layout.marginWidth = 0;
		buttonBar.setLayout(layout);

		final Button background = new Button(buttonBar, SWT.PUSH);
		background.setText("Run in &background");
		data = new GridData(SWT.RIGHT, SWT.BOTTOM, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(background);
		background.setLayoutData(data);
		background.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					background.getShell().dispose();
				} catch (Exception e) {
					ErrorHandler.error("Error closing dialog", e);
				}
			}
		});

		background.setFocus();
		comp.getShell().setDefaultButton(background);
	}
}