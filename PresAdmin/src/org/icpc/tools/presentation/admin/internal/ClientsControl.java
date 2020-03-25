package org.icpc.tools.presentation.admin.internal;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.icpc.tools.client.core.BasicClient.Client;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.presentation.core.internal.PresentationInfo;

public class ClientsControl extends Canvas {
	protected static final int BORDER = 5;
	protected static final int GAP = 10;
	protected static final int TEXT_GAP = 3;
	protected static final int SEL_MARGIN = 4;
	protected static final Dimension DEFAULT_IMG_DIM = new Dimension(320, 180);
	protected static final Dimension SMALL_IMG_DIM = new Dimension(160, 90);

	protected Dimension IMG_DIM = DEFAULT_IMG_DIM;

	protected Client[] clients = new Client[0];
	protected Map<Integer, Rectangle> clientRects = new HashMap<>();
	protected Map<Integer, ClientInfo> clientStates = new HashMap<>();

	protected Object uiLock = new Object();

	protected List<SelectionListener> listeners;
	protected IDropListener dropListener;
	protected List<Integer> selection = new ArrayList<>();
	protected int yOrigin = 0;
	private boolean waitingForRedraw;
	protected List<String> filter = new ArrayList<>();

	class ClientInfo {
		int width;
		int height;
		int fps;
		boolean hidden;
		int fullScreen = -1;
		String pres;
		Image thumbnail;
	}

	public interface IDropListener {
		void drop(int clientUID, PresentationInfo pres);
	}

