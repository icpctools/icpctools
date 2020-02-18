package org.icpc.tools.resolver.awards;

import java.io.IOException;
import java.util.prefs.Preferences;

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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.EventFeedContestSource;
import org.icpc.tools.contest.model.feed.RESTContestSource;

public class ContestConnectionDialog extends Dialog {
	private static Preferences prefs = AwardUI.getPreferences().node("connect");
	private static final String RECENT_SEP = "^";

	private static final String PREF_RECENT = "recent";
	private static final String PREF_URL = "rest.url";
	private static final String PREF_USER = "rest.user";
	private static final String PREF_PASSWORD = "rest.password";
	private static final String PREF_FILE = "disk.file";

	enum ConnectMethod {
		RECENT, REST, DISK
	}

	protected ConnectMethod method = ConnectMethod.REST;

	protected boolean hasRecent;

	protected String[] recent;

	protected String url;
	protected String user;
	protected String password;

	protected String file;

	protected Button connect;
	protected int rc;

	public ContestConnectionDialog(Shell parent) {
		super(parent);
	}

	public int open() {
		Shell parent = getParent();
		Shell s = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		s.setText("Connect to Contest");
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

	public ContestSource getContestSource() throws IOException {
		if (method == ConnectMethod.REST)
			return new RESTContestSource(url, user, password);
		else if (method == ConnectMethod.DISK)
			return new EventFeedContestSource(file);
		else if (method == ConnectMethod.RECENT) {
			String[] vals = recent;
			if ("REST".equals(vals[1]))
				return new RESTContestSource(vals[2], vals[3], vals[4]);
			else if ("FILE".equals(vals[1]))
				return new EventFeedContestSource(vals[2]);
		}
		return null;
	}

	private String getContestSourceString() {
		if (method == ConnectMethod.RECENT)
			return String.join("`", recent);
		else if (method == ConnectMethod.REST)
			return user + " @ " + url + "`REST`" + url + "`" + user + "`" + password;
		else if (method == ConnectMethod.DISK)
			return file + "`FILE`" + file;
		return null;
	}

	protected void validate() {
		boolean valid = true;
		if (method == ConnectMethod.RECENT) {
			if (recent == null)
				valid = false;
		} else if (method == ConnectMethod.REST) {
			if (url == null || url.isEmpty())
				valid = false;
		} else if (method == ConnectMethod.DISK) {
			if (file == null || file.length() < 5)
				valid = false;
		}
		if (connect != null)
			connect.setEnabled(valid);
	}

	protected Composite createRecentTab(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		composite.setLayout(layout);

		Table recentTable = new Table(composite,
				SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = 400;
		data.heightHint = 100;
		data.verticalSpan = 5;
		data.horizontalSpan = 2;
		recentTable.setLayoutData(data);
		recentTable.setHeaderVisible(false);
		recentTable.setLinesVisible(true);

		TableColumn column = new TableColumn(recentTable, SWT.NONE);
		column.setWidth(400);
		column.setAlignment(SWT.LEFT);

		// prefs.remove(PREF_RECENT);
		String recentPref = prefs.get(PREF_RECENT, null);
		if (recentPref != null) {
			String[] recents = recentPref.split("\\^");
			for (String r : recents) {
				TableItem item = new TableItem(recentTable, SWT.NONE);
				String[] vals = r.split("\\`");
				item.setText(vals[0]);
				item.setData(vals);
				if (!hasRecent) {
					recent = vals;
					hasRecent = true;
				}
			}
		}

		recentTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				recent = (String[]) recentTable.getSelection()[0].getData();
				validate();
			}
		});

		if (hasRecent)
			recentTable.setSelection(0);

