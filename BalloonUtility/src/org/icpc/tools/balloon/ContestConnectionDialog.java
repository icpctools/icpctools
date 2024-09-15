package org.icpc.tools.balloon;

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
import org.eclipse.swt.widgets.DirectoryDialog;
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
import org.icpc.tools.contest.model.feed.DiskContestSource;
import org.icpc.tools.contest.model.feed.RESTContestSource;

public class ContestConnectionDialog extends Dialog {
	private static Preferences prefs = BalloonUtility.getPreferences().node("connect");
	private static final String RECENT_SEP = "^";

	private static final String PREF_RECENT = "recent";
	private static final String PREF_URL = "rest.url";
	private static final String PREF_USER = "rest.user";
	private static final String PREF_PASSWORD = "rest.password";
	private static final String PREF_FOLDER = "disk.folder";
	private static final String PREF_FILE = "disk.file";

	enum ConnectMethod {
		RECENT, CONTEST_API, CONTEST_PACKAGE, EVENT_FEED
	}

	protected ConnectMethod method = ConnectMethod.CONTEST_API;

	protected boolean hasRecent;

	protected String[] recent;

	protected String url;
	protected String user;
	protected String password;

	protected String folder;

	protected String file;

	protected Button connect;
	protected int rc;

	public ContestConnectionDialog(Shell parent) {
		super(parent);
	}

	public int open() {
		Shell parent = getParent();
		Shell s = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		s.setText("Contest Connection");
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
		if (method == ConnectMethod.CONTEST_API)
			return new RESTContestSource(url, user, password);
		else if (method == ConnectMethod.CONTEST_PACKAGE)
			return new DiskContestSource(folder);
		else if (method == ConnectMethod.EVENT_FEED)
			return new DiskContestSource(file);
		else if (method == ConnectMethod.RECENT) {
			String[] vals = recent;
			if ("REST".equals(vals[1]))
				return new RESTContestSource(vals[2], vals[3], vals[4]);
			else if ("FOLDER".equals(vals[1]))
				return new DiskContestSource(vals[2]);
			else if ("DISK".equals(vals[1]))
				return new DiskContestSource(vals[2]);
		}
		return null;
	}

	private String getContestSourceString() {
		if (method == ConnectMethod.RECENT)
			return String.join("`", recent);
		else if (method == ConnectMethod.CONTEST_API)
			return user + " @ " + url + "`REST`" + url + "`" + user + "`" + password;
		else if (method == ConnectMethod.CONTEST_PACKAGE)
			return file + "`FOLDER`" + file;
		else if (method == ConnectMethod.EVENT_FEED)
			return file + "`DISK`" + file;
		return null;
	}

	protected void validate() {
		boolean valid = true;
		if (method == ConnectMethod.RECENT) {
			if (recent == null)
				valid = false;
		} else if (method == ConnectMethod.CONTEST_API) {
			if (url == null || url.isEmpty())
				valid = false;
		} else if (method == ConnectMethod.CONTEST_PACKAGE) {
			if (folder == null || folder.length() < 5)
				valid = false;
		} else if (method == ConnectMethod.EVENT_FEED) {
			if (file == null || file.length() < 5)
				valid = false;
		}
		if (connect != null)
			connect.setEnabled(valid);
	}

