package org.icpc.tools.presentation.admin.internal;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.RESTContestSource;

public class Admin {
	public static void main(String[] args) {
		Trace.init("ICPC Presentation Admin", "presAdmin", args);

		if (args == null || args.length != 3) {
			System.out.println();
			System.out.println("Usage: presAdmin.bat/sh cdsURL user password");
			System.out.println();
			System.out.println("   cdsURL");
			System.out.println("      an HTTPS URL to a CDS");
			System.out.println("   user/password");
			System.out.println("      HTTPS authentication");
			System.out.println();
			return;
		}

		ContestSource source = null;
		try {
			if (args.length == 3)
				source = ContestSource.parseSource(args[0], args[1], args[2]);
			else
				source = ContestSource.parseSource(args[0]);
		} catch (IOException e) {
			Trace.trace(Trace.ERROR, "Could not parse source: " + e.getMessage());
			return;
		}

		if (!(source instanceof RESTContestSource)) {
			Trace.trace(Trace.ERROR, "Source argument must be a CDS");
			return;
		}

		RESTContestSource cdsSource = (RESTContestSource) source;
		if (cdsSource.getURL().getPath() != null && cdsSource.getURL().getPath().length() > 1) {
			Trace.trace(Trace.ERROR, "URL should not contain a path");
			return;
		}
		cdsSource.checkForUpdates("presentationAdmin-");

		View v = new View(cdsSource);

		Display.setAppName("Presentation Admin");
		final Display display = new Display();
		ImageResource.initializeImageRegistry(v.getClass(), display);
		PresentationHelper.setDisplay(display);

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

		shell.setText("Presentation Admin");
		shell.setImage(ImageResource.getImage(ImageResource.IMG_ICON));
		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent event) {
				int style = SWT.APPLICATION_MODAL | SWT.YES | SWT.NO;
				MessageBox messageBox = new MessageBox(shell, style);
				messageBox.setText(shell.getText());
				messageBox.setMessage("Are you sure you want to quit?");
				event.doit = messageBox.open() == SWT.YES;
			}
		});

		v.createPartControl(shell);

		shell.setSize(1000, 600);
		shell.open();

		v.connect();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		ImageResource.dispose();
		PresentationHelper.dispose();
	}

	private static void showAbout(Shell shell) {
		MessageBox dialog = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
		dialog.setText("About " + shell.getText());
		Package pack = Admin.class.getPackage();
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