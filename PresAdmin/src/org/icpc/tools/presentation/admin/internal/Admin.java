package org.icpc.tools.presentation.admin.internal;

import java.awt.Taskbar;
import java.awt.Taskbar.Feature;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

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
import org.icpc.tools.contest.model.feed.CDSUtil;
import org.icpc.tools.contest.model.util.ArgumentParser;
import org.icpc.tools.contest.model.util.ArgumentParser.OptionParser;
import org.icpc.tools.contest.model.util.ArgumentParser.Source;

public class Admin {
	protected static void showHelp() {
		System.out.println();
		System.out.println("Usage: presAdmin.bat/sh cdsURL user password [options]");
		System.out.println();
		System.out.println("  Options:");
		System.out.println("     --help");
		System.out.println("         Shows this message");
		System.out.println("     --version");
		System.out.println("         Displays version information");
	}

	public static void main(String[] args) {
		Trace.init("ICPC Presentation Admin", "presAdmin", args);

		Source source = ArgumentParser.parseSource(args, new OptionParser() {
			@Override
			public boolean setOption(String option, List<Object> options) throws IllegalArgumentException {
				return false;
			}

			@Override
			public void showHelp() {
				Admin.showHelp();
			}
		});

		if (source == null) {
			showHelp();
			return;
		}

		String url = source.src[0];

		try {
			URL url2 = new URI(url).toURL();
			if (url2.getPath() != null && url2.getPath().length() > 1) {
				url = url.substring(0, url.length() - url2.getPath().length());
				Trace.trace(Trace.WARNING, "CDS URL should not contain a path, trying " + url + " instead");
			}
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Could not parse URL");
			return;
		}

		CDSUtil util = new CDSUtil(url, source.user, source.password);
		try {
			util.verifyCDS();
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "CDS connection failure: " + e.getMessage());
			return;
		}

		util.checkForUpdates("presentationAdmin-");

		View v = new View(url, source.user, source.password);

		Display.setAppName("Presentation Admin");
		if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Feature.ICON_IMAGE)) {
			try {
				BufferedImage image = ImageIO.read(Admin.class.getResourceAsStream("/images/adminIcon.png"));
				Taskbar.getTaskbar().setIconImage(image);
			} catch (Exception e) {
				Trace.trace(Trace.INFO, "Couldn't set taskbar icon", e);
			}
		}

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
				int style = SWT.APPLICATION_MODAL | SWT.ICON_WARNING | SWT.YES | SWT.NO;
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