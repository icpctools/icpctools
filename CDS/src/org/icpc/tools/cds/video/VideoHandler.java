package org.icpc.tools.cds.video;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class VideoHandler {
	public interface IStreamListener {
		public void write(byte[] b);

		public void write(byte[] b, int off, int len);

		public void flush();

		public boolean isDone();
	}

	protected abstract String getFormat();

	/**
	 * Write header/cached information for the given stream.
	 *
	 * @throws IOException
	 */
	protected void writeHeader(OutputStream out, Object stream) throws IOException {
		// ignore
	}

	protected void clearCache(Object stream) {
		// ignore
	}

	protected abstract void createReader(InputStream in, Object stream, IStreamListener listener) throws IOException;
}