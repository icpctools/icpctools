package org.icpc.tools.contest.model;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.contest.model.feed.JSONParser.JsonObject;
import org.icpc.tools.contest.model.feed.RelativeTime;
import org.icpc.tools.contest.model.feed.Timestamp;

public class RemovedInterval {
	private static final String START = "start";
	private static final String END = "end";
	private static final String CONTEST_TIME = "contest_time";

	private Long start;
	private Long end;
	private Long contestTime;

	public RemovedInterval(JsonObject obj) throws ParseException {
		start = Timestamp.parse(obj.getString(START));
		end = Timestamp.parse(obj.getString(END));
		contestTime = RelativeTime.parse(obj.getString(CONTEST_TIME));
	}

	public RemovedInterval(Object value) throws ParseException {
		this(JSONParser.getOrReadObject(value));
	}

	protected static Long parseRelativeTime(Object value) throws ParseException {
		if (value == null || "null".equals(value))
			return null;
		return RelativeTime.parse((String) value);
	}

	/**
	 * Returns the start time.
	 *
	 * @return the start
	 */
	public Long getStart() {
		return start;
	}

	/**
	 * Returns the end time.
	 *
	 * @return the end
	 */
	public Long getEnd() {
		return end;
	}

	/**
	 * Returns the contest time.
	 *
	 * @return the contest time
	 */
	public Long getContestTime() {
		return contestTime;
	}

	public String getJSON() {
		StringBuilder sb = new StringBuilder("{");
		sb.append("\"" + START + "\":\"" + Timestamp.format(start) + "\",");
		sb.append("\"" + CONTEST_TIME + "\":\"" + RelativeTime.format(contestTime) + "\"");
		if (end != null)
			sb.append(",\"" + END + "\":\"" + Timestamp.format(end) + "\"");
		sb.append("}");
		return sb.toString();
	}

	public List<String> validate(IContest c) {
		List<String> errors = new ArrayList<String>();

		if (start <= 0)
			errors.add("Invalid start time");

		if (start <= 0)
			errors.add("Invalid contest time");

		if (end < 0)
			errors.add("Invalid end time");

		if (errors.isEmpty())
			return null;
		return errors;
	}
}