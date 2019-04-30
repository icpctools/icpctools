package org.icpc.tools.contest.model.feed;

import java.io.PrintWriter;

import org.icpc.tools.contest.model.IContestObject;

public abstract class ModelWriter {
	protected PrintWriter pw;

	public ModelWriter(PrintWriter pw) {
		this.pw = pw;
	}

	public String getContentType() {
		return IContentType.TEXT;
	}

	public void writePrelude() {
		// do nothing
	}

	public abstract void write(IContestObject e);

	public void writeSeparator() {
		// do nothing
	}

	public void writePostlude() {
		// do nothing
	}

	public void flush() {
		pw.flush();
	}

	public boolean checkError() {
		return pw.checkError();
	}
}