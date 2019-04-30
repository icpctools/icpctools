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
import org.eclipse.swt.widgets.Shell;
import org.icpc.tools.contest.model.IAward.AwardType;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.AwardUtil;

public class ExistingAwardDialog extends Dialog {
	protected Contest contest;
	protected AwardType[] types;

	protected int rc;

	public ExistingAwardDialog(Shell parent, Contest contest, AwardType[] types) {
		super(parent);
		this.contest = contest;
		this.types = types;
	}

	public int open() {
		Shell parent = getParent();
		Shell s = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		s.setText("Existing Awards");
		s.setImage(parent.getImage());
		createUI(s);

		rc = 1;
		SWTUtil.centerAndOpenDialog(s, true);

		return rc;
	}

	protected void createUI(Composite comp) {
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		comp.setLayout(layout);

		Label label = new Label(comp, SWT.NONE);
		label.setText("This contest already has " + AwardUtil.getAwardTypeNames(types) + " awards assigned.\n"
				+ "If you proceed the existing awards will be replaced.");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		// buttons
		Composite buttonBar = new Composite(comp, SWT.NONE);
		GridData data = new GridData(SWT.RIGHT, SWT.BOTTOM, false, true);
		data.verticalIndent = 20;
		buttonBar.setLayoutData(data);

		layout = new GridLayout(2, true);
		layout.marginWidth = 0;
		buttonBar.setLayout(layout);

		final Button apply = new Button(buttonBar, SWT.PUSH);
		apply.setText("O&k");
		data = new GridData(SWT.RIGHT, SWT.BOTTOM, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(apply);
		apply.setLayoutData(data);
		apply.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					rc = 0;
					apply.getShell().dispose();
				} catch (Exception e) {
					ErrorHandler.error("Error closing dialog", e);
				}
			}
		});

		final Button cancel = new Button(buttonBar, SWT.PUSH);
		cancel.setText("Ca&ncel");
		data = new GridData(SWT.RIGHT, SWT.BOTTOM, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(cancel);
		cancel.setLayoutData(data);
		cancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					rc = 2;
					cancel.getShell().dispose();
				} catch (Exception e) {
					ErrorHandler.error("Error closing dialog", e);
				}
			}
		});

		apply.setFocus();
		comp.getShell().setDefaultButton(apply);
	}
}