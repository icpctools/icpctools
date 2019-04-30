package org.icpc.tools.contest.util.problemset;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class ProblemSetEditor {
	protected File file;
	protected List<Problem> problems = new ArrayList<>();

	protected Table table;
	protected TableEditor editor;

	protected Button remove;
	protected Button up;
	protected Button down;
	protected Button html;
	protected MenuItem saveMenu;

	protected boolean changed;

	protected ProblemSetEditor(final Shell shell) {
		createMenu(shell);

		shell.setLayout(new GridLayout());

		Composite comp = new Composite(shell, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		comp.setLayoutData(data);

		comp.setLayout(new GridLayout(2, false));

		createUI(comp);

		shell.pack();

		shell.addListener(SWT.Close, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (!changed) {
					event.doit = true;
					return;
				}

				MessageBox dialog = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
				dialog.setText(shell.getText());
				dialog.setMessage("There are unsaved changes. Are you sure you want to exit?");
				event.doit = (dialog.open() == SWT.YES);
			}
		});
	}

	private void createMenu(final Shell shell) {
		Menu m = new Menu(shell, SWT.BAR);
		shell.setMenuBar(m);

		MenuItem fileMenu = new MenuItem(m, SWT.CASCADE);
		fileMenu.setText("&File");

		Menu subMenu = new Menu(shell, SWT.DROP_DOWN);
		fileMenu.setMenu(subMenu);
		MenuItem openMenu = new MenuItem(subMenu, SWT.PUSH);
		openMenu.setText("&Open...");
		openMenu.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (changed) {
					MessageBox dialog = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
					dialog.setText(shell.getText());
					dialog.setMessage("There are unsaved changes. Are you sure you want to proceed?");
					if (dialog.open() == SWT.CANCEL)
						return;
				}

				FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
				fileDialog.setText("Open");
				fileDialog.setFileName("problemset.yaml");
				fileDialog.setFilterExtensions(new String[] { "*.yaml" });
				String filePath = fileDialog.open();
				if (filePath != null) {
					problems.clear();
					table.removeAll();
					setFile(new File(filePath));
				}
			}
		});

		saveMenu = new MenuItem(subMenu, SWT.PUSH);
		saveMenu.setText("&Save");
		saveMenu.setEnabled(false);
		saveMenu.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				save();
			}
		});

		final MenuItem saveAsMenu = new MenuItem(subMenu, SWT.PUSH);
		saveAsMenu.setText("Save &As...");

		saveAsMenu.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);
				fileDialog.setText("Save As");
				fileDialog.setFileName("problemset.yaml");
				fileDialog.setFilterExtensions(new String[] { "*.yaml" });
				String filePath = fileDialog.open();
				if (filePath != null) {
					file = new File(filePath);
					save();
				}
			}
		});
	}

	protected void showError(String s, Exception e) {
		Shell shell = table.getShell();
		MessageBox dialog = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
		dialog.setText(shell.getText());
		if (e != null)
			dialog.setMessage(s + ": " + e.getMessage());
		else
			dialog.setMessage(s);
		dialog.open();
	}

	protected void save() {
		try {
			YamlUtil.writeProblemSet(file, problems);
			changed = false;
		} catch (Exception e) {
			showError("Error saving problem set", e);
		}
		setEnablement();
	}

	protected void setFile(File file) {
		this.file = file;
		if (file != null && file.exists()) {
			try {
				problems = YamlUtil.readProblemSet(file);
				fillTable();
			} catch (Exception e) {
				showError("Could not read problem set", e);
			}
		}
		changed = false;
		setEnablement();
	}

	protected void createColumn(String name, int width, int align) {
		TableColumn column = new TableColumn(table, SWT.NONE);
		column.setText(name);
		column.pack();
		if (column.getWidth() < width)
			column.setWidth(width);
		column.setAlignment(align);
	}

	protected void tableSelected(MouseEvent e, boolean doubleClick) {
		Control oldEditor = editor.getEditor();
		if (oldEditor != null)
			oldEditor.dispose();

		for (TableItem item : table.getItems()) {
			for (int i = 0; i < 5; i++) {
				Rectangle rect = item.getBounds(i);
				if (rect.contains(e.x, e.y)) {
					final int column = i;

					if (column == 4 || (column == 3 && doubleClick)) {
						ColorDialog dialog = new ColorDialog(table.getShell());
						dialog.setText("Select a Color");

						Problem p = (Problem) item.getData();
						RGB rgb = p.getRGBVal();
						if (rgb != null)
							dialog.setRGB(rgb);

						rgb = dialog.open();
						if (rgb != null) {
							StringBuilder sb = new StringBuilder();
							sb.append(Integer.toHexString(rgb.red));
							if (sb.length() == 1)
								sb.insert(0, "0");
							sb.append(Integer.toHexString(rgb.green));
							if (sb.length() == 3)
								sb.insert(2, "0");
							sb.append(Integer.toHexString(rgb.blue));
							if (sb.length() == 5)
								sb.insert(4, "0");

							p.setRGB(sb.toString());
							updateItem(item, p);
							table.redraw();
							changed = true;
							setEnablement();
						}
						return;
					}

					// The control that will be the editor must be a child of the Table
					Text newEditor = new Text(table, SWT.NONE);
					newEditor.setText(item.getText(column));
					final TableItem ti = item;
					newEditor.addModifyListener(new ModifyListener() {
						@Override
						public void modifyText(ModifyEvent me) {
							Text text = (Text) editor.getEditor();
							String s = text.getText();
							Problem p = (Problem) ti.getData();
							if (column == 0)
								p.letter = s;
							else if (column == 1)
								p.shortName = s;
							else if (column == 2)
								p.color = s;
							else if (column == 3)
								p.setRGB(s);
							updateItem(ti, p);
							table.redraw();
							changed = true;
							setEnablement();
						}
					});
					newEditor.selectAll();
					newEditor.setFocus();
					newEditor.addFocusListener(new FocusAdapter() {
						@Override
						public void focusLost(FocusEvent event) {
							Control oldEditor2 = editor.getEditor();
							if (oldEditor2 != null)
								oldEditor2.dispose();
						}
					});
					newEditor.addKeyListener(new KeyAdapter() {
						@Override
						public void keyPressed(KeyEvent event) {
							if (event.character == '\n' || event.character == '\r') {
								Control oldEditor2 = editor.getEditor();
								if (oldEditor2 != null)
									oldEditor2.dispose();
							}
						}
					});
					editor.setEditor(newEditor, item, column);
				}
			}
		}
	}

	protected void createUI(final Composite parent) {
		table = new Table(parent, SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.verticalSpan = 8;
		data.heightHint = 200;
		table.setLayoutData(data);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		createColumn("Letter", 50, SWT.LEFT);
		createColumn("Problem Short Name", 125, SWT.LEFT);
		createColumn("Color Name", 100, SWT.LEFT);
		createColumn("RGB (hex)", 75, SWT.LEFT);
		createColumn("Preview", 75, SWT.LEFT);

		editor = new TableEditor(table);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		final Color selColor = table.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
		table.addListener(SWT.EraseItem, new Listener() {
			@Override
			public void handleEvent(Event event) {
				// event.detail &= ~SWT.HOT;
				if ((event.detail & SWT.SELECTED) == 0)
					return; // item not selected
				if ((event.detail & SWT.BACKGROUND) == 0)
					return; // background draw
				int clientWidth = table.getClientArea().width;
				GC gc = event.gc;
				gc.setForeground(selColor);
				gc.fillRectangle(0, event.y, clientWidth, event.height);
				event.detail &= ~SWT.SELECTED;
			}
		});

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				tableSelected(e, true);
			}

			@Override
			public void mouseDown(MouseEvent e) {
				tableSelected(e, false);
			}
		});

		final Button add = SWTUtil.createButton(parent, "Add");

		remove = SWTUtil.createButton(parent, "Remove");
		remove.setEnabled(false);

		up = SWTUtil.createButton(parent, "Up");
		data = (GridData) up.getLayoutData();
		data.verticalIndent = 25;
		up.setEnabled(false);

		down = SWTUtil.createButton(parent, "Down");
		down.setEnabled(false);

		add.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String letter = getFirstUnusedLetter();

				Problem p = new Problem();
				p.letter = letter;
				problems.add(p);

				TableItem ti = new TableItem(table, SWT.NONE);
				updateItem(ti, p);
				table.setSelection(ti);
				changed = true;
				setEnablement();
			}
		});

		remove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				int index = table.getSelectionIndex();
				if (index < 0)
					return;

				problems.remove(index);
				table.remove(index);
				changed = true;
				setEnablement();
			}
		});
		up.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				int index = table.getSelectionIndex();
				swap(index, true);
				changed = true;
				setEnablement();
			}
		});
		down.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				int index = table.getSelectionIndex();
				swap(index, false);
				changed = true;
				setEnablement();
			}
		});

		html = SWTUtil.createButton(parent, "Generate HTML...");
		data = (GridData) html.getLayoutData();
		data.verticalIndent = 25;
		html.setEnabled(false);
		html.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				FileDialog fileDialog = new FileDialog(html.getShell(), SWT.SAVE);
				fileDialog.setText("Save HTML");
				fileDialog.setFileName("problemset.html");
				fileDialog.setFilterExtensions(new String[] { "*.html" });
				String filePath = fileDialog.open();
				if (filePath != null) {
					file = new File(filePath);
					saveHTML(file);
				}
			}
		});

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				setEnablement();
			}
		});
	}

	protected void saveHTML(File f) {
		try {
			HTMLGen.saveAsHTML(file, problems);
		} catch (Exception e) {
			showError("Error saving problem set as HTML", e);
		}
	}

	protected void setEnablement() {
		int index = table.getSelectionIndex();
		int count = table.getItemCount();
		remove.setEnabled(index != -1);
		up.setEnabled(index > 0);
		down.setEnabled(index >= 0 && index < count - 1);
		html.setEnabled(!problems.isEmpty());
		saveMenu.setEnabled(file != null && changed);
		if (file == null)
			table.getShell().setText("Problem Set Editor");
		else {
			if (changed)
				table.getShell().setText("Problem Set Editor - *" + file.getAbsolutePath());
			else
				table.getShell().setText("Problem Set Editor - " + file.getAbsolutePath());
		}
	}

	protected void swap(int index, boolean goUp) {
		int ind = index + 1;
		if (goUp)
			ind = index - 1;
		Problem p1 = problems.get(index);
		Problem p2 = problems.get(ind);
		problems.set(index, p2);
		problems.set(ind, p1);
		updateItem(table.getItem(index), p2);
		updateItem(table.getItem(ind), p1);
		table.setSelection(ind);
	}

	protected String getFirstUnusedLetter() {
		int ch = 'A';

		do {
			String l = "" + (char) ch;
			boolean used = false;
			for (Problem p : problems) {
				if (p.letter != null && p.letter.equals(l))
					used = true;
			}
			if (!used)
				return l;
			ch++;
		} while (true);
	}

	protected void fillTable() {
		for (Problem p : problems) {
			TableItem item = new TableItem(table, SWT.NONE);
			updateItem(item, p);
		}
	}

	protected static String notNull(String s) {
		if (s == null)
			return "";
		return s;
	}

	protected void updateItem(TableItem item, Problem p) {
		item.setData(p);
		item.setText(new String[] { notNull(p.letter), notNull(p.shortName), notNull(p.color), notNull(p.getRGB()), "" });

		RGB rgb = p.getRGBVal();
		Color c = null;
		if (rgb != null)
			c = new Color(table.getDisplay(), rgb);
		Color oldColor = item.getBackground(4);
		item.setBackground(4, c);
		if (oldColor != null)
			oldColor.dispose();
	}

	private static void showHelp() {
		System.out.println();
		System.out.println("ICPC Problem Set Editor");
		System.out.println();
		System.out.println("Usage: problemset [problemset.yaml]");
		System.out.println();
	}

	private static String getVersion(String ver) {
		if (ver == null)
			return "dev";
		return ver;
	}

	public static void main(String[] args) {
		if (args != null && args.length == 1 && "--version".equals(args[0])) {
			Package pack = ProblemSetEditor.class.getPackage();
			System.out.println("ICPC Problem Set Editor version " + getVersion(pack.getSpecificationVersion()) + " (build "
					+ getVersion(pack.getImplementationVersion()) + ")");
			System.exit(0);
		}

		if (args != null && args.length > 1) {
			showHelp();
			System.exit(1);
		}
		Display.setAppName("Problem Set Editor");
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

		shell.setText("Problem Set Editor");
		Image image = new Image(display, ProblemSetEditor.class.getResourceAsStream("/images/problemSetIcon.png"));
		shell.setImage(image);

		ProblemSetEditor editor = new ProblemSetEditor(shell);

		if (args != null && args.length == 1) {
			File file = new File(args[0]);
			editor.setFile(file);
		}

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
		Package pack = ProblemSetEditor.class.getPackage();
		dialog.setMessage(shell.getText() + " version " + getVersion(pack.getSpecificationVersion()) + " (build "
				+ getVersion(pack.getImplementationVersion()) + ")");
		dialog.open();
	}
}