package org.icpc.tools.contest.model.feed;

import java.io.PrintWriter;

import org.icpc.tools.contest.model.IContestObject;
import org.icpc.tools.contest.model.internal.ContestObject;

public class JSONArrayWriter extends JSONWriter {
	public JSONArrayWriter(PrintWriter pw) {
		super(pw);
	}

	@Override
	public String getContentType() {
		return IContentType.JSON;
	}

	@Override
	public void writePrelude() {
		pw.write("[");
	}

	public void write(IContestObject obj) {
		((ContestObject) obj).write(je);
	}

	@Override
	public void writeSeparator() {
		pw.write(",\n ");
	}

	@Override
	public void writePostlude() {
		pw.write("]");
	}
}