		return composite;
	}

	protected Composite createRESTTab(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		composite.setLayout(layout);

		Label urlLabel = new Label(composite, SWT.NONE);
		urlLabel.setText("&URL:");
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		urlLabel.setLayoutData(data);

		Text urlText = new Text(composite, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 250;
		urlText.setLayoutData(data);
		urlText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				url = urlText.getText();
				validate();
			}
		});
		urlText.setText(prefs.get(PREF_URL, "https://cds/api/contests/finals"));

		Label userLabel = new Label(composite, SWT.NONE);
		userLabel.setText("Us&er:");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		userLabel.setLayoutData(data);

		Text userText = new Text(composite, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		userText.setLayoutData(data);
		userText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				user = userText.getText();
				validate();
			}
		});
		userText.setText(prefs.get(PREF_USER, "admin"));

		Label passLabel = new Label(composite, SWT.NONE);
		passLabel.setText("&Password:");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		passLabel.setLayoutData(data);

		Text passText = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		passText.setLayoutData(data);
		passText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				password = passText.getText();
				validate();
			}
		});
		passText.setText(prefs.get(PREF_PASSWORD, ""));

		return composite;
	}

	protected Composite createDiskTab(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		composite.setLayout(layout);

		Label fileLabel = new Label(composite, SWT.NONE);
		fileLabel.setText("&File:");
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		fileLabel.setLayoutData(data);

		Text fileText = new Text(composite, SWT.BORDER);
		fileText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fileText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				file = fileText.getText();
				validate();
			}
		});
		fileText.setText(prefs.get(PREF_FILE, "events.xml"));

		Button fileBrowse = new Button(composite, SWT.PUSH);
		fileBrowse.setText("&Browse...");
		data = new GridData(SWT.END, SWT.CENTER, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(fileBrowse);
		fileBrowse.setLayoutData(data);
		fileBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				FileDialog dialog = new FileDialog(fileBrowse.getShell(), SWT.OPEN);
				dialog.setFileName(fileText.getText());
				dialog.setText("Select JSON or XML event feed");
				dialog.setFilterExtensions(new String[] { "*.xml;*.json", "*.*" });
				String f = dialog.open();
				if (f != null) {
					fileText.setText(f);
				}
			}
		});

		return composite;
	}

	protected void createUI(Composite connectGroup) {
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		connectGroup.setLayout(layout);

		Label label = new Label(connectGroup, SWT.NONE);
		label.setText("How do you want to connect to a contest?");
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		label.setLayoutData(data);

		TabFolder tabFolder = new TabFolder(connectGroup, SWT.NONE);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = 2;
		tabFolder.setLayoutData(data);

		TabItem recentTab = new TabItem(tabFolder, SWT.NONE);
		recentTab.setText("Re&cent");
		recentTab.setToolTipText("Recent contests");
		recentTab.setControl(createRecentTab(tabFolder));
		recentTab.setData(ConnectMethod.RECENT);

		TabItem restTab = new TabItem(tabFolder, SWT.NONE);
		restTab.setText("&REST");
		restTab.setToolTipText("Connect via REST");
		restTab.setControl(createRESTTab(tabFolder));
		restTab.setData(ConnectMethod.REST);

		TabItem diskTab = new TabItem(tabFolder, SWT.NONE);
		diskTab.setText("&Disk");
		diskTab.setToolTipText("Load from disk");
		diskTab.setControl(createDiskTab(tabFolder));
		diskTab.setData(ConnectMethod.DISK);

		tabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				method = (ConnectMethod) tabFolder.getSelection()[0].getData();
				validate();
			}
		});

		// buttons
		Composite buttonBar = new Composite(connectGroup, SWT.NONE);
		data = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		data.verticalIndent = 20;
		buttonBar.setLayoutData(data);

		layout = new GridLayout(2, true);
		layout.marginWidth = 0;
		buttonBar.setLayout(layout);

		connect = new Button(buttonBar, SWT.PUSH);
		connect.setText("&Connect");
		data = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(connect);
		connect.setLayoutData(data);
		connect.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					rc = 0;
					updatePreferences();
					connect.getShell().dispose();
				} catch (Exception e) {
					ErrorHandler.error("Error closing connection dialog", e);
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
					ErrorHandler.error("Error closing connection dialog", e);
				}
			}
		});

		if (hasRecent) {
			method = ConnectMethod.RECENT;
			tabFolder.setSelection(recentTab);
		} else
			tabFolder.setSelection(restTab);

		connect.setFocus();
		connectGroup.getShell().setDefaultButton(connect);
	}

	protected void updatePreferences() {
		String recentPref = prefs.get(PREF_RECENT, null);
		try {
			String srcStr = getContestSourceString();
			String[] recents = null;
			if (recentPref == null) {
				recents = new String[] { srcStr };
			} else {
				recents = recentPref.split("\\^");
				int found = -1;
				for (int i = 0; i < recents.length; i++) {
					if (recents[i].equals(srcStr))
						found = i;
				}

				if (found < 0) {
					// new item - expand array and drop the last one off if necessary
					int num = Math.min(9, recents.length);
					String[] temp = new String[num + 1];
					for (int i = 0; i < num; i++)
						temp[i + 1] = recents[i];
					recents = temp;
				} else if (found > 0) {
					// already in the list. move higher ones down to put it back to top of list
					for (int i = found - 1; i >= 0; i--)
						recents[i + 1] = recents[i];
				}
				recents[0] = srcStr;
			}

			prefs.put(PREF_RECENT, String.join(RECENT_SEP, recents));
			// prefs.put(PREF_RECENT, srcStr);
		} catch (Exception e) {
			e.printStackTrace();
			// ignore
		}
		// System.out.println(prefs.get(PREF_RECENT, null));

		prefs.put(PREF_URL, url);
		prefs.put(PREF_USER, user);
		prefs.put(PREF_PASSWORD, password);
		prefs.put(PREF_FILE, file);
		try {
			prefs.sync();
		} catch (Exception e) {
			ErrorHandler.error("Error saving preferences", e);
		}
	}
}
