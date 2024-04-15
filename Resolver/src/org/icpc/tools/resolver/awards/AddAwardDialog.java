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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IAward.DisplayMode;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.internal.Award;
import org.icpc.tools.contest.model.internal.Contest;

public class AddAwardDialog extends Dialog {
	protected Contest contest;
	protected Contest previewContest;
	protected ITeam[] teams;
	protected Award award = new Award();

	protected int rc;

	public AddAwardDialog(Shell parent, Contest contest, ITeam[] teams) {
		super(parent);
		this.contest = contest;
		previewContest = contest.clone(true);
		this.teams = teams;
		List<String> ids = new ArrayList<>();
		for (ITeam team : teams)
			ids.add(team.getId());
		award.setTeamIds(ids.toArray(new String[0]));
	}

	public int open() {
		Shell parent = getParent();
		Shell s = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		s.setText("Add Award");
		s.setImage(parent.getImage());
		createUI(s);

		rc = 1;
		SWTUtil.centerAndOpenDialog(s, true);
		return rc;
	}

	protected void createUI(Composite comp) {
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		comp.setLayout(layout);

		Label label = new Label(comp, SWT.NONE);
		label.setText("Team(s):");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		label = new Label(comp, SWT.NONE);
		if (teams.length == 1)
			label.setText(teams[0].getActualDisplayName());
		else
			label.setText(teams.length + " teams");

		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		Label typeLabel = new Label(comp, SWT.NONE);
		typeLabel.setText("Type:");
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		typeLabel.setLayoutData(data);

		// types of medals
		List<String> awardNames = new ArrayList<>();
		List<String> awardIds = new ArrayList<>();

		awardNames.add("Winner");
		awardIds.add("winner");

		awardNames.add("Gold");
		awardIds.add("gold-medal");
		awardNames.add("Silver");
		awardIds.add("silver-medal");
		awardNames.add("Bronze");
		awardIds.add("bronze-medal");

		for (IProblem p : contest.getProblems()) {
			awardNames.add("First to solve problem " + p.getLabel());
			awardIds.add("first-to-solve-" + p.getId());
		}

		for (IGroup g : contest.getGroups()) {
			awardNames.add("Winner of group " + g.getName());
			awardIds.add("group-winner--" + g.getId());
		}

		awardNames.add("Other");
		awardIds.add("id-" + Math.random());

		final Combo typeCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		typeCombo.setItems(awardNames.toArray(new String[0]));
		typeCombo.select(0);
		award.add("id", awardIds.get(0));
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		typeCombo.setLayoutData(data);

		Label citationLabel = new Label(comp, SWT.NONE);
		citationLabel.setText("Citation:");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		citationLabel.setLayoutData(data);

		Text citation = new Text(comp, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		citation.setLayoutData(data);

		// show
		Combo mode = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		mode.setItems(new String[] { "Stop to show details", "Pause and move on", "Show as list", "Show as photos" });
		mode.setText("Show team picture and citation (after pausing in the resolver)");
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.horizontalSpan = 3;
		mode.setLayoutData(data);
		mode.select(DisplayMode.DETAIL.ordinal());

		Label warning = new Label(comp, SWT.NONE);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.horizontalSpan = 2;
		warning.setLayoutData(data);

		IAward old = contest.getAwardById(award.getId());
		if (old != null)
			warning.setText("Warning: Award already assigned and will be removed from prior team(s)!");
		else
			warning.setText("   ");

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
		apply.setEnabled(false);
		data = new GridData(SWT.RIGHT, SWT.BOTTOM, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(apply);
		apply.setLayoutData(data);
		apply.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					rc = 0;
					apply.getShell().dispose();

					applyAward();
				} catch (Exception e) {
					ErrorHandler.error("Error closing award dialog", e);
				}
			}
		});

		// events
		typeCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				int i = typeCombo.getSelectionIndex();
				award.add("id", awardIds.get(i));

				IAward old2 = contest.getAwardById(award.getId());
				if (old2 != null)
					warning.setText("Warning: Award already assigned and will be removed from prior team(s)!");
				else
					warning.setText("");
			}
		});
		mode.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				award.setDisplayMode(DisplayMode.values()[mode.getSelectionIndex()]);
			}
		});
		citation.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				String citationStr = citation.getText();
				apply.setEnabled(citationStr.length() > 1);
				award.setCitation(citationStr);
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

	protected void applyAward() {
		IAward old = contest.getAwardById(award.getId());
		if (old != null)
			contest.removeFromHistory(old);
		contest.add(award);
	}
}