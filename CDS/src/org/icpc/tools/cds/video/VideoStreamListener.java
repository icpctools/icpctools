package org.icpc.tools.cds.video;

import java.io.IOException;
import java.io.OutputStream;

public class VideoStreamListener {
	private OutputStream out;
	private long startTime;
	private boolean analyst;
	private boolean done;

	public VideoStreamListener(OutputStream out, boolean analyst) {
		this.out = out;
		this.analyst = analyst;
		startTime = System.currentTimeMillis();
	}

	public long getStartTime() {
		return startTime;
	}

	public boolean isDone() {
		return done;
	}

	public boolean isAnalyst() {
		return analyst;
	}

	public void write(byte[] b) throws IOException {
		if (done)
			return;

		out.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		if (done)
			return;

		out.write(b, off, len);
	}

	public void flush() throws IOException {
		if (done)
			return;

		out.flush();
	}

	/**
	 * Close the stream.
	 *
	 * @return
	 */
	public void close() {
		if (done)
			return;

		try {
			out.close();
		} catch (Exception e) {
			// ignore
		}
		done = true;
	}
}