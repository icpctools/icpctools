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

public class CompositePresentationSaveDialog extends Dialog {
	protected Shell shell;
	protected Button save;

	protected boolean saveOk;
	protected String name;
	protected String description;
	protected String category;

	public CompositePresentationSaveDialog(Shell parent) {
		super(parent);
	}

	public CompositePresentationSaveDialog(Shell parent, CompositePresentationInfo info) {
		super(parent);
		name = info.getName();
		description = info.getDescription();
		category = info.getCategory();
	}

	public boolean open() {
		shell = new Shell(getParent());
		shell.setImage(getParent().getImage());
		shell.setText("Save Presentation");
		createUI(shell);

		shell.pack();
		shell.open();

		Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		return saveOk;
	}

	protected void createUI(Composite comp) {
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 5;
		comp.setLayout(layout);

		Label label = new Label(comp, SWT.NONE);
		label.setText("Name:");
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final Text text = new Text(comp, SWT.BORDER);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 175;
		text.setLayoutData(data);
		if (name != null)
			text.setText(name);
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				name = text.getText();
				setOkEnablement();
			}
		});

		label = new Label(comp, SWT.NONE);
		label.setText("Description:");
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final Text text2 = new Text(comp, SWT.BORDER);
		text2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (description != null)
			text2.setText(description);
		text2.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				description = text2.getText();
				setOkEnablement();
			}
		});

		label = new Label(comp, SWT.NONE);
		label.setText("Category:");
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final Text text3 = new Text(comp, SWT.BORDER);
		text3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (category != null)
			text3.setText(category);
		text3.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				category = text3.getText();
				setOkEnablement();
			}
		});

		Composite buttonComp = new Composite(comp, SWT.NONE);
		data = new GridData(GridData.END, GridData.CENTER, false, false);
		data.horizontalSpan = 2;
		buttonComp.setLayoutData(data);

		layout = new GridLayout(2, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 0;
		buttonComp.setLayout(layout);

		save = new Button(buttonComp, SWT.PUSH);
		save.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		save.setText("&Save");
		save.setEnabled(false);
		save.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				saveOk = true;
				shell.close();
			}
		});

		Button cancel = new Button(buttonComp, SWT.PUSH);
		cancel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		cancel.setText("&Cancel");

		cancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				shell.close();
			}
		});
	}

	protected void setOkEnablement() {
		boolean ok = true;
		if (name == null || name.trim().isEmpty())
			ok = false;
		else {
			for (char c : name.toCharArray()) {
				if (!Character.isLetterOrDigit(c) && c != ' ')
					ok = false;
			}
		}
		if (category == null || category.trim().isEmpty())
			ok = false;
		else {
			for (char c : category.toCharArray()) {
				if (!Character.isLetterOrDigit(c) && c != ' ')
					ok = false;
			}
		}
		save.setEnabled(ok);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getCategory() {
		return category;
	}
}