package org.icpc.tools.presentation.admin.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.icpc.tools.client.core.BasicClient;
import org.icpc.tools.client.core.IConnectionListener;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.internal.NetworkUtil;
import org.icpc.tools.presentation.core.internal.PresentationInfo;
import org.icpc.tools.presentation.core.internal.PresentationsParser;

public class View {
	private static final String PREF_ID = "org.icpc.tools.presentation.admin";
	private static final String PREF_DISPLAY_AREA = "displayArea";
	private static Preferences prefs = new PropertiesPreferences(PREF_ID);

	private PresentationInfoListControl presentationList;
	private List<PresentationInfo> presentations = new ArrayList<>();
	private List<PresentationInfo> transitions = new ArrayList<>();
	private List<CompositePresentationInfo> composites = new ArrayList<>();
	protected ClientsControl clientsControl;
	protected Label clientDetailLabel;
	protected Label statusLabel;
	protected ProgressBar progress;
	protected boolean requestedPresentations;
	protected Combo propCombo;

	protected BasicClient client;

	protected Map<Widget, AdminAction> actions = new HashMap<>();

	protected abstract class AdminAction {
		public int[] getSelectedClients() {
			return clientsControl.getSelection();
		}

		public PresentationInfo getSelectedPresentation() {
			return presentationList.getSelection();
		}

		public boolean isEnabled() {
			return true;
		}

		public abstract void run() throws Exception;
	}

	protected abstract class RemoteAction extends AdminAction {
		// no content - tag class
	}

	protected abstract class ClientAction extends RemoteAction {
		@Override
		public boolean isEnabled() {
			return getSelectedClients() != null && getSelectedClients().length > 0;
		}
	}