	protected Composite createRecentTab(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		composite.setLayout(layout);

		Label label = new Label(composite, SWT.NONE);
		label.setText("Load a previous contest.");
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		label.setLayoutData(data);

		Table recentTable = new Table(composite,
				SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = 400;
		data.heightHint = 100;
		data.verticalSpan = 5;
		data.verticalIndent = 10;
		recentTable.setLayoutData(data);
		recentTable.setHeaderVisible(false);
		recentTable.setLinesVisible(false);

		TableColumn column = new TableColumn(recentTable, SWT.NONE);
		column.setWidth(400);
		column.setAlignment(SWT.LEFT);

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
				if (recentTable.getSelectionCount() == 0)
					recent = null;
				else
					recent = (String[]) recentTable.getSelection()[0].getData();
				validate();
			}
		});

		if (hasRecent)
			recentTable.setSelection(0);

		return composite;
	}

	protected Composite createContestAPITab(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		composite.setLayout(layout);

		Label label = new Label(composite, SWT.NONE);
		label.setText("Connect to a remote Contest API.");
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		label.setLayoutData(data);

		Label urlLabel = new Label(composite, SWT.NONE);
		urlLabel.setText("&URL:");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.verticalIndent = 10;
		urlLabel.setLayoutData(data);

		Text urlText = new Text(composite, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 250;
		data.verticalIndent = 10;
		urlText.setLayoutData(data);
		urlText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				url = urlText.getText();
				validate();
			}
		});
		urlText.setText(prefs.get(PREF_URL, "https://cds/api/contests"));

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
		userText.setText(prefs.get(PREF_USER, "balloon"));

		Label passLabel = new Label(composite, SWT.NONE);
		passLabel.setText("Pa&ssword:");
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

	protected Composite createEventFeedTab(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		composite.setLayout(layout);

		Label label = new Label(composite, SWT.NONE);
		label.setText("Read contest data from a standalone event feed.");
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalSpan = 3;
		label.setLayoutData(data);

		Label fileLabel = new Label(composite, SWT.NONE);
		fileLabel.setText("&File:");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.verticalIndent = 10;
		fileLabel.setLayoutData(data);

		Text fileText = new Text(composite, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.verticalIndent = 10;
		fileText.setLayoutData(data);
		fileText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				file = fileText.getText();
				validate();
			}
		});
		fileText.setText(prefs.get(PREF_FILE, "event-feed.ndjson"));

		Button fileBrowse = new Button(composite, SWT.PUSH);
		fileBrowse.setText("&Browse...");
		data = new GridData(SWT.END, SWT.CENTER, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(fileBrowse);
		data.verticalIndent = 10;
		fileBrowse.setLayoutData(data);
		fileBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				FileDialog dialog = new FileDialog(fileBrowse.getShell(), SWT.OPEN);
				dialog.setFileName(fileText.getText());
				dialog.setText("Select JSON event feed");
				dialog.setFilterExtensions(new String[] { "*.ndjson;*.json", "*.*" });
				String f = dialog.open();
				if (f != null) {
					fileText.setText(f);
				}
			}
		});

		return composite;
	}

	protected Composite createContestPackageTab(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginWidth = 15;
		layout.marginHeight = 15;
		composite.setLayout(layout);

		Label label = new Label(composite, SWT.NONE);
		label.setText("Read contest data from a local contest package.");
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalSpan = 3;
		label.setLayoutData(data);

		Label fileLabel = new Label(composite, SWT.NONE);
		fileLabel.setText("Pa&th:");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.verticalIndent = 10;
		fileLabel.setLayoutData(data);

		Text folderText = new Text(composite, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.verticalIndent = 10;
		folderText.setLayoutData(data);
		folderText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				folder = folderText.getText();
				validate();
			}
		});
		folderText.setText(prefs.get(PREF_FOLDER, ""));

		Button folderBrowse = new Button(composite, SWT.PUSH);
		folderBrowse.setText("&Browse...");
		data = new GridData(SWT.END, SWT.CENTER, false, false);
		data.widthHint = SWTUtil.getButtonWidthHint(folderBrowse);
		data.verticalIndent = 10;
		folderBrowse.setLayoutData(data);
		folderBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog dialog = new DirectoryDialog(folderBrowse.getShell());
				dialog.setText("Select Contest Package folder");
				String f = dialog.open();
				if (f != null) {
					folderText.setText(f);
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
		label.setText("Select the method and details to load a contest.");
		GridData data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		data.horizontalSpan = 2;
		label.setLayoutData(data);

		TabFolder tabFolder = new TabFolder(connectGroup, SWT.NONE);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = 2;
		data.verticalIndent = 8;
		tabFolder.setLayoutData(data);

		TabItem recentTab = new TabItem(tabFolder, SWT.NONE);
		recentTab.setText("Re&cent");
		recentTab.setToolTipText("Recent contests");
		recentTab.setControl(createRecentTab(tabFolder));
		recentTab.setData(ConnectMethod.RECENT);

		TabItem contestAPITab = new TabItem(tabFolder, SWT.NONE);
		contestAPITab.setText("Contest &API");
		contestAPITab.setToolTipText("Connect to a remote Contest API");
		contestAPITab.setControl(createContestAPITab(tabFolder));
		contestAPITab.setData(ConnectMethod.CONTEST_API);

		TabItem contestPackageTab = new TabItem(tabFolder, SWT.NONE);
		contestPackageTab.setText("Contest &Package");
		contestPackageTab.setToolTipText("Load a local Contest Package");
		contestPackageTab.setControl(createContestPackageTab(tabFolder));
		contestPackageTab.setData(ConnectMethod.CONTEST_PACKAGE);

		TabItem eventFeedTab = new TabItem(tabFolder, SWT.NONE);
		eventFeedTab.setText("Event &Feed");
		eventFeedTab.setToolTipText("Load a local event feed");
		eventFeedTab.setControl(createEventFeedTab(tabFolder));
		eventFeedTab.setData(ConnectMethod.EVENT_FEED);

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
		connect.setText("&Load");
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
			tabFolder.setSelection(contestAPITab);

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
		} catch (Exception e) {
			e.printStackTrace();
			// ignore
		}

		prefs.put(PREF_URL, url);
		prefs.put(PREF_USER, user);
		prefs.put(PREF_PASSWORD, password);
		prefs.put(PREF_FOLDER, folder);
		prefs.put(PREF_FILE, file);
		try {
			prefs.sync();
		} catch (Exception e) {
			ErrorHandler.error("Error saving preferences", e);
		}
	}
}
