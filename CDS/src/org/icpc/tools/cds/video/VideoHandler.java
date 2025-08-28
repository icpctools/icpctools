package org.icpc.tools.cds.video;

import java.io.IOException;
import java.io.InputStream;

public abstract class VideoHandler {
	/**
	 * Interface to set and get a stream-specific object, most commonly used to store header
	 * information.
	 */
	public interface IStore {
		void setObject(Object obj);

		Object getObject();
	}

	protected abstract String getName();

	protected abstract String getFileExtension();

	protected String getFileName() {
		return null;
	}

	protected abstract String getMimeType();

	/**
	 * Return true if the stream is valid for this handler. Return false if it isn't.
	 *
	 * @param in
	 * @return
	 * @throws IOException
	 */
	protected abstract boolean validate(InputStream in) throws IOException;
}