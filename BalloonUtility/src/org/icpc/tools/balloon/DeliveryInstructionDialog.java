package org.icpc.tools.balloon;

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

public class DeliveryInstructionDialog extends Dialog {
	protected String[] messages;

	protected int rc;

	public DeliveryInstructionDialog(Shell parent, String[] messages) {
		super(parent);

		this.messages = messages;
	}

	public int open() {
		Shell parent = getParent();
		Shell s = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		s.setText("Delivery Instructions");
		s.setImage(parent.getImage());
		createUI(s);

		rc = 1;
		s.pack();
		s.open();

		Display display = s.getDisplay();
		while (!s.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		return rc;
	}

	public String[] getMessages() {
		return messages;
	}

	protected void createUI(Composite comp) {
		GridLayout layout = new GridLayout(3, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		comp.setLayout(layout);

		Label label = new Label(comp, SWT.NONE);
		label.setText("Configure delivery instructions for notable balloons.");
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalSpan = 3;
		label.setLayoutData(data);

		// contest
		label = new Label(comp, SWT.NONE);
		label.setText("First solution in &contest:");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.verticalIndent = 15;
		data.horizontalSpan = 3;
		label.setLayoutData(data);

		final Text contestText = new Text(comp, SWT.BORDER | SWT.MULTI);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 350;
		data.horizontalIndent = 20;
		data.horizontalSpan = 3;
		contestText.setLayoutData(data);
		contestText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				messages[0] = contestText.getText();
			}
		});
		contestText.setText(messages[0]);

		// group
		label = new Label(comp, SWT.NONE);
		label.setText("First solution for &group:");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalSpan = 3;
		label.setLayoutData(data);

		final Text groupText = new Text(comp, SWT.BORDER | SWT.MULTI);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.horizontalIndent = 20;
		data.horizontalSpan = 3;
		groupText.setLayoutData(data);
		groupText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				messages[1] = groupText.getText();
			}
		});
		groupText.setText(messages[1]);

		// problem
		label = new Label(comp, SWT.NONE);
		label.setText("First solution for &problem:");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalSpan = 3;
		label.setLayoutData(data);

		final Text problemText = new Text(comp, SWT.BORDER | SWT.MULTI);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.horizontalIndent = 20;
		data.horizontalSpan = 3;
		problemText.setLayoutData(data);
		problemText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				messages[2] = problemText.getText();
			}
		});
		problemText.setText(messages[2]);

		// team
		label = new Label(comp, SWT.NONE);
		label.setText("First solution for &team:");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalSpan = 3;
		label.setLayoutData(data);

		final Text teamText = new Text(comp, SWT.BORDER | SWT.MULTI);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.horizontalIndent = 20;
		data.horizontalSpan = 3;
		teamText.setLayoutData(data);
		teamText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				messages[3] = teamText.getText();
			}
		});
		teamText.setText(messages[3]);

		// messages
		label = new Label(comp, SWT.NONE);
		label.setText("Substitution values:");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalSpan = 3;
		data.verticalIndent = 15;
		label.setLayoutData(data);

		label = new Label(comp, SWT.NONE);
		label.setText("{0} - group\n{1} - problem label\n{2} - balloon color");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalIndent = 20;
		data.horizontalSpan = 3;
		label.setLayoutData(data);

		// buttons
		Composite buttonBar = new Composite(comp, SWT.NONE);
		data = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		data.horizontalSpan = 3;
		data.verticalIndent = 20;
		buttonBar.setLayoutData(data);

		layout = new GridLayout(3, true);
		layout.marginWidth = 0;
		buttonBar.setLayout(layout);

		final Button reset = new Button(buttonBar, SWT.PUSH);
		reset.setText("&Reset");
		data = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(reset);
		reset.setLayoutData(data);
		reset.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				messages = BalloonPrinter.DEFAULT_MESSAGES.clone();
				contestText.setText(messages[0]);
				groupText.setText(messages[1]);
				problemText.setText(messages[2]);
				teamText.setText(messages[3]);
			}
		});

		final Button save = new Button(buttonBar, SWT.PUSH);
		save.setText("&Save");
		data = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(save);
		save.setLayoutData(data);
		save.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					rc = 0;
					save.getShell().dispose();
				} catch (Exception e) {
					ErrorHandler.error("Error closing delivery instruction dialog", e);
				}
			}
		});

		final Button cancel = new Button(buttonBar, SWT.PUSH);
		cancel.setText("Ca&ncel");
		data = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(cancel);
		cancel.setLayoutData(data);
		cancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					rc = 2;
					cancel.getShell().dispose();
				} catch (Exception e) {
					ErrorHandler.error("Error closing delivery instruction dialog", e);
				}
			}
		});

		contestText.setFocus();
		comp.getShell().setDefaultButton(save);
	}
}