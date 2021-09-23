package org.icpc.tools.resolver.awards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IAward.AwardType;
import org.icpc.tools.contest.model.IAward.DisplayMode;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.AwardUtil;

public class GroupHighlightAwardDialog extends AbstractAwardDialog {
	protected String groupId;
	protected int numToHighlight = 1;
	protected String citation = "Champions";
	protected boolean showPicture;

	public GroupHighlightAwardDialog(Shell parent, Contest contest) {
		super(parent, contest);
	}

	@Override
	protected String getTitle() {
		return "Highlight Group Awards";
	}

	@Override
	protected String getDescription() {
		return "Give awards to highlight the top X teams from a particular group";
	}

	@Override
	protected void createAwardUI(Composite comp) {
		Label label = new Label(comp, SWT.NONE);
		label.setText("Group to highlight:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		final Combo groupCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		final IGroup[] groups = getContest().getGroups();
		String[] groupNames = new String[groups.length];
		for (int i = 0; i < groups.length; i++)
			groupNames[i] = groups[i].getName();
		groupCombo.setItems(groupNames);
		if (groups.length > 0)
			groupId = groups[0].getId();

		groupCombo.select(0);
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, true, false);
		data.horizontalSpan = 2;
		groupCombo.setLayoutData(data);
		groupCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				groupId = groups[groupCombo.getSelectionIndex()].getId();
				updateAwards();
			}
		});

		label = new Label(comp, SWT.NONE);
		label.setText("Teams to highlight:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		final Combo numCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
		List<String> list = new ArrayList<>();
		for (int i = 1; i < 25; i++)
			list.add(i + "");

		numCombo.setItems(list.toArray(new String[0]));
		numCombo.select(0);
		data = new GridData(SWT.BEGINNING, SWT.CENTER, true, false);
		data.horizontalSpan = 2;
		numCombo.setLayoutData(data);
		numCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				numToHighlight = numCombo.getSelectionIndex() + 1;
				updateAwards();
			}
		});

		label = new Label(comp, SWT.NONE);
		label.setText("Citation:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		final Text citationText = new Text(comp, SWT.BORDER);
		citationText.setText(citation);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.horizontalSpan = 2;
		citationText.setLayoutData(data);
		citationText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				citation = citationText.getText();
				updateAwards();
			}
		});

		final Button showPictureButton = new Button(comp, SWT.CHECK);
		showPictureButton.setText("Show team picture and citation in resolver");
		showPictureButton.setSelection(true);
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalSpan = 3;
		showPictureButton.setLayoutData(data);
		showPictureButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				showPicture = showPictureButton.getSelection();
				updateAwards();
			}
		});
	}

	@Override
	protected AwardType[] getAwardTypes() {
		return new AwardType[] { IAward.GROUP_HIGHLIGHT };
	}

	@Override
	protected void applyAwards(Contest aContest) {
		AwardUtil.createGroupHighlightAwards(aContest, groupId, numToHighlight, citation, showPicture ? DisplayMode.DETAIL : null);
	}
}