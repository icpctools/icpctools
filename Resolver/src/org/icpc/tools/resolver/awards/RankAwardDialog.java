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

public class RankAwardDialog extends AbstractAwardDialog {
	protected int numAwards = 1;

	public RankAwardDialog(Shell parent, Contest contest) {
		super(parent, contest);
	}

	@Override
	protected String getTitle() {
		return "Rank Awards";
	}

	@Override
	protected String getDescription() {
		return "Assign rank (place) awards to the top X teams in the contest.";
	}

	@Override
	protected void createAwardUI(Composite comp) {
		Label label = new Label(comp, SWT.NONE);
		label.setText("Teams to award:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		final Combo groupCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		int numTeams = getContest().getNumTeams();
		String[] items = new String[numTeams];
		for (int i = 0; i < numTeams; i++)
			items[i] = (i + 1) + "";
		groupCombo.setItems(items);
		groupCombo.select(0);
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, true, false);
		data.horizontalSpan = 2;
		groupCombo.setLayoutData(data);
		groupCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				numAwards = groupCombo.getSelectionIndex() + 1;
				updateAwards();
			}
		});
	}

	@Override
	protected AwardType[] getAwardTypes() {
		return new AwardType[] { IAward.RANK };
	}

	@Override
	protected void applyAwards(Contest aContest) {
		AwardUtil.createRankAwards(aContest, numAwards);
	}
}