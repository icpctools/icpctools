package org.icpc.tools.cds.video;

import java.io.IOException;
import java.io.InputStream;

public abstract class VideoHandler {
	public interface IStreamListener {
		void write(byte[] b);

		void write(byte[] b, int off, int len);

		void flush();

		boolean isDone();
	}

	protected abstract String getFileExtension();

	protected abstract String getMimeType();

	/**
	 * Return true if the stream is valid for this handler. Return false if it isn't.
	 *
	 * @param in
	 * @return
	 * @throws IOException
	 */
	protected abstract boolean validate(InputStream in) throws IOException;

	/**
	 * Write header/cached information for the given stream.
	 *
	 * @throws IOException
	 */
	protected void writeHeader(Object stream, IStreamListener listener) throws IOException {
		// ignore
	}

	protected void clearCache(Object stream) {
		// ignore
	}

	protected abstract void createReader(InputStream in, Object stream, IStreamListener listener) throws IOException;
}