package org.icpc.tools.resolver.awards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IAward.AwardType;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.AwardUtil;

public class FirstToSolveAwardDialog extends AbstractAwardDialog {
	protected boolean beforeFreeze = true;
	protected boolean afterFreeze = true;

	public FirstToSolveAwardDialog(Shell parent, Contest contest) {
		super(parent, contest);
	}

	@Override
	protected String getTitle() {
		return "First to Solve Awards";
	}

	@Override
	protected String getDescription() {
		return "Assign awards to the first teams to solve each problem.";
	}

	@Override
	protected void createAwardUI(Composite comp) {
		final Button beforeFreezeButton = new Button(comp, SWT.CHECK);
		beforeFreezeButton.setText("Show team picture and citation for first-to-solve before freeze");
		beforeFreezeButton.setSelection(true);
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalSpan = 3;
		beforeFreezeButton.setLayoutData(data);
		beforeFreezeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				beforeFreeze = beforeFreezeButton.getSelection();
				updateAwards();
			}
		});

		final Button afterFreezeButton = new Button(comp, SWT.CHECK);
		afterFreezeButton.setText("Show team picture and citation for first-to-solve after freeze");
		afterFreezeButton.setSelection(true);
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalSpan = 3;
		afterFreezeButton.setLayoutData(data);
		afterFreezeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				afterFreeze = afterFreezeButton.getSelection();
				updateAwards();
			}
		});
	}

	@Override
	protected AwardType[] getAwardTypes() {
		return new AwardType[] { IAward.FIRST_TO_SOLVE };
	}

	@Override
	protected void applyAwards(Contest aContest) {
		AwardUtil.createFirstToSolveAwards(aContest, beforeFreeze, afterFreeze);
	}
}