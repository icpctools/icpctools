package org.icpc.tools.cds.video.containers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.cds.video.VideoStreamHandler;

/**
 * Ogg container handler.
 */
public class OggHandler extends VideoStreamHandler {
	@Override
	protected String getName() {
		return "OGG";
	}

	@Override
	protected String getFileExtension() {
		return "ogg";
	}

	@Override
	protected String getMimeType() {
		return "video/ogg";
	}

	@Override
	protected void writeHeader(IStore store, IStreamListener listener) throws IOException {
		@SuppressWarnings("unchecked")
		List<byte[]> hdr = (List<byte[]>) store.getObject();
		if (hdr == null)
			return;

		for (byte[] b : hdr)
			listener.write(b);
	}

	@Override
	protected boolean validate(InputStream in) throws IOException {
		// read the first 4 bytes
		int n = 0;
		byte[] b = new byte[4];
		while (n < b.length) {
			n += in.read(b, n, b.length - n);
			if (n == -1)
				throw new IOException("Invalid stream");
		}

		// confirm ogg packet 79.103.103.83 = OggS
		if (b[0] != 79 || b[1] != 103 || b[2] != 103 || b[3] != 83)
			return false;
		return true;
	}

	@Override
	protected void createReader(InputStream in, IStore store, IStreamListener listener) throws IOException {
		byte[] b = new byte[27];

		while (!listener.isDone()) {
			// read the first 27 bytes
			int n = 0;
			while (n < b.length) {
				n += in.read(b, n, b.length - n);
				if (n == -1)
					return;
			}

			// confirm ogg packet 79.103.103.83 = OggS
			if (b[0] != 79 || b[1] != 103 || b[2] != 103 || b[3] != 83)
				throw new IOException("Failed checksum");

			// byte 26 is the # of segments. read segment sizes next
			int numSegments = Byte.toUnsignedInt(b[26]);
			byte[] bb = new byte[numSegments];
			n = 0;
			while (n < numSegments)
				n += in.read(bb, n, bb.length - n);

			// total segments
			int segmentLength = 0;
			for (int i = 0; i < bb.length; i++)
				segmentLength += Byte.toUnsignedInt(bb[i]);

			// read all segments
			byte[] d = new byte[segmentLength];
			n = 0;
			while (n < segmentLength)
				n += in.read(d, n, d.length - n);

			// output page
			listener.write(b);
			listener.write(bb);
			listener.write(d);

			// if granule position (bytes 6 - 13) is all 0s, this is a header packet
			boolean isHeader = true;
			for (int i = 6; i < 14; i++) {
				if (Byte.toUnsignedInt(b[i]) != 0)
					isHeader = false;
			}
			if (isHeader) {
				// save headers for next client
				byte[] hdr2 = new byte[b.length + bb.length + d.length];
				System.arraycopy(b, 0, hdr2, 0, b.length);
				System.arraycopy(bb, 0, hdr2, b.length, bb.length);
				System.arraycopy(d, 0, hdr2, b.length + bb.length, d.length);

				@SuppressWarnings("unchecked")
				List<byte[]> hdr = (List<byte[]>) store.getObject();
				if (hdr == null) {
					hdr = new ArrayList<byte[]>(8);
					store.setObject(hdr);
				}
				hdr.add(hdr2);
			}
		}
	}
}