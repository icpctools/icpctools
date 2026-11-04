package org.icpc.tools.cds.video;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.icpc.tools.contest.Trace;

public abstract class VideoStreamHandler extends VideoHandler {
	public interface IStreamListener {
		void write(byte[] b);

		void write(byte[] b, int off, int len);

		void flush();

		boolean isDone();
	}

	/**
	 * Write header/cached information for the given stream.
	 *
	 * @throws IOException
	 */
	protected void writeHeader(IStore store, IStreamListener listener) throws IOException {
		// default no-op
	}

	protected abstract void createReader(InputStream in, IStore store, IStreamListener listener) throws IOException;

	/**
	 * Records the stream to a single file by adding a listener to the stream.
	 */
	@Override
	protected IRecording startRecording(VideoStream stream, File dir, String baseName) throws IOException {
		File file = getRecordingFile(dir, baseName);
		VideoStreamListener listener = new VideoStreamListener(
				new BufferedOutputStream(new FileOutputStream(file)), true);
		try {
			stream.addListener(listener);
		} catch (IOException e) {
			// header couldn't be written, but the listener may still have been added
			Trace.trace(Trace.WARNING, "Could not write header info for recording " + file);
		}

		return new IRecording() {
			@Override
			public boolean isEmpty() {
				return !file.exists() || file.length() == 0;
			}

			@Override
			public void stop() {
				// removes and closes the listener (and file), or just close if already removed
				if (!stream.removeListener(listener))
					listener.close();
			}

			@Override
			public String toString() {
				return file.toString();
			}
		};
	}
}
