package org.icpc.tools.cds.video;

import java.io.File;
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

	/**
	 * An in-progress recording of a stream to disk.
	 */
	public interface IRecording {
		/**
		 * Returns true if no video has been recorded.
		 */
		boolean isEmpty();

		/**
		 * Stop recording and finalize all files.
		 */
		void stop();
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

	/**
	 * Returns the primary file of a recording, i.e. the file that should be referenced and served.
	 *
	 * @param dir the folder to record into
	 * @param baseName the base name for the recording, without any extension
	 * @return the primary recording file
	 */
	protected File getRecordingFile(File dir, String baseName) {
		return new File(dir, baseName + "." + getFileExtension());
	}

	/**
	 * Start recording the given stream to disk.
	 *
	 * @param stream the stream to record
	 * @param dir the folder to record into
	 * @param baseName the base name for the recording, without any extension, e.g. "reaction.webcam"
	 * @return the recording
	 * @throws IOException if the recording could not be started
	 */
	protected abstract IRecording startRecording(VideoStream stream, File dir, String baseName) throws IOException;
}