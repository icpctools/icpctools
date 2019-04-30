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

public class MedalAwardDialog extends AbstractAwardDialog {
	private int numGold = 4;
	private int numSilver = 4;
	private int numBronze = 4;

	public MedalAwardDialog(Shell parent, Contest contest) {
		super(parent, contest);
	}

	@Override
	protected String getTitle() {
		return "Medal Awards";
	}

	@Override
	protected String getDescription() {
		return "Assign gold, silver, and bronze medals.";
	}

	@Override
	protected void createAwardUI(Composite comp) {
		Label label = new Label(comp, SWT.NONE);
		label.setText("Number of gold:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		final Combo goldCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		goldCombo.setItems(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" });
		goldCombo.select(4);
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, true, false);
		data.horizontalSpan = 2;
		goldCombo.setLayoutData(data);
		goldCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				numGold = goldCombo.getSelectionIndex();
				updateAwards();
			}
		});

		label = new Label(comp, SWT.NONE);
		label.setText("Number of silver:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		final Combo silverCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		silverCombo.setItems(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" });
		silverCombo.select(4);
		data = new GridData(SWT.BEGINNING, SWT.CENTER, true, false);
		data.horizontalSpan = 2;
		silverCombo.setLayoutData(data);
		silverCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				numSilver = silverCombo.getSelectionIndex();
				updateAwards();
			}
		});

		label = new Label(comp, SWT.NONE);
		label.setText("Number of bronze:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		final Combo bronzeCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		bronzeCombo.setItems(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" });
		bronzeCombo.select(4);
		data = new GridData(SWT.BEGINNING, SWT.CENTER, true, false);
		data.horizontalSpan = 2;
		bronzeCombo.setLayoutData(data);
		bronzeCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				numBronze = bronzeCombo.getSelectionIndex();
				updateAwards();
			}
		});
	}

	@Override
	protected AwardType[] getAwardTypes() {
		return new AwardType[] { IAward.MEDAL };
	}

	@Override
	protected void applyAwards(Contest aContest) {
		AwardUtil.createMedalAwards(aContest, numGold, numSilver, numBronze);
	}
}