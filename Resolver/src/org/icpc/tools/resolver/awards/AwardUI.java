package org.icpc.tools.resolver.awards;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.ContestUtil;
import org.icpc.tools.contest.model.IAward;
import org.icpc.tools.contest.model.IAward.AwardType;
import org.icpc.tools.contest.model.IContest;
import org.icpc.tools.contest.model.IContestListener.Delta;
import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.IGroup;
import org.icpc.tools.contest.model.IInfo;
import org.icpc.tools.contest.model.IStanding;
import org.icpc.tools.contest.model.ITeam;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.internal.Contest;
import org.icpc.tools.contest.model.util.AwardUtil;

public class AwardUI {
	private static final String PREF_ID = "org.icpc.tools.award";
	private static final Collator collator = Collator.getInstance(Locale.US);

	public static final int SORT_RANK = 0;
	public static final int SORT_TEAM_ID = 1;
	public static final int SORT_TEAM = 2;
	public static final int SORT_GROUP = 3;
	public static final int SORT_SOLVED = 4;
	public static final int SORT_PENALTY = 5;
	public static final int SORT_AWARDS = 6;

	private static Preferences awardPrefs = new PropertiesPreferences(PREF_ID);

	protected Thread readThread;
	protected Contest contest;
	protected ITeam[] teams;

	protected Label contestNameLabel;
	protected Label contestStartLabel;
	protected Label contestLengthLabel;
	protected Label contestFreezeLabel;

	protected Table teamTable;
	protected int sortColumn;
	protected boolean sortIncreasing;
	protected boolean modified = false;

	protected Button rankAwards;
	protected Button medalAwards;
	protected Button groupAwards;
	protected Button groupHighlightAwards;
	protected Button ftsAwards;
	protected Button finalsAwards;
	protected Button save;

	private Map<String, List<IAward>> awards;

	protected AwardUI(final Shell shell) {
		shell.setLayout(new GridLayout());

		Composite comp = new Composite(shell, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		comp.setLayoutData(data);

		comp.setLayout(new GridLayout(3, false));

		createUI(comp);

		shell.pack();
		shell.setSize(950, 650);

		shell.addListener(SWT.Close, (Event event) -> {
			if (!modified) {
				event.doit = true;
				return;
			}

			MessageBox dialog = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
			dialog.setText(shell.getText());
			dialog.setMessage("There are unsaved awards. Are you sure you want to exit?");
			event.doit = (dialog.open() == SWT.YES);
		});
	}

	protected void createUI(final Composite parent) {
		createContestInfoUI(parent);

		createAwardUI(parent);

		createOperationsUI(parent);

		createTeamUI(parent);
	}

	private void createContestInfoUI(final Composite parent) {
		Group contestGroup = new Group(parent, SWT.NONE);
		contestGroup.setText("Contest Information");
		contestGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		GridLayout layout = new GridLayout(2, false);
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
		label.setText("Freeze duration:");
		label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		contestFreezeLabel = new Label(contestGroup, SWT.NONE);
		contestFreezeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	private void createAwardUI(final Composite parent) {
		Group awardGroup = new Group(parent, SWT.NONE);
		awardGroup.setText("Add Awards");
		awardGroup.setLayoutData(new GridData(SWT.END, SWT.FILL, false, false));

		GridLayout layout = new GridLayout(2, true);
		awardGroup.setLayout(layout);

		final Shell shell = awardGroup.getShell();

		rankAwards = SWTUtil.createButton(awardGroup, "Rank...");
		rankAwards.setEnabled(false);
		rankAwards.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				launchAwardDialog(new RankAwardDialog(shell, contest));
			}
		});

