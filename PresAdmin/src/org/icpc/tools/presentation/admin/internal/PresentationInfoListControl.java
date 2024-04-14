package org.icpc.tools.presentation.admin.internal;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.presentation.core.internal.PresentationInfo;

/**
 * A generic list control that shows columns of objects with images and a label underneath.
 *
 * TODO: - category boxes should be slightly smaller and right justified - category labels should
 * go 2-3 lines when necessary - categories shouldn't start as the last box on a line
 */
public class PresentationInfoListControl extends Canvas {
	protected static final int BORDER = 4;
	protected static final int GAP = 6;
	protected static final int TEXT_GAP = 2;
	protected static final int SEL_MARGIN = 2;
	protected static final Dimension POPUP_SIZE = new Dimension(320, 180);
	protected static final Dimension DEFAULT_THUMBNAIL_SIZE = new Dimension(72, 40);
	protected Dimension thumbnailSize = DEFAULT_THUMBNAIL_SIZE;

	protected List<PresentationInfo> list = new ArrayList<>();

	protected List<Rectangle> listRects = new ArrayList<>();
	protected List<Category> categories = new ArrayList<>();
	protected List<String> closedCategories = new ArrayList<>();

	class Category {
		String name;
		Rectangle rect;
	}

	enum DisplayStyle {
		LIST, CATEGORY, TIMELINE
	}

	protected DisplayStyle displayStyle = DisplayStyle.LIST;
	protected SelectionListener listener;
	protected PresentationInfo selection;

	protected Shell hoverShell;
	protected boolean fixedContents;

	protected int fontHeight = 16;
	protected View.ForceTheme forceTheme;

	public PresentationInfoListControl(Composite parent, int style) {
		this(parent, style, DEFAULT_THUMBNAIL_SIZE, true, DisplayStyle.CATEGORY);
	}

