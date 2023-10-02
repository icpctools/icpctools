package org.icpc.tools.cds.video.containers;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.icpc.tools.cds.video.VideoStreamHandler;

public class FLVHandler extends VideoStreamHandler {
	@Override
	protected String getName() {
		return "FLV";
	}

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
	protected void writeHeader(IStore store, IStreamListener listener) throws IOException {
		// TODO
		/*FLVWriter.writeHeader(new DataOutputStream(out));
		
		FLVReader r = readers.get(stream);
		if (r != null)
			r.sendCache(out);*/
	}

	@Override
	protected void createReader(InputStream in, IStore store, IStreamListener listener) throws IOException {
		final DataInputStream din = new DataInputStream(in);
		FLVReader r = (FLVReader) store.getObject();
		if (r == null) {
			r = new FLVReader();
			store.setObject(r);
		}

		r.read(din, listener);
	}
}