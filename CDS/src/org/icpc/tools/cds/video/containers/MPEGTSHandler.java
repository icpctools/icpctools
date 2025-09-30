package org.icpc.tools.cds.video.containers;

import java.io.IOException;
import java.io.InputStream;

import org.icpc.tools.cds.video.VideoStreamHandler;

/**
 * MPEG-TS handler.
 *
 * For HD webcam, stream is averaging over 1850 packets/s.
 */
public class MPEGTSHandler extends VideoStreamHandler {
	private static final int PACKET_LEN = 188;

	@Override
	protected String getName() {
		return "MPEG";
	}

	@Override
	protected String getFileExtension() {
		return "ts";
	}

	@Override
	protected String getMimeType() {
		return "video/mp2t";
	}

	@Override
	protected boolean validate(InputStream in) throws IOException {
		// read the first byte
		int b = in.read();
		if (b == -1)
			throw new IOException("Invalid stream");

		// confirm mpegg-ts packet 71 = G
		if (b != 71)
			return false;
		return true;
	}

	@Override
	protected void createReader(InputStream in, IStore stream, IStreamListener listener) throws IOException {
		byte[] b = new byte[PACKET_LEN * 500]; // a little over 90K
		int offset = 0;
		int len = 0;

		while (!listener.isDone()) {
			int n = in.read(b, offset + len, b.length - offset - len);
			if (n == -1)
				return;

			len += n;

			int numPackets = len / PACKET_LEN;
			if (numPackets > 0) {
				int sendBytes = PACKET_LEN * numPackets;
				listener.write(b, offset, sendBytes);

				offset += sendBytes;
				len -= sendBytes;

				listener.flush();
			}

			if (len == 0)
				offset = 0;

			if (offset > b.length / 3) {
				// start is getting far enough into the array, move remaining bytes back to the
				// beginning
				System.arraycopy(b, offset, b, 0, len);
				offset = 0;
			}
		}
	}
}