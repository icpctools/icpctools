package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

public class TeamVideoPresentation extends AbstractICPCPresentation {
	private MediaPlayerFactory mediaPlayerFactory;
	private EmbeddedMediaPlayer mediaPlayer;
	private BufferedImage snapshot;
	private int[] rgbBuffer;

	public TeamVideoPresentation() {
		// do nothing
	}

	@Override
	public void init() {
		mediaPlayerFactory = new MediaPlayerFactory();
		mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();

		BufferFormatCallbackAdapter callbackAdapter = new BufferFormatCallbackAdapter() {
			@Override
			public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
				snapshot = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_RGB);
				snapshot.setAccelerationPriority(1);
				rgbBuffer = new int[width * height];
				return new RV32BufferFormat(sourceWidth, sourceHeight);
			}
		};

		RenderCallback renderCallback = new RenderCallback() {
			@Override
			public void display(MediaPlayer player, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat,
					int displayWidth, int displayHeight) {
				IntBuffer ib = nativeBuffers[0].asIntBuffer();
				ib.get(rgbBuffer);

				snapshot.setRGB(0, 0, width, height, rgbBuffer, 0, width);
			}

			@Override
			public void lock(MediaPlayer player) {
				// do nothing
			}

			@Override
			public void unlock(MediaPlayer player) {
				// do nothing
			}
		};
		mediaPlayer.videoSurface()
				.set(mediaPlayerFactory.videoSurfaces().newVideoSurface(callbackAdapter, renderCallback, true));

		mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
			@Override
			public void finished(MediaPlayer player) {
				snapshot = null;
			}
		});
	}

	@Override
	public long getDelayTimeMs() {
		return 50;
	}

	private void play(String url) {
		mediaPlayer.media().play(url);
	}

	@Override
	public void aboutToShow() {
		play("http://localhost:8080");
	}

	@Override
	public void dispose() {
		mediaPlayer.release();
	}

	@Override
	public void paint(Graphics2D g) {
		if (snapshot != null)
			g.drawImage(snapshot, (width - snapshot.getWidth()) / 2, (height - snapshot.getHeight()) / 2, null);
	}

	@Override
	public void setProperty(String value) {
		try {
			URL url = new URL(value);
			play(url.toExternalForm());
		} catch (Exception e) {
			// ignore
		}
		if (value.startsWith("webcam:")) {
			try {
				play(getContest().getTeamById(value.substring(7)).getWebcamURL());
			} catch (Exception e) {
				// ignore
			}
		}
		if (value.startsWith("desktop:")) {
			try {
				play(getContest().getTeamById(value.substring(8)).getDesktopURL());
			} catch (Exception e) {
				// ignore
			}
		}
	}
}