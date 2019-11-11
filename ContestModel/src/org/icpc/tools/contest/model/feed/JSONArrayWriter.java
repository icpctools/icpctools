package org.icpc.tools.contest.model.feed;

import java.io.PrintWriter;

import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.internal.ContestObject;

public class JSONArrayWriter {
	protected PrintWriter pw;
	protected JSONEncoder je;

	public JSONArrayWriter(PrintWriter pw) {
		this.pw = pw;
		je = new JSONEncoder(pw);
	}

	public String getContentType() {
		return IContentType.JSON;
	}

	public void writePrelude() {
		pw.write("[");
	}

	public void write(IContestObject obj) {
		je.open();
		((ContestObject) obj).writeBody(je);
		je.close();
	}

	public void writeSeparator() {
		je.writeSeparator();
	}

	public void writePostlude() {
		pw.write("]");
	}
}