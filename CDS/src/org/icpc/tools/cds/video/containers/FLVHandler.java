package org.icpc.tools.cds.video.containers;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.icpc.tools.cds.video.VideoHandler;

public class FLVHandler extends VideoHandler {
	private Map<Object, FLVReader> readers = new HashMap<>();

	@Override
	protected String getFileExtension() {
		return "flv";
	}

	@Override
	protected String getMimeType() {
		return "video/x-flv";
	}

	@Override
	protected boolean validate(InputStream in) throws IOException {
		// read the first 3 bytes
		int n = 0;
		byte[] b = new byte[3];
		while (n < b.length) {
			n += in.read(b, n, b.length - n);
			if (n == -1)
				throw new IOException("Invalid stream");
		}

		// confirm FLV header 70.76.86 = FLV
		if (b[0] != 70 || b[1] != 76 || b[2] != 86)
			return false;
		return true;
	}

	@Override
	protected void writeHeader(Object stream, IStreamListener listener) throws IOException {
		// TODO
		/*FLVWriter.writeHeader(new DataOutputStream(out));

		FLVReader r = readers.get(stream);
		if (r != null)
			r.sendCache(out);*/
	}

	@Override
	protected void clearCache(Object stream) {
		readers.remove(stream);
	}

	@Override
	protected void createReader(InputStream in, Object stream, IStreamListener listener) throws IOException {
		final DataInputStream din = new DataInputStream(in);
		FLVReader r = readers.get(stream);
		if (r == null) {
			r = new FLVReader();
			readers.put(stream, r);
		}

		r.read(din, listener);
	}
}