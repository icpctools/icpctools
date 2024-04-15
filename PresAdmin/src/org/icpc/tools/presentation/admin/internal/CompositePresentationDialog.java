package org.icpc.tools.presentation.admin.internal;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.icpc.tools.presentation.admin.internal.PresentationInfoListControl.DisplayStyle;
import org.icpc.tools.presentation.core.internal.PresentationInfo;

public class CompositePresentationDialog extends Dialog {
	private List<PresentationInfo> presentations = new ArrayList<>();
	private List<PresentationInfo> transitions = new ArrayList<>();

	private PresentationInfoListControl presentationList;
	private PresentationInfoListControl transitionList;
	private PresentationInfoListControl buildList;

	protected String name, description, category;
	protected CompositePresentationSaveDialog saveDialog;

	public CompositePresentationDialog(Shell parent, List<PresentationInfo> presentations,
			List<PresentationInfo> transitions) {
		super(parent);

		this.presentations = presentations;
		this.transitions = transitions;
	}

	public CompositePresentationDialog(Shell parent, List<PresentationInfo> presentations,
			List<PresentationInfo> transitions, CompositePresentationInfo info) {
		this(parent, presentations, transitions);
		// TODO - for edit

	}

	public CompositePresentationInfo getPresentation() {
		boolean first = true;
		StringBuilder sb = new StringBuilder();

		for (PresentationInfo info2 : buildList.getPresentationInfos()) {
			if (first)
				first = false;
			else
				sb.append("|");

			if (info2.getClassName().contains("transition"))
				sb.append("t/");
			sb.append(info2.getClassName());
			String[] infoData = info2.getData();
			if (infoData != null && infoData.length > 0)
				sb.append(":" + infoData[0]);
		}

		return new CompositePresentationInfo(name, category, sb.toString(), description,
				buildList.getPresentationInfos());
	}

	public boolean open() {
		Shell parent = getParent();
		Shell s = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		s.setText("New Composite Presentation");
		s.setImage(parent.getImage());
		s.setSize(700, 800);
		createUI(s);

		s.open();

		Display display = s.getDisplay();
		while (!s.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		boolean save = saveDialog.isSaveOk();
		name = saveDialog.getName();
		description = saveDialog.getDescription();
		category = saveDialog.getCategory();

		return save;
	}

	protected void createUI(Composite comp) {
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		comp.setLayout(layout);

		createPresentationSection(comp);
		createTransitionSection(comp);
		createDisplayPlanSection(comp);

		// save panel
		Composite saveSection = new Composite(comp, SWT.NONE);
		saveSection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		GridLayout saveLayout = new GridLayout(1, false);
		saveSection.setLayout(saveLayout);

		saveDialog = new CompositePresentationSaveDialog(comp.getShell());
		Composite saveInput = new Composite(saveSection, SWT.NONE);
		saveInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		saveDialog.createUI(saveInput);
		saveDialog.createButtons(saveSection);

		/*Composite buttonBar = new Composite(comp, SWT.NONE);
		GridData data = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		data.verticalIndent = 15;
		buttonBar.setLayoutData(data);

		layout = new GridLayout(2, true);
		layout.marginWidth = 0;
		buttonBar.setLayout(layout);

		ok = new Button(buttonBar, SWT.PUSH);
		ok.setText("&Ok");
		ok.setEnabled(false);
		data = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(ok);
		ok.setLayoutData(data);
		ok.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					CompositePresentationSaveDialog dialog = new CompositePresentationSaveDialog(ok.getShell());
					if (dialog.open()) {
						save = true;
						name = dialog.getName();
						description = dialog.getDescription();
						category = dialog.getCategory();
						ok.getShell().close();
					}
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error closing display plan dialog", e);
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
					cancel.getShell().close();
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error closing delivery instruction dialog", e);
				}
			}
		});

		// TODO contestText.setFocus();
		comp.getShell().setDefaultButton(cancel);
		 */
	}

	private void createPresentationSection(Composite parent2) {
		Group parent = new Group(parent2, SWT.NONE);
		parent.setText("Presentations");
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		parent.setLayout(layout);

		presentationList = new PresentationInfoListControl(parent, SWT.BORDER);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		data.widthHint = 170;
		data.horizontalSpan = 3;
		presentationList.setLayoutData(data);
		for (PresentationInfo info : presentations)
			presentationList.add(info);

		Label label = new Label(parent, SWT.NONE);
		label.setText("Properties:");
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(data);

		final Combo propCombo = new Combo(parent, SWT.BORDER | SWT.DROP_DOWN);
		propCombo.setToolTipText("Optional properties for the currently selected presentation");
		propCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER));
		propCombo.setEnabled(false);
		propCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String s = propCombo.getText();
				if (s == null || s.length() < 0)
					return;

