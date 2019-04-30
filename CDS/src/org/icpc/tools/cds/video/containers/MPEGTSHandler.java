package org.icpc.tools.cds.video.containers;

import java.io.IOException;
import java.io.InputStream;

import org.icpc.tools.cds.video.VideoHandler;

/**
 * MPEG-TS handler.
 *
 * For HD webcam, stream is averaging over 1850 packets/s.
 */
public class MPEGTSHandler extends VideoHandler {
	private static final int PACKET_LEN = 188;

	@Override
	protected String getFormat() {
		return "m2ts";
	}

	@Override
	protected void createReader(InputStream in, Object stream, IStreamListener listener) throws IOException {
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