package org.icpc.tools.balloon;

import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.icpc.tools.client.core.BasicClient;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IInfo;
import org.icpc.tools.contest.model.IJudgement;
import org.icpc.tools.contest.model.IJudgementType;
import org.icpc.tools.contest.model.IProblem;
import org.icpc.tools.contest.model.ISubmission;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.ContestSource.ConnectionState;
import org.icpc.tools.contest.model.feed.ContestSource.ContestSourceListener;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.ArgumentParser;
import org.icpc.tools.contest.model.util.ArgumentParser.OptionParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class BalloonUtility {
	private static final String PREF_ID = "org.icpc.tools.balloon";
	private static final String PREF_AUTOPRINT = "autoPrint";
	private static final String PREF_FILTER = "filter";
	private static final String PREF_PRINTER = "printer";
	private static final String PREF_MESSAGES = "messages";
	private static final String PREF_MESSAGES_SEPARATOR = "###";

	private static final SimpleDateFormat SDF = new SimpleDateFormat();
	private static final NumberFormat nf = NumberFormat.getInstance();

	private static Preferences balloonPrefs = new PropertiesPreferences(PREF_ID);

	private String[] messages = null;

	static {
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);
	}

	protected BalloonContest bc = new BalloonContest();

	protected Label contestNameLabel;
	protected Label contestStartLabel;
	protected Label contestLengthLabel;
	protected Label contestTimeLabel;
	protected Label selectionLabel;
	protected Label connectionLabel;

	protected boolean autoPrint = false;
	protected Button autoPrintButton;

	private static PrinterData printerData;
	protected Link filterLink;
	protected List<String> filter; // null = all, empty = none
	protected BalloonPrinter balloonPrinter = new BalloonPrinter();
	protected MenuItem printSummary;

	protected Table balloonTable;
	protected int sortColumn;
	protected boolean sortIncreasing;
	protected ContestSource contestSource;

	protected Label updateLabel;

	protected BalloonUtility() {
		// do nothing
	}

	protected void createUI(Shell shell) {
		createMenu(shell);

		shell.setLayout(new GridLayout());

		Composite comp = new Composite(shell, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		comp.setLayoutData(data);

		comp.setLayout(new GridLayout(2, true));

		createUI(comp);

		shell.pack();
		shell.setSize(1000, 650);

		String messagesPref = getPreferences().get(PREF_MESSAGES, null);
		if (messagesPref != null)
			messages = messagesPref.split(PREF_MESSAGES_SEPARATOR);
		if (messages == null || messages.length != 4)
			messages = BalloonPrinter.DEFAULT_MESSAGES.clone();
	}

	protected void createUI(final Composite parent) {
		createContestInfoUI(parent);

		createBalloonUI(parent);

		KeyAdapter keyListener = new KeyAdapter() {
			protected StringBuilder sb = new StringBuilder();

			@Override
			public void keyPressed(KeyEvent event) {
				updateLabel.setText("");
				char c = event.character;
				if (c != '\n' && c != '\r') {
					try {
						Integer.parseInt(c + "");
						sb.append(c);
					} catch (NumberFormatException nfe) {
						// ignore
						sb = new StringBuilder();
					}

					return;
				}

				String s = sb.toString();
				try {
					int n = Integer.parseInt(s);
					if (n > 1000000)
						n = Integer.parseInt(s.substring(0, s.length() - 1)) - 100000;

					boolean found = false;
					TableItem[] tis = balloonTable.getItems();
					for (TableItem ti : tis) {
						Balloon b = (Balloon) ti.getData();
						if (b.getId() == n) {
							boolean d = b.isDelivered();
							if (!d) {
								b.setDelivered(true);
								bc.save();
								updateTableItem(ti, b);
								updateLabel.setText("Delivered: " + n);
							} else
								updateLabel.setText("Redelivered: " + n);
							found = true;
						}
					}

					if (!found)
						updateLabel.setText("Not found: " + s);
				} catch (Exception e) {
					updateLabel.setText("Error: \"" + s + "\"");
				}
				sb = new StringBuilder();
			}
		};

		recursivelyAddKeyListener(parent, keyListener);
	}

	protected void recursivelyAddKeyListener(Composite comp, KeyListener keyListener) {
		comp.addKeyListener(keyListener);
		Control[] children = comp.getChildren();
		for (Control c : children) {
			if (c instanceof Composite) {
				recursivelyAddKeyListener((Composite) c, keyListener);
			} else {
				c.addKeyListener(keyListener);
			}
		}
	}

	private void createBalloonUI(final Composite parent) {
		Group problemGroup = new Group(parent, SWT.NONE);
		problemGroup.setText("Balloons");
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = 2;
		problemGroup.setLayoutData(data);

		problemGroup.setLayout(new GridLayout(3, false));

		String filterList = getPreferences().get(PREF_FILTER, null);
		if (filterList != null) {
			filter = new ArrayList<>();
			StringTokenizer st = new StringTokenizer(filterList, "|");
			while (st.hasMoreTokens()) {
				filter.add(st.nextToken());
			}
		}

		Label label = new Label(problemGroup, SWT.NONE);
		label.setText("Filter by group:");
		label.setLayoutData(new GridData(GridData.BEGINNING, SWT.CENTER, false, false));

		filterLink = new Link(problemGroup, SWT.NONE);
		updateFilterLabel();
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.horizontalSpan = 2;
		filterLink.setLayoutData(data);

		filterLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				filterLink.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						createFilterPopup();
					}
				});
			}
		});

		balloonTable = new Table(problemGroup, SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = 900;
		data.heightHint = 500;
		data.verticalSpan = 5;
		data.horizontalSpan = 2;
		balloonTable.setLayoutData(data);
		balloonTable.setHeaderVisible(true);
		balloonTable.setLinesVisible(true);

		createColumn("#", 45, SWT.RIGHT, 0);
		createColumn("Sub.", 45, SWT.RIGHT, 1);
		createColumn("Problem", 100, SWT.LEFT, 2);
		createColumn("Team", 55, SWT.RIGHT, 3);
		createColumn("University", 300, SWT.LEFT, 4);
		createColumn("Groups", 100, SWT.LEFT, 5);
		createColumn("Time", 60, SWT.LEFT, 6);
		createColumn("Firsts", 60, SWT.LEFT, 7);
		createColumn("Printed", 70, SWT.LEFT, 8);
		createColumn("Delivered", 70, SWT.LEFT, 9);

		final Button preview = SWTUtil.createButton(problemGroup, "Print Preview...");
		preview.setEnabled(false);
		((GridData) preview.getLayoutData()).horizontalAlignment = SWT.FILL;
		preview.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				TableItem[] tis = balloonTable.getSelection();
				if (tis == null || tis.length == 0)
					return;

				for (TableItem ti : tis) {
					Balloon b = (Balloon) ti.getData();
					try {
						BalloonPrinter previewPrinter = new BalloonPrinter();
						previewPrinter.printPreview(preview.getShell(), bc, b, messages);
					} catch (Exception e) {
						ErrorHandler.error("Error in print preview", e);
					}
				}
			}
		});

		final Button print = SWTUtil.createButton(problemGroup, "Print");
		print.setEnabled(false);
		((GridData) print.getLayoutData()).horizontalAlignment = SWT.FILL;
		print.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				TableItem[] tis = balloonTable.getSelection();
				if (tis == null || tis.length == 0)
					return;

				for (TableItem ti : tis) {
					Balloon b = (Balloon) ti.getData();
					print(b);
				}
			}
		});

		final Button deliver = SWTUtil.createButton(problemGroup, "   Toggle Delivery   ");
		((GridData) deliver.getLayoutData()).horizontalAlignment = SWT.FILL;
		deliver.setEnabled(false);
		deliver.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				TableItem[] tis = balloonTable.getSelection();
				if (tis == null || tis.length == 0)
					return;

				for (TableItem ti : tis) {
					Balloon b = (Balloon) ti.getData();
					b.setDelivered(!b.isDelivered());
					updateTableItem(ti, b);
				}

				bc.save();
			}
		});

		updateLabel = new Label(problemGroup, SWT.CENTER);
		updateLabel.setLayoutData(new GridData(SWT.FILL, SWT.END, false, false));

		final Composite comp = new Composite(problemGroup, SWT.NONE);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.horizontalSpan = 2;
		comp.setLayoutData(data);

		GridLayout layout = new GridLayout(5, false);
		layout.marginHeight = 0;
		comp.setLayout(layout);

		autoPrintButton = new Button(comp, SWT.CHECK);
		autoPrintButton.setText("Automatically print new balloons");
		data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		autoPrint = getPreferences().getBoolean(PREF_AUTOPRINT, true);
		autoPrintButton.setSelection(autoPrint);
		autoPrintButton.setLayoutData(data);
		autoPrintButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				autoPrint = autoPrintButton.getSelection();
				Preferences prefs = getPreferences();
				prefs.putBoolean(PREF_AUTOPRINT, autoPrint);
				try {
					prefs.sync();
				} catch (Exception e) {
					ErrorHandler.error("Error saving preferences", e);
				}
			}
		});

		selectionLabel = new Label(comp, SWT.NONE);
		selectionLabel.setAlignment(SWT.RIGHT);
		selectionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		connectionLabel = new Label(problemGroup, SWT.NONE);
		connectionLabel.setAlignment(SWT.CENTER);
		connectionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		balloonTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				TableItem[] tis = balloonTable.getSelection();
				boolean enabled = tis != null && tis.length > 0;
				preview.setEnabled(enabled);
				print.setEnabled(enabled && (printerData != null));
				deliver.setEnabled(enabled);
				printSummary.setEnabled(enabled && (printerData != null));
				updateSelectionLabel();
			}
		});
		updateSelectionLabel();
	}

	protected void createColumn(String name, int width, int align, final int col) {
		TableColumn column = new TableColumn(balloonTable, SWT.NONE);
		column.setText(name);
		column.setWidth(width);
		column.setAlignment(align);
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				sort(col);
			}
		});
	}

	protected void updateSelectionLabel() {
		StringBuilder sb = new StringBuilder();
		int sel = balloonTable.getSelectionCount();
		if (sel < 0)
			sel = 0;
		sb.append(sel);
		sb.append(" of ");
		sb.append(balloonTable.getItemCount());
		sb.append(" balloons");
		selectionLabel.setText(sb.toString());
	}

	protected void print(Balloon b) {
		if (printerData == null)
			return;

		try {
			balloonPrinter.print(printerData, bc, b, messages);
			if (!b.isPrinted()) {
				b.setPrinted(true);
				bc.save();
			}
			updateBalloon(b);
		} catch (Exception e) {
			ErrorHandler.error("Error printing balloon", e);
		}
	}

	protected boolean isRunFromGroup(Balloon b) {
		if (filter == null)
			return true;

		if (filter.isEmpty())
			return false;

		IContest contest = bc.getContest();
		if (contest == null)
			return true;

		String submisssionId = b.getSubmissionId();
		ISubmission submisssion = contest.getSubmissionById(submisssionId);
		if (submisssion == null)
			return true;

		String teamId = submisssion.getTeamId();
		ITeam team = contest.getTeamById(teamId);
		if (team == null)
			return true;

		for (String f : filter) {
			IGroup[] groups = contest.getGroupsByIds(team.getGroupIds());
			if (groups != null) {
				for (IGroup group : groups)
					if (group.getName().equals(f))
						return true;
			}
		}

		return false;
	}

	protected void createBalloon(final ISubmission submission) {
		Balloon b = new Balloon(submission.getId(), "x");
		bc.add(b);

		if (isRunFromGroup(b)) {
			// search for existing table item first
			TableItem ti = null;
			TableItem[] items = balloonTable.getItems();
			for (TableItem ti2 : items) {
				if (b.equals(ti2.getData())) {
					ti = ti2;
				}
			}

			if (ti == null)
				ti = new TableItem(balloonTable, SWT.NONE);
			updateTableItem(ti, b);
			updateSelectionLabel();

			// print & update flags
			if (autoPrint && b.getFlags() >= 0)
				print(b);

			bc.updateFlags(new BalloonContest.FlagListener() {
				@Override
				public void updatedFlags(Balloon bb) {
					updateBalloon(bb);
					if (autoPrint && isRunFromGroup(bb))
						print(bb);
				}
			});
		}
		bc.save();
	}

	protected void updateOnSolvedJudgement(IContest contest, final IJudgement sj) {
		IJudgementType jt = contest.getJudgementTypeById(sj.getJudgementTypeId());
		if (jt == null || !jt.isSolved())
			return;

		ISubmission submission = contest.getSubmissionById(sj.getSubmissionId());
		if (submission != null) {
			ITeam team = contest.getTeamById(submission.getTeamId());
			if (team != null && contest.isTeamHidden(team))
				return;
		}

		// check if balloon has already been awarded for this team/problem
		if (bc.balloonAlreadyExists(submission)) {
			balloonTable.getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					if (balloonTable.isDisposed())
						return;

					final Balloon b = bc.getBalloon(submission.getId());
					if (b != null)
						updateBalloon(b);
				}
			});
			return;
		}

		IContest c = bc.getContest();
		if (c.getFreezeDuration() >= 0 && submission.getContestTime() > (c.getDuration() - c.getFreezeDuration())) {
			if (bc.getNumBalloons(submission.getTeamId()) > 2) {
				return;
			}
		}

		if (balloonTable.isDisposed())
			return;

		balloonTable.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				if (balloonTable.isDisposed())
					return;
				balloonTable.setSortColumn(null);

				final Balloon b = bc.getBalloon(submission.getId());
				if (b != null)
					updateBalloon(b);
				else
					createBalloon(submission);
			}
		});
	}

	protected void updateBalloon(Balloon b) {
		TableItem[] tis = balloonTable.getItems();
		for (TableItem ti : tis) {
			Balloon ob = (Balloon) ti.getData();
			if (ob.equals(b)) {
				updateTableItem(ti, b);
				return;
			}
		}
	}

	private static String getLabelColor(IProblem p) {
		if (p == null)
			return "";
		if (p.getColor() == null)
			return p.getLabel();
		return p.getLabel() + " (" + p.getColor() + ")";
	}

	private static String getGroupLabel(IContest contest, ITeam team) {
		IGroup[] groups = contest.getGroupsByIds(team.getGroupIds());
		if (groups == null || groups.length == 0)
			return "";

		String groupName = "";
		boolean first = true;
		for (IGroup g : groups) {
			if (!first)
				groupName += ", ";
			groupName += g.getName();
			first = false;
		}
		return groupName;
	}

	protected void updateTableItem(TableItem ti, Balloon b) {
		synchronized (balloonTable) {
			// #, problem, team, time, status
			ti.setData(b);
			String[] text = new String[] { (b.getId()) + "", b.getSubmissionId() + "", "", "", "", "", "", b.getStatus(),
					b.isPrinted() ? "Y" : "", b.isDelivered() ? "Y" : "" };
			IContest contest = bc.getContest();
			if (contest != null) {
				ISubmission submission = contest.getSubmissionById(b.getSubmissionId());
				if (submission != null) {
					IProblem problem = contest.getProblemById(submission.getProblemId());
					String tId = submission.getTeamId();
					text[2] = getLabelColor(problem);
					text[3] = tId;

					ITeam team = contest.getTeamById(tId);
					if (team != null) {
						text[4] = team.getActualDisplayName();
						text[5] = getGroupLabel(contest, team);
					}
					text[6] = ContestUtil.getTime(submission.getContestTime());
				}
			}
			ti.setText(text);
		}
	}

	private void sort(int i) {
		if (sortColumn != i) {
			sortColumn = i;
			sortIncreasing = true;
		} else
			sortIncreasing = !sortIncreasing;

		synchronized (balloonTable) {
			TableColumn c = balloonTable.getColumn(sortColumn);
			balloonTable.setSortColumn(c);
			balloonTable.setSortDirection(sortIncreasing ? SWT.DOWN : SWT.UP);

			bc.sort(sortColumn, sortIncreasing);

			Balloon[] bi = bc.getBalloons();
			TableItem[] items = balloonTable.getItems();
			int j = 0;
			for (TableItem ti : items)
				updateTableItem(ti, bi[j++]);
		}
	}

	private void createContestInfoUI(final Composite parent) {
		Group contestGroup = new Group(parent, SWT.NONE);
		contestGroup.setText("Contest Information");
		GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		data.horizontalSpan = 2;
		contestGroup.setLayoutData(data);

		GridLayout layout = new GridLayout(4, false);
		contestGroup.setLayout(layout);

		Label label = new Label(contestGroup, SWT.NONE);
		label.setText("Name:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		contestNameLabel = new Label(contestGroup, SWT.NONE);
		contestNameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		label = new Label(contestGroup, SWT.NONE);
		label.setText("Start time:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		contestStartLabel = new Label(contestGroup, SWT.NONE);
		contestStartLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		label = new Label(contestGroup, SWT.NONE);
		label.setText("Duration:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		contestLengthLabel = new Label(contestGroup, SWT.NONE);
		contestLengthLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		label = new Label(contestGroup, SWT.NONE);
		label.setText("Contest time:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		contestTimeLabel = new Label(contestGroup, SWT.NONE);
		contestTimeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Thread t = new Thread("Time thread") {
			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (Exception e) {
					// ignore
				}
				while (true && !contestTimeLabel.isDisposed()) {
					contestTimeLabel.getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							try {
								contestTimeLabel.setText(getContestTime(bc.getContest()));
							} catch (Exception e) {
								// ignore
							}
						}
					});

					try {
						Thread.sleep(300);
					} catch (Exception e) {
						// ignore
					}
				}
			}
		};
		t.setDaemon(true);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	protected void connectToContest() {
		boolean[] promptedForLoad = new boolean[] { false };

		runAsync(() -> {
			balloonTable.setSortColumn(null);
		});

		IContestListener listener = (contest, e, d) -> {
			if (e instanceof IJudgement) {
				IJudgement sj = (IJudgement) e;
				List<String> list = sj.validate(contest);
				if (list != null)
					Trace.trace(Trace.WARNING,
							"Submission judgement " + sj.getId() + " is not valid. Run \"eventFeed --validate\" for details");

				updateOnSolvedJudgement(contest, sj);
			} else if (e instanceof IInfo) {
				if (!promptedForLoad[0] && BalloonFileUtil.saveFileExists(contest)) {
					promptedForLoad[0] = true;
					balloonTable.getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							Shell shell = balloonTable.getShell();
							MessageBox dialog = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
							dialog.setText(shell.getText());
							dialog.setMessage(
									"Balloon data was found from the last time you connected to this contest. Do you want to reload it?");
							if (dialog.open() != SWT.YES)
								return;

							bc.load();
							refreshTable();
						}
					});
				}

				runAsync(() -> {
					String name = contest.getName();
					if (name != null)
						contestNameLabel.setText(name);
					else
						contestNameLabel.setText("unknown");
					contestStartLabel.setText(ContestUtil.formatStartTime(contest));
					contestLengthLabel.setText(ContestUtil.formatDuration(contest.getDuration()));
				});
			}
		};

		final Color RED = connectionLabel.getDisplay().getSystemColor(SWT.COLOR_RED);
		final Color BLACK = connectionLabel.getForeground();
		contestSource.addListener(new ContestSourceListener() {
			@Override
			public void stateChanged(final ConnectionState state) {
				runAsync(() -> {
					if (ConnectionState.FAILED == state || ConnectionState.RECONNECTING == state)
						connectionLabel.setForeground(RED);
					else
						connectionLabel.setForeground(BLACK);
					connectionLabel.setText(ContestSource.getStateLabel(state));
				});
			}
		});

		Contest contest = contestSource.loadContest(listener);
		bc.setContest(contest);

		if (contestSource instanceof RESTContestSource) {
			RESTContestSource restContestSource = (RESTContestSource) contestSource;

			if (restContestSource.isCDS()) {
				BasicClient client = new BasicClient(restContestSource, "Balloon");
				client.connect(true);
			}
		}
		/*runAsync(new Runnable() {
			@Override
			public void run() {
				balloonTable.setEnabled(true);
			}
		});*/
	}

	private void updateFilterPref() {
		Preferences prefs = getPreferences();
		if (filter == null) {
			prefs.remove(PREF_FILTER);
		} else {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (String s : filter) {
				if (!first) {
					sb.append("|");
				}
				sb.append(s);

				first = false;
			}
			prefs.put(PREF_FILTER, sb.toString());
		}

		try {
			prefs.sync();
		} catch (Exception e) {
			ErrorHandler.error("Error saving preferences", e);
		}

		updateFilterLabel();
		refreshTable();
	}

	private void updateFilterLabel() {
		String label = "?";
		if (filter == null)
			label = "All";
		else if (filter.isEmpty())
			label = "None";
		else if (filter.size() == 1)
			label = filter.get(0);
		else if (filter.size() > 1)
			label = filter.size() + " groups";
		filterLink.setText("<a>" + label + "...</a>");
	}

	private void changeGroup(String name, boolean add) {
		if (!add) {
			if (filter == null) {
				filter = new ArrayList<>();
				IContest contest = bc.getContest();
				if (contest != null) {
					for (IGroup g : contest.getGroups()) {
						filter.add(g.getName());
					}
				}
			}
			filter.remove(name);
		} else {
			if (filter == null)
				filter = new ArrayList<>();
			filter.add(name);
		}
		updateFilterPref();
	}

	private void createFilterPopup() {
		final Shell shell = new Shell(filterLink.getShell(), SWT.TOOL | SWT.NO_TRIM | SWT.ON_TOP | SWT.BORDER);

		GridLayout layout = new GridLayout(2, true);
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		shell.setLayout(layout);

		// add known groups
		List<String> found = new ArrayList<>();
		IContest contest = bc.getContest();
		if (contest != null) {
			for (IGroup g : contest.getGroups()) {
				final Button button = new Button(shell, SWT.CHECK);
				final String name = g.getName();
				button.setText(name);
				button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));
				found.add(name);

				if (filter == null || filter.contains(name))
					button.setSelection(true);

				button.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent event) {
						changeGroup(name, button.getSelection());
					}
				});
			}
		}

		// add previously selected groups
		if (filter != null) {
			for (final String name : filter) {
				if (!found.contains(name)) {
					final Button button = new Button(shell, SWT.CHECK);
					button.setText(name);
					button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));
					button.setSelection(true);
					button.addSelectionListener(new SelectionAdapter() {

						@Override
						public void widgetSelected(SelectionEvent event) {
							changeGroup(name, button.getSelection());
						}
					});
				}
			}
		}

		if (shell.getChildren().length == 0) {
			Label label = new Label(shell, SWT.NONE);
			label.setText("No groups found");
			label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false, 2, 1));
		} else {
			Button none = new Button(shell, SWT.PUSH);
			none.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
			none.setText("Select None");
			none.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					shell.close();
					filter = new ArrayList<>();
					updateFilterPref();
				}
			});

			Button all = new Button(shell, SWT.PUSH);
			all.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
			all.setText("Select All");
			all.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					shell.close();
					filter = null;
					updateFilterPref();
				}
			});
		}

		shell.pack();

		Point p = filterLink.getParent().toDisplay(filterLink.getLocation());
		p.y += filterLink.getSize().y;

		final Display display = shell.getDisplay();
		Rectangle r = display.getBounds();
		if (p.y + shell.getSize().y > r.y + r.height)
			p.y = Math.max(r.y, r.y + r.height - shell.getSize().y);

		shell.setLocation(p);
		shell.setVisible(true);
		shell.setFocus();

		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellDeactivated(ShellEvent event) {
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						if (!shell.isDisposed()) {
							shell.close();
						}
					}
				});
			}
		});

		while (!shell.isDisposed() && shell.isVisible()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		if (!shell.isDisposed())
			shell.dispose();
	}

	protected static Document readDocument(InputStream in) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder parser = factory.newDocumentBuilder();
			return parser.parse(new InputSource(in));
		} catch (Exception e) {
			// ignore
		} finally {
			try {
				in.close();
			} catch (Exception e) {
				// ignore
			}
		}
		return null;
	}

	protected static void showHelp() {
		System.out.println("Usage: balloon.bat/sh [options]");
		System.out.println("   or: balloon.bat/sh contestURL user password [options]");
		System.out.println("   or: balloon.bat/sh contestPath [options]");
		System.out.println();
		System.out.println("  Options:");
		System.out.println("     --clean");
		System.out.println("         Removes all saved balloon data");
		System.out.println("     --help");
		System.out.println("         Shows this message");
		System.out.println("     --version");
		System.out.println("         Displays version information");
	}

	public static void main(String[] args) {
		Trace.init("ICPC Balloon Utility", "balloon", args);

		ContestSource contestSource = ArgumentParser.parse(args, new OptionParser() {
			@Override
			public boolean setOption(String option, List<Object> options) throws IllegalArgumentException {
				if ("--clean".equals(option)) {
					BalloonFileUtil.cleanAll();
					return true;
				}
				return false;
			}

			@Override
			public void showHelp() {
				BalloonUtility.showHelp();
			}
		});

		Display.setAppName("Balloon Utility");
		Display display = new Display();
		final Shell shell = new Shell(display);
		if (contestSource == null) {
			try {
				ContestConnectionDialog cfd = new ContestConnectionDialog(shell);
				if (cfd.open() != 0)
					return;

				contestSource = cfd.getContestSource();
			} catch (Exception e) {
				ErrorHandler.error("Error connecting to contest", e);
			}
		}

		contestSource.outputValidation();
		if (contestSource instanceof RESTContestSource) {
			RESTContestSource restSource = (RESTContestSource) contestSource;
			if (restSource.isCDS())
				restSource.checkForUpdates("balloonUtil-");
		}

		BalloonUtility ui = new BalloonUtility();
		ui.contestSource = contestSource;

		Menu sysMenu = display.getSystemMenu();
		if (sysMenu != null) {
			for (MenuItem m : sysMenu.getItems()) {
				if (m.getID() == SWT.ID_ABOUT) {
					m.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							showAbout(shell);
						}
					});
				}
			}
		}
		shell.setText("Balloon Utility");
		Image image = new Image(display, BalloonUtility.class.getResourceAsStream("/images/balloonIcon.png"));
		shell.setImage(image);
		ErrorHandler.setShell(shell);

		String printerName = getPreferences().get(PREF_PRINTER, null);
		if (printerName != null) {
			try {
				PrinterData[] printers = Printer.getPrinterList();
				if (printers != null) {
					for (PrinterData printer : printers) {
						if (printerName.equals(printer.name)) {
							printerData = printer;
							continue;
						}
					}
				}
			} catch (Exception e) {
				ErrorHandler.error("Could not reconnect to printer", e);
			}
		}

		ui.createUI(shell);
		ui.toString();

		shell.pack();
		shell.open();

		ui.connectToContest();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		image.dispose();
	}

	protected static Preferences getPreferences() {
		return balloonPrefs;
	}

	protected void refreshTable() {
		balloonTable.removeAll();

		for (Balloon b : bc.getBalloons()) {
			if (isRunFromGroup(b)) {
				TableItem ti = new TableItem(balloonTable, SWT.NONE);
				updateTableItem(ti, b);
			}
		}
		updateSelectionLabel();
	}

	private void createMenu(final Shell shell) {
		Menu m = new Menu(shell, SWT.BAR);
		shell.setMenuBar(m);

		// print menu
		MenuItem printMenu = new MenuItem(m, SWT.CASCADE);
		printMenu.setText("&Print");
		Menu submenu = new Menu(shell, SWT.DROP_DOWN);
		printMenu.setMenu(submenu);

		MenuItem configurePrint = new MenuItem(submenu, SWT.PUSH);
		configurePrint.setText("&Configure Printer...");

		MenuItem separator = new MenuItem(submenu, SWT.SEPARATOR);
		separator.setData(null);

		final MenuItem printTest = new MenuItem(submenu, SWT.PUSH);
		printTest.setText("Print &Test Page");
		printTest.setEnabled(printerData != null);

		final MenuItem printSample = new MenuItem(submenu, SWT.PUSH);
		printSample.setText("Print &Sample Balloon");
		printSample.setEnabled(printerData != null);

		final MenuItem previewSample = new MenuItem(submenu, SWT.PUSH);
		previewSample.setText("Pre&view Sample Balloon");

		// reports menu
		MenuItem reportsMenu = new MenuItem(m, SWT.CASCADE);
		reportsMenu.setText("&Reports");
		submenu = new Menu(shell, SWT.DROP_DOWN);
		reportsMenu.setMenu(submenu);

		printSummary = new MenuItem(submenu, SWT.PUSH);
		printSummary.setText("Print &Selected Balloon Summary");
		printSummary.setEnabled(printerData != null);
		printSummary.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				TableItem[] tis = balloonTable.getSelection();
				if (tis == null || tis.length == 0)
					return;

				List<Balloon> list = new ArrayList<>();
				for (TableItem ti : tis)
					list.add((Balloon) ti.getData());

				Printer printer = new Printer(printerData);
				SummaryPrint sp = new SummaryPrint(printer);
				try {
					sp.print(bc, list);
				} catch (Exception e) {
					ErrorHandler.error("Error printing summary", e);
				}
				printer.dispose();
			}
		});

		final MenuItem printFullSummary = new MenuItem(submenu, SWT.PUSH);
		printFullSummary.setText("Print &Balloon Summary");
		printFullSummary.setEnabled(printerData != null);
		printFullSummary.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				List<Balloon> list = new ArrayList<>();
				for (TableItem ti : balloonTable.getItems())
					list.add((Balloon) ti.getData());

				Printer printer = new Printer(printerData);
				SummaryPrint sp = new SummaryPrint(printer);
				try {
					sp.print(bc, list);
				} catch (Exception e) {
					ErrorHandler.error("Error printing summary", e);
				}
				printer.dispose();
			}
		});

		final MenuItem printTeamSummary = new MenuItem(submenu, SWT.PUSH);
		printTeamSummary.setText("Print &Team Summary");
		printTeamSummary.setEnabled(printerData != null);
		printTeamSummary.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				Printer printer = new Printer(printerData);
				TeamSummaryPrint sp = new TeamSummaryPrint(printer);
				try {
					sp.print(bc.getContest());
				} catch (Exception e) {
					ErrorHandler.error("Error printing team summary", e);
				}
				printer.dispose();
			}
		});

		configurePrint.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				PrintDialog printDialog = new PrintDialog(shell);
				printerData = printDialog.open();
				if (printerData != null) {
					printTest.setEnabled(true);
					printFullSummary.setEnabled(true);
					printTeamSummary.setEnabled(true);
					printSample.setEnabled(true);

					try {
						Preferences prefs = getPreferences();
						prefs.put(PREF_PRINTER, printerData.name);
						prefs.sync();
					} catch (Exception e) {
						ErrorHandler.error("Error saving preferences", e);
					}
				}
			}
		});

		printTest.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (printerData == null)
					return;

				Printer printer = new Printer(printerData);
				if (!printer.startJob("Printer Test"))
					return;
				if (!printer.startPage())
					return;

				GC gc = new GC(printer);
				Rectangle r = printer.getClientArea();

				FontMetrics fm = gc.getFontMetrics();
				gc.drawText("Test print @ " + new Date().toString(), r.x, r.y + fm.getAscent());

				gc.dispose();
				printer.endPage();
				printer.endJob();
				printer.dispose();
			}
		});

		printSample.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (printerData == null)
					return;

				try {
					balloonPrinter.print(printerData, null, null, messages);
				} catch (Exception e) {
					ErrorHandler.error("Error printing sample", e);
				}
			}
		});

		previewSample.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					BalloonPrinter previewPrinter = new BalloonPrinter();
					previewPrinter.printPreview(shell, null, null, messages);
				} catch (Exception e) {
					ErrorHandler.error("Error printing sample", e);
				}
			}
		});

		// preferences menu
		MenuItem prefMenu = new MenuItem(m, SWT.CASCADE);
		prefMenu.setText("P&references");

		submenu = new Menu(shell, SWT.DROP_DOWN);
		prefMenu.setMenu(submenu);

		final MenuItem deliveryMenu = new MenuItem(submenu, SWT.PUSH);
		deliveryMenu.setText("&Delivery Instructions...");

		deliveryMenu.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					DeliveryInstructionDialog dialog = new DeliveryInstructionDialog(shell, messages.clone());
					if (dialog.open() == 0) {
						messages = dialog.getMessages();
						try {
							Preferences prefs = getPreferences();
							prefs.put(PREF_MESSAGES, messages[0] + PREF_MESSAGES_SEPARATOR + messages[1]
									+ PREF_MESSAGES_SEPARATOR + messages[2] + PREF_MESSAGES_SEPARATOR + messages[3]);
							prefs.sync();
						} catch (Exception e) {
							ErrorHandler.error("Error saving delivery preferences", e);
						}
					}
				} catch (Exception e) {
					ErrorHandler.error("Error opening delivery instruction dialog", e);
				}
			}
		});
	}

	protected static String getDateString() {
		return SDF.format(new Date());
	}

	protected void runAsync(final Runnable r) {
		if (balloonTable.isDisposed())
			return;

		balloonTable.getDisplay().asyncExec(() -> {
			if (balloonTable.isDisposed())
				return;

			r.run();
		});
	}

	/**
	 * Format contest time as a string.
	 *
	 * @param time contest time, in seconds
	 * @return
	 */
	protected String getContestTime(IContest contest) {
		if (contest == null)
			return "";

		Long startTime = contest.getStartStatus();
		if (startTime == null)
			return "";

		long time = startTime.longValue();
		if (time < 0)
			return getTime(-time) + " (paused)";

		time = System.currentTimeMillis() - time;
		if (time < 0)
			return "-" + getTime(-time);
		if (time > contest.getDuration())
			return "contest over";
		return getTime(time) + " (unofficial)";
	}

	private static String getTime(long s2) {
		int ss = (int) Math.floor(s2 / 1000.0);
		int m = (ss / 60) % 60;
		int h = (ss / 3600) % 48;
		int s = (ss % 60);

		StringBuilder sb = new StringBuilder();
		sb.append(h + ":");
		if (m < 10)
			sb.append("0");
		sb.append(m + ":");
		if (s < 10)
			sb.append("0");
		sb.append(Math.abs(s));

		return sb.toString();
	}

	private static void showAbout(Shell shell) {
		MessageBox dialog = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
		dialog.setText("About " + shell.getText());
		Package pack = BalloonUtility.class.getPackage();
		dialog.setMessage(shell.getText() + " version " + getVersion(pack.getSpecificationVersion()) + " (build "
				+ getVersion(pack.getImplementationVersion()) + ")");
		dialog.open();
	}

	private static String getVersion(String ver) {
		if (ver == null)
			return "dev";
		return ver;
	}
}