		medalAwards = SWTUtil.createButton(awardGroup, "Medal...");
		medalAwards.setEnabled(false);
		medalAwards.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				launchAwardDialog(new MedalAwardDialog(shell, contest));
			}
		});

		groupAwards = SWTUtil.createButton(awardGroup, "Group...");
		groupAwards.setEnabled(false);
		groupAwards.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				launchAwardDialog(new GroupAwardDialog(shell, contest));
			}
		});

		groupHighlightAwards = SWTUtil.createButton(awardGroup, "Group Highlight...");
		groupHighlightAwards.setEnabled(false);
		groupHighlightAwards.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				launchAwardDialog(new GroupHighlightAwardDialog(shell, contest));
			}
		});

		ftsAwards = SWTUtil.createButton(awardGroup, "  First to Solve...  ");
		ftsAwards.setEnabled(false);
		ftsAwards.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				launchAwardDialog(new FirstToSolveAwardDialog(shell, contest));
			}
		});

		finalsAwards = SWTUtil.createButton(awardGroup, "World Finals...");
		finalsAwards.setEnabled(false);
		finalsAwards.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				launchAwardDialog(new WorldFinalsAwardDialog(shell, contest));
			}
		});
	}

	protected void launchAwardDialog(AbstractAwardDialog dialog) {
		if (verifyNoAwardsOfType(dialog.getAwardTypes()) && dialog.open() == 0) {
			updateTable();
			modified = true;
		}
	}

	private void createOperationsUI(final Composite parent) {
		Group opsGroup = new Group(parent, SWT.NONE);
		opsGroup.setText("Operations");
		opsGroup.setLayoutData(new GridData(SWT.END, SWT.FILL, false, false));

		GridLayout layout = new GridLayout(1, true);
		opsGroup.setLayout(layout);

		save = SWTUtil.createButton(opsGroup, "Save...");
		save.setEnabled(false);
		save.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				FileDialog dialog = new FileDialog(parent.getShell(), SWT.SAVE);
				dialog.setFilterExtensions(new String[] { "*.json", "*.xml" });
				String name = dialog.open();
				if (name != null) {
					File f = new File(name);
					if (!f.exists() || promptToOverwrite(f)) {
						try {
							Awards.save(f, contest);
							modified = false;
							Trace.trace(Trace.INFO, "Event feed saved to: " + f.getAbsolutePath());
						} catch (Exception e) {
							Trace.trace(Trace.ERROR, "Could not save", e);
						}
					} else
						Trace.trace(Trace.INFO, "Save cancelled");
				}
			}
		});
	}

	protected boolean promptToOverwrite(File f) {
		Shell shell = teamTable.getShell();
		MessageBox dialog = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
		dialog.setText(shell.getText());
		dialog.setMessage(f.getName() + " already exists and will be overwritten. Do you want to continue?");
		return (dialog.open() == SWT.OK);
	}

	protected boolean verifyNoAwardsOfType(AwardType[] types) {
		if (types == null)
			return true;

		boolean found = false;
		for (IAward aw : contest.getAwards()) {
			for (AwardType type : types) {
				if (type.equals(aw.getAwardType())) {
					found = true;
				}
			}
		}

		if (!found)
			return true;

		ExistingAwardDialog dialog = new ExistingAwardDialog(teamTable.getShell(), contest, types);
		if (dialog.open() == 0)
			return true;
		return false;
	}

	protected void connectToContest(final ContestSource contestSource) {
		try {
			final ProgressDialog dialog = new ProgressDialog(contestNameLabel.getShell(), "Loading event feed...");
			final Shell progressShell = dialog.open();

			if (readThread != null)
				readThread.interrupt();

			readThread = new Thread() {
				@Override
				public void run() {
					runAsync(() -> {
						contestNameLabel.setText("Connecting...");
					});

					contest = contestSource.loadContest((IContest contest2, IContestObject obj, Delta d) -> {
						if (obj instanceof IInfo) {
							runAsync(() -> {
								String name = contest2.getName();
								if (name != null)
									contestNameLabel.setText(name);
								else
									contestNameLabel.setText("unknown");
								contestStartLabel.setText(ContestUtil.formatStartTime(contest2));
								contestLengthLabel.setText(ContestUtil.formatDuration(contest2.getDuration()));
								contestFreezeLabel.setText(ContestUtil.formatDuration(contest2.getFreezeDuration()));
							});
						}
					});

					contestSource.waitForContest(10000);
					if (!contest.isDoneUpdating())
						ErrorHandler.error("Warning: contest not done updating!");

					cacheAwards();

					runAsync(() -> {
						teamTable.setSortColumn(null);

						teams = contest.getOrderedTeams();
						for (ITeam team : teams) {
							TableItem ti = new TableItem(teamTable, SWT.NONE);
							updateTableItem(ti, team);
						}

						rankAwards.setEnabled(true);
						medalAwards.setEnabled(true);
						groupAwards.setEnabled(true);
						groupHighlightAwards.setEnabled(true);
						ftsAwards.setEnabled(true);
						finalsAwards.setEnabled(true);

						save.setEnabled(true);

						progressShell.dispose();
					});
				}
			};
			readThread.setDaemon(true);
			readThread.start();
		} catch (Exception e) {
			ErrorHandler.error("Error connecting to event feed", e);
		}
	}

	private void cacheAwards() {
		// cache all the awards by team id
		IAward[] contestAwards = contest.getAwards();
		AwardUtil.sortAwards(contest, contestAwards);
		awards = new HashMap<>();
		for (IAward award : contestAwards) {
			String[] teamIds = award.getTeamIds();
			if (teamIds != null) {
				for (String teamId : teamIds) {
					List<IAward> aw = awards.get(teamId);
					if (aw == null) {
						aw = new ArrayList<>();
						awards.put(teamId, aw);
					}
					aw.add(award);
				}
			}
		}
	}

	private void sortTeams(final int col, boolean increasing) {
		int in = 1;
		if (increasing)
			in = -1;
		final int inc = in;
		Comparator<ITeam> comparator = (ITeam t1, ITeam t2) -> {
			IStanding s1 = contest.getStanding(t1);
			IStanding s2 = contest.getStanding(t2);
			if (col == SORT_RANK) {
				try {
					int r1 = Integer.parseInt(s1.getRank());
					int r2 = Integer.parseInt(s2.getRank());
					if (r1 > r2)
						return inc;
					if (r1 < r2)
						return -inc;
					return 0;
				} catch (Exception e) {
					// ignore and fall back to string sorting
					return s1.getRank().compareTo(s2.getRank()) * inc;
				}
			} else if (col == SORT_TEAM_ID) {
				try {
					Integer in1 = Integer.parseInt(t1.getId());
					Integer in2 = Integer.parseInt(t2.getId());
					return in1.compareTo(in2) * inc;
				} catch (Exception e) {
					// ignore
				}
				return t1.getId().compareTo(t2.getId()) * inc;
			} else if (col == SORT_TEAM) {
				return collator.compare(t1.getActualDisplayName(), t2.getActualDisplayName()) * inc;
			} else if (col == SORT_GROUP) {
				String g1 = getGroupLabel(t1);
				String g2 = getGroupLabel(t2);
				if (g1 == null && g2 == null)
					return 0;
				if (g1 == null)
					return inc;
				if (g2 == null)
					return -inc;
				return collator.compare(g1, g2) * inc;
			} else if (col == SORT_SOLVED) {
				if (s1.getNumSolved() > s2.getNumSolved())
					return -inc;
				if (s1.getNumSolved() < s2.getNumSolved())
					return inc;
				return 0;
			} else if (col == SORT_PENALTY) {
				if (s1.getTime() > s2.getTime())
					return -inc;
				if (s1.getTime() < s2.getTime())
					return inc;
				return 0;
			} else if (col == SORT_AWARDS) {
				String a1 = getAwardString(t1);
				String a2 = getAwardString(t2);
				return -collator.compare(a1, a2) * inc;
			}
			return 0;
		};

		int size = teams.length;
		for (int i = 0; i < size - 1; i++) {
			for (int j = i + 1; j < size; j++) {
				ITeam t1 = teams[i];
				ITeam t2 = teams[j];
				if (comparator.compare(t1, t2) < 0) {
					teams[i] = t2;
					teams[j] = t1;
				}
			}
		}
	}

	private void sort(int i) {
		if (sortColumn != i) {
			sortColumn = i;
			sortIncreasing = true;
		} else
			sortIncreasing = !sortIncreasing;

		synchronized (teamTable) {
			TableColumn c = teamTable.getColumn(i);
			teamTable.setSortColumn(c);
			teamTable.setSortDirection(sortIncreasing ? SWT.DOWN : SWT.UP);

			sortTeams(i, sortIncreasing);

			TableItem[] items = teamTable.getItems();
			int j = 0;
			for (TableItem ti : items)
				updateTableItem(ti, teams[j++]);
		}
	}

	protected void updateTable() {
		cacheAwards();

		for (TableItem ti : teamTable.getItems()) {
			updateTableItem(ti, (ITeam) ti.getData());
		}
	}

	protected String getAwardString(ITeam team) {
		List<IAward> list = awards.get(team.getId());
		if (list == null || list.isEmpty())
			return "";

		List<AwardType> types = new ArrayList<>();
		for (IAward a : list) {
			AwardType at = a.getAwardType();
			types.add(at);
		}

		return AwardUtil.getAwardTypeNames(types);
	}

	private String getGroupLabel(ITeam team) {
		IGroup[] groups = contest.getGroupsByIds(team.getGroupIds());
		if (groups == null || groups.length == 0)
			return "";

		String groupName = "";
		boolean first = true;
		for (IGroup g : groups) {
			if (!first)
				groupName += ", ";
			if (g != null)
				groupName += g.getName();
			first = false;
		}
		return groupName;
	}

	protected void updateTableItem(TableItem ti, ITeam team) {
		synchronized (teamTable) {
			ti.setData(team);
			IStanding standing = contest.getStanding(team);
			String awardStr = getAwardString(team);
			String groupName = getGroupLabel(team);
			ti.setText(new String[] { standing.getRank(), team.getId(), team.getActualDisplayName(), groupName,
					standing.getNumSolved() + "", standing.getTime() + "", awardStr });
		}
	}

	protected void createColumn(String name, int width, int align, final int col) {
		TableColumn column = new TableColumn(teamTable, SWT.NONE);
		column.setText(name);
		column.pack();
		if (column.getWidth() < width)
			column.setWidth(width);
		column.setAlignment(align);
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				sort(col);
			}
		});
	}

	private void createTeamUI(final Composite parent) {
		Group problemGroup = new Group(parent, SWT.NONE);
		problemGroup.setText("Teams");
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = 3;
		problemGroup.setLayoutData(data);

		problemGroup.setLayout(new GridLayout(3, false));

		teamTable = new Table(problemGroup, SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = 750;
		data.verticalSpan = 5;
		data.horizontalSpan = 2;
		teamTable.setLayoutData(data);
		teamTable.setHeaderVisible(true);
		teamTable.setLinesVisible(true);

		createColumn("Rank", 45, SWT.RIGHT, SORT_RANK);
		createColumn("Id", 45, SWT.RIGHT, SORT_TEAM_ID);
		createColumn("Team", 250, SWT.LEFT, SORT_TEAM);
		createColumn("Groups", 150, SWT.LEFT, SORT_GROUP);
		createColumn("Solved", 45, SWT.RIGHT, SORT_SOLVED);
		createColumn("Penalty", 45, SWT.RIGHT, SORT_PENALTY);
		createColumn("Awards", 175, SWT.LEFT, SORT_AWARDS);

		final Button edit = SWTUtil.createButton(problemGroup, "Edit...");
		edit.setEnabled(false);
		edit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				TableItem[] tis = teamTable.getSelection();
				if (tis == null || tis.length != 1)
					return;

				ITeam team = (ITeam) tis[0].getData();
				EditAwardsDialog dialog = new EditAwardsDialog(edit.getShell(), contest, team);
				if (dialog.open() == 0)
					updateTable();
			}
		});

		teamTable.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				TableItem[] tis = teamTable.getSelection();
				edit.setEnabled(tis != null && tis.length == 1);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				TableItem[] tis = teamTable.getSelection();
				if (tis == null || tis.length != 1)
					return;

				ITeam team = (ITeam) tis[0].getData();
				EditAwardsDialog dialog = new EditAwardsDialog(edit.getShell(), contest, team);
				if (dialog.open() == 0)
					updateTable();
			}
		});
	}

	protected void runAsync(final Runnable r) {
		if (teamTable.isDisposed())
			return;

		teamTable.getDisplay().asyncExec(() -> {
			if (teamTable.isDisposed())
				return;

			r.run();
		});
	}

	protected static Preferences getPreferences() {
		return awardPrefs;
	}

	private static void showHelp() {
		System.out.println();
		System.out.println("ICPC Award Utility");
		System.out.println();
		System.out.println("Usage: awardUtil");
		System.out.println("     --version");
		System.out.println("         Displays version information");
		System.out.println();
	}

	public static void main(String[] args) {
		if (args != null && args.length > 0) {
			if (args.length == 1 && "--help".equals(args[0])) {
				showHelp();
				System.exit(0);
			}
			Trace.trace(Trace.ERROR, "This tool does not support any command line options.");
			System.exit(1);
		}

		Display.setAppName("Award Utility");
		Display display = new Display();

		final Shell shell = new Shell(display);
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

		shell.setText("Award Utility");
		Image image = new Image(display, AwardUI.class.getResourceAsStream("/images/resolverIcon.png"));
		shell.setImage(image);
		ErrorHandler.setShell(shell);

		ContestSource source = null;
		try {
			ContestConnectionDialog cfd = new ContestConnectionDialog(shell);
			if (cfd.open() != 0)
				return;

			source = cfd.getContestSource();
		} catch (Exception e) {
			ErrorHandler.error("Error connecting to contest", e);
		}

		AwardUI ui = new AwardUI(shell);
		ui.connectToContest(source);
		ui.toString();

		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		image.dispose();
	}

	private static void showAbout(Shell shell) {
		MessageBox dialog = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
		dialog.setText("About " + shell.getText());
		Package pack = AwardUI.class.getPackage();
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