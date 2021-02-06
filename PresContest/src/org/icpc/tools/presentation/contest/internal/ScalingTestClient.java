package org.icpc.tools.presentation.contest.internal;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.icpc.tools.client.core.IConnectionListener;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.ContestSource;
import org.icpc.tools.contest.model.feed.RESTContestSource;
import org.icpc.tools.presentation.core.PresentationWindow;
import org.icpc.tools.presentation.core.internal.PresentationWindowImpl;

public class ScalingTestClient {
	protected static final int NUM_CLIENTS = 100;
	protected static final boolean SEND_THUMBNAILS = true;

	protected static PresentationClient[] clients;

	public static void main(final String[] args) throws Exception {
		Trace.init("ICPC Client Scaling Test", "scalingTest", args);
		System.setProperty("apple.awt.application.name", "Scaling Test Client");

		RESTContestSource source = (RESTContestSource) ContestSource.parseSource(args[0], args[1], args[2]);

		int START = 0;

		clients = new PresentationClient[NUM_CLIENTS + 1];
		for (int teamId = 1; teamId <= NUM_CLIENTS; teamId++) {
			PresentationClient client = new PresentationClient(source, "team" + (teamId + START), teamId + START, null);

			clients[teamId] = client;
			createWindow(client, SEND_THUMBNAILS && teamId == 1);

			final PresentationClient client2 = client;
			final boolean[] once = new boolean[1];
			final int teamId2 = teamId;
			client.addListener(new IConnectionListener() {
				@Override
				public void connectionStateChanged(boolean connected) {
					PresentationWindowImpl windowImpl = (PresentationWindowImpl) client2.window;
					windowImpl.reduceThumbnails();
					if (connected && !once[0] && teamId2 == 1) {
						windowImpl.openInBackground();
						once[0] = true;
					}
				}
			});
			client.connect();
		}
	}

	protected static void sendThumbs(BufferedImage image) {
		for (PresentationClient client : clients) {
			if (client != null)
				client.writeInfoUpdate(image);
		}
	}

	protected static void createWindow(final PresentationClient client, final boolean sendthumbnails) {
		if (client.window != null)
			return;

		PresentationWindowImpl windowImpl = (PresentationWindowImpl) PresentationWindow.create();
		client.window = windowImpl;
		for (PresentationClient cl : clients) {
			if (cl != null)
				cl.writeInfo();
		}

		windowImpl.setThumbnailListener(new PresentationWindowImpl.IThumbnailListener() {
			@Override
			public void handleThumbnail(BufferedImage image) {
				if (sendthumbnails)
					sendThumbs(image);
			}
		});

		Rectangle r = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		windowImpl.setBounds(r.x, r.y, r.width / 2, r.height / 2);
	}
}