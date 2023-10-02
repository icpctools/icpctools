package org.icpc.tools.cds.video;

import java.io.IOException;
import java.io.InputStream;

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
}