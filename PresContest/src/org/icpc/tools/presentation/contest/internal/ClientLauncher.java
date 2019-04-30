package org.icpc.tools.presentation.contest.internal;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.icpc.tools.client.core.IConnectionListener;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.presentation.contest.internal.standalone.TeamUtil;
import org.icpc.tools.presentation.core.IPresentationHandler.DeviceMode;
import org.icpc.tools.presentation.core.PresentationWindow;
import org.icpc.tools.presentation.core.internal.PresentationWindowImpl;

public class ClientLauncher {
	protected static PresentationClient instance;

	public static PresentationClient getInstance() {
		return instance;
	}

	public static RESTContestSource parseSource(String[] args) {
		ContestSource source = null;
		try {
			if (args.length == 4)
				source = ContestSource.parseSource(args[1], args[2], args[3]);
			else
				source = ContestSource.parseSource(args[1]);
		} catch (IOException e) {
			Trace.trace(Trace.ERROR, "Invalid contest source: " + e.getMessage());
			System.exit(1);
		}

		if (source instanceof RESTContestSource)
			return (RESTContestSource) source;

		Trace.trace(Trace.ERROR, "Source argument must be a CDS");
		System.exit(1);
		return null;
	}

	public static void showHelp() {
		System.out.println("Usage: client.bat/sh id cdsURL [user] [password]");
		System.out.println();
		System.out.println("   cdsURL");
		System.out.println("      an HTTP(S) URL to a CDS");
		System.out.println("   [user/password]");
		System.out.println("      HTTPS authentication");
	}

	public static void main(final String[] args) {
		Trace.init("ICPC Presentation Client", "client", args);
		System.setProperty("apple.awt.application.name", "Presentation Client");

		if (args == null || !(args.length == 2 || args.length == 4)) {
			showHelp();
			return;
		}

		RESTContestSource source = parseSource(args);
		source.outputValidation();

		source.checkForUpdates("presentations-");

		String id = args[0];
		PresentationClient client = null;
		int uid = -1;
		if ("team".equals(id)) {
			String teamLabel = TeamUtil.getTeamId();
			uid = Integer.parseInt(teamLabel);
			client = new PresentationClient(source, teamLabel, uid, "!admin");
		} else
			client = new PresentationClient(id, "!admin", source);
		instance = client;

		// open window
		createWindow(client, true);
		Trace.trace(Trace.INFO, client + " connecting to " + source);
		final PresentationClient client2 = client;
		String arg = args[0];
		String displayStr2 = null;
		if (arg != null && arg.startsWith("test"))
			displayStr2 = arg.substring(4);

		final String displayStr = displayStr2;
		client.addListener(new IConnectionListener() {
			@Override
			public void connectionStateChanged(boolean connected) {
				if (!connected)
					return;
				PresentationWindowImpl windowImpl = (PresentationWindowImpl) client2.window;
				if (client2.getUID() != -1)
					windowImpl.reduceThumbnails();
				if (connected && !windowImpl.isVisible()) {
					windowImpl.setWindow(new DeviceMode(displayStr));
					windowImpl.openIt();
				}
			}
		});
		client.connect(false);
	}

	protected static void createWindow(final PresentationClient client, final boolean sendthumbnails) {
		if (client.window != null)
			return;

		PresentationWindowImpl windowImpl = (PresentationWindowImpl) PresentationWindow.create();
		client.window = windowImpl;
		windowImpl.setThumbnailListener(new PresentationWindowImpl.IThumbnailListener() {
			@Override
			public void handleThumbnail(BufferedImage image) {
				if (sendthumbnails)
					client.writeThumbnail(image);
			}

			@Override
			public void handleInfo() {
				client.writeInfo();
			}
		});
	}
}