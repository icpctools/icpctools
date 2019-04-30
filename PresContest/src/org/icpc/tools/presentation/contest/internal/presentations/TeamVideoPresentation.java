package org.icpc.tools.presentation.contest.internal.presentations;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import org.icpc.tools.presentation.contest.internal.AbstractICPCPresentation;

import com.sun.jna.Memory;

import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallback;

public class TeamVideoPresentation extends AbstractICPCPresentation {
	protected static final String[] DEFAULT_FACTORY_ARGUMENTS = { "--ignore-config", "--video-title=vlcj video output",
			"--no-plugins-cache", "--no-video-title-show", "--no-snapshot-preview", "--quiet", "--quiet-synchro",
			"--sub-filter=logo:marq", "--intf=dummy" };

	private MediaPlayerFactory mediaPlayerFactory;
	private DirectMediaPlayer mediaPlayer;
	private BufferedImage snapshot;

	public TeamVideoPresentation() {
		// do nothing
	}

	@Override
	public void init() {
		// String vlcHome = "C:\\Program Files (x86)\\VideoLAN\\VLC";
		// String vlcHome = "/Applications/VLC.app/Contents/MacOS/lib/libvlc.5.dylib";
		// NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), vlcHome);
		// Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);

		mediaPlayerFactory = new MediaPlayerFactory(DEFAULT_FACTORY_ARGUMENTS);
		mediaPlayer = mediaPlayerFactory.newDirectMediaPlayer(new BufferFormatCallback() {
			@Override
			public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
				snapshot = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_RGB);
				return new BufferFormat("RV32", sourceWidth, sourceHeight, new int[] { sourceWidth * 4 },
						new int[] { sourceHeight });
			}
		}, new RenderCallback() {
			@Override
			public void display(DirectMediaPlayer player, Memory[] nativeBuffers, BufferFormat bufferFormat) {
				Memory currentBuffer = nativeBuffers[0];
				int pixels = (bufferFormat.getHeight() * bufferFormat.getWidth());

				currentBuffer.getByteBuffer(0L, currentBuffer.size()).asIntBuffer()
						.get(((DataBufferInt) snapshot.getRaster().getDataBuffer()).getData(), 0, pixels);
			}
		});
	}

	@Override
	public long getDelayTimeMs() {
		return 50;
	}

	@Override
	public void aboutToShow() {
		mediaPlayer.prepareMedia("http://localhost:8080");
		mediaPlayer.play();
		mediaPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
			@Override
			public void finished(MediaPlayer player) {
				snapshot = null;
			}
		});
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
}