	public PresentationInfoListControl(Composite parent, int style, Dimension thumbnailSize, boolean fixedContents,
			DisplayStyle displayStyle) {
		super(parent, displayStyle != DisplayStyle.TIMELINE ? style | SWT.DOUBLE_BUFFERED | SWT.V_SCROLL
				: style | SWT.DOUBLE_BUFFERED | SWT.H_SCROLL);
		this.thumbnailSize = thumbnailSize;
		this.fixedContents = fixedContents;
		this.displayStyle = displayStyle;

		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent event) {
				paint(event);
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				select(e, false);
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				select(e, true);
			}
		});

		createDragSource();
		createDropTarget();

		ScrollBar vScroll = getVerticalBar();
		if (vScroll != null) {
			vScroll.setMinimum(0);
			vScroll.setPageIncrement(50);
			vScroll.setIncrement(50);
			vScroll.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent sel) {
					redraw();
				}
			});
		}
		ScrollBar hScroll = getHorizontalBar();
		if (hScroll != null) {
			hScroll.setMinimum(0);
			hScroll.setPageIncrement(50);
			hScroll.setIncrement(50);
			hScroll.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent sel) {
					redraw();
				}
			});
		}
		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				adjustScrollbars();
			}
		});
		adjustScrollbars();

		final Dimension thumbnailSize2 = thumbnailSize;
		addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseHover(MouseEvent event) {
				PresentationInfo sel = getPresentationAt(event.x, event.y);

				try {
					showPopup(sel, new Point(event.x + thumbnailSize2.width, event.y));
				} catch (Exception e) {
					Trace.trace(Trace.ERROR, "Could not open popup", e);
				}
			}

			@Override
			public void mouseExit(MouseEvent event) {
				showPopup(null, null);
			}
		});

		closedCategories.add("Fun");
		closedCategories.add("Test");
		closedCategories.add("Resolver");
		closedCategories.add("Beta");
	}

	protected void createDragSource() {
		int operations = DND.DROP_COPY;

		DragSource source = new DragSource(this, operations);
		final PresentationTransfer transfer = PresentationTransfer.getInstance();
		source.setTransfer(new Transfer[] { transfer });

		source.addDragListener(new DragSourceListener() {
			@Override
			public void dragStart(DragSourceEvent e) {
				PresentationInfo newSelection = getPresentationAt(e.x, e.y);
				if (newSelection == null)
					e.doit = false;
				else {
					Image image = createImage(newSelection, thumbnailSize);
					transfer.setSelection(newSelection, image);
					e.image = image;
				}
			}

			@Override
			public void dragSetData(DragSourceEvent e) {
				if (PresentationTransfer.getInstance().isSupportedType(e.dataType)) {
					e.data = transfer.getSelection();
				}
			}

			@Override
			public void dragFinished(DragSourceEvent e) {
				// future - could drag out of build list
				// if (e.detail == DND.DROP_MOVE)
				// remove(transfer.getSelection());

				transfer.setSelection(null, null);
			}
		});
	}

	protected void createDropTarget() {
		if (fixedContents)
			return;

		int operations = DND.DROP_COPY | DND.DROP_DEFAULT;
		DropTarget target = new DropTarget(this, operations);

		final PresentationTransfer transfer = PresentationTransfer.getInstance();
		target.setTransfer(new Transfer[] { transfer });

		target.addDropListener(new DropTargetAdapter() {
			@Override
			public void dragEnter(DropTargetEvent event) {
				if (event.detail == DND.DROP_DEFAULT)
					event.detail = DND.DROP_COPY;
			}

			@Override
			public void dragOperationChanged(DropTargetEvent event) {
				if (event.detail == DND.DROP_DEFAULT)
					event.detail = DND.DROP_COPY;
			}

			@Override
			public void drop(DropTargetEvent event) {
				if (transfer.isSupportedType(event.currentDataType))
					add(transfer.getSelection());
			}
		});
	}

	private void adjustScrollbars() {
		Point p = computeSize(SWT.DEFAULT, SWT.DEFAULT);
		ScrollBar vScroll = getVerticalBar();
		if (vScroll != null) {
			if (p.y - getSize().y < 0) {
				vScroll.setEnabled(false);
				vScroll.setSelection(0);
			} else {
				vScroll.setEnabled(true);
				vScroll.setMaximum(p.y);
				vScroll.setThumb(getSize().y);
			}
		}
		ScrollBar hScroll = getHorizontalBar();
		if (hScroll != null) {
			if (p.x - getSize().x < 0) {
				hScroll.setEnabled(false);
				hScroll.setSelection(0);
			} else {
				hScroll.setEnabled(true);
				hScroll.setMaximum(p.x);
				hScroll.setThumb(getSize().x);
			}
		}
	}

	public void addSelectionListener(SelectionListener newListener) {
		listener = newListener;
	}

	public List<PresentationInfo> getPresentationInfos() {
		return list;
	}

	public void add(PresentationInfo info) {
		add(info, true);
	}

	public void add(PresentationInfo info, boolean allowDuplicates) {
		if (list.contains(info) && !allowDuplicates) // replacing existing presentation
			list.remove(info);

		list.add(info);

		// sort by category & name
		if (displayStyle != DisplayStyle.TIMELINE) {
			int size = list.size();
			for (int i = 0; i < size - 1; i++) {
				for (int j = i + 1; j < size; j++) {
					PresentationInfo pi = list.get(i);
					PresentationInfo pj = list.get(j);
					boolean swap = false;
					if ((pi.getCategory() == null && pj.getCategory() != null) || (pi.getCategory() != null
							&& pj.getCategory() != null && pi.getCategory().compareTo(pj.getCategory()) > 0)) {
						swap = true;
					} else if ((pi.getCategory() == null && pj.getCategory() == null)
							|| (pi.getCategory() != null && pi.getCategory().equals(pj.getCategory()))) {
						if (pi.getName().compareTo(pj.getName()) > 0)
							swap = true;
					}
					if (swap) {
						list.set(i, pj);
						list.set(j, pi);
					}
				}
			}
		}

		redraw();
		adjustScrollbars();

		fireSelectionEvent();
	}

	public void remove(PresentationInfo info) {
		list.remove(info);

		if (selection == info)
			selection = null;

		redraw();
		adjustScrollbars();

		fireSelectionEvent();
	}

	public void clear() {
		list.clear();

		selection = null;

		redraw();
		adjustScrollbars();

		fireSelectionEvent();
	}

	public PresentationInfo getSelection() {
		return selection;
	}

	protected PresentationInfo getPresentationAt(int x, int y) {
		int xx = x;
		int yy = y;
		if (getVerticalBar() != null)
			yy += getVerticalBar().getSelection();
		if (getHorizontalBar() != null)
			xx += getHorizontalBar().getSelection();

		if (listRects.size() != list.size())
			return null;

		for (int i = 0; i < list.size(); i++) {
			Rectangle r = listRects.get(i);
			if (r != null && r.contains(xx, yy))
				return list.get(i);
		}
		return null;
	}

	protected String getCategoryAt(int x, int y) {
		int xx = x;
		int yy = y;
		if (getVerticalBar() != null)
			yy += getVerticalBar().getSelection();
		if (getHorizontalBar() != null)
			xx += getHorizontalBar().getSelection();

		if (categories.isEmpty())
			return null;

		for (Category c : categories) {
			if (c.rect.contains(xx, yy))
				return c.name;
		}
		return null;
	}

	protected void select(MouseEvent e, boolean doubleClick) {
		PresentationInfo oldSelection = selection;
		PresentationInfo newSelection = getPresentationAt(e.x, e.y);

		if (selection == newSelection && !doubleClick)
			selection = null;
		else
			selection = newSelection;

		if (newSelection == null) {
			String cat = getCategoryAt(e.x, e.y);
			if (cat != null) {
				if (closedCategories.contains(cat))
					closedCategories.remove(cat);
				else
					closedCategories.add(cat);

				redraw();
				adjustScrollbars();
				return;
			}
		}

		if (oldSelection == selection)
			return;

		fireSelectionEvent(e.x, e.y, doubleClick);
		redraw();
	}

	protected void fireSelectionEvent() {
		fireSelectionEvent(0, 0, false);
	}

	protected void fireSelectionEvent(int x, int y, boolean doubleClick) {
		if (listener == null)
			return;

		Event ev = new Event();
		ev.x = x;
		ev.y = y;
		ev.widget = this;
		if (doubleClick)
			listener.widgetDefaultSelected(new SelectionEvent(ev));
		else
			listener.widgetSelected(new SelectionEvent(ev));
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		if (displayStyle == DisplayStyle.TIMELINE) {
			int numPres = 0;
			int numTrans = 0;
			for (PresentationInfo info : list) {
				if (info.isTransition())
					numTrans++;
				else
					numPres++;
			}
			double columns = Math.max(numPres, numTrans + 0.5);
			return new Point((int) (thumbnailSize.width * columns) + (int) (GAP * Math.ceil(columns - 1)) + BORDER * 2,
					(int) (thumbnailSize.height * 1.5) + GAP + (TEXT_GAP + fontHeight + BORDER) * 2);
		}
		int ww = getSize().x;
		if (wHint != SWT.DEFAULT)
			ww = wHint;

		String lastCategory = "The wrong category";
		int row = 0;
		int column = -1;
		int presentationsPerLine = (ww - BORDER * 2 + GAP) / (thumbnailSize.width + GAP);

		boolean categoryIsClosed = false;
		for (PresentationInfo obj : list) {
			if (displayStyle == DisplayStyle.CATEGORY) {
				String category = obj.getCategory();
				if ((lastCategory == null && category != null)
						|| (lastCategory != null && !lastCategory.equals(category))) {
					lastCategory = category;

					String s = lastCategory;
					if (s == null)
						s = "Other";

					if (closedCategories.contains(s))
						categoryIsClosed = true;
					else
						categoryIsClosed = false;

					column++;
					if (column >= presentationsPerLine - 1) {
						row++;
						column = 0;
					}
				}
			}
			if (categoryIsClosed)
				continue;

			column++;
			if (column >= presentationsPerLine) {
				row++;
				column = 0;
			}
		}

		row++;
		return new Point(ww, row * (thumbnailSize.height + TEXT_GAP + fontHeight) + (row - 1) * GAP + BORDER * 2);
	}

	protected void paint(PaintEvent event) {
		Rectangle rect = getClientArea();
		if (rect.width == 0 || rect.height == 0)
			return;

		GC gc = event.gc;
		Color back = getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		if (forceTheme != null) {
			switch (forceTheme) {
				case DARK:
					back = new Color(getDisplay(), 31, 31, 31);
					break;
				case LIGHT:
					back = new Color(getDisplay(), 239, 239, 239);
					break;
			}
		}
		gc.setBackground(back);
		gc.fillRectangle(rect.x + 1, rect.y + 1, rect.width - 2, rect.height - 2);

		Font font = getDisplay().getSystemFont();
		gc.setFont(font);

		int vs = 0;
		int hs = 0;
		if (getVerticalBar() != null)
			vs = getVerticalBar().getSelection();
		if (getHorizontalBar() != null)
			hs = getHorizontalBar().getSelection();
		Transform trans = new Transform(gc.getDevice());
		trans.translate(-hs, -vs);
		gc.setTransform(trans);
		trans.dispose();

		if (fontHeight == 10) {
			fontHeight = gc.textExtent("AZjy").y + 1;
			adjustScrollbars();
		}

		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);
		gc.setTextAntialias(SWT.ON);

		String lastCategory = "The wrong category";
		int row = 0;
		int column = -1;
		int px = 0;
		int tx = 0;
		boolean lastIsSelection = false;
		int presentationsPerLine = (rect.width - BORDER * 2 + GAP) / (thumbnailSize.width + GAP);
		listRects.clear();
		categories.clear();
		boolean categoryIsClosed = false;
		for (PresentationInfo info : list) {
			if (displayStyle == DisplayStyle.CATEGORY) {
				String category = info.getCategory();
				if ((lastCategory == null && category != null)
						|| (lastCategory != null && !lastCategory.equals(category))) {
					lastCategory = category;

					String s = lastCategory;
					if (s == null)
						s = "Other";

					if (closedCategories.contains(s))
						categoryIsClosed = true;
					else
						categoryIsClosed = false;

					column++;
					if (column >= presentationsPerLine) {
						row++;
						column = 0;
					}

					int x = BORDER + column * (thumbnailSize.width + GAP);
					int y = BORDER + row * (thumbnailSize.height + GAP + TEXT_GAP + fontHeight);

					// draw category
					if (categoryIsClosed)
						gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND));
					else
						gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));

					Path path = new Path(getDisplay());
					path.moveTo(x + GAP + thumbnailSize.height / 2, y);
					path.lineTo(x + thumbnailSize.width + GAP, y);
					path.lineTo(x + thumbnailSize.width + GAP, y + thumbnailSize.height + 1);
					path.lineTo(x + GAP, y + thumbnailSize.height + 1);
					path.lineTo(x + GAP, y + thumbnailSize.height / 2);
					gc.fillPath(path);

					Rectangle clipR = gc.getClipping();
					gc.setClipping(path);
					path.dispose();

					gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
					int x1 = x + thumbnailSize.width + GAP - 6;
					int x2 = x + thumbnailSize.width + GAP - 9;
					if (categoryIsClosed) {
						int t = x1;
						x1 = x2;
						x2 = t;
					}
					gc.drawLine(x1, y + 3, x2, y + 6);
					gc.drawLine(x1, y + 9, x2, y + 6);
					gc.drawLine(x1 - 4, y + 3, x2 - 4, y + 6);
					gc.drawLine(x1 - 4, y + 9, x2 - 4, y + 6);

					String[] ss = splitString(gc, s, thumbnailSize.width - GAP - 2);
					for (int i = 0; i < ss.length; i++) {
						Point p = gc.textExtent(ss[i]);
						gc.drawString(ss[i], x + thumbnailSize.width - p.x + 2,
								y + thumbnailSize.height - p.y - 3 - (ss.length - i - 1) * p.y, true);
					}
					gc.setClipping(clipR);

					Category c = new Category();
					c.name = s;
					c.rect = new Rectangle(x, y, thumbnailSize.width, thumbnailSize.height);
					categories.add(c);

					lastIsSelection = false;
				}
			}
			if (categoryIsClosed) {
				listRects.add(new Rectangle(-10, -10, 5, 5));
				continue;
			}
			column++;
			if (column >= presentationsPerLine) {
				row++;
				column = 0;
				lastIsSelection = false;
			}
			int x = BORDER + column * (thumbnailSize.width + GAP);
			int y = BORDER + row * (thumbnailSize.height + GAP + TEXT_GAP + fontHeight);

			int rh = thumbnailSize.height;
			if (displayStyle == DisplayStyle.TIMELINE) {
				if (!info.isTransition()) {
					x = BORDER + px * (thumbnailSize.width + GAP);
					y = BORDER;
					px++;
				} else {
					x = BORDER + (int) ((0.5 + tx) * (thumbnailSize.width + GAP));
					y = BORDER + (thumbnailSize.height + GAP + TEXT_GAP + fontHeight);
					tx++;
					rh = thumbnailSize.height / 2;
				}
			}

			if (displayStyle == DisplayStyle.CATEGORY) {
				gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
				if (lastIsSelection)
					gc.fillRectangle(x - GAP + 1 + SEL_MARGIN, y, GAP - 1 - SEL_MARGIN, thumbnailSize.height + 1);
				else
					gc.fillRectangle(x - GAP + 1, y, GAP - 1, thumbnailSize.height + 1);
			}

			if (info == selection) {
				gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
				gc.fillRectangle(x - SEL_MARGIN, y - SEL_MARGIN, thumbnailSize.width + SEL_MARGIN * 2 + 1,
						rh + SEL_MARGIN * 2 + 2 + fontHeight);
				lastIsSelection = true;
			} else
				lastIsSelection = false;

			Image img = getImage(info);
			Rectangle r = img.getBounds();

			gc.drawImage(img, 0, 0, r.width, r.height, x, y, thumbnailSize.width, rh);
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
			gc.drawRectangle(x, y, thumbnailSize.width, rh);

			if (selection == info) {
				gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
			} else {
				Color fore = getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
				if (forceTheme != null) {
					switch (forceTheme) {
						case DARK:
							fore = getDisplay().getSystemColor(SWT.COLOR_GRAY);
							break;
						case LIGHT:
							fore = getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
							break;
					}
				}
				gc.setForeground(fore);
			}

			String name = getName(info);
			Point p = gc.textExtent(name);
			int i = name.length();
			while (p.x > thumbnailSize.width && i > 0) {
				i--;
				name = name.substring(0, i) + "...";
				p = gc.textExtent(name);
			}

			gc.drawString(name, x, y + rh + TEXT_GAP, true);
			listRects.add(new Rectangle(x, y, thumbnailSize.width, rh + TEXT_GAP + p.y));
		}
	}

	protected static String[] splitString(GC gc, String str, int width) {
		if (str == null)
			return new String[0];

		String s = str;
		List<String> list = new ArrayList<>();

		while (gc.textExtent(s).x > width) {
			// find spot
			int x = s.length() - 1;
			while (x > 0 && gc.textExtent(s.substring(0, x)).x > width)
				x--;

			if (x == 0) // too narrow, can't even crop a char!
				return new String[] { s };

			// try to find space a few chars back
			int y = x;
			while (y > x * 0.6f && s.charAt(y) != ' ')
				y--;

			// otherwise crop anyway
			if (s.charAt(y) != ' ') {
				list.add(s.substring(0, x));
				s = "-" + s.substring(x);
			} else {
				list.add(s.substring(0, y));
				s = s.substring(y + 1);
			}
		}
		list.add(s);
		return list.toArray(new String[0]);
	}

	protected void showPopup(PresentationInfo info, Point p) {
		if (hoverShell != null) {
			hoverShell.dispose();
			hoverShell = null;
		}

		if (info == null)
			return;

		hoverShell = new Shell(getShell(), SWT.TOOL | SWT.NO_FOCUS);
		hoverShell.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		Point loc = toDisplay(p);
		hoverShell.setLocation(loc.x, loc.y);
		hoverShell.setLayout(new GridLayout());

		final Label imageLabel = new Label(hoverShell, SWT.NONE);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		Image image2 = createImage(info, POPUP_SIZE);
		data.widthHint = POPUP_SIZE.width;
		data.heightHint = image2.getBounds().height;
		imageLabel.setLayoutData(data);
		imageLabel.setImage(image2);
		imageLabel.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				Image image = imageLabel.getImage();
				if (image != null)
					image.dispose();
			}
		});

		Label description = new Label(hoverShell, SWT.WRAP);
		data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
		data.widthHint = POPUP_SIZE.width;
		description.setLayoutData(data);
		description.setBackground(hoverShell.getBackground());

		String s = getName(info);
		String desc = getDescription(info);
		if (desc != null)
			s += ": " + desc;
		description.setText(s);

		String[] propData = info.getProperties();
		if (propData != null && propData.length > 0) {
			for (String prop : propData) {
				Label propLabel = new Label(hoverShell, SWT.WRAP);
				propLabel.setText(prop);
				data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
				data.horizontalIndent = 15;
				propLabel.setLayoutData(data);
				propLabel.setBackground(hoverShell.getBackground());
			}
		}

		hoverShell.pack();
		hoverShell.setVisible(true);
	}

	private Image createImage(PresentationInfo info, Dimension d) {
		Image img = getImage(info);
		Rectangle r = img.getBounds();

		float scale = Math.min(1f, Math.min(d.width / (float) r.width, d.height / (float) r.height));
		int nw = (int) (r.width * scale) - 1;
		int nh = (int) (r.height * scale) - 1;

		Image image = new Image(getDisplay(), d.width, nh);
		GC gc = new GC(image);
		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);

		gc.drawImage(img, 0, 0, r.width, r.height, 0, 0, nw, nh);
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		gc.drawRectangle(0, 0, nw, nh);
		gc.dispose();
		return image;
	}

	protected String getName(PresentationInfo info) {
		String s = info.getName();
		if (info.getData() != null && info.getData().length > 0)
			s += " (" + info.getData()[0] + ")";
		return s;
	}

	protected String getDescription(PresentationInfo info) {
		return info.getDescription();
	}

	protected Image getImage(PresentationInfo info) {
		return PresentationHelper.getPresentationImage(info);
	}

	protected void setForceTheme(View.ForceTheme forceTheme) {
		this.forceTheme = forceTheme;
	}
}