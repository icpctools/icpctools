package org.icpc.tools.balloon;

import java.io.DataOutput;
import java.io.IOException;
import java.util.StringTokenizer;

public class Balloon {
	private static int ID = 1;
	private static final String DELIM = ",";
	public static final int FIRST_IN_CONTEST = 1;
	public static final int FIRST_IN_GROUP = 2;
	public static final int FIRST_FOR_PROBLEM = 4;
	public static final int FIRST_FOR_TEAM = 8;

	private int id;
	private String submissionId;
	private int flags = -1;
	private boolean printed;
	private boolean delivered;

	public Balloon(String submissionId) {
		id = ID++;
		this.submissionId = submissionId;
	}

	public Balloon(String s, String x) throws NumberFormatException {
		load(s);
	}

	public int getId() {
		return id;
	}

	public String getSubmissionId() {
		return submissionId;
	}

	public boolean isPrinted() {
		return printed;
	}

	public boolean isDelivered() {
		return delivered;
	}

	public String getStatus() {
		if (flags < 0)
			return "Pending";

		if (flags == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		if ((flags & FIRST_IN_CONTEST) != 0)
			sb.append("C");
		if ((flags & FIRST_IN_GROUP) != 0)
			sb.append("R");
		if ((flags & FIRST_FOR_PROBLEM) != 0)
			sb.append("P");
		if ((flags & FIRST_FOR_TEAM) != 0)
			sb.append("T");
		return sb.toString();
	}

	protected int getFlags() {
		return flags;
	}

	public void setPrinted(boolean printed) {
		this.printed = printed;
	}

	public void setDelivered(boolean delivered) {
		this.delivered = delivered;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public boolean isFirstInContest() {
		return flags > 0 && (flags & FIRST_IN_CONTEST) != 0;
	}

	public boolean isFirstInGroup() {
		return flags > 0 && (flags & FIRST_IN_GROUP) != 0;
	}

	public boolean isFirstForTeam() {
		return flags > 0 && (flags & FIRST_FOR_TEAM) != 0;
	}

	public boolean isFirstForProblem() {
		return flags > 0 && (flags & FIRST_FOR_PROBLEM) != 0;
	}

	public void load(String s) throws NumberFormatException {
		StringTokenizer st = new StringTokenizer(s, DELIM);
		id = new Integer(st.nextToken()).intValue();
		submissionId = st.nextToken();
		flags = new Integer(st.nextToken()).intValue();
		printed = new Boolean(st.nextToken()).booleanValue();
		delivered = new Boolean(st.nextToken()).booleanValue();
	}

	public String save() {
		StringBuilder sb = new StringBuilder();
		sb.append(id);
		sb.append(DELIM);
		sb.append(submissionId);
		sb.append(DELIM);
		sb.append(flags);
		sb.append(DELIM);
		sb.append(printed);
		sb.append(DELIM);
		sb.append(delivered);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return submissionId.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Balloon))
			return false;

		Balloon b = (Balloon) o;
		return b.submissionId.equals(submissionId);
	}

	public void save(DataOutput out) throws IOException {
		out.writeInt(id);
		out.writeUTF(submissionId);
		out.writeInt(flags);
		out.writeBoolean(printed);
		out.writeBoolean(delivered);
	}

	@Override
	public String toString() {
		return "Balloon [" + id + "-" + submissionId + "]";
	}
}