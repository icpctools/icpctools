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

	/**
	 * Interface to set and get a stream-specific object, most commonly used to store header
	 * information.
	 */
	public interface IStore {
		void setObject(Object obj);

		Object getObject();
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
	protected void writeHeader(IStore store, IStreamListener listener) throws IOException {
		// default no-op
	}

	protected abstract void createReader(InputStream in, IStore store, IStreamListener listener) throws IOException;
}