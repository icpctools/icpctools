package org.icpc.tools.resolver.awards;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IAward.AwardType;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.Award;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.AwardUtil;

public class LoadTemplateAwardDialog extends AbstractAwardDialog {
	private IAward[] template;

	public LoadTemplateAwardDialog(Shell parent, Contest contest) {
		super(parent, contest);
	}

	@Override
	protected String getTitle() {
		return "Load from Template";
	}

	@Override
	protected String getDescription() {
		return "Load and apply the awards from an award template.";
	}

	@Override
	protected void createAwardUI(Composite comp) {
		Label label = new Label(comp, SWT.NONE);
		label.setText("File:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		Label filename = new Label(comp, SWT.NONE);
		filename.setText("<none>");
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		// data.horizontalSpan = 2;
		filename.setLayoutData(data);

		final Button fileButton = new Button(comp, SWT.PUSH);
		fileButton.setText("Select");
		data = new GridData(SWT.END, SWT.CENTER, false, false);
		// data.horizontalSpan = 3;
		fileButton.setLayoutData(data);
		fileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				FileDialog dialog = new FileDialog(fileButton.getShell());
				dialog.setFilterExtensions(new String[] { "*.json" });
				String file = dialog.open();
				if (file == null) {
					template = null;
					filename.setText("<none>");
				} else {
					filename.setText(file);
					try {
						loadFromFile(file);
						filename.setText(file + " (" + template.length + "awards)");
					} catch (Exception e) {
						Trace.trace(Trace.ERROR, "Error loading from template", e);
						filename.setText(file + " - " + e.getMessage());
					}
				}

				updateAwards();
			}
		});
	}

	protected void loadFromFile(String name) throws IOException {
		JSONParser parser = new JSONParser(new FileInputStream(name));
		Object[] arr = parser.readArray();
		List<Award> list = new ArrayList<>();
		for (Object obj : arr) {
			JsonObject data = (JsonObject) obj;
			Award award = new Award();
			for (String key : data.props.keySet())
				award.add(key, data.props.get(key));

			list.add(award);
		}
		template = list.toArray(new Award[0]);
	}

	@Override
	protected AwardType[] getAwardTypes() {
		return new AwardType[] { IAward.WINNER, IAward.FIRST_TO_SOLVE, IAward.GROUP, IAward.MEDAL };
	}

	@Override
	protected void applyAwards(Contest aContest) {
		if (template != null)
			AwardUtil.applyAwards(aContest, template);
	}
}