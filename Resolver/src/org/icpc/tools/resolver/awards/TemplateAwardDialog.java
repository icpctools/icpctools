package org.icpc.tools.resolver.awards;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IAward.AwardType;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.internal.Award;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.AwardUtil;

public class TemplateAwardDialog extends AbstractAwardDialog {
	protected String templateString = "{\"id\":\"winner\"}\n" + // champion
			"{\"id\":\"gold-medal\",\"parameter\":\"4\"}\n" + // gold medals
			"{\"id\":\"silver-medal\",\"parameter\":\"4\"}\n" + // silver medals
			"{\"id\":\"bronze-medal\",\"parameter\":\"4\"}\n" + // bronze medals
			"{\"id\":\"first-to-solve-*\"}\n" + // first to solve awards
			"{\"id\":\"highest-honors\",\"parameter\":\"0-1\"}\n" + // All medalists + all solving the same number of problems as the lowest medalist
			"{\"id\":\"high-honors\",\"parameter\":\"1-2\"}\n" + // All teams solving one fewer than the lowest medalist
			"{\"id\":\"honors\",\"parameter\":\"2-p50\"}\n" + // All teams not receiving highest honors or high honors solving the same or more problems than the median scoring team
			"{\"id\":\"honors-mention\",\"parameter\":\"p50-p100\"}\n" + // honorable mention for teams
			// scoring below 50th percentile
			"{\"id\":\"group-winner-*\"}"; // group winners

	protected Text text;
	protected Label status;

	private IAward[] template;

	public TemplateAwardDialog(Shell parent, Contest contest) {
		super(parent, contest);
	}

	@Override
	protected String getTitle() {
		return "Awards from Template";
	}

	@Override
	protected String getDescription() {
		return "Use a template to assign awards.";
	}

	@Override
	protected void createAwardUI(Composite comp) {
		text = new Text(comp, SWT.WRAP | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		text.setText(templateString);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = 600;
		data.heightHint = 150;
		data.horizontalSpan = 2;
		text.setLayoutData(data);
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				templateString = text.getText();
				updateAwards();
			}
		});

		final Button fileButton = new Button(comp, SWT.PUSH);
		fileButton.setText("Load from File...");
		data = new GridData(SWT.END, SWT.BEGINNING, false, false);
		fileButton.setLayoutData(data);
		fileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				FileDialog dialog = new FileDialog(fileButton.getShell());
				dialog.setFilterExtensions(new String[] { "*.json" });
				String file = dialog.open();
				if (file != null)
					loadFromFile(file);
			}
		});

		status = new Label(comp, SWT.NONE);
		status.setText("");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, true, false);
		data.horizontalSpan = 2;
		status.setLayoutData(data);
	}

	protected void loadFromFile(String name) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(name));
			String s = br.readLine();
			StringBuilder sb = new StringBuilder();
			while (s != null) {
				sb.append(s);
				s = br.readLine();
			}
			text.setText(sb.toString());
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading from: " + name, e);
		}
	}

	@Override
	protected AwardType[] getAwardTypes() {
		return new AwardType[] { IAward.WINNER, IAward.FIRST_TO_SOLVE, IAward.GROUP, IAward.MEDAL, IAward.TOP,
				IAward.HONORS, IAward.EXPECTED_TO_ADVANCE, IAward.SOLVED };
	}

	protected Award parseAward(JsonObject data) {
		Award award = new Award();
		for (String key : data.props.keySet())
			award.add(key, data.props.get(key));
		return award;
	}

	@Override
	protected void applyAwards(Contest aContest) {
		template = new IAward[0];
		String error = null;

		String firstLine = templateString.trim();
		if (firstLine.length() < 3 || !(firstLine.startsWith("[") || firstLine.startsWith("{"))) {
			status.setText("No awards found");
		} else if (firstLine.startsWith("[")) { // [ - parse as JSON array
			try {
				List<IAward> list = new ArrayList<>();
				JSONParser parser = new JSONParser(templateString);
				Object[] arr = parser.readArray();
				for (Object obj : arr)
					list.add(parseAward((JsonObject) obj));

				template = list.toArray(new Award[0]);
				status.setText(template.length + " awards applied");
			} catch (Exception e) {
				status.setText("Could not parse awards: " + e.getMessage());
			}
		} else { // { - parse as NDJSON
			List<IAward> list = new ArrayList<>();
			String[] lines = templateString.split("\n");
			for (int i = 0; i < lines.length; i++) {
				if (lines[i] != null && lines[i].trim().length() > 0)
					try {
						JSONParser parser = new JSONParser(lines[i]);
						JsonObject obj = parser.readObject();
						list.add(parseAward(obj));
					} catch (Exception e) {
						error = "Could not parse line " + i + ": " + e.getMessage();
					}
			}
			template = list.toArray(new Award[0]);
		}

		try {
			AwardUtil.applyAwards(aContest, template);
			if (error != null)
				status.setText(error + " (" + template.length + " templates applied)");
			else
				status.setText(template.length + " templates applied");
		} catch (Exception e) {
			status.setText("Couldn't apply: " + e.getMessage());
		}
	}
}
