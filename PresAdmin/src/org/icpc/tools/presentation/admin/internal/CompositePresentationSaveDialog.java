package org.icpc.tools.presentation.admin.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class CompositePresentationSaveDialog {
	protected Shell shell;
	protected Button save;

	protected boolean saveOk;
	protected String name;
	protected String description;
	protected String category;
	protected boolean presentationsOk;
	protected Label statusLabel;

	public CompositePresentationSaveDialog(Shell parent) {
		this.shell = parent;
	}
	/*public CompositePresentationSaveDialog(Shell parent) {
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
	}*/

	protected void createUI(Composite comp) {
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 5;
		comp.setLayout(layout);
		
		// The save button will be created in createButtons

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
	}

	protected void createButtons(Composite comp) {
		Composite buttonComp = new Composite(comp, SWT.NONE);
		GridData data = new GridData(GridData.FILL, GridData.CENTER, true, false);
		data.horizontalSpan = 2;
		buttonComp.setLayoutData(data);

		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 0;
		buttonComp.setLayout(layout);

		statusLabel = new Label(buttonComp, SWT.PUSH);
		statusLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		statusLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

		save = new Button(buttonComp, SWT.PUSH);
		save.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
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
		cancel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
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
		String message = "";
		List<String> missing = new ArrayList<>();
		
		// Check for presentation
		if (!presentationsOk) {
			missing.add("presentation");
			ok = false;
		}
		
		// Check name
		if (name == null || name.trim().isEmpty()) {
			missing.add("name");
			ok = false;
		} else {
			for (char c : name.toCharArray()) {
				if (!Character.isLetterOrDigit(c) && c != ' ') {
					message = "Name can only contain letters, numbers and spaces";
					ok = false;
					break;
				}
			}
		}
		
		// Check category
		if (category == null || category.trim().isEmpty()) {
			missing.add("category");
			ok = false;
		} else {
			for (char c : category.toCharArray()) {
				if (!Character.isLetterOrDigit(c) && c != ' ') {
					if (message.isEmpty()) {
						message = "Category can only contain letters, numbers and spaces";
					}
					ok = false;
					break;
				}
			}
		}
		
		// Set status message
		if (ok) {
			setStatusMessage("");
		} else if (!message.isEmpty()) {
			setStatusMessage(message);
		} else if (!missing.isEmpty()) {
			setStatusMessage("Add " + String.join(", ", missing));
		}
		
		save.setEnabled(ok && presentationsOk);
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
	
	public boolean isSaveOk() {
		return saveOk;
	}
	
	public void setPresentationsOk(boolean ok) {
		presentationsOk = ok;
		setOkEnablement();
	}

	public void setStatusMessage(String message) {
		if (statusLabel != null && !statusLabel.isDisposed()) {
			statusLabel.setText(message);
			statusLabel.getParent().layout();
		}
	}
}