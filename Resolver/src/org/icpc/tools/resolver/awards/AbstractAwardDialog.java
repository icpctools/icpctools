package org.icpc.tools.resolver.awards;

import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IAward.AwardType;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.AwardUtil;

public abstract class AbstractAwardDialog extends Dialog {
	private Contest contest;
	private Contest previewContest;
	private Table previewTable;

	private int rc;

	public AbstractAwardDialog(Shell parent, Contest contest) {
		super(parent);
		this.contest = contest;
	}

	protected Contest getContest() {
		return contest;
	}

	public int open() {
		Shell parent = getParent();
		Shell s = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		s.setText(getTitle());
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

	protected abstract String getTitle();

	protected abstract String getDescription();

	protected abstract AwardType[] getAwardTypes();

	protected abstract void createAwardUI(Composite comp);

	protected abstract void applyAwards(Contest aContest);

	protected void updateAwards() {
		previewContest = contest.clone(true);

		AwardType[] types = getAwardTypes();
		for (IAward award : previewContest.getAwards()) {
			for (AwardType type : types)
				if (type.equals(award.getAwardType())) {
					previewContest.removeFromHistory(award);
				}
		}

		applyAwards(previewContest);

		// update table
		previewTable.removeAll();

		IAward[] awards = previewContest.getAwards();
		AwardUtil.sortAwards(previewContest, awards);

		for (IAward award : awards) {
			for (AwardType type : types)
				if (type == null || type.equals(award.getAwardType())) {
					String[] teamIds = award.getTeamIds();
					for (String teamId : teamIds) {
						ITeam team = previewContest.getTeamById(teamId);
						IStanding standing = previewContest.getStanding(team);

						TableItem ti = new TableItem(previewTable, SWT.NONE);
						ti.setText(new String[] { standing.getRank(), team.getId(), team.getName(), award.getCitation() });
					}
				}
		}
	}

	protected void createColumn(String name, int width, int align) {
		TableColumn column = new TableColumn(previewTable, SWT.NONE);
		column.setText(name);
		column.setWidth(width);
		column.setAlignment(align);
	}

	protected void createUI(Composite comp) {
		GridLayout layout = new GridLayout(3, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		comp.setLayout(layout);

		Label label = new Label(comp, SWT.NONE);
		label.setText(getDescription());
		GridData data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
		data.horizontalSpan = 3;
		data.verticalSpan = 2;
		label.setLayoutData(data);

		Composite comp2 = new Composite(comp, SWT.NONE);
		data = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false);
		comp2.setLayoutData(data);
		layout = new GridLayout(3, false);
		layout.marginWidth = 10;
		layout.marginHeight = 7;
		comp2.setLayout(layout);
		createAwardUI(comp2);

		if (comp2.getChildren().length == 0)
			comp2.dispose();

		// preview
		label = new Label(comp, SWT.NONE);
		label.setText("Award preview:");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalSpan = 3;
		data.verticalIndent = 10;
		label.setLayoutData(data);

		previewTable = new Table(comp, SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = 600;
		data.heightHint = 150;
		data.horizontalSpan = 3;
		previewTable.setLayoutData(data);
		previewTable.setHeaderVisible(true);
		previewTable.setLinesVisible(true);

		createColumn("Rank", 45, SWT.RIGHT);
		createColumn("#", 45, SWT.RIGHT);
		createColumn("Team", 250, SWT.LEFT);
		createColumn("Citation", 250, SWT.LEFT);

		updateAwards();

		// buttons
		Composite buttonBar = new Composite(comp, SWT.NONE);
		data = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		data.horizontalSpan = 3;
		data.verticalIndent = 20;
		buttonBar.setLayoutData(data);

		layout = new GridLayout(2, true);
		layout.marginWidth = 0;
		buttonBar.setLayout(layout);

		final Button apply = new Button(buttonBar, SWT.PUSH);
		apply.setText("&Apply");
		data = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(apply);
		apply.setLayoutData(data);
		apply.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					rc = 0;
					AwardType[] types = getAwardTypes();
					for (IAward award : contest.getAwards()) {
						for (AwardType type : types)
							if (type.equals(award.getAwardType())) {
								contest.removeFromHistory(award);
							}
					}
					applyAwards(contest);
					updatePreferences();
					apply.getShell().dispose();
				} catch (Exception e) {
					ErrorHandler.error("Error closing award dialog", e);
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
					ErrorHandler.error("Error closing award dialog", e);
				}
			}
		});

		apply.setFocus();
		comp.getShell().setDefaultButton(apply);
	}

	protected void updatePreferences() {
		// ignore for now
	}
}