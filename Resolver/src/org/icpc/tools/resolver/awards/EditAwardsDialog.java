package org.icpc.tools.resolver.awards;

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
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Award;
import org.icpc.tools.contest.model.internal.Contest;

public class EditAwardsDialog extends Dialog {
	protected Contest contest;
	protected Contest previewContest;
	protected ITeam team;
	protected List<IAward> originalAwards = new ArrayList<>();
	protected List<IAward> otherAwards = new ArrayList<>();
	protected List<IAward> awards = new ArrayList<>();
	protected Award selectedAward;

	protected Table awardTable;
	protected Button remove;
	protected Button split;

	protected Label citationLabel;
	protected Text citation;
	protected Button show;

	protected int rc;

	public EditAwardsDialog(Shell parent, Contest contest, ITeam team) {
		super(parent);
		this.contest = contest;
		previewContest = contest.clone(true);
		this.team = team;

		for (IAward aw : previewContest.getAwards()) {
			String[] teamIds = aw.getTeamIds();
			for (String teamId : teamIds) {
				if (teamId.equals(team.getId())) {
					originalAwards.add(aw);
					awards.add(aw);
				}
			}
		}
	}

	public int open() {
		Shell parent = getParent();
		Shell s = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		s.setText("Edit Team Awards");
		s.setImage(parent.getImage());
		createUI(s);

		rc = 1;
		SWTUtil.centerAndOpenDialog(s, true);
		return rc;
	}

	private String getGroupLabel() {
		IGroup[] groups = contest.getGroupsByIds(team.getGroupIds());
		if (groups == null || groups.length == 0)
			return "";

		String groupName = "";
		boolean first = true;
		for (IGroup g : groups) {
			if (!first)
				groupName += ", ";
			if (g != null)
				groupName += g.getName();
			first = false;
		}
		return groupName;
	}

	protected void createUI(Composite comp) {
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		comp.setLayout(layout);

		Label label = new Label(comp, SWT.NONE);
		label.setText("Team:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		label = new Label(comp, SWT.NONE);
		label.setText(team.getActualDisplayName());
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		label = new Label(comp, SWT.NONE);
		label.setText("Groups:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		label = new Label(comp, SWT.NONE);
		label.setText(getGroupLabel());
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		Group awardGroup = new Group(comp, SWT.NONE);
		awardGroup.setText("Awards");
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = 2;
		awardGroup.setLayoutData(data);

		awardGroup.setLayout(new GridLayout(3, false));

		awardTable = new Table(awardGroup, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = 200;
		data.heightHint = 60;
		data.horizontalSpan = 2;
		data.verticalSpan = 2;
		awardTable.setLayoutData(data);
		awardTable.setHeaderVisible(false);
		awardTable.setLinesVisible(false);

		remove = new Button(awardGroup, SWT.PUSH);
		remove.setText("Remove");
		data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(remove);
		remove.setLayoutData(data);

		remove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (isMultiTeamAwardSelected())
					removeTeamFromAward();
				else
					awards.remove(selectedAward);

				selectedAward = null;
				updateAwards();

				if (!awards.isEmpty()) {
					awardTable.select(0);
					awardSelected(0);
				}
			}
		});

		split = new Button(awardGroup, SWT.PUSH);
		split.setText("Split");
		split.setToolTipText("Split a shared award so that this team has their own copy");
		data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(split);
		split.setLayoutData(data);

		split.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				removeTeamFromAward();

				Award a = new Award(selectedAward.getAwardType(), team.getId(), selectedAward.getCitation(),
						selectedAward.showAward());
				awards.add(a);

				updateAwards();

				int sel = awards.indexOf(a);
				awardTable.select(sel);
				awardSelected(sel);
			}
		});

		// citation
		citationLabel = new Label(awardGroup, SWT.NONE);
		citationLabel.setText("Citation:");
		citationLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		citation = new Text(awardGroup, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.horizontalSpan = 2;
		data.widthHint = 300;
		citation.setLayoutData(data);
		citation.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				if (selectedAward != null)
					selectedAward.setCitation(citation.getText());
			}
		});

		// show
		show = new Button(awardGroup, SWT.CHECK);
		show.setText("Show team picture and citation (after pausing in the resolver)");
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.horizontalSpan = 3;
		show.setLayoutData(data);
		show.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (selectedAward != null)
					selectedAward.setShowAward(show.getSelection());
			}
		});

		awardTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				int sel = awardTable.getSelectionIndex();
				awardSelected(sel);
			}
		});

		updateAwards();

		awardTable.select(0);
		awardSelected(0);

		// buttons
		Composite buttonBar = new Composite(comp, SWT.NONE);
		data = new GridData(SWT.RIGHT, SWT.BOTTOM, false, true);
		data.horizontalSpan = 3;
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

					applyAwards();
				} catch (Exception e) {
					ErrorHandler.error("Error closing award dialog", e);
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
					ErrorHandler.error("Error closing award dialog", e);
				}
			}
		});

		apply.setFocus();
		comp.getShell().setDefaultButton(apply);
	}

	private boolean isMultiTeamAwardSelected() {
		return selectedAward.getTeamIds().length > 1;
	}

	private void removeTeamFromAward() {
		String[] teamIds = selectedAward.getTeamIds();
		String[] newTeamIds = new String[teamIds.length - 1];
		int count = 0;
		for (int i = 0; i < teamIds.length; i++) {
			if (!teamIds[i].equals(team.getId())) {
				newTeamIds[count] = teamIds[i];
				count++;
			}
		}
		selectedAward.setTeamIds(newTeamIds);
		otherAwards.add(selectedAward);

		awards.remove(selectedAward);
	}

	protected void updateAwards() {
		awardTable.removeAll();

		if (awards.isEmpty()) {
			TableItem ti = new TableItem(awardTable, SWT.NONE);
			ti.setText("No awards");
			ti.setGrayed(true);
			awardTable.setEnabled(false);

			citation.setText("");
			show.setSelection(false);

			citationLabel.setEnabled(false);
			citation.setEnabled(false);
			show.setEnabled(false);
			remove.setEnabled(false);
		} else {
			for (IAward a : awards) {
				TableItem ti = new TableItem(awardTable, SWT.NONE);
				String name = a.getAwardType().getName();
				if (a.getTeamIds().length == 2)
					name += " (shared with 1 other team)";
				else if (a.getTeamIds().length > 2)
					name += " (shared with " + (a.getTeamIds().length - 1) + " other teams)";
				ti.setText(name);
				ti.setData(a);
			}

			int size = awards.size();
			String[] s = new String[size];
			for (int i = 0; i < size; i++) {
				s[i] = awards.get(i).getAwardType().getName();
			}
		}
		split.setEnabled(false);
	}

	protected void awardSelected(int sel) {
		if (awards.isEmpty()) {
			selectedAward = null;
			return;
		}

		selectedAward = (Award) awards.get(sel);
		citation.setText(selectedAward.getCitation());
		show.setSelection(selectedAward.showAward());

		remove.setEnabled(true);
		split.setEnabled(isMultiTeamAwardSelected());
	}

	protected void applyAwards() {
		// remove awards from original contest
		for (IAward aw : originalAwards)
			contest.removeFromHistory(aw);

		// and replace with new ones
		for (IAward aw : awards)
			contest.add(aw);

		for (IAward aw : otherAwards)
			contest.add(aw);
	}
}