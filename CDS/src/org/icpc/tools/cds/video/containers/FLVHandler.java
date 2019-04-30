package org.icpc.tools.cds.video.containers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.icpc.tools.cds.video.VideoHandler;

public class FLVHandler extends VideoHandler {
	private Map<Object, FLVReader> readers = new HashMap<>();

	@Override
	protected String getFormat() {
		return "flv";
	}

	@Override
	protected void writeHeader(OutputStream out, Object stream) throws IOException {
		FLVWriter.writeHeader(new DataOutputStream(out));

		FLVReader r = readers.get(stream);
		if (r != null)
			r.sendCache(out);
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