				int ind = s.indexOf(" - ");
				if (ind > 0) {
					s = s.substring(0, ind);
					propCombo.setText(s);
					propCombo.setSelection(new Point(s.length(), s.length()));
					propCombo.setFocus();
				}
			}
		});

		final Button add = createButton(parent, "  Add  ",
				"Add the currently selected presentation into the display plan");
		add.setEnabled(false);
		add.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addToBuild(presentationList.getSelection(), propCombo.getText());
			}
		});

		presentationList.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				PresentationInfo info = presentationList.getSelection();
				propCombo.setEnabled(info != null && info.getProperties() != null);
				add.setEnabled(info != null);

				String[] props = new String[0];
				if (info != null && info.getProperties() != null)
					props = info.getProperties();
				propCombo.setItems(props);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				addToBuild(presentationList.getSelection(), null);
			}
		});
	}

	protected void addToBuild(PresentationInfo info, String properties) {
		if (info == null)
			return;

		PresentationInfo newInfo = info.clone();
		if (properties != null && properties.length() > 0)
			newInfo.setData(properties);
		buildList.add(newInfo);
		enableOk();
	}

	protected void enableOk() {
		boolean hasPresentations = !buildList.getPresentationInfos().isEmpty();
		saveDialog.setPresentationsOk(hasPresentations);
	}

	private static Button createButton(Composite parent, String text, String tooltip) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setToolTipText(tooltip);
		button.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
		return button;
	}

	private void createTransitionSection(Composite parent2) {
		Group parent = new Group(parent2, SWT.NONE);
		parent.setText("Transitions");
		parent.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		parent.setLayout(layout);

		transitionList = new PresentationInfoListControl(parent, SWT.BORDER, new Dimension(72, 15), true,
				DisplayStyle.LIST);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
		data.horizontalSpan = 3;
		transitionList.setLayoutData(data);
		for (PresentationInfo info : transitions)
			transitionList.add(info);

		Label label = new Label(parent, SWT.NONE);
		label.setText("Properties:");
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		final Combo propCombo = new Combo(parent, SWT.BORDER | SWT.DROP_DOWN);
		propCombo.setToolTipText("Optional properties for the currently selected transition");
		propCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		propCombo.setEnabled(false);
		propCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String s = propCombo.getText();
				if (s == null || s.length() < 0)
					return;

				int ind = s.indexOf(" - ");
				if (ind > 0) {
					s = s.substring(0, ind);
					propCombo.setText(s);
					propCombo.setSelection(new Point(s.length(), s.length()));
					propCombo.setFocus();
				}
			}
		});

		Button add = createButton(parent, "  Add  ", "Add the currently selected transition into the display plan");
		add.setEnabled(false);
		add.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addToBuild(transitionList.getSelection(), propCombo.getText());
			}
		});

		transitionList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				PresentationInfo info = transitionList.getSelection();
				propCombo.setEnabled(info != null && info.getProperties() != null);
				add.setEnabled(info != null);

				String[] props = new String[0];
				if (info != null && info.getProperties() != null)
					props = info.getProperties();
				propCombo.setItems(props);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				addToBuild(transitionList.getSelection(), null);
			}
		});
	}

	private void createDisplayPlanSection(Composite parent) {
		Group buildComp = new Group(parent, SWT.NONE);
		buildComp.setText("Presentation Plan");
		buildComp.setLayoutData(new GridData(SWT.FILL, SWT.END, true, false));

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buildComp.setLayout(layout);

		buildList = new PresentationInfoListControl(buildComp, SWT.BORDER, new Dimension(72, 40), false,
				DisplayStyle.TIMELINE);
		GridData data2 = new GridData(SWT.FILL, SWT.FILL, true, true);
		data2.horizontalSpan = 2;
		data2.verticalSpan = 3;
		buildList.setLayoutData(data2);

		final Button remove = createButton(buildComp, "Remove",
				"Remove selected presentation or transition from the plan");
		remove.setEnabled(false);
		remove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				buildList.remove(buildList.getSelection());
			}
		});
		final Button removeAll = createButton(buildComp, "Remove All", "Remove all content from the plan");
		removeAll.setEnabled(false);
		removeAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				buildList.clear();
				enableOk();
			}
		});
		buildList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				remove.setEnabled(buildList.getSelection() != null);
				removeAll.setEnabled(!buildList.getPresentationInfos().isEmpty());
				enableOk();
			}
		});
	}
}