	public View(RESTContestSource source) {
		String s = source.getUser() + NetworkUtil.getLocalAddress();

		client = new BasicClient(source, source.getUser(), s.hashCode(), "admin", "pres-admin") {
			/**
			 * @throws IOException
			 */
			@Override
			protected void handlePresentationList(JsonObject obj) throws IOException {
				File f = File.createTempFile("presentAdmin", "zip");
				f.deleteOnExit();
				FileOutputStream out2 = new FileOutputStream(f);
				byte[] b = decodeFile(obj);
				out2.write(b);
				out2.close();

				PresentationsParser parser = PresentationHelper.loadPresentation(f);
				View.this.addPresentations(parser.getPresentations());
				View.this.addTransitions(parser.getTransitions());
			}

			@Override
			protected void clientsChanged(Client[] clients) {
				if (clientsControl == null || clientsControl.isDisposed())
					return;

				clientsControl.setClients(clients);
				clientsControl.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						updateActions();
					}
				});
			}

			@Override
			protected void handleThumbnail(JsonObject obj) {
				if (clientsControl == null || clientsControl.isDisposed())
					return;
				int sourceUID = getUID(obj, "source");
				int fps = obj.getInt("fps");
				boolean hidden = obj.getBoolean("hidden");
				byte[] b = decodeImage(obj);
				clientsControl.handleThumbnail(sourceUID, b, fps, hidden);
			}

			@Override
			protected void handleInfo(JsonObject obj) {
				if (clientsControl == null || clientsControl.isDisposed())
					return;
				int sourceUID = getUID(obj, "source");
				clientsControl.handleInfo(sourceUID, obj);
			}

			@Override
			protected void handleLogResponse(JsonObject obj) throws IOException {
				int sourceUID = getUID(obj, "source");
				clientsControl.handleLog(sourceUID, obj.getString("data"));
			}

			@Override
			protected void handleSnapshotResponse(JsonObject obj) throws IOException {
				int sourceUID = getUID(obj, "source");
				byte[] b = decodeImage(obj);
				clientsControl.handleSnapshot(sourceUID, b);
			}
		};

		client.addListener(new IConnectionListener() {
			@Override
			public void connectionStateChanged(final boolean connected) {
				if (presentationList.isDisposed())
					return;

				presentationList.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (connected)
							setStatus("Connected");
						else
							setStatus("Disconnected");
					}
				});

				if (!connected || requestedPresentations)
					return;

				try {
					client.requestPresentationList();
					requestedPresentations = true;
				} catch (IOException e) {
					Trace.trace(Trace.ERROR, "Error sending jar", e);
				}
			}
		});
	}

	protected void connect() {
		client.connect(true);
	}

	protected void addPresentations(final List<PresentationInfo> list) {
		for (PresentationInfo info : list)
			presentations.add(info);

		if (presentationList == null) {
			Trace.trace(Trace.ERROR, "No presentation list");
			return;
		}

		presentationList.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (clientsControl == null || clientsControl.isDisposed())
					return;

				for (PresentationInfo info : list) {
					presentationList.add(info, false);
				}
			}
		});
	}

	protected void addTransitions(final List<PresentationInfo> list) {
		for (PresentationInfo info : list) {
			transitions.add(info);
		}
	}

	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 5;
		layout.marginHeight = 5;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 5;
		parent.setLayout(layout);

		SashForm hSash = new SashForm(parent, SWT.HORIZONTAL);
		hSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite leftComp = new Composite(hSash, SWT.NONE);

		GridLayout layout2 = new GridLayout();
		layout2.numColumns = 2;
		layout2.marginWidth = 0;
		layout2.marginHeight = 0;
		leftComp.setLayout(layout2);

		createClientSection(leftComp);

		statusLabel = new Label(leftComp, SWT.NONE);
		statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.END, true, false));
		statusLabel.setText("Connecting...");

		progress = new ProgressBar(leftComp, SWT.HORIZONTAL | SWT.INDETERMINATE);
		progress.setLayoutData(new GridData(SWT.END, SWT.END, false, false));
		progress.setVisible(false);

		createPresentationSection(hSash);

		hSash.setWeights(new int[] { 50, 35 });

		try {
			composites = PresentationListIO.load();
			for (CompositePresentationInfo list : composites)
				presentationList.add(list);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error loading custom plans", e);
		}

		createMenu(parent.getShell());
		clientsControl.setFocus();
	}

	protected void writeProperty(String key, String value) throws Exception {
		client.sendProperty(clientsControl.getSelection(), key, value);
	}

	protected void createClientSection(Composite parent) {
		Group clientComp = new Group(parent, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = 2;
		clientComp.setLayoutData(data);
		clientComp.setText("Clients");

		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		clientComp.setLayout(layout);

		clientsControl = new ClientsControl(clientComp, SWT.BORDER);
		clientsControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		clientsControl.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateActions();
			}
		});
		clientsControl.setDropListener(new ClientsControl.IDropListener() {
			@Override
			public void drop(final int clientUID, final PresentationInfo info) {
				executeAction(new RemoteAction() {
					@Override
					public void run() throws Exception {
						client.sendProperty(new int[] { clientUID }, "presentation", "1100|" + info.getClassName());
						// if (propCombo.getText().length() > 0)
						// writeProperty(info.getClassName(), propCombo.getText());
					}
				});
			}
		});

		client.addListener(new IConnectionListener() {
			@Override
			public void connectionStateChanged(boolean connected) {
				if (clientsControl == null || clientsControl.isDisposed())
					return;

				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (clientsControl == null || clientsControl.isDisposed())
							return;

						clientsControl.redraw();
					}
				});
			}
		});

		clientDetailLabel = new Label(clientComp, SWT.NONE);
		clientDetailLabel.setAlignment(SWT.RIGHT);
		clientDetailLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		final Composite buttonComp = new Composite(clientComp, SWT.NONE);
		data = new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.VERTICAL_ALIGN_END);
		buttonComp.setLayoutData(data);

		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 4;
		layout.makeColumnsEqualWidth = true;
		buttonComp.setLayout(layout);

		registerAction(createButton(buttonComp, "Show", "Make presentations visible on all selected clients"),
				new ClientAction() {
					@Override
					public void run() throws Exception {
						writeProperty("hidden", "");
					}
				});

		registerAction(createButton(buttonComp, "Hide", "Hide presentations (go black) on all selected clients"),
				new ClientAction() {
					@Override
					public void run() throws Exception {
						writeProperty("hidden", "true");
					}
				});

		registerAction(createButton(buttonComp, "Select All", "Select all clients"), new AdminAction() {
			@Override
			public void run() {
				clientsControl.selectAll();
			}
		});

		registerAction(createButton(buttonComp, "Deselect All", "Clear selection from all clients"), new AdminAction() {
			@Override
			public void run() {
				clientsControl.deselectAll();
			}
		});

		registerAction(createButton(buttonComp, "Snapshot", "Download and display a snapshot from the selected clients"),
				new ClientAction() {
					@Override
					public void run() throws Exception {
						client.sendSnapshotRequest(getSelectedClients());
					}
				});

		createFullScreenControl(buttonComp);

		registerAction(createButton(buttonComp, "Stop", "Terminate the presentation process on the selected clients"),
				new ClientAction() {
					@Override
					public void run() throws Exception {
						MessageBox mb = new MessageBox(buttonComp.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
						mb.setText("Presentation Admin");
						mb.setMessage("Are you sure you want to close the client(s)?");
						if (mb.open() == SWT.OK)
							client.sendStop(getSelectedClients());
					}
				});

		registerAction(createButton(buttonComp, "Restart", "Restart the presentation process on the selected clients"),
				new ClientAction() {
					@Override
					public void run() throws Exception {
						MessageBox mb = new MessageBox(buttonComp.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
						mb.setText("Presentation Admin");
						mb.setMessage("Are you sure you want to restart the client(s)?");
						if (mb.open() == SWT.OK)
							client.sendRestart(getSelectedClients());
					}
				});
	}

	private static Button createButton(Composite parent, String text, String tooltip) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setToolTipText(tooltip);
		button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		return button;
	}

	private void updateActions() {
		updateActions(false);
	}

	private void updateActions(boolean isRemoteAction) {
		clientDetailLabel.setText(clientsControl.getSelectionDetail());

		for (Widget w : actions.keySet()) {
			AdminAction action = actions.get(w);
			boolean enabled;

			// disable all remote actions while this one is running
			if (isRemoteAction && action instanceof RemoteAction)
				enabled = false;
			else
				enabled = action.isEnabled();

			if (w instanceof Button) {
				((Button) w).setEnabled(enabled);
			} else if (w instanceof MenuItem) {
				((MenuItem) w).setEnabled(enabled);
			} else if (w instanceof Combo) {
				((Combo) w).setEnabled(enabled);
			} else if (w instanceof Label) {
				((Label) w).setEnabled(enabled);
			}
		}
	}

	private void executeAction(final AdminAction action) {
		if (!(action instanceof RemoteAction)) {
			try {
				action.run();
				setStatus("Action successful");
			} catch (final Exception ex) {
				Trace.trace(Trace.ERROR, "Error running action", ex);
				setStatus("Error executing action, check logs");

				MessageBox mb = new MessageBox(progress.getShell(), SWT.ICON_ERROR | SWT.OK);
				mb.setText("Presentation Admin");
				mb.setMessage("Error: " + ex.getMessage());
				mb.open();
			}
			updateActions(false);
			return;
		}

		// disable all remote actions
		setStatus("Working...");
		progress.setVisible(true);
		updateActions(true);

		Thread t = new Thread("Remote action") {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
				} catch (final Exception ex) {
					// ignore
				}
				progress.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						try {
							action.run();
							setStatus("Action successful");
						} catch (final Exception ex) {
							Trace.trace(Trace.ERROR, "Error running action", ex);
							setStatus("Error executing action, check logs");

							MessageBox mb = new MessageBox(progress.getShell(), SWT.ICON_ERROR | SWT.OK);
							mb.setText("Presentation Admin");
							mb.setMessage("Error: " + ex.getMessage());
							mb.open();
						}

						progress.setVisible(false);
						updateActions(false);
					}
				});
			}
		};
		t.setDaemon(true);
		t.start();
	}

	protected void setStatus(final String s) {
		if (statusLabel == null || statusLabel.isDisposed())
			return;

		statusLabel.setText(s);

		Thread t = new Thread("Update status") {
			@Override
			public void run() {
				try {
					Thread.sleep(3000);
				} catch (final Exception ex) {
					// ignore
				}
				progress.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (statusLabel == null || statusLabel.isDisposed())
							return;

						if (s.equals(statusLabel.getText()))
							statusLabel.setText("");
					}
				});
			}
		};
		t.setDaemon(true);
		t.start();
	}

	private void registerAction(Widget w, final AdminAction action) {
		registerAction(w, action, null);
	}

	private void registerAction(Widget w, final AdminAction action, Control ct) {
		actions.put(w, action);
		if (ct != null)
			actions.put(ct, action);

		if (w instanceof Button) {
			final Button b = (Button) w;
			b.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					executeAction(action);
				}
			});
			b.setEnabled(action.isEnabled());
		} else if (w instanceof MenuItem) {
			final MenuItem m = (MenuItem) w;
			m.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					executeAction(action);
				}
			});
			m.setEnabled(action.isEnabled());
		} else if (w instanceof Combo) {
			final Combo c = (Combo) w;
			c.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					executeAction(action);
				}
			});
			boolean enabled = action.isEnabled();
			c.setEnabled(enabled);
			if (ct != null)
				ct.setEnabled(enabled);
		}
	}

	private void createFullScreenControl(Composite parent) {
		Composite buttonComp = new Composite(parent, SWT.NONE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		buttonComp.setLayoutData(data);

		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		buttonComp.setLayout(layout);

		final Button button = new Button(buttonComp, SWT.PUSH);
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		button.setText("Enter Full Screen");
		button.setToolTipText("Switch to full screen exclusive mode on the selected clients");
		registerAction(button, new ClientAction() {
			@Override
			public boolean isEnabled() {
				return clientsControl.isSelectionFullScreenAvailable(0);
			}

			@Override
			public void run() throws Exception {
				writeProperty("window", "1");
			}
		});

		Button downButton = new Button(buttonComp, SWT.ARROW | SWT.DOWN);
		downButton.setToolTipText("Full screen exclusive mode options");
		downButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_FILL));
		downButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				Shell shell = button.getShell();
				Menu menu = new Menu(shell, SWT.POP_UP);
				MenuItem item1 = new MenuItem(menu, SWT.PUSH);
				item1.setText("Enter Full Screen on 2nd Display");
				item1.setToolTipText("Switch to full screen exclusive mode on the second display");
				registerAction(item1, new ClientAction() {
					@Override
					public boolean isEnabled() {
						return clientsControl.isSelectionFullScreenAvailable(1);
					}

					@Override
					public void run() throws Exception {
						writeProperty("window", "2");
					}
				});

				MenuItem item3 = new MenuItem(menu, SWT.PUSH);
				item3.setText("Enter Full Screen on 3rd Display");
				item3.setToolTipText("Switch to full screen exclusive mode on the third display");
				registerAction(item3, new ClientAction() {
					@Override
					public boolean isEnabled() {
						return clientsControl.isSelectionFullScreenAvailable(2);
					}

					@Override
					public void run() throws Exception {
						writeProperty("window", "3");
					}
				});

				MenuItem item4 = new MenuItem(menu, SWT.PUSH);
				item4.setText("Maximum resolution");
				item4.setToolTipText("Switch to max resolution on current window");
				registerAction(item4, new ClientAction() {
					@Override
					public boolean isEnabled() {
						return clientsControl.isSelectionFullScreenAvailable(0);
					}

					@Override
					public void run() throws Exception {
						writeProperty("window", "1x");
					}
				});

				Point loc = button.getLocation();
				Rectangle rect = button.getBounds();

				Point mLoc = new Point(loc.x, loc.y + rect.height);

				menu.setLocation(shell.getDisplay().map(button.getParent(), null, mLoc));

				menu.setVisible(true);
			}
		});
	}

	private static String getPresentationKey(String className) {
		int ind = className.lastIndexOf(".");
		return "property[" + className.substring(ind + 1) + "|" + className.hashCode() + "]";
	}

	private void createPresentationSection(Composite parent2) {
		Group parent = new Group(parent2, SWT.NONE);
		parent.setText("Presentations");

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		parent.setLayout(layout);

		presentationList = new PresentationInfoListControl(parent, SWT.BORDER);
		presentationList.showCategories();
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		data.widthHint = 170;
		data.horizontalSpan = 3;
		presentationList.setLayoutData(data);

		Label label = new Label(parent, SWT.NONE);
		label.setText("Properties:");
		data = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(data);

		propCombo = new Combo(parent, SWT.BORDER | SWT.DROP_DOWN);
		propCombo.setToolTipText("Optional properties for the currently selected presentation");
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		propCombo.setLayoutData(data);

		registerAction(propCombo, new AdminAction() {
			@Override
			public boolean isEnabled() {
				return getSelectedPresentation() != null && getSelectedPresentation().getProperties() != null;
			}

			@Override
			public void run() throws Exception {
				String s = propCombo.getText();
				if (s == null || s.length() < 0)
					return;

				int ind = s.indexOf(" - ");
				if (ind > 0) {
					s = s.substring(0, ind);
					propCombo.setText(s);
					propCombo.setSelection(new Point(s.length(), s.length()));
					propCombo.setFocus();
				}
			}
		}, label);
		propCombo.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				final PresentationInfo pw = presentationList.getSelection();
				if (pw == null)
					return;

				if ((event.character == '\n' || event.character == '\r') && propCombo.getText().length() > 0) {
					executeAction(new RemoteAction() {
						@Override
						public void run() throws Exception {
							writeProperty(getPresentationKey(pw.getClassName()), propCombo.getText());
						}
					});
				}
			}
		});

		registerAction(createButton(parent, "Apply",
				"Apply the currently selected presentation to all selected clients immediately"), new RemoteAction() {
					@Override
					public boolean isEnabled() {
						return getSelectedClients() != null && getSelectedClients().length > 0
								&& getSelectedPresentation() != null;
					}

					@Override
					public void run() throws Exception {
						PresentationInfo info = getSelectedPresentation();
						writeProperty("presentation", "1100|" + info.getClassName());
						if (propCombo.getText().length() > 0)
							writeProperty(getPresentationKey(info.getClassName()), propCombo.getText());
					}
				});

		Composite comp = new Composite(parent, SWT.NONE);
		data = new GridData(GridData.CENTER, GridData.CENTER, true, false);
		data.horizontalSpan = 3;
		comp.setLayoutData(data);

		layout = new GridLayout(3, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 0;
		comp.setLayout(layout);

		registerAction(createButton(comp, "New...", "Create a new composite presentation"), new RemoteAction() {
			@Override
			public boolean isEnabled() {
				return !presentations.isEmpty();
			}

			@Override
			public void run() throws Exception {
				CompositePresentationDialog dialog = new CompositePresentationDialog(parent.getShell(), presentations,
						transitions);
				if (dialog.open()) {
					CompositePresentationInfo newInfo = dialog.getPresentation();
					presentationList.add(newInfo);
					composites.add(newInfo);
					try {
						PresentationListIO.save(composites);
					} catch (IOException e) {
						Trace.trace(Trace.ERROR, "Error creating new presentation", e);
					}
				}
			}
		});

		registerAction(createButton(comp, "Edit...", "Edit a composite presentation"), new RemoteAction() {
			@Override
			public boolean isEnabled() {
				return false; // !presentations.isEmpty(); // TODO and custom
			}

			@Override
			public void run() throws Exception {
				CompositePresentationDialog dialog = new CompositePresentationDialog(parent.getShell(), presentations,
						transitions);
				if (dialog.open()) {
					CompositePresentationInfo newInfo = dialog.getPresentation();
					presentationList.add(newInfo);
					composites.add(newInfo);
					try {
						PresentationListIO.save(composites);
					} catch (IOException e) {
						Trace.trace(Trace.ERROR, "Error editing presentation", e);
					}
				}
			}
		});

		registerAction(createButton(comp, "Remove", "Remove a custom presentation"), new RemoteAction() {
			@Override
			public boolean isEnabled() {
				return getSelectedPresentation() instanceof CompositePresentationInfo;
			}

			@Override
			public void run() throws Exception {
				try {
					CompositePresentationInfo info = (CompositePresentationInfo) getSelectedPresentation();
					composites.remove(info);
					presentationList.remove(info);
					PresentationListIO.save(composites);
				} catch (IOException e) {
					Trace.trace(Trace.ERROR, "Error creating new presentation", e);
				}
			}
		});

		presentationList.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				updateActions();
				PresentationInfo info = presentationList.getSelection();

				String[] props = new String[0];
				if (info != null && info.getProperties() != null)
					props = info.getProperties();
				propCombo.setItems(props);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				PresentationInfo info = presentationList.getSelection();
				try {
					writeProperty("presentation", "1100|" + info.getClassName());
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error launching presentation", e);
				}
			}
		});
	}

	private void createMenu(final Shell shell) {
		Menu m = new Menu(shell, SWT.BAR);
		shell.setMenuBar(m);

		// window menu
		MenuItem windowMenu = new MenuItem(m, SWT.CASCADE);
		windowMenu.setText("&Window");

		Menu submenu = new Menu(shell, SWT.DROP_DOWN);
		windowMenu.setMenu(submenu);

		final MenuItem smallThumbsMenu = new MenuItem(submenu, SWT.CHECK);
		smallThumbsMenu.setText("&Small Thumbnails");
		smallThumbsMenu.addSelectionListener(new SelectionAdapter() {
			boolean smallThumbs = true;

			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					clientsControl.setSmallThumbnails(smallThumbs);
					smallThumbs = !smallThumbs;
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Error setting small thumbnails", e);
				}
			}
		});

		// advanced menu
		MenuItem advancedMenu = new MenuItem(m, SWT.CASCADE);
		advancedMenu.setText("&Advanced");

		submenu = new Menu(shell, SWT.DROP_DOWN);
		advancedMenu.setMenu(submenu);

		final MenuItem logMenu = new MenuItem(submenu, SWT.PUSH);
		logMenu.setText("View Client &Log...");
		logMenu.setToolTipText("Download and display the log files from the selected clients");
		registerAction(logMenu, new ClientAction() {
			@Override
			public void run() throws Exception {
				client.sendLogRequest(getSelectedClients());
			}
		});

		final MenuItem resetMenu = new MenuItem(submenu, SWT.PUSH);
		resetMenu.setText("&Reset Settings");
		resetMenu.setToolTipText("Clear settings for the selected clients");
		registerAction(resetMenu, new ClientAction() {
			@Override
			public void run() throws Exception {
				writeProperty(null, null);
			}
		});

		final MenuItem displayAreaMenu = new MenuItem(submenu, SWT.PUSH);
		displayAreaMenu.setText("Custom &Display Area...");
		displayAreaMenu.setToolTipText(
				"Set the visible region of the screen for the selected clients. Useful when part of the client screen is obscured or not visible");
		registerAction(displayAreaMenu, new ClientAction() {
			@Override
			public void run() throws Exception {
				DisplayAreaDialog dialog = new DisplayAreaDialog(shell, prefs.get(PREF_DISPLAY_AREA, ""));
				if (!dialog.open())
					return;

				String rect = dialog.getRect();
				if (rect == null)
					prefs.remove(PREF_DISPLAY_AREA);
				else
					prefs.put(PREF_DISPLAY_AREA, rect);
				try {
					prefs.sync();
				} catch (Exception e) {
					// ignore
				}
				writeProperty("displayRect", rect);
			}
		});

		final MenuItem displayMenu = new MenuItem(submenu, SWT.PUSH);
		displayMenu.setText("Custom &Display...");
		displayMenu.setToolTipText("Set the custom display option for the selected clients.");
		registerAction(displayMenu, new ClientAction() {
			@Override
			public void run() throws Exception {
				DisplayDialog dialog = new DisplayDialog(shell, "1");
				if (!dialog.open())
					return;

				writeProperty("window", dialog.getValue());
			}
		});

		final MenuItem defaultPresMenu = new MenuItem(submenu, SWT.PUSH);
		defaultPresMenu.setText("Set &Default Presentation");
		defaultPresMenu.setToolTipText("Set the default presentation to show for new clients");
		registerAction(defaultPresMenu, new ClientAction() {
			@Override
			public boolean isEnabled() {
				return getSelectedPresentation() != null;
			}

			@Override
			public void run() throws Exception {
				PresentationInfo info = getSelectedPresentation();
				String s = info.getClassName();
				writeProperty("default:presentation", "1000|" + s);
				if (propCombo.getText().length() > 0) {
					writeProperty("default:" + getPresentationKey(info.getClassName()), propCombo.getText());
				}
			}
		});
	}
}