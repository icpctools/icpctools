package org.icpc.tools.contest.model.feed;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BackupInputStream extends InputStream {
	private OutputStream out;
	private InputStream in;

	public BackupInputStream(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
	}

	@Override
	public int read() throws IOException {
		int b = in.read();
		if (b != -1)
			out.write(b);
		return b;
	}

	@Override
	public int read(byte b[]) throws IOException {
		int n = in.read(b);
		if (n > 0)
			out.write(b, 0, n);
		return n;
	}

	@Override
	public int read(byte b[], int off, int len) throws IOException {
		int n = in.read(b, off, len);
		if (n > 0)
			out.write(b, off, n);
		return n;
	}

	@Override
	public void close() throws IOException {
		in.close();
	}
}