	/**
	 * ClientsControl constructor.
	 *
	 * @param parent
	 * @param style
	 */
	public ClientsControl(Composite parent, int style) {
		super(parent, style | SWT.V_SCROLL | SWT.DOUBLE_BUFFERED | SWT.NO_REDRAW_RESIZE);

		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent event) {
				paint(event);
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				select(e.x, e.y, e.stateMask);
			}
		});

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent event) {
				if ((event.stateMask & SWT.CTRL) != 0 && event.keyCode == 'a') {
					List<Integer> sel = new ArrayList<>();

					for (Client c : clients)
						sel.add(c.uid);

					selection = sel;
					fireEvent(0, 0);
					redraw();
				}
			}
		});

		final ScrollBar vScroll = getVerticalBar();
		vScroll.setMinimum(0);
		vScroll.setIncrement(50);
		vScroll.setPageIncrement(50);
		vScroll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				yOrigin = vScroll.getSelection();
				redraw();
			}
		});

		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent event) {
				resize();
			}
		});
		resize();

		createDropTarget();
	}

	protected void createDropTarget() {
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
			public void dragOver(DropTargetEvent event) {
				Point p = toControl(event.x, event.y);
				if (getClientAt(p.x, p.y) == null)
					event.detail = DND.DROP_NONE;
				else
					event.detail = DND.DROP_COPY;
			}

			@Override
			public void dragOperationChanged(DropTargetEvent event) {
				if (event.detail == DND.DROP_DEFAULT)
					event.detail = DND.DROP_COPY;
			}

			@Override
			public void drop(DropTargetEvent event) {
				final PresentationInfo info = transfer.getSelection();
				Point p = toControl(event.x, event.y);
				Client c = getClientAt(p.x, p.y);
				if (c != null)
					dropListener.drop(c.uid, info);
			}
		});
	}

	public void setDropListener(IDropListener newListener) {
		dropListener = newListener;
	}

	public void addSelectionListener(SelectionListener newListener) {
		if (listeners == null)
			listeners = new ArrayList<>();
		listeners.add(newListener);
	}

	public void setClients(Client[] s) {
		Client[] st = new Client[s.length];
		System.arraycopy(s, 0, st, 0, s.length);
		Arrays.sort(st, new Comparator<Client>() {
			@Override
			public int compare(Client c1, Client c2) {
				try {
					Integer in1 = Integer.parseInt(c1.name);
					Integer in2 = Integer.parseInt(c2.name);
					return in1.compareTo(in2);
				} catch (Exception e) {
					// ignore
				}

				return c1.name.compareTo(c2.name);
			}
		});

		synchronized (uiLock) {
			Client[] oldClients = clients;
			clients = st;

			if (oldClients != null) {
				for (Client c : oldClients) {
					int uid = c.uid;
					boolean found = false;
					for (Client cc : clients)
						if (cc.uid == uid)
							found = true;

					if (!found) {
						clientRects.remove(uid);
						clientStates.remove(uid);

						ClientInfo ci = clientStates.get(uid);
						if (ci != null && ci.thumbnail != null)
							ci.thumbnail.dispose();
					}
				}
			}
		}
		getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				resize();
			}
		});
	}

	public void addTypeFilter(String type) {
		filter.add(type);

		// check for current selection
		Client[] temp = clients;
		List<Integer> remove = new ArrayList<>();
		for (Integer in : selection) {
			for (Client c : temp) {
				if (c.uid == in)
					remove.add(in);
			}
		}
		if (!remove.isEmpty()) {
			for (Integer in : remove)
				selection.remove(in);
			fireEvent(0, 0);
		}

		resizeRects();
		doRedraw();
	}

	public void removeTypeFilter(String type) {
		filter.remove(type);
		resizeRects();
		doRedraw();
	}

	private void doRedraw() {
		if (waitingForRedraw)
			return;

		getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				waitingForRedraw = false;
				if (isDisposed())
					return;

				redraw();
			}
		});
		waitingForRedraw = true;
	}

	public void handleState(int id, JsonObject obj) {
		ClientInfo ci = clientStates.get(id);
		if (ci == null)
			ci = new ClientInfo();

		try {
			synchronized (uiLock) {
				if (obj.containsKey("width"))
					ci.width = obj.getInt("width");
				if (obj.containsKey("height"))
					ci.height = obj.getInt("height");
				if (obj.containsKey("hidden"))
					ci.hidden = obj.getBoolean("hidden");
				if (obj.containsKey("full_screen_window"))
					ci.fullScreen = obj.getInt("full_screen_window");
				if (obj.containsKey("fps"))
					ci.fps = obj.getInt("fps");
				if (obj.containsKey("presentation"))
					ci.pres = obj.getString("presentation");

				clientStates.put(id, ci);
			}
		} catch (Exception e) {
			Trace.trace(Trace.WARNING, "No client info", e);
		}

		doRedraw();
	}

	public void setThumbnail(int id, byte[] b) {
		ImageLoader il = new ImageLoader();
		ImageData[] id2 = il.load(new ByteArrayInputStream(b));

		Device device = getDisplay();
		Image img = new Image(device, id2[0]);

		// rescale if necessary
		Rectangle r = img.getBounds();
		double scale = Math.min((float) IMG_DIM.width / (float) r.width, (float) IMG_DIM.height / (float) r.height);
		if (scale != 1.0) {
			Point p = new Point((int) (r.width * scale), (int) (r.height * scale));

			Image img2 = new Image(device, p.x, p.y);
			GC gc = new GC(img2);
			gc.drawImage(img, 0, 0, r.width, r.height, 0, 0, p.x, p.y);
			gc.dispose();

			img.dispose();
			img = img2;
		}

		Image oldImg = null;
		synchronized (uiLock) {
			ClientInfo ci = clientStates.get(id);
			if (ci == null)
				ci = new ClientInfo();
			oldImg = ci.thumbnail;
			ci.thumbnail = img;
			clientStates.put(id, ci);
			if (oldImg != null)
				oldImg.dispose();
		}

		doRedraw();
	}

	private String getNameFromUID(int uid) {
		for (Client c : clients) {
			if (c.uid == uid)
				return c.name;
		}
		return "<unknown>";
	}

	public void handleSnapshot(final int uid, byte[] b) {
		ImageLoader il = new ImageLoader();
		ImageData[] id2 = il.load(new ByteArrayInputStream(b));
		Device device = getDisplay();
		final Image img = new Image(device, id2[0]);

		getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				showSnapshot(uid, img);
			}
		});
	}

	private void showSnapshot(int uid, final Image img) {
		Display display = getDisplay();
		final Shell shell = new Shell(display);
		shell.setText("Snapshot from " + getNameFromUID(uid));
		shell.setImage(ImageResource.getImage(ImageResource.IMG_ICON));

		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		shell.setLayout(layout);

		ScrolledComposite c = new ScrolledComposite(shell, SWT.H_SCROLL | SWT.V_SCROLL);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = 800;
		data.heightHint = 600;
		c.setLayoutData(data);

		Label l = new Label(c, SWT.NONE);
		l.setImage(img);
		Rectangle r = img.getBounds();
		l.setSize(r.width, r.height);

		data = new GridData(GridData.FILL_BOTH);
		l.setLayoutData(data);
		c.setContent(l);

		shell.pack();
		shell.open();
		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				img.dispose();
			}
		});
	}

	public void handleLog(final int uid, String log) {
		getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				showLog(uid, log);
			}
		});
	}

	private void showLog(int uid, final String log) {
		Display display = getDisplay();
		final Shell shell = new Shell(display);
		shell.setText("Logs from " + getNameFromUID(uid));
		shell.setImage(ImageResource.getImage(ImageResource.IMG_ICON));

		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 1;
		shell.setLayout(layout);

		Text t = new Text(shell, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		t.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		t.setText(log);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = 800;
		data.heightHint = 600;
		t.setLayoutData(data);

		shell.pack();
		shell.open();
	}

	public int[] getSelection() {
		int size = selection.size();
		int[] sel = new int[size];
		for (int i = 0; i < size; i++)
			sel[i] = selection.get(i);
		return sel;
	}

	public boolean isSelectionFullScreenAvailable(int display) {
		int size = selection.size();
		if (size == 0)
			return false;
		for (int i = 0; i < size; i++) {
			int uid = selection.get(i);
			for (int j = 0; j < clients.length; j++) {
				if (clients[j].uid == uid && clients[j].displays != null && display >= clients[j].displays.length)
					return false;
			}
			ClientInfo ci = clientStates.get(uid);
			if (ci != null && ci.fullScreen == display)
				return false;
		}
		return true;
	}

	public boolean isSelectionFullScreen() {
		int size = selection.size();
		if (size == 0)
			return false;
		for (int i = 0; i < size; i++) {
			int uid = selection.get(i);
			ClientInfo ci = clientStates.get(uid);
			if (ci != null) {
				if (ci.fullScreen == -1)
					return false;
			}
		}
		return true;
	}

	protected Dimension resizeRects() {
		Map<Integer, Rectangle> map = new HashMap<>();

		Rectangle rect = getClientArea();
		if (rect.width == 0 || rect.height == 0)
			return null;

		int i = rect.x + BORDER;
		int j = rect.y + BORDER;
		Font font = getDisplay().getSystemFont();
		int fh = font.getFontData()[0].getHeight() + 2;
		int maxHeight = 0;
		int count = 0;
		Dimension max = new Dimension(0, 0);

		int numColumns = Math.max(1, rect.width / (IMG_DIM.width + GAP));

		for (Client c : clients) {
			if (filter.contains(c.type))
				continue;

			Rectangle rr = new Rectangle(i, j, IMG_DIM.width, IMG_DIM.height + fh + TEXT_GAP);
			maxHeight = Math.max(maxHeight, rr.height);
			map.put(c.uid, rr);
			max.width = Math.max(max.width, i + BORDER + rr.width);
			max.height = Math.max(max.height, j + BORDER + rr.height);

			i += rr.width;
			i += GAP;
			count++;
			if (count >= numColumns) {
				count = 0;
				i = rect.x + BORDER;
				j += maxHeight + GAP;
				maxHeight = 0;
			}
		}
		clientRects = map;

		return max;
	}

	protected void select(int x, int y, int statemask) {
		List<Integer> sel = selection;
		if ((statemask & SWT.CTRL) == 0)
			sel = new ArrayList<>();

		Client c = getClientAt(x, y);
		if (c != null) {
			int uid = c.uid;
			boolean selected = sel.contains(uid);
			if (selected)
				sel.remove((Integer) uid);
			else
				sel.add(uid);

			selection = sel;

			int ys = getVerticalBar().getSelection();
			fireEvent(x, y + ys);
			redraw();
		} else if ((statemask & SWT.CTRL) == 0) {
			selection = new ArrayList<>();
			fireEvent(0, 0);
			redraw();
		}
	}

	protected Client getClientAt(int x, int y) {
		int ys = getVerticalBar().getSelection();
		for (Client c : clients) {
			Rectangle r = clientRects.get(c.uid);
			if (r != null && r.contains(x, y + ys))
				return c;
		}
		return null;
	}

	protected void fireEvent(int x, int y) {
		if (listeners == null)
			return;

		Event ev = new Event();
		ev.x = x;
		ev.y = y;
		ev.widget = this;

		for (SelectionListener listener : listeners) {
			listener.widgetSelected(new SelectionEvent(ev));
		}
	}

	protected void selectAll() {
		List<Integer> sel = new ArrayList<>();

		for (Client c : clients) {
			if (clientRects.containsKey(c.uid))
				sel.add(c.uid);
		}

		selection = sel;
		fireEvent(0, 0);
		redraw();
	}

	protected void deselectAll() {
		selection = new ArrayList<>();
		fireEvent(0, 0);
		redraw();
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		Point min = new Point(IMG_DIM.width * 2 + BORDER * 2 + GAP, IMG_DIM.height + BORDER * 2);
		if (clients.length == 0)
			return min;
		Dimension d = resizeRects();
		if (d != null)
			return new Point(Math.max(d.width, min.x), Math.max(d.height, min.y));

		return min;
	}

	/*
	 * Process the paint event
	 */
	protected void paint(PaintEvent event) {
		Rectangle rect = getClientArea();
		if (rect.width == 0 || rect.height == 0)
			return;

		// long time = System.currentTimeMillis();

		GC gc = event.gc;
		gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		gc.fillRectangle(rect.x + 1, rect.y + 1, rect.width - 2, rect.height - 2);

		Font font = getDisplay().getSystemFont();
		int fh = font.getFontData()[0].getHeight() + 2;
		gc.setFont(font);

		resizeRects();

		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);
		gc.setTextAntialias(SWT.ON);

		Transform trans = new Transform(gc.getDevice());
		trans.translate(0, -yOrigin);
		gc.setTransform(trans);
		trans.dispose();

		synchronized (uiLock) {
			for (Client c : clients) {
				int uid = c.uid;
				Rectangle rr = clientRects.get(uid);

				if (rr == null) // client just connected or disconnected
					continue;

				// don't draw clients that aren't currently visible
				if (rr.y - yOrigin > rect.y + rect.height || rr.y + rr.height - yOrigin < rect.y)
					continue;

				if (selection.contains(uid)) {
					gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
					gc.fillRectangle(rr.x - SEL_MARGIN, rr.y - SEL_MARGIN, rr.width + SEL_MARGIN * 2 + 1,
							rr.height + SEL_MARGIN * 2 + 2);
				}

				ClientInfo ci = clientStates.get(uid);
				Image img = null;
				if (ci != null)
					img = ci.thumbnail;

				if (img != null) {
					Rectangle r = img.getBounds();
					Point p = new Point(rr.x + (IMG_DIM.width - r.width) / 2, rr.y + (IMG_DIM.height - r.height) / 2);
					gc.drawImage(img, p.x, p.y);
					gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
					gc.drawRectangle(p.x, p.y, r.width, r.height);
				} else {
					gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_GRAY));
					gc.fillRectangle(rr.x, rr.y, IMG_DIM.width, IMG_DIM.height);
					gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));

					String s = "No image available";
					Point p = gc.textExtent(s);
					gc.drawString(s, rr.x + (rr.width - p.x) / 2, rr.y + rr.height / 2 - fh, true);
				}

				if (selection.contains(uid))
					gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
				else
					gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));

				// name & current resolution/fps
				gc.drawString(c.name, rr.x, rr.y + rr.height - fh, true);
				if (c.contestIds != null && c.contestIds.length > 0)
					gc.drawString(c.contestIds[0], rr.x + (rr.width - gc.textExtent(c.contestIds[0]).x) / 2,
							rr.y + rr.height - fh, true);

				if (ci != null) {
					String ss = ci.width + "x" + ci.height + "@" + ci.fps + "fps";
					gc.drawString(ss, rr.x + rr.width - gc.textExtent(ss).x, rr.y + rr.height - fh, true);
				}
			}
		}

		// Trace.trace(Trace.USER, "Painted: " + (System.currentTimeMillis() - time));
	}

	protected void resize() {
		Dimension d = resizeRects();
		Rectangle client = getClientArea();

		ScrollBar vScroll = getVerticalBar();
		if (vScroll == null || d == null)
			return;

		vScroll.setMaximum(d.height);
		vScroll.setThumb(Math.min(d.height, client.height));
		int vPage = d.height - client.height;
		vScroll.setEnabled(vPage > 0);
		int vSelection = vScroll.getSelection();
		if (vSelection >= vPage) {
			if (vPage <= 0)
				vSelection = 0;
			yOrigin = vSelection;
		}
		redraw();
	}

	protected void setSmallThumbnails(boolean smallThumbs) {
		if (smallThumbs)
			IMG_DIM = SMALL_IMG_DIM;
		else
			IMG_DIM = DEFAULT_IMG_DIM;

		// throw out cached thumbnails
		synchronized (uiLock) {
			for (Client c : clients) {
				int uid = c.uid;

				ClientInfo ci = clientStates.get(uid);
				if (ci != null) {
					Image img = ci.thumbnail;
					if (img != null) {
						ci.thumbnail = null;
						img.dispose();
					}
				}
			}
		}
		resize();
	}

	protected String getSelectionDetail() {
		int size = selection.size();
		int total = clients.length;
		if (size != 1)
			return size + " / " + total + " selected";

		synchronized (uiLock) {
			for (Client c : clients) {
				int uid = c.uid;

				if (selection.contains(uid)) {
					ClientInfo ci = clientStates.get(uid);
					if (ci == null)
						return "(unavailable)";

					String ss = "no presentation";
					if (!"null".equals(ci.pres))
						ss = ci.pres;

					if (ci.hidden)
						ss += " (hidden)";

					ss += " / ";
					if (c.displays != null) {
						for (int i = 0; i < c.displays.length; i++) {
							if (i > 0)
								ss += ", ";
							ss += c.displays[i].width + "x" + c.displays[i].height + "@" + c.displays[i].refresh + "hz";
							if (ci.fullScreen == i)
								ss += "*";
						}
					}
					return ss;
				}
			}
		}

		return "";
	}
}