package org.icpc.tools.resolver.awards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IAward.AwardType;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.AwardUtil;

public class GroupAwardDialog extends AbstractAwardDialog {
	protected int numPerGroup = 1;

	public GroupAwardDialog(Shell parent, Contest contest) {
		super(parent, contest);
	}

	@Override
	protected String getTitle() {
		return "Group Awards";
	}

	@Override
	protected String getDescription() {
		return "Assign awards to the top X teams from each group.";
	}

	@Override
	protected void createAwardUI(Composite comp) {
		Label label = new Label(comp, SWT.NONE);
		label.setText("Awards per group:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		final Combo groupCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		groupCombo.setItems(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" });
		groupCombo.select(0);
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, true, false);
		data.horizontalSpan = 2;
		groupCombo.setLayoutData(data);
		groupCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				numPerGroup = groupCombo.getSelectionIndex() + 1;
				updateAwards();
			}
		});
	}

	@Override
	protected AwardType[] getAwardTypes() {
		return new AwardType[] { IAward.GROUP };
	}

	@Override
	protected void applyAwards(Contest aContest) {
		AwardUtil.createGroupAwards(aContest, numPerGroup);